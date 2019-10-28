/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.connecteddevice

import android.app.ActivityManager
import android.content.Context
import com.android.car.connecteddevice.ble.BleCentralManager
import com.android.car.connecteddevice.ble.BlePeripheralManager
import com.android.car.connecteddevice.ble.CarBleCentralManager
import com.android.car.connecteddevice.ble.CarBleManager
import com.android.car.connecteddevice.ble.CarBlePeripheralManager
import com.android.car.connecteddevice.ble.DeviceMessage
import com.android.car.connecteddevice.model.ConnectedDevice
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage
import com.android.car.connecteddevice.util.ThreadSafeCallbacks
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.loge
import com.android.car.connecteddevice.util.logw
import com.android.internal.annotations.GuardedBy
import com.android.internal.annotations.VisibleForTesting
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "ConnectedDeviceManager"

/** Manager of devices connected to the car. */
class ConnectedDeviceManager internal constructor(
    private val storage: CarCompanionDeviceStorage,
    private val centralManager: CarBleCentralManager,
    private val peripheralManager: CarBlePeripheralManager
) {
    internal constructor(
        context: Context,
        storage: CarCompanionDeviceStorage,
        bleCentralManager: BleCentralManager,
        blePeripheralManager: BlePeripheralManager,
        serviceUuid: UUID,
        bgMask: String,
        writeCharacteristicUuid: UUID,
        readCharacteristicUuid: UUID
    ) : this(
        storage,
        CarBleCentralManager(context, bleCentralManager, storage, serviceUuid, bgMask,
            writeCharacteristicUuid, readCharacteristicUuid),
        CarBlePeripheralManager(blePeripheralManager, storage, writeCharacteristicUuid,
            readCharacteristicUuid)
    )

    constructor(context: Context) : this(
        context,
        CarCompanionDeviceStorage(context),
        BleCentralManager(context),
        BlePeripheralManager(context),
        UUID.fromString(context.getString(R.string.car_service_uuid)),
        context.getString(R.string.car_bg_mask),
        UUID.fromString(context.getString(R.string.car_secure_write_uuid)),
        UUID.fromString(context.getString(R.string.car_secure_read_uuid))
    )

    private val activeUserConnectionCallbacks = ThreadSafeCallbacks<ConnectionCallback>()

    private val allUserConnectionCallbacks = ThreadSafeCallbacks<ConnectionCallback>()

    // deviceId -> (recipientId -> callbacks)
    private val deviceCallbacks =
        ConcurrentHashMap<String, MutableMap<UUID, ThreadSafeCallbacks<DeviceCallback>>>()

    // deviceId -> device
    private val connectedDevices = ConcurrentHashMap<String, InternalConnectedDevice>()

    // Recipient ids that received multiple callback registrations indicate that the recipient id
    // has been compromised. Another party now has access the messages intended for that recipient.
    // As a safeguard, that recipient id will be added to this list and blocked from further
    // callback notifications.
    private val blacklistedRecipients = CopyOnWriteArraySet<UUID>()

    private val callbackExecutor = Executors.newSingleThreadExecutor()

    private val lock = ReentrantLock()
    @GuardedBy("lock")
    @Volatile
    private var isConnectingToUserDevice = false

    init {
        centralManager.registerCallback(generateCarBleCallback(centralManager), callbackExecutor)
        peripheralManager.registerCallback(generateCarBleCallback(peripheralManager),
            callbackExecutor)
    }

    /** Returns [List<ConnectedDevice>] of devices currently connected. */
    fun getActiveUserConnectedDevices(): List<ConnectedDevice> {
        return connectedDevices.values.map { it.connectedDevice }.toList()
    }

    /**
     * Register a [callback] for manager triggered connection events for only the
     * currently active user's devices.
     */
    fun registerActiveUserConnectionCallback(callback: ConnectionCallback, executor: Executor) {
        activeUserConnectionCallbacks.add(callback, executor)
    }

    /** Unregister a connection [callback] from manager. */
    fun unregisterConnectionCallback(callback: ConnectionCallback) {
        activeUserConnectionCallbacks.remove(callback)
        allUserConnectionCallbacks.remove(callback)
    }

    /** Connect to a device for the active user if available. */
    fun connectToActiveUserDevice() {
        lock.withLock {
            if (isConnectingToUserDevice) {
                // Already connecting, no further action needed.
                return
            }
            val userDeviceId = getActiveUserDeviceIds().firstOrNull()
            if (userDeviceId == null) {
                logw(TAG, "No devices associated with active user. Ignoring.")
            }
            if (connectedDevices.contains(userDeviceId)) {
                // Device has already connected, no further action needed.
                return
            }

            isConnectingToUserDevice = true
            peripheralManager.connectToDevice(UUID.fromString(userDeviceId))
        }
    }

    /**
     * Register [callback] for specific [device] and [recipientId] device events to be triggered
     * on [executor].
     */
    fun registerDeviceCallback(
        device: ConnectedDevice,
        recipientId: UUID,
        callback: DeviceCallback,
        executor: Executor
    ) {
        if (isRecipientBlacklisted(recipientId)) {
            loge(
                TAG,
                "Multiple callbacks registered for recipient $recipientId! Your recipient " +
                    "id is no longer secure and has been blacklisted by the process."
            )
            callback.onDeviceError(device, DeviceError.INSECURE_RECIPIENT_ID_DETECTED)
            return
        }
        logd(
            TAG,
            "New callback registered on device ${device.deviceId} for recipient " +
                "$recipientId."
        )
        val deviceId = device.deviceId
        val recipientCallbacks = deviceCallbacks.getOrPut(deviceId, { mutableMapOf() })

        // Device already has a callback registered with this recipient UUID. For the
        // protection of the user, this UUID is now blacklisted from future subscriptions
        // and the original subscription is removed.
        if (recipientCallbacks.containsKey(recipientId)) {
            blacklistRecipient(deviceId, recipientId)
            return
        }

        recipientCallbacks[recipientId] = ThreadSafeCallbacks<DeviceCallback>().apply {
            add(callback, executor)
        }
    }

    /** Unregister device [callback] for specific [device] and [recipientId]. */
    fun unregisterDeviceCallback(
        device: ConnectedDevice,
        recipientId: UUID,
        callback: DeviceCallback
    ) {
        logd(
            TAG,
            "Device callback unregistered on device ${device.deviceId} for recipient " +
                "$recipientId."
        )
        val recipientCallbacks = deviceCallbacks[device.deviceId]
        // Validate callback is the one registered for recipient before removing.
        val shouldDelete = recipientCallbacks?.get(recipientId)?.let {
            it.remove(callback)
            it.size == 0
        } ?: false

        if (shouldDelete) {
            deviceCallbacks[device.deviceId]?.remove(recipientId)
        }
    }

    /**
     * Send encrypted [message] to [recipientId] on [device]. Throws
     * [IllegalStateException] if secure channel has not been established.
     */
    @Throws(IllegalStateException::class)
    fun sendMessageSecurely(
        device: ConnectedDevice,
        recipientId: UUID,
        message: ByteArray
    ) {
        sendMessage(device, recipientId, message, isEncrypted = true)
    }

    /** Send unencrypted [message] to [recipientId] on [device]. */
    fun sendMessageUnsecurely(
        device: ConnectedDevice,
        recipientId: UUID,
        message: ByteArray
    ) {
        sendMessage(device, recipientId, message, isEncrypted = false)
    }

    /** Start internal processes and begin discovering devices. Must be called before any
     * connections can be made using [connectToActiveUserDevice]. */
    fun start() {
        logd(TAG, "Starting ConnectedDeviceManager.")
        centralManager.start()
        peripheralManager.start()
    }

    /** Clean up internal processes and disconnect any active connections. */
    fun cleanup() {
        logd(TAG, "Cleaning up ConnectedDeviceManager.")
        centralManager.stop()
        peripheralManager.stop()
        deviceCallbacks.clear()
        activeUserConnectionCallbacks.clear()
        allUserConnectionCallbacks.clear()
    }

    private fun sendMessage(
        device: ConnectedDevice,
        recipientId: UUID,
        message: ByteArray,
        isEncrypted: Boolean
    ) {
        val deviceId = device.deviceId
        logd(
            TAG,
            "Sending new message to device $deviceId for $recipientId containing " +
                "${message.size}. Message will be sent securely: [$isEncrypted]."
        )

        val connectedDevice = connectedDevices[deviceId]
        if (connectedDevice == null) {
            loge(TAG, "Attempted to send message to unknown device $deviceId")
            return
        }

        if (isEncrypted) {
            check(connectedDevice.connectedDevice.hasSecureChannel) {
                "Cannot send a message securely to device that has not established a secure " +
                    "channel."
            }
        }
        connectedDevice.carBleManager.sendMessage(
            deviceId,
            DeviceMessage(recipientId, isEncrypted, message)
        )
    }

    private fun isRecipientBlacklisted(recipientId: UUID) =
        blacklistedRecipients.contains(recipientId)

    private fun blacklistRecipient(deviceId: String, recipientId: UUID) {
        val recipientCallbacks = deviceCallbacks[deviceId]
        val connectedDevice = connectedDevices[deviceId]?.connectedDevice
        if (connectedDevice != null) {
            recipientCallbacks?.get(recipientId)?.invoke {
                it.onDeviceError(connectedDevice, DeviceError.INSECURE_RECIPIENT_ID_DETECTED)
            }
        }
        recipientCallbacks?.remove(recipientId)
        blacklistedRecipients.add(recipientId)
    }

    @VisibleForTesting
    internal fun addConnectedDevice(deviceId: String, bleManager: CarBleManager) {
        if (connectedDevices.containsKey(deviceId)) {
            // Device already connected. No-op until secure channel established.
            return
        }
        logd(TAG, "New device with id $deviceId connected.")
        val connectedDevice = ConnectedDevice(
            deviceId,
            deviceName = null,
            belongsToActiveUser = getActiveUserDeviceIds().contains(deviceId),
            hasSecureChannel = false
        )
        connectedDevices[deviceId] = InternalConnectedDevice(connectedDevice, bleManager)
        invokeConnectionCallbacks(connectedDevice.belongsToActiveUser) {
            it.onDeviceConnected(connectedDevice)
        }
    }

    @VisibleForTesting
    internal fun removeConnectedDevice(deviceId: String, bleManager: CarBleManager) {
        val connectedDevice = connectedDeviceForManagerOrNull(deviceId, bleManager)
        if (connectedDevice != null) {
            connectedDevices.remove(deviceId)
            invokeConnectionCallbacks(connectedDevice.connectedDevice.belongsToActiveUser) {
                it.onDeviceDisconnected(connectedDevice.connectedDevice)
            }
        }
        // If disconnect happened on peripheral, open for future requests to connect.
        if (bleManager == peripheralManager) {
            lock.withLock { isConnectingToUserDevice = false }
        }
    }

    @VisibleForTesting
    internal fun secureChannelEstablished(deviceId: String, bleManager: CarBleManager) {
        var notifyCallbacks = false
        val connectedDevice = connectedDevices[deviceId]?.connectedDevice
        if (connectedDevice == null) {
            loge(TAG, "Secure channel established on unknown device $deviceId.")
            return
        }

        val newConnectedDevice = ConnectedDevice(
            connectedDevice.deviceId,
            connectedDevice.deviceName,
            connectedDevice.belongsToActiveUser,
            hasSecureChannel = true
        )

        notifyCallbacks = connectedDeviceForManagerOrNull(deviceId, bleManager) != null

        // TODO (b/143088482) Implement interrupt
        // Ignore if central already holds the active device connection and interrupt the
        // connection.

        connectedDevices[deviceId] = InternalConnectedDevice(newConnectedDevice, bleManager)
        logd(TAG, "Secure channel established to $deviceId. Notifying callbacks: " +
            "[$notifyCallbacks]")
        if (notifyCallbacks) {
            notifyAllDeviceCallbacks(deviceId) { it.onSecureChannelEstablished(newConnectedDevice) }
        }
    }

    @VisibleForTesting
    internal fun messageReceived(deviceId: String, message: DeviceMessage) {
        logd(
            TAG,
            "New message received from device $deviceId intended for ${message.recipient} " +
                "containing ${message.message.size} bytes."
        )
        val connectedDevice = connectedDevices[deviceId]?.connectedDevice
        if (connectedDevice != null) {
            deviceCallbacks[deviceId]?.get(message.recipient)?.invoke {
                it.onMessageReceived(connectedDevice, message.message)
            }
        } else {
            logw(
                TAG,
                "Received message from unknown device $deviceId or to unknown recipient " +
                    "${message.recipient}."
            )
        }
    }

    @VisibleForTesting
    internal fun deviceErrorOccurred(deviceId: String) {
        val connectedDevice = connectedDevices[deviceId]?.connectedDevice
        if (connectedDevice != null) {
            notifyAllDeviceCallbacks(deviceId) {
                // Only error returned from SecureBleChannel is on a bad security key.
                it.onDeviceError(connectedDevice, DeviceError.INVALID_SECURITY_KEY)
            }
        } else {
            logw(TAG, "Failed to establish secure channel on unknown device $deviceId.")
        }
    }

    private fun getActiveUserDeviceIds() =
        storage.getTrustedDevicesForUser(ActivityManager.getCurrentUser())

    private fun connectedDeviceForManagerOrNull(
        deviceId: String,
        bleManager: CarBleManager
    ): InternalConnectedDevice? =
        connectedDevices[deviceId]?.takeIf { it.carBleManager === bleManager }

    private fun invokeConnectionCallbacks(
        belongsToActiveUser: Boolean,
        notification: (ConnectionCallback) -> Unit
    ) {
        logd(
            TAG,
            "Notifying connection callbacks for device belonging to active user " +
                "[$belongsToActiveUser]"
        )
        if (belongsToActiveUser) {
            activeUserConnectionCallbacks.invoke(notification)
        }
        allUserConnectionCallbacks.invoke(notification)
    }

    private fun generateCarBleCallback(carBleManager: CarBleManager): CarBleManager.Callback =
        object : CarBleManager.Callback {
            override fun onDeviceConnected(deviceId: String) {
                addConnectedDevice(deviceId, carBleManager)
            }

            override fun onDeviceDisconnected(deviceId: String) {
                removeConnectedDevice(deviceId, carBleManager)
            }

            override fun onSecureChannelEstablished(deviceId: String) {
                secureChannelEstablished(deviceId, carBleManager)
            }

            override fun onMessageReceived(deviceId: String, message: DeviceMessage) {
                messageReceived(deviceId, message)
            }

            override fun onSecureChannelError(deviceId: String) {
                deviceErrorOccurred(deviceId)
            }
        }

    private fun notifyAllDeviceCallbacks(
        deviceId: String,
        notification: (DeviceCallback) -> Unit
    ) {
        logd(TAG, "Notifying all device callbacks for device $deviceId.")
        deviceCallbacks[deviceId]?.values?.forEach { callbacks ->
            callbacks.invoke { notification(it) }
        }
    }

    /** Callback for triggered connection events from [ConnectedDeviceManager]. */
    interface ConnectionCallback {
        /** Triggered when a new [device] has connected. */
        fun onDeviceConnected(device: ConnectedDevice)

        /** Triggered when a [device] has disconnected. */
        fun onDeviceDisconnected(device: ConnectedDevice)
    }

    /** Triggered device events for a connected device from [ConnectedDeviceManager]. */
    interface DeviceCallback {
        /**
         * Triggered when secure channel has been established on [device]. Encrypted messaging now
         * available.
         */
        fun onSecureChannelEstablished(device: ConnectedDevice)

        /** Triggered when a new [message] is received from [device]. */
        fun onMessageReceived(device: ConnectedDevice, message: ByteArray)

        /** Triggered when an [error] has occurred for a [device]. */
        fun onDeviceError(device: ConnectedDevice, error: DeviceError)
    }

    /** Possible device errors. */
    enum class DeviceError {
        // Unable to establish a secure connection because of key mis-match.
        INVALID_SECURITY_KEY,
        // Multiple subscriptions have been made using the same recipientId and is no longer deemed
        // secure.
        INSECURE_RECIPIENT_ID_DETECTED
    }

    private data class InternalConnectedDevice(
        var connectedDevice: ConnectedDevice,
        val carBleManager: CarBleManager
    )
}
