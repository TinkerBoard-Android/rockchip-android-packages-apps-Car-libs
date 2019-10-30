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
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.Looper
import com.android.car.connecteddevice.util.ByteUtils
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.loge
import com.android.car.connecteddevice.util.logw
import com.android.car.connecteddevice.BleStreamProtos.BleDeviceMessageProto.BleDeviceMessage
import com.android.car.connecteddevice.BleStreamProtos.BlePacketProto.BlePacket
import com.android.car.connecteddevice.BleStreamProtos.BleOperationProto.OperationType
import com.android.car.connecteddevice.BleStreamProtos.VersionExchangeProto.BleVersionExchange
import com.android.car.protobuf.ByteString
import com.android.car.protobuf.InvalidProtocolBufferException
import com.android.internal.annotations.GuardedBy
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "BleDeviceMessageStream"

// Only version 2 of the messaging and version 1 of the security supported.
private const val MESSAGING_VERSION = 2
private const val SECURITY_VERSION = 1

// During bandwidth testing, it was discovered that allowing the stream to send as fast as it can
// blocked outgoing notifications from being received by the connected device. Adding a throttle to
// the outgoing messages alleviated this block and allowed both sides to send/receive in parallel
// successfully.
private const val THROTTLE_DEFAULT = 10L
private const val THROTTLE_WAIT = 75L

