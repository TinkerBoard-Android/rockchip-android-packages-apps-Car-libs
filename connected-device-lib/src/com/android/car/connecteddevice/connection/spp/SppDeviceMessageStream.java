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

package com.android.car.connecteddevice.connection.spp;

import static com.android.car.connecteddevice.StreamProtos.VersionExchangeProto.BleVersionExchange;
import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import android.annotation.NonNull;
import android.bluetooth.BluetoothDevice;

import com.android.car.connecteddevice.StreamProtos.DeviceMessageProto.BleDeviceMessage;
import com.android.car.connecteddevice.StreamProtos.OperationProto;
import com.android.car.connecteddevice.connection.DeviceMessage;
import com.android.car.connecteddevice.connection.DeviceMessageStream;
import com.android.car.connecteddevice.util.ByteUtils;
import com.android.car.protobuf.ByteString;
import com.android.car.protobuf.InvalidProtocolBufferException;
import com.android.internal.annotations.VisibleForTesting;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spp message stream to a device.
 */
class SppDeviceMessageStream extends DeviceMessageStream {

    private static final String TAG = "SppDeviceMessageStream";

    // Only version 2 of the messaging and version 2 of the security supported.
    @VisibleForTesting
    static final int MESSAGING_VERSION = 2;
    @VisibleForTesting
    static final int SECURITY_VERSION = 2;

    private final AtomicBoolean mIsVersionExchanged = new AtomicBoolean(false);
    private final SppManager mSppManager;
    private final BluetoothDevice mDevice;
    private final Executor mCallbackExecutor = Executors.newSingleThreadExecutor();


    SppDeviceMessageStream(@NonNull SppManager sppManager,
            @NonNull BluetoothDevice device) {
        mSppManager = sppManager;
        mDevice = device;
        mSppManager.addOnMessageReceivedListener(this::onMessageReceived, mCallbackExecutor);
    }

    /**
     * Send the given message to remote connected bluetooth device.
     *
     * @param deviceMessage The data object contains recipient, isPayloadEncrypted and message.
     */
    @Override
    public void writeMessage(@NonNull DeviceMessage deviceMessage,
            OperationProto.OperationType operationType) {
        BleDeviceMessage.Builder builder = BleDeviceMessage.newBuilder()
                .setOperation(operationType)
                .setIsPayloadEncrypted(deviceMessage.isMessageEncrypted())
                .setPayload(ByteString.copyFrom(deviceMessage.getMessage()));

        UUID recipient = deviceMessage.getRecipient();
        if (recipient != null) {
            builder.setRecipient(ByteString.copyFrom(ByteUtils.uuidToBytes(recipient)));
        }

        BleDeviceMessage bleDeviceMessage = builder.build();
        mSppManager.write(bleDeviceMessage.toByteArray());
    }

    @VisibleForTesting
    void onMessageReceived(@NonNull BluetoothDevice device, @NonNull byte[] value) {
        logd(TAG, "Received a message from a device (" + device.getAddress() + ").");
        if (!mDevice.equals(device)) {
            logw(TAG, "Received a message from a device (" + device.getAddress() + ") that is not "
                    + "the expected device (" + mDevice.getAddress() + ") registered to this "
                    + "stream. Ignoring.");
            return;
        }

        if (!mIsVersionExchanged.get()) {
            processVersionExchange(device, value);
            return;
        }

        logd(TAG, "Received complete device message: " + value.length
                + " bytes.");
        BleDeviceMessage message;
        try {
            message = BleDeviceMessage.parseFrom(value);
        } catch (InvalidProtocolBufferException e) {
            loge(TAG, "Cannot parse device message from client.", e);
            notifyMessageReceivedErrorListener(e);
            return;
        }

        DeviceMessage deviceMessage = new DeviceMessage(
                ByteUtils.bytesToUUID(message.getRecipient().toByteArray()),
                message.getIsPayloadEncrypted(), message.getPayload().toByteArray());
        notifyMessageReceivedListener(deviceMessage, message.getOperation());
    }


    private void processVersionExchange(@NonNull BluetoothDevice device, @NonNull byte[] value) {
        BleVersionExchange versionExchange;
        try {
            versionExchange = BleVersionExchange.parseFrom(value);
        } catch (InvalidProtocolBufferException e) {
            loge(TAG, "Could not parse version exchange message", e);
            notifyMessageReceivedErrorListener(e);
            return;
        }
        int minMessagingVersion = versionExchange.getMinSupportedMessagingVersion();
        int maxMessagingVersion = versionExchange.getMaxSupportedMessagingVersion();
        int minSecurityVersion = versionExchange.getMinSupportedSecurityVersion();
        int maxSecurityVersion = versionExchange.getMaxSupportedSecurityVersion();
        if (minMessagingVersion > MESSAGING_VERSION || maxMessagingVersion < MESSAGING_VERSION
                || minSecurityVersion > SECURITY_VERSION || maxSecurityVersion < SECURITY_VERSION) {
            loge(TAG, "Unsupported message version for min " + minMessagingVersion + " and max "
                    + maxMessagingVersion + " or security version for " + minSecurityVersion
                    + " and max " + maxSecurityVersion + ".");
            notifyMessageReceivedErrorListener(new IllegalStateException("Unsupported version."));
            return;
        }

        BleVersionExchange headunitVersion = BleVersionExchange.newBuilder()
                .setMinSupportedMessagingVersion(MESSAGING_VERSION)
                .setMaxSupportedMessagingVersion(MESSAGING_VERSION)
                .setMinSupportedSecurityVersion(SECURITY_VERSION)
                .setMaxSupportedSecurityVersion(SECURITY_VERSION)
                .build();
        mSppManager.write(headunitVersion.toByteArray());
        mIsVersionExchanged.set(true);
        logd(TAG, "Sent supported version to the phone.");
    }
}
