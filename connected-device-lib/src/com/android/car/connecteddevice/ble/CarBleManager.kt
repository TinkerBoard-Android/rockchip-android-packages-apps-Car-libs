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

package com.android.car.connecteddevice.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage
import com.android.car.connecteddevice.util.ThreadSafeCallbacks
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.logw
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

private const val TAG = "CarBleManager"

/**
 * Generic BLE manager for a car that keeps track of connected devices and their associated
 * callbacks.
 *
 * @param carCompanionDeviceStorage Shared [CarCompanionDeviceStorage] for companion features.
 */
internal abstract class CarBleManager(
    private val carCompanionDeviceStorage: CarCompanionDeviceStorage
) {

    protected val connectedDevices = CopyOnWriteArraySet<BleDevice>()

    protected val callbacks = ThreadSafeCallbacks<Callback>()

    open fun start() {
    }

    open fun stop() {
        connectedDevices.forEach { it.gatt?.close() }
        connectedDevices.clear()
        callbacks.clear()
    }

    /**
     * Register a [callback] to be notified on the [executor].
     */
    fun registerCallback(callback: Callback, executor: Executor) {
        callbacks.add(callback, executor)
    }

    /**
     * Unregister a callback
     *
     * @param callback The [Callback] to unregister.
     */
    fun unregisterCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    /**
     * Send a message to a connected device.
     *
     * @param deviceId Id of connected device.
     * @param message [DeviceMessage] to send.
     */
    fun sendMessage(deviceId: String, message: DeviceMessage) {
        getConnectedDevice(deviceId)?.run {
            sendMessage(this, message)
        } ?: logw(TAG, "Attempted to send message to unknown device $deviceId. Ignored")
    }

    /**
     * Send a message to a connected device.
     *
     * @param bleDevice The connected [BleDevice].
     * @param message [DeviceMessage] to send.
     */
    protected fun sendMessage(bleDevice: BleDevice, message: DeviceMessage) {
        logd(
            TAG,
            "Writing ${message.message.size} bytes to " +
                "${bleDevice.deviceId ?: "Unidentified device"}."
        )

        if (message.isMessageEncrypted) {
            bleDevice.secureChannel?.sendEncryptedMessage(message)
        } else {
            bleDevice.secureChannel?.stream?.writeMessage(message)
        }
    }

    /**
     * Get the [BleDevice] with matching [gatt] if available. Returns `null` if no matches
     * found.
     */
    protected fun getConnectedDevice(gatt: BluetoothGatt): BleDevice? {
        return connectedDevices.firstOrNull { it.gatt == gatt }
    }

    /**
     * Get the [BleDevice] with matching [device] if available. Returns `null` if no
     * matches found.
     */
    protected fun getConnectedDevice(device: BluetoothDevice): BleDevice? {
        return connectedDevices.firstOrNull { it.device == device }
    }

    /**
     * Get the [BleDevice] with matching [deviceId] if available. Returns `null` if no matches
     * found.
     */
    protected fun getConnectedDevice(deviceId: String): BleDevice? {
        return connectedDevices.firstOrNull { it.deviceId == deviceId }
    }

    /** Add the [bleDevice] that has connected. */
    protected fun addConnectedDevice(bleDevice: BleDevice) {
        connectedDevices.add(bleDevice)
    }

    /** Return the number of devices currently connected. */
    protected fun connectedDevicesCount(): Int {
        return connectedDevices.count()
    }

    /** Remove [bleDevice] that has been disconnected. */
    protected fun removeConnectedDevice(bleDevice: BleDevice) {
        connectedDevices.remove(bleDevice)
    }

    /**
     * Container class to hold information about a connected device.
     *
     * @param device The [BluetoothDevice] from the connection.
     * @param gatt The [BluetoothGatt] from the connection. Only applicable for central connections.
     */
    protected data class BleDevice(
        val device: BluetoothDevice,
        val gatt: BluetoothGatt?
    ) {
        var state: BleDeviceState = BleDeviceState.UNKNOWN
        var deviceId: String? = null
        var secureChannel: SecureBleChannel? = null
    }

    /** State for a connected device. */
    protected enum class BleDeviceState {
        CONNECTING,
        PENDING_VERIFICATION,
        CONNECTED,
        UNKNOWN
    }

    /**
     * Callback for triggered events from [CarBleManager].
     */
    interface Callback {

        /**
         * Triggered when device is connected and device id retrieved. Device is now ready to
         * receive messages.
         *
         * @param deviceId Id of device that has connected.
         */
        fun onDeviceConnected(deviceId: String)

        /**
         * Triggered when device is disconnected.
         *
         * @param deviceId Id of device that has disconnected.
         */
        fun onDeviceDisconnected(deviceId: String)

        /**
         * Triggered when device has established encryption for secure communication.
         *
         * @param deviceId Id of device that has established encryption.
         */
        fun onSecureChannelEstablished(deviceId: String)

        /**
         * Triggered when a new message is received.
         *
         * @param deviceId Id of the device that sent the message.
         * @param message [DeviceMessage] received.
         */
        fun onMessageReceived(deviceId: String, message: DeviceMessage)

        /**
         * Triggered when an error when establishing the secure channel.
         *
         * @param deviceId Id of the device that experienced the error.
         */
        fun onSecureChannelError(deviceId: String)
    }
}