/** BLE message stream to a device. */
internal class BleDeviceMessageStream(
    private val blePeripheralManager: BlePeripheralManager,
    private val device: BluetoothDevice,
    private val writeCharacteristic: BluetoothGattCharacteristic,
    private val readCharacteristic: BluetoothGattCharacteristic
) {
    /** Listener that notifies on new messages received. */
    var messageReceivedListener: MessageReceivedListener? = null

    // Explicitly using an ArrayDequeue here for performance when used as a queue.
    private val packetQueue = ArrayDeque<BlePacket>()
    // This maps messageId to ByteArrayOutputStream of received packets
    private val pendingData = mutableMapOf<Int, ByteArrayOutputStream>()
    private val messageIdGenerator = MessageIdGenerator()
    private val handler = Handler(Looper.getMainLooper())
    private var isInSending = false
    private var isVersionExchanged = false
    private var messageReceivedErrorListener: MessageReceivedErrorListener? = null
    private var throttleDelay = AtomicLong(THROTTLE_DEFAULT)

    /**
     * The maximum amount of bytes that can be written over BLE.
     *
     * This initial value is 20 because BLE has a default write of 23 bytes. However, 3 bytes
     * are subtracted due to bytes being reserved for the command type and attribute ID.
     */
    internal var maxWriteSize = 20

    init {
        blePeripheralManager.addOnCharacteristicWriteListener(::onCharacteristicWrite)
        blePeripheralManager.addOnCharacteristicReadListener(::onCharacteristicRead)
    }

    /**
     * Writes the given message to the [writeCharacteristic] of this stream.
     *
     * This method will handle the chunking of messages based on [maxWriteSize]. If it is
     * a handshake message, the [deviceMessage.recipient] should be `null` and it cannot be
     * encrypted.
     *
     * @param deviceMessage The data object contains recipient, isPayloadEncrypted and message.
     * @param operationType The [OperationType] of this message. Defaults to `CLIENT_MESSAGE`.
     */
    fun writeMessage(
        deviceMessage: DeviceMessage,
        operationType: OperationType = OperationType.CLIENT_MESSAGE
    ) {
        logd(TAG, "Writing message to device: ${device.name}")
        val builder =
            BleDeviceMessage.newBuilder()
                .setOperation(operationType)
                .setIsPayloadEncrypted(deviceMessage.isMessageEncrypted)
                .setPayload(ByteString.copyFrom(deviceMessage.message))

        val recipient = deviceMessage.recipient
        if (recipient != null) {
            builder.recipient = ByteString.copyFrom(ByteUtils.uuidToBytes(recipient))
        }
        val bleDeviceMessage = builder.build()
        val rawBytes = bleDeviceMessage.toByteArray()
        val blePackets = makeBlePackets(rawBytes, messageIdGenerator.next(), maxWriteSize)
        packetQueue.addAll(blePackets)
        writeNextMessageInQueue()
    }

    private fun writeNextMessageInQueue() {
        handler.postDelayed(
            postDelayed@{
                if (isInSending || packetQueue.isEmpty()) {
                    return@postDelayed
                }
                val packet = packetQueue.remove()
                logd(
                    TAG,
                    "Writing packet ${packet.packetNumber} of ${packet.totalPackets} for " +
                        "${packet.messageId}."
                )
                writeCharacteristic.setValue(packet.toByteArray())
                blePeripheralManager.notifyCharacteristicChanged(device, writeCharacteristic, false)
                isInSending = true
            },
            throttleDelay.get()
        )
    }

    /**
     * Send the next packet in the queue to the client.
     */
    private fun onCharacteristicRead(device: BluetoothDevice) {
        if (this.device != device) {
            logw(
                TAG,
                "Received a message from a device (${device.address}) that is not the " +
                    "expected device (${device.address}) registered to this stream. Ignoring."
            )
            return
        }
        isInSending = false
        writeNextMessageInQueue()
    }

    /**
     * Processes a message from the client and notifies the [messageReceivedListener] of the
     * success of this call.
     */
    private fun onCharacteristicWrite(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (this.device != device) {
            logw(
                TAG,
                "Received a message from a device (${device.address}) that is not the " +
                    "expected device (${device.address}) registered to this stream. Ignoring."
            )
            return
        }
        if (characteristic.uuid != readCharacteristic.uuid) {
            logw(
                TAG,
                "Received a write to a characteristic (${characteristic.uuid}) that is not the " +
                    "expected UUID (${readCharacteristic.uuid}). Ignoring."
            )
            return
        }
        if (!isVersionExchanged) {
            processVersionExchange(device, value)
            return
        }

        val packet = try {
            BlePacket.parseFrom(value)
        } catch (e: InvalidProtocolBufferException) {
            loge(TAG, "Can not parse Ble packet from client.", e)
            messageReceivedErrorListener?.onMessageReceivedError(e)
            return
        }

        processPacket(packet)
    }

    private fun processVersionExchange(device: BluetoothDevice, value: ByteArray) {
        val versionExchange = try {
            BleVersionExchange.parseFrom(value)
        } catch (e: InvalidProtocolBufferException) {
            loge(TAG, "Could not parse version exchange message", e)
            messageReceivedErrorListener?.onMessageReceivedError(e)
            return
        }
        val minMessagingVersion = versionExchange.minSupportedMessagingVersion
        val maxMessagingVersion = versionExchange.maxSupportedMessagingVersion
        val minSecurityVersion = versionExchange.minSupportedSecurityVersion
        val maxSecurityVersion = versionExchange.maxSupportedSecurityVersion
        if ((minMessagingVersion > MESSAGING_VERSION)
            or (maxMessagingVersion < MESSAGING_VERSION)
            or (minSecurityVersion > SECURITY_VERSION)
            or (maxSecurityVersion < SECURITY_VERSION)
        ) {
            loge(
                TAG,
                "Unsupported message version for min $minMessagingVersion and max " +
                    "$maxMessagingVersion or security version for $minSecurityVersion and max " +
                    "$maxSecurityVersion."
            )
            messageReceivedErrorListener?.onMessageReceivedError(
                IllegalStateException("No supported version.")
            )
            return
        }
        val headunitVersion = BleVersionExchange.newBuilder()
            .setMinSupportedMessagingVersion(MESSAGING_VERSION)
            .setMaxSupportedMessagingVersion(MESSAGING_VERSION)
            .setMinSupportedSecurityVersion(SECURITY_VERSION)
            .setMaxSupportedSecurityVersion(SECURITY_VERSION)
            .build()
        writeCharacteristic.value = headunitVersion.toByteArray()
        blePeripheralManager.notifyCharacteristicChanged(device, writeCharacteristic, false)
        isVersionExchanged = true
        logd(TAG, "Sent supported version to the phone.")
    }

    private fun processPacket(packet: BlePacket) {
        // Messages are coming in. Need to throttle outgoing messages to allow outgoing notifications
        // to make it to the device.
        throttleDelay.set(THROTTLE_WAIT)
        val messageId = packet.messageId
        val currentPayloadStream = pendingData.getOrPut(
            messageId,
            {
                logd(TAG, "Creating new stream for message $messageId.")
                ByteArrayOutputStream()
            }
        )
        currentPayloadStream.write(packet.payload.toByteArray())
        logd(
            TAG,
            "Parsed packet ${packet.packetNumber} of ${packet.totalPackets} for " +
                "message $messageId. Writing ${packet.payload.toByteArray().size}."
        )
        if (packet.packetNumber != packet.totalPackets) {
            return
        }
        val messageBytes = currentPayloadStream.toByteArray()
        pendingData.remove(messageId)

        // All message packets received. Resetting throttle back to default until next message started.
        throttleDelay.set(THROTTLE_DEFAULT)

        logd(TAG, "Received complete device message $messageId of ${messageBytes.size} bytes.")
        val bleDeviceMessage = try {
            BleDeviceMessage.parseFrom(messageBytes)
        } catch (e: InvalidProtocolBufferException) {
            loge(TAG, "Cannot parse device message from client.", e)
            messageReceivedErrorListener?.onMessageReceivedError(e)
            return
        }

        val deviceMessage = DeviceMessage(
            ByteUtils.bytesToUUID(bleDeviceMessage.recipient.toByteArray()),
            bleDeviceMessage.isPayloadEncrypted,
            bleDeviceMessage.payload.toByteArray()
        )
        messageReceivedListener?.onMessageReceived(deviceMessage)
    }

    /**
     * Register the given listener to be notified when there was an error during receiving
     * message from the client. If [listener] is `null`, unregister.
     */
    fun registerMessageReceivedErrorListener(listener: MessageReceivedErrorListener?) {
        messageReceivedErrorListener = listener
    }

    /**
     * Listener to be invoked when a complete message is received from the client.
     */
    interface MessageReceivedListener {

        /**
         * Called when a complete message is received from the client.
         *
         * @param deviceMessage The message received from the client.
         */
        fun onMessageReceived(deviceMessage: DeviceMessage)
    }

    /**
     * Listener to be invoked when there was an error during receiving message from the client.
     */
    interface MessageReceivedErrorListener {
        /**
         * Called when there was an error during receiving message from the client.
         *
         * @param exception The error.
         */
        fun onMessageReceivedError(exception: Exception)
    }
}

/**
 * Holds the needed data from a [BleDeviceMessage].
 *
 * Note: comparisons can yield unexpected results. This class is intended to just hold a message
 * for transport.
 */
data class DeviceMessage(
    val recipient: UUID?,
    val isMessageEncrypted: Boolean,
    var message: ByteArray
)

/**
 * A generator of unique IDs for messages.
 */
class MessageIdGenerator {
    private val lock = ReentrantLock()
    @Volatile
    @GuardedBy("lock")
    private var messageId = 0

    /**
     * Return a unique id for identifying message
     */
    fun next(): Int {
        return lock.withLock {
            val currentMessageId = messageId
            messageId = if (messageId < Int.MAX_VALUE) (messageId + 1) else 0
            currentMessageId
        }
    }
}
