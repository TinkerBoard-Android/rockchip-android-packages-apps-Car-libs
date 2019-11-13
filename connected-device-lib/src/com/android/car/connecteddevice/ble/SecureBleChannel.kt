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

import android.annotation.IntDef
import android.car.encryptionrunner.EncryptionRunner
import android.car.encryptionrunner.EncryptionRunnerFactory
import android.car.encryptionrunner.HandshakeException
import android.car.encryptionrunner.HandshakeMessage.HandshakeState
import android.car.encryptionrunner.Key
import com.android.car.connecteddevice.BleStreamProtos.BleOperationProto.OperationType
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage
import com.android.car.connecteddevice.util.ByteUtils
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.loge
import com.android.internal.R.attr.key
import com.android.internal.annotations.GuardedBy
import java.lang.Exception
import java.security.SignatureException
import java.util.concurrent.locks.ReentrantLock
import kotlin.annotation.Retention
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private const val TAG = "SecureBleChannel"
val CONFIRMATION_SIGNAL = "True".toByteArray()

/**
 * Establishes a secure channel with [EncryptionRunner] over [BleDeviceMessageStream] as server
 * side, sends and receives messages securely after the secure channel has been established.
 *
 * It only applies to the client that has been associated with the IHU before.
 */
internal class SecureBleChannel(
    internal val stream: BleDeviceMessageStream,
    private val storage: CarCompanionDeviceStorage,
    private val isReconnect: Boolean = true,
    private val runner: EncryptionRunner = EncryptionRunnerFactory.newRunner()
) {
    @IntDef(
            CHANNEL_ERROR_INVALID_HANDSHAKE,
            CHANNEL_ERROR_INVALID_MSG,
            CHANNEL_ERROR_INVALID_DEVICE_ID,
            CHANNEL_ERROR_INVALID_VERIFICATION,
            CHANNEL_ERROR_INVALID_STATE,
            CHANNEL_ERROR_INVALID_ENCRYPTION_KEY,
            CHANNEL_ERROR_STORAGE_ERROR
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ChannelError

    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private var encryptionKey: Key? = null
    private val streamListener = object : BleDeviceMessageStream.MessageReceivedListener {
        override fun onMessageReceived(deviceMessage: DeviceMessage, operationType: OperationType) {
            val payload = deviceMessage.message
            when (operationType) {
                OperationType.ENCRYPTION_HANDSHAKE -> {
                    logd(TAG, "Message received and handed off to handshake.")
                    try {
                        processHandshake(payload)
                    } catch (e: HandshakeException) {
                        loge(TAG, "Handshake failed.", e)
                        notifyCallback {
                            it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_HANDSHAKE)
                        }
                    }
                }
                OperationType.CLIENT_MESSAGE -> {
                    logd(TAG, "Received client message")
                    if (!deviceMessage.isMessageEncrypted) {
                        notifyCallback { it.onMessageReceived(deviceMessage) }
                        return
                    }
                    val key = lock.withLock { encryptionKey }
                    if (key == null) {
                        loge(TAG, "Received encrypted message before secure channel has been " +
                            "established.")
                        notifyCallback { it.onMessageReceivedError() }
                        return
                    }
                    try {
                        val decryptedPayload = key.decryptData(payload)
                        deviceMessage.message = decryptedPayload
                        notifyCallback { it.onMessageReceived(deviceMessage) }
                    } catch (e: SignatureException) {
                        loge(TAG, "Could not decrypt client credentials.", e)
                        notifyCallback { it.onMessageReceivedError(e) }
                    }
                }
                else -> {
                    loge(TAG, "Received unexpected operation type: $operationType.")
                }
            }
        }
    }

    private var deviceId: String? = null
    private var state = HandshakeState.UNKNOWN
    /** Listener that notifies to show verification code. */
    var showVerificationCodeListener: ShowVerificationCodeListener? = null
    /** Callback that notifies secure channel events.  */
    var channelCallback: Callback? = null

    init {
        runner.setIsReconnect(isReconnect)
        state = HandshakeState.UNKNOWN
        stream.messageReceivedListener = streamListener
    }

    private fun processHandshake(message: ByteArray) {
        when (state) {
            HandshakeState.UNKNOWN -> {
                if (deviceId != null) {
                    logd(TAG, "Responding to handshake init request.")
                    val handshakeMessage = runner.respondToInitRequest(message)
                    state = handshakeMessage.handshakeState
                    sendHandshakeMessage(handshakeMessage.nextMessage)
                    return
                }
                val id = ByteUtils.bytesToUUID(message)?.toString()
                if (id == null) {
                    loge(TAG, "Received invalid device id. Ignoring.")
                    return
                }
                logd(TAG, "Received device id: $id.")
                deviceId = id
                if (isReconnect && !hasEncryptionKey(id)) {
                    loge(TAG, "This client has not been associated before.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID)
                    }
                }
                notifyCallback { it.onDeviceIdReceived(id) }
                sendUniqueIdToClient()
            }
            HandshakeState.IN_PROGRESS -> {
                logd(TAG, "Continuing handshake.")
                val handshakeMessage = runner.continueHandshake(message)
                state = handshakeMessage.handshakeState
                val isValidStateForAssociation = !isReconnect &&
                        (state == HandshakeState.VERIFICATION_NEEDED)
                val isValidStateForReconnect = isReconnect &&
                        (state == HandshakeState.RESUMING_SESSION)
                if (!isValidStateForAssociation && !isValidStateForReconnect) {
                    loge(TAG, "Encounter unexpected handshake state: $state.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_STATE)
                    }
                    return
                }
                if (!isValidStateForAssociation) {
                    return
                }
                val code = handshakeMessage.verificationCode
                if (code == null) {
                    loge(TAG, "Unable to get verification code.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_VERIFICATION)
                    }
                    return
                }
                logd(TAG, "Showing pairing code: $code")
                showVerificationCodeListener?.showVerificationCode(code)
            }
            HandshakeState.RESUMING_SESSION -> {
                logd(TAG, "Start reconnection authentication.")
                val id = deviceId
                if (id == null) {
                    loge(TAG, "Unable to resume session, device id is null.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID)
                    }
                    return
                }
                val previousKey = storage.getEncryptionKey(id)
                if (previousKey == null) {
                    loge(TAG, "Unable to resume session, previous key is null.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY)
                    }
                    return
                }
                val handshakeMessage = runner.authenticateReconnection(message, previousKey)
                state = handshakeMessage.handshakeState
                if (state != HandshakeState.FINISHED) {
                    loge(TAG, "Unable to resume session, unexpected next handshake state: $state.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_STATE)
                    }
                    return
                }
                val newKey = handshakeMessage.key
                if (newKey == null) {
                    loge(TAG, "Unable to resume session, new key is null.")
                    notifyCallback {
                        it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY)
                    }
                    return
                }
                logd(TAG, "Saved new key for reconnection.")
                storage.saveEncryptionKey(id, newKey.asBytes())
                lock.withLock { encryptionKey = newKey }
                sendServerAuthToClient(handshakeMessage.nextMessage)
                notifyCallback { it.onSecureChannelEstablished(newKey) }
            }
            HandshakeState.INVALID, HandshakeState.FINISHED,
            HandshakeState.VERIFICATION_NEEDED -> {
                loge(TAG, "Encountered unexpected handshake state: $state." +
                    " Received message: $message.")
                notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_STATE) }
            }
            else -> {
                loge(TAG, "Encountered unrecognized handshake state: $state." +
                        " Received message: $message.")
                notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_STATE) }
            }
        }
    }

    private fun sendUniqueIdToClient() {
        val uniqueId = storage.uniqueId
        logd(TAG, "Send car id: $uniqueId")
        val deviceMessage = DeviceMessage(
            recipient = null,
            isMessageEncrypted = false,
            message = ByteUtils.uuidToBytes(uniqueId)
        )
        logd(TAG, "Sending car's device id of $uniqueId to device.")
        stream.writeMessage(
            deviceMessage,
            operationType = OperationType.ENCRYPTION_HANDSHAKE
        )
    }

    private fun hasEncryptionKey(id: String) = storage.getEncryptionKey(id) != null

    private fun sendHandshakeMessage(message: ByteArray?) {
        if (message != null) {
            logd(TAG, "Send handshake message: $message.")
            val deviceMessage = DeviceMessage(
                recipient = null,
                isMessageEncrypted = false,
                message = message
            )
            stream.writeMessage(
                deviceMessage,
                operationType = OperationType.ENCRYPTION_HANDSHAKE
            )
        } else {
            loge(TAG, "Unable to send next handshake message, message is null.")
            notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_MSG) }
        }
    }

    private fun sendServerAuthToClient(message: ByteArray?) {
        if (message != null) {
            stream.writeMessage(
                DeviceMessage(recipient = null, isMessageEncrypted = false, message = message),
                operationType = OperationType.CLIENT_MESSAGE
            )
        } else {
            loge(TAG, "Unable to send server authentication message to client, message is null.")
            notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_MSG) }
        }
    }

    // This should be called only after the secure channel has been established.
    fun sendEncryptedMessage(deviceMessage: DeviceMessage) {
        if (!deviceMessage.isMessageEncrypted) {
            loge(TAG, "Encryption not required for this message $deviceMessage.")
            return
        }
        val key = lock.withLock { encryptionKey }
        if (key != null) {
            val encryptedMessage = key.encryptData(deviceMessage.message)
            deviceMessage.message = encryptedMessage
            stream.writeMessage(
                deviceMessage,
                OperationType.CLIENT_MESSAGE
            )
        } else {
            throw IllegalStateException("Secure channel has not been established.")
        }
    }

    /**
     * Called by the client to notify that the user has accepted a pairing code or any out-of-band
     * confirmation, and send confirmation signals to remote bluetooth device.
     */
    fun notifyOutOfBandAccepted() {
        val message = try {
            runner.verifyPin()
        } catch (e: HandshakeException) {
            loge(TAG, "Error during PIN verification", e)
            notifyCallback {
                it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_VERIFICATION)
            }
            return
        } catch (e: IllegalStateException) {
            loge(TAG, "Error during PIN verification", e)
            notifyCallback {
                it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_VERIFICATION)
            }
            return
        }
        if (message.handshakeState != HandshakeState.FINISHED) {
            loge(TAG, "Handshake not finished after calling verify PIN." +
                    " Instead got state: $message.")
            notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_STATE) }
            return
        }

        val localKey = message.key
        if (localKey == null) {
            loge(TAG, "Unable to finish association, generated key is null.")
            notifyCallback {
                it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY)
            }
            return
        }

        state = message.handshakeState
        lock.withLock { encryptionKey = localKey }
        val localDeviceId = deviceId
        if (localDeviceId == null) {
            loge(TAG, "Unable to finish association, device id is null.")
            notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID) }
            return
        }

        if (!storage.saveEncryptionKey(localDeviceId, localKey.asBytes())) {
            loge(TAG, "Failed to save encryption key.")
            notifyCallback { it.onEstablishSecureChannelFailure(CHANNEL_ERROR_STORAGE_ERROR) }
            return
        }
        logd(TAG, "Pairing code successfully verified and encryption key saved. " +
            "Sending confirmation to device.")
        notifyCallback { it.onSecureChannelEstablished(localKey) }
        val deviceMessage = DeviceMessage(
            recipient = null,
            isMessageEncrypted = false,
            message = CONFIRMATION_SIGNAL
        )
        stream.writeMessage(deviceMessage, operationType = OperationType.ENCRYPTION_HANDSHAKE)
    }

    private fun notifyCallback(notification: (Callback) -> Unit) {
        val localCallback = channelCallback
        if (localCallback == null) {
            loge(TAG, "Call to notify callback but channel callback is null. Ignoring.")
            return
        }
        thread(start = true) { notification(localCallback) }
    }

    /**
     * Callbacks that will be invoked during establishing secure channel, sending and receiving
     * messages securely.
     */
    interface Callback {
        /**
         * Invoked when secure channel has been established successfully.
         *
         * @param encryptionKey The new key generated in handshake.
         */
        fun onSecureChannelEstablished(encryptionKey: Key)

        /**
         * Invoked when a [ChannelError] has been encountered in attempting to establish
         * a secure channel.
         *
         * @param error The failure indication.
         */
        fun onEstablishSecureChannelFailure(@ChannelError error: Int)

        /**
         * Invoked when a complete message is received securely from the client and decrypted.
         *
         * @param deviceMessage The [DeviceMessage] with decrypted message.
         */
        fun onMessageReceived(deviceMessage: DeviceMessage)

        /**
         * Invoked when there was an error during a processing or decrypting of a client message.
         *
         * @param exception The error.
         */
        fun onMessageReceivedError(exception: Exception? = null)

        /**
         * Invoked when the device id was received from the client.
         *
         * @param deviceId The unique device id of client.
         */
        fun onDeviceIdReceived(deviceId: String)
    }

    /**
     * Listener that will be invoked to display verification code.
     */
    interface ShowVerificationCodeListener {
        /**
         * Invoke when a verification need to be displayed during device association.
         *
         * @param code The verification code to show.
         */
        fun showVerificationCode(code: String)
    }

    companion object {
        /** Indicates an error during a Handshake of EncryptionRunner. */
        const val CHANNEL_ERROR_INVALID_HANDSHAKE = 0
        /** Received an invalid handshake message or has an invalid handshake message to send. */
        const val CHANNEL_ERROR_INVALID_MSG = 1
        /** Unable to retrieve a valid id. */
        const val CHANNEL_ERROR_INVALID_DEVICE_ID = 2
        /** Unable to get verification code ot there's a error during pin verification. */
        const val CHANNEL_ERROR_INVALID_VERIFICATION = 3
        /** Encountered an unexpected handshake state. */
        const val CHANNEL_ERROR_INVALID_STATE = 4
        /** Failed to get a valid previous/new encryption key.*/
        const val CHANNEL_ERROR_INVALID_ENCRYPTION_KEY = 5
        /** Failed to save the encryption key*/
        const val CHANNEL_ERROR_STORAGE_ERROR = 6
    }
}
