/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.connecteddevice.ble;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.car.encryptionrunner.EncryptionRunner;
import android.car.encryptionrunner.EncryptionRunnerFactory;
import android.car.encryptionrunner.HandshakeException;
import android.car.encryptionrunner.HandshakeMessage;
import android.car.encryptionrunner.HandshakeMessage.HandshakeState;
import android.car.encryptionrunner.Key;

import com.android.car.connecteddevice.BleStreamProtos.BleOperationProto.OperationType;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.car.connecteddevice.util.ByteUtils;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.SignatureException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Establishes a secure channel with {@link EncryptionRunner} over {@link BleDeviceMessageStream} as
 * server side, sends and receives messages securely after the secure channel has been established.
 */
class SecureBleChannel {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "CHANNEL_ERROR" },
            value = {
                    CHANNEL_ERROR_INVALID_HANDSHAKE,
                    CHANNEL_ERROR_INVALID_MSG,
                    CHANNEL_ERROR_INVALID_DEVICE_ID,
                    CHANNEL_ERROR_INVALID_VERIFICATION,
                    CHANNEL_ERROR_INVALID_STATE,
                    CHANNEL_ERROR_INVALID_ENCRYPTION_KEY,
                    CHANNEL_ERROR_STORAGE_ERROR
            }
    )
    @interface ChannelError { }

    /** Indicates an error during a Handshake of EncryptionRunner. */
    static final int CHANNEL_ERROR_INVALID_HANDSHAKE = 0;
    /** Received an invalid handshake message or has an invalid handshake message to send. */
    static final int CHANNEL_ERROR_INVALID_MSG = 1;
    /** Unable to retrieve a valid id. */
    static final int CHANNEL_ERROR_INVALID_DEVICE_ID = 2;
    /** Unable to get verification code or there's a error during pin verification. */
    static final int CHANNEL_ERROR_INVALID_VERIFICATION = 3;
    /** Encountered an unexpected handshake state. */
    static final int CHANNEL_ERROR_INVALID_STATE = 4;
    /** Failed to get a valid previous/new encryption key.*/
    static final int CHANNEL_ERROR_INVALID_ENCRYPTION_KEY = 5;
    /** Failed to save the encryption key*/
    static final int CHANNEL_ERROR_STORAGE_ERROR = 6;

    @VisibleForTesting
    static final byte[] CONFIRMATION_SIGNAL = "True".getBytes();

    private static final String TAG = "SecureBleChannel";

    private final BleDeviceMessageStream mStream;

    private final ConnectedDeviceStorage mStorage;

    private final boolean mIsReconnect;

    private final EncryptionRunner mEncryptionRunner;

    private final AtomicReference<Key> mEncryptionKey = new AtomicReference<>();

    private @HandshakeState int mState = HandshakeState.UNKNOWN;

    private String mDeviceId;

    private Callback mCallback;

    private ShowVerificationCodeListener mShowVerificationCodeListener;

    SecureBleChannel(@NonNull BleDeviceMessageStream stream,
            @NonNull ConnectedDeviceStorage storage) {
        this(stream, storage, /* isReconnect = */ true, EncryptionRunnerFactory.newRunner());
    }

    SecureBleChannel(@NonNull BleDeviceMessageStream stream,
            @NonNull ConnectedDeviceStorage storage, boolean isReconnect,
            @NonNull EncryptionRunner encryptionRunner) {
        mStream = stream;
        mStorage = storage;
        mIsReconnect = isReconnect;
        mEncryptionRunner = encryptionRunner;
        mEncryptionRunner.setIsReconnect(isReconnect);
        mStream.setMessageReceivedListener(mStreamListener);
    }

    private void processHandshake(@NonNull byte[] message) throws HandshakeException {
        switch (mState) {
            case HandshakeState.UNKNOWN:
                processHandshakeUnknown(message);
                break;
            case HandshakeState.IN_PROGRESS:
                processHandshakeInProgress(message);
                break;
            case HandshakeState.RESUMING_SESSION:
                processHandshakeResumingSession(message);
                break;
            default:
                loge(TAG, "Encountered unexpected handshake state: " + mState + ". Received "
                        + "message: " + ByteUtils.byteArrayToHexString(message) + ".");
                notifySecureChannelFailure(CHANNEL_ERROR_INVALID_STATE);
        }
    }

    private void processHandshakeUnknown(@NonNull byte[] message) throws HandshakeException {
        if (mDeviceId != null) {
            logd(TAG, "Responding to handshake init request.");
            HandshakeMessage handshakeMessage = mEncryptionRunner.respondToInitRequest(message);
            mState = handshakeMessage.getHandshakeState();
            sendHandshakeMessage(handshakeMessage.getNextMessage());
            return;
        }
        UUID deviceId = ByteUtils.bytesToUUID(message);
        if (deviceId == null) {
            loge(TAG, "Received invalid device id. Ignoring.");
            return;
        }
        mDeviceId = deviceId.toString();
        if (mIsReconnect && !hasEncryptionKey(mDeviceId)) {
            loge(TAG, "Attempted to reconnect device but no key found. Aborting secure channel.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID);
            return;
        }
        notifyCallback(callback -> callback.onDeviceIdReceived(mDeviceId));
        sendUniqueIdToClient();
    }

    private void processHandshakeInProgress(@NonNull byte[] message) throws HandshakeException {
        logd(TAG, "Continuing handshake.");
        HandshakeMessage handshakeMessage = mEncryptionRunner.continueHandshake(message);
        mState = handshakeMessage.getHandshakeState();

        boolean isValidStateForAssociation = !mIsReconnect
                && mState == HandshakeState.VERIFICATION_NEEDED;
        boolean isValidStateForReconnect = mIsReconnect
                && mState == HandshakeState.RESUMING_SESSION;
        if (!isValidStateForAssociation && !isValidStateForReconnect) {
            loge(TAG, "processHandshakeInProgress: Encountered unexpected handshake state: "
                    + mState + ".");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_STATE);
            return;
        }

        if (!isValidStateForAssociation) {
            return;
        }

        String code = handshakeMessage.getVerificationCode();
        if (code == null) {
            loge(TAG, "Unable to get verification code.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_VERIFICATION);
            return;
        }

        if (mShowVerificationCodeListener != null) {
            logd(TAG, "Showing pairing code: " + code);
            mShowVerificationCodeListener.showVerificationCode(code);
        }
    }

    private void processHandshakeResumingSession(@NonNull byte[] message)
            throws HandshakeException {
        logd(TAG, "Start reconnection authentication.");
        if (mDeviceId == null) {
            loge(TAG, "processHandshakeResumingSession: Unable to resume session, device id is "
                    + "null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID);
            return;
        }

        byte[] previousKey = mStorage.getEncryptionKey(mDeviceId);
        if (previousKey == null) {
            loge(TAG, "Unable to resume session, previous key is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY);
            return;
        }

        HandshakeMessage handshakeMessage = mEncryptionRunner.authenticateReconnection(message,
                previousKey);
        mState = handshakeMessage.getHandshakeState();
        if (mState != HandshakeState.FINISHED) {
            loge(TAG, "Unable to resume session, unexpected next handshake state: " + mState + ".");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_STATE);
            return;
        }

        Key newKey = handshakeMessage.getKey();
        if (newKey == null) {
            loge(TAG, "Unable to resume session, new key is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY);
            return;
        }

        logd(TAG, "Saved new key for reconnection.");
        mStorage.saveEncryptionKey(mDeviceId, newKey.asBytes());
        mEncryptionKey.set(newKey);
        sendServerAuthToClient(handshakeMessage.getNextMessage());
        notifyCallback(callback -> callback.onSecureChannelEstablished());
    }

    private void sendUniqueIdToClient() {
        UUID uniqueId = mStorage.getUniqueId();
        DeviceMessage deviceMessage = new DeviceMessage(/* recipient = */ null,
                /* isMessageEncrypted = */ false, ByteUtils.uuidToBytes(uniqueId));
        logd(TAG, "Sending car's device id of " + uniqueId + " to device.");
        mStream.writeMessage(deviceMessage, OperationType.ENCRYPTION_HANDSHAKE);
    }

    private boolean hasEncryptionKey(@NonNull String id) {
        return mStorage.getEncryptionKey(id) != null;
    }

    private void sendHandshakeMessage(@Nullable byte[] message) {
        if (message == null) {
            loge(TAG, "Unable to send next handshake message, message is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_MSG);
            return;
        }

        logd(TAG, "Send handshake message: " + ByteUtils.byteArrayToHexString(message) + ".");
        DeviceMessage deviceMessage = new DeviceMessage(/* recipient = */ null,
                /* isMessageEncrypted = */ false, message);
        mStream.writeMessage(deviceMessage, OperationType.ENCRYPTION_HANDSHAKE);
    }

    private void sendServerAuthToClient(@Nullable byte[] message) {
        if (message == null) {
            loge(TAG, "Unable to send server authentication message to client, message is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_MSG);
            return;
        }
        DeviceMessage deviceMessage = new DeviceMessage(/* recipient = */ null,
                /* isMessageEncrypted = */ false, message);
        mStream.writeMessage(deviceMessage, OperationType.ENCRYPTION_HANDSHAKE);
    }

    /**
     * Send an encrypted message.
     * <p>Note: This should be called only after the secure channel has been established.</p>
     *
     * @param deviceMessage The {@link DeviceMessage} to encrypt and send.
     */
    void sendEncryptedMessage(@NonNull DeviceMessage deviceMessage) throws IllegalStateException {
        if (!deviceMessage.isMessageEncrypted()) {
            loge(TAG, "Encryption not required for this message " + deviceMessage + ".");
            return;
        }
        Key key = mEncryptionKey.get();
        if (key == null) {
            throw new IllegalStateException("Secure channel has not been established.");
        }

        byte[] encryptedMessage = key.encryptData(deviceMessage.getMessage());
        deviceMessage.setMessage(encryptedMessage);
        mStream.writeMessage(deviceMessage, OperationType.CLIENT_MESSAGE);
    }

    /**
     * Called by the client to notify that the user has accepted a pairing code or any out-of-band
     * confirmation, and send confirmation signals to remote bluetooth device.
     */
    void notifyOutOfBandAccepted() {
        HandshakeMessage message;
        try {
            message = mEncryptionRunner.verifyPin();
        } catch (HandshakeException e) {
            loge(TAG, "Error during PIN verification", e);
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_VERIFICATION);
            return;
        }
        if (message.getHandshakeState() != HandshakeState.FINISHED) {
            loge(TAG, "Handshake not finished after calling verify PIN. Instead got "
                    + "state: " + message.getHandshakeState() + ".");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_STATE);
            return;
        }

        Key localKey = message.getKey();
        if (localKey == null) {
            loge(TAG, "Unable to finish association, generated key is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_ENCRYPTION_KEY);
            return;
        }

        mState = message.getHandshakeState();
        mStorage.saveEncryptionKey(mDeviceId, localKey.asBytes());
        mEncryptionKey.set(localKey);
        if (mDeviceId == null) {
            loge(TAG, "Unable to finish association, device id is null.");
            notifySecureChannelFailure(CHANNEL_ERROR_INVALID_DEVICE_ID);
            return;
        }
        logd(TAG, "Pairing code successfully verified and encryption key saved. Sending "
                + "confirmation to device.");
        notifyCallback(Callback::onSecureChannelEstablished);
        DeviceMessage deviceMessage = new DeviceMessage(/* recipient = */ null,
                /* isMessageEncrypted = */ false, CONFIRMATION_SIGNAL);
        mStream.writeMessage(deviceMessage, OperationType.ENCRYPTION_HANDSHAKE);
    }

    /** Get the BLE stream backing this channel. */
    @NonNull
    BleDeviceMessageStream getStream() {
        return mStream;
    }

    /**Set the listener that notifies to show verification code. {@code null} to clear.*/
    void setShowVerificationCodeListener(@Nullable ShowVerificationCodeListener listener) {
        mShowVerificationCodeListener = listener;
    }

    @VisibleForTesting
    @Nullable
    ShowVerificationCodeListener getShowVerificationCodeListener() {
        return mShowVerificationCodeListener;
    }

    /** Register a callback that notifies secure channel events. */
    void registerCallback(Callback callback) {
        mCallback = callback;
    }

    /** Unregister a callback. */
    void unregisterCallback(Callback callback) {
        if (callback == mCallback) {
            mCallback = null;
        }
    }

    @VisibleForTesting
    @Nullable
    Callback getCallback() {
        return mCallback;
    }

    private void notifyCallback(Consumer<Callback> notification) {
        if (mCallback != null) {
            notification.accept(mCallback);
        }
    }

    private void notifySecureChannelFailure(@ChannelError int error) {
        loge(TAG, "Secure channel error: " + error);
        notifyCallback(callback -> callback.onEstablishSecureChannelFailure(error));
    }

    private final BleDeviceMessageStream.MessageReceivedListener mStreamListener =
            new BleDeviceMessageStream.MessageReceivedListener() {
                @Override
                public void onMessageReceived(DeviceMessage deviceMessage,
                        OperationType operationType) {
                    byte[] message = deviceMessage.getMessage();
                    switch(operationType) {
                        case ENCRYPTION_HANDSHAKE:
                            logd(TAG, "Message received and handed off to handshake.");
                            try {
                                processHandshake(message);
                            } catch (HandshakeException e) {
                                loge(TAG, "Handshake failed.", e);
                                notifyCallback(callback -> callback.onEstablishSecureChannelFailure(
                                        CHANNEL_ERROR_INVALID_HANDSHAKE));
                            }
                            break;
                        case CLIENT_MESSAGE:
                            logd(TAG, "Received client message.");
                            if (!deviceMessage.isMessageEncrypted()) {
                                notifyCallback(callback -> callback.onMessageReceived(
                                        deviceMessage));
                                return;
                            }
                            Key key = mEncryptionKey.get();
                            if (key == null) {
                                loge(TAG, "Received encrypted message before secure channel has "
                                        + "been established.");
                                notifyCallback(callback -> callback.onMessageReceivedError(null));
                                return;
                            }
                            try {
                                byte[] decryptedPayload =
                                        key.decryptData(deviceMessage.getMessage());
                                deviceMessage.setMessage(decryptedPayload);
                                notifyCallback(
                                        callback -> callback.onMessageReceived(deviceMessage));
                            } catch (SignatureException e) {
                                loge(TAG, "Could not decrypt client credentials.", e);
                                notifyCallback(callback -> callback.onMessageReceivedError(e));
                            }
                            break;
                        default:
                            loge(TAG, "Received unexpected operation type: " + operationType + ".");
                    }
                }
            };

    /**
     * Callbacks that will be invoked during establishing secure channel, sending and receiving
     * messages securely.
     */
    interface Callback {
        /**
         * Invoked when secure channel has been established successfully.
         */
        void onSecureChannelEstablished();

        /**
         * Invoked when a {@link ChannelError} has been encountered in attempting to establish
         * a secure channel.
         *
         * @param error The failure indication.
         */
        void onEstablishSecureChannelFailure(@SecureBleChannel.ChannelError int error);

        /**
         * Invoked when a complete message is received securely from the client and decrypted.
         *
         * @param deviceMessage The {@link DeviceMessage} with decrypted message.
         */
        void onMessageReceived(@NonNull DeviceMessage deviceMessage);

        /**
         * Invoked when there was an error during a processing or decrypting of a client message.
         *
         * @param exception The error.
         */
        void onMessageReceivedError(@Nullable Exception exception);

        /**
         * Invoked when the device id was received from the client.
         *
         * @param deviceId The unique device id of client.
         */
        void onDeviceIdReceived(@NonNull String deviceId);
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
        void showVerificationCode(@NonNull String code);
    }
}
