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

import static com.android.car.connecteddevice.ConnectedDeviceManager.DEVICE_ERROR_INVALID_HANDSHAKE;
import static com.android.car.connecteddevice.ConnectedDeviceManager.DEVICE_ERROR_UNEXPECTED_DISCONNECTION;
import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.android.car.connecteddevice.AssociationCallback;
import com.android.car.connecteddevice.connection.AssociationSecureChannel;
import com.android.car.connecteddevice.connection.CarBluetoothManager;
import com.android.car.connecteddevice.connection.DeviceMessage;
import com.android.car.connecteddevice.connection.DeviceMessageStream;
import com.android.car.connecteddevice.connection.SecureChannel;
import com.android.car.connecteddevice.model.AssociatedDevice;
import com.android.car.connecteddevice.oob.OobChannel;
import com.android.car.connecteddevice.oob.OobConnectionManager;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.car.connecteddevice.util.EventLog;
import com.android.internal.annotations.VisibleForTesting;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Communication manager that allows for targeted connections to a specific device from the car
 * using {@link SppManager} .
 */
public class CarSppManager extends CarBluetoothManager {

    private static final String TAG = "CarSppManager";

    private final SppManager mSppManager;

    private String mClientDeviceName;

    private String mClientDeviceAddress;

    private String mReconnectDeviceId;

    private AssociationCallback mAssociationCallback;

    private OobConnectionManager mOobConnectionManager;

    private Executor mCallbackExecutor;

    /**
     * Initialize a new instance of manager.
     *
     * @param sppManager             {@link SppManager} for establishing connection.
     * @param connectedDeviceStorage Shared {@link ConnectedDeviceStorage} for companion features.
     */
    public CarSppManager(@NonNull SppManager sppManager,
            @NonNull ConnectedDeviceStorage connectedDeviceStorage) {
        super(connectedDeviceStorage);
        mSppManager = sppManager;
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start() {
        super.start();
        mSppManager.startListening();
        mSppManager.unregisterCallback(mAssociationSppCallback);
        mSppManager.registerCallback(mReconnectSppCallback, mCallbackExecutor);
    }

    @Override
    public void stop() {
        super.stop();
        reset();
    }

    @Override
    public void disconnectDevice(@NonNull String deviceId) {
        ConnectedRemoteDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null || !deviceId.equals(connectedDevice.mDeviceId)) {
            return;
        }
        reset();
    }

    private void reset() {
        mClientDeviceAddress = null;
        mClientDeviceName = null;
        mAssociationCallback = null;
        mReconnectDeviceId = null;
        mSppManager.cleanup();
        mConnectedDevices.clear();
    }

    @Nullable
    private ConnectedRemoteDevice getConnectedDevice() {
        if (mConnectedDevices.isEmpty()) {
            return null;
        }
        // Directly return the next because there will only be one device connected at one time.
        return mConnectedDevices.iterator().next();
    }

    /**
     * Start the association by listening to incoming connect request.
     */
    public void startAssociation(@NonNull AssociationCallback callback) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            loge(TAG, "Bluetooth is unavailable on this device. Unable to start associating.");
            return;
        }

        reset();
        mAssociationCallback = callback;
        mSppManager.unregisterCallback(mReconnectSppCallback);
        mSppManager.registerCallback(mAssociationSppCallback, mCallbackExecutor);
        if (mSppManager.startListening()) {
            callback.onAssociationStartSuccess(adapter.getName());
        } else {
            callback.onAssociationStartFailure();
        }
    }

    /**
     * Stop the association with any device.
     */
    public void stopAssociation() {
        if (!isAssociating()) {
            return;
        }
        reset();
    }

    /**
     * Start the association with a new device using out of band verification code exchange
     */
    public void startOutOfBandAssociation(@NonNull OobChannel oobChannel,
            @NonNull AssociationCallback callback) {

        logd(TAG, "Starting out of band association.");
        startAssociation(new AssociationCallback() {
            @Override
            public void onAssociationStartSuccess(String deviceName) {
                mAssociationCallback = callback;
                boolean success = mOobConnectionManager.startOobExchange(oobChannel);
                if (!success) {
                    callback.onAssociationStartFailure();
                    return;
                }
                callback.onAssociationStartSuccess(deviceName);
            }

            @Override
            public void onAssociationStartFailure() {
                callback.onAssociationStartFailure();
            }
        });
        mOobConnectionManager = new OobConnectionManager();
    }

    /**
     * Notify that the user has accepted a pairing code or other out-of-band confirmation.
     */
    public void notifyOutOfBandAccepted() {
        if (getConnectedDevice() == null) {
            disconnectWithError("Null connected device found when out-of-band confirmation "
                    + "received.");
            return;
        }

        AssociationSecureChannel secureChannel =
                (AssociationSecureChannel) getConnectedDevice().mSecureChannel;
        if (secureChannel == null) {
            disconnectWithError("Null SecureBleChannel found for the current connected device "
                    + "when out-of-band confirmation received.");
            return;
        }

        secureChannel.notifyOutOfBandAccepted();
    }

    @VisibleForTesting
    @Nullable
    SecureChannel getConnectedDeviceChannel() {
        ConnectedRemoteDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null) {
            return null;
        }

        return connectedDevice.mSecureChannel;
    }

    private void setDeviceIdAndNotifyCallbacks(@NonNull String deviceId) {
        logd(TAG, "Setting device id: " + deviceId);
        ConnectedRemoteDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null) {
            disconnectWithError("Null connected device found when device id received.");
            return;
        }

        connectedDevice.mDeviceId = deviceId;
        mCallbacks.invoke(callback -> callback.onDeviceConnected(deviceId));
    }

    private void disconnectWithError(@NonNull String errorMessage, @Nullable Exception e) {
        loge(TAG, errorMessage, e);
        if (isAssociating()) {
            mAssociationCallback.onAssociationError(DEVICE_ERROR_INVALID_HANDSHAKE);
        }
        reset();
    }

    private void disconnectWithError(@NonNull String errorMessage) {
        disconnectWithError(errorMessage, null);
    }

    private void onDeviceConnected(BluetoothDevice device, boolean isReconnect) {
        onDeviceConnected(device, isReconnect, /* isOob= */ false);
    }

    private void onDeviceConnected(BluetoothDevice device, boolean isReconnect, boolean isOob) {
        EventLog.onDeviceConnected();
        mClientDeviceAddress = device.getAddress();
        mClientDeviceName = device.getName();
        DeviceMessageStream secureStream = new SppDeviceMessageStream(mSppManager, device);
        secureStream.setMessageReceivedErrorListener(
                exception -> {
                    disconnectWithError("Error occurred in stream: " + exception.getMessage(),
                            exception);
                });
        SecureChannel secureChannel;
        // TODO(b/157492943): Define an out of band version of ReconnectSecureChannel
        if (isReconnect) {
            // TODO(b/160813572): Define a spp version of ReconnectSecureChannel
            loge(TAG, "Reconnect is currently not available for Spp");
            return;
        } else if (isOob) {
            // TODO(b/160901821): Integrate Oob with Spp channel
            loge(TAG, "Oob verification is currently not available for Spp");
            return;
        } else {
            secureChannel = new AssociationSecureChannel(secureStream, mStorage);
        }
        secureChannel.registerCallback(mSecureChannelCallback);
        ConnectedRemoteDevice connectedDevice = new ConnectedRemoteDevice(device, /* gatt= */ null);
        connectedDevice.mSecureChannel = secureChannel;
        addConnectedDevice(connectedDevice);
        if (isReconnect) {
            setDeviceIdAndNotifyCallbacks(mReconnectDeviceId);
            mReconnectDeviceId = null;
        }
    }

    private boolean isAssociating() {
        return mAssociationCallback != null;
    }

    private final SppManager.ConnectionCallback mReconnectSppCallback =
            new SppManager.ConnectionCallback() {
                @Override
                public void onRemoteDeviceConnected(BluetoothDevice device) {
                    onDeviceConnected(device, /* isReconnect= */ true);
                }

                @Override
                public void onRemoteDeviceDisconnected(BluetoothDevice device) {
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice(device);
                    // Reset before invoking callbacks to avoid a race condition with reconnect
                    // logic.
                    reset();
                    String deviceId = connectedDevice == null ? mReconnectDeviceId
                            : connectedDevice.mDeviceId;
                    if (deviceId != null) {
                        logd(TAG, "Connected device " + deviceId + " disconnected.");
                        mCallbacks.invoke(callback -> callback.onDeviceDisconnected(deviceId));
                    }
                }
            };

    private final SppManager.ConnectionCallback mAssociationSppCallback =
            new SppManager.ConnectionCallback() {
                @Override
                public void onRemoteDeviceConnected(BluetoothDevice device) {
                    onDeviceConnected(device, /* isReconnect= */ false);
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mSecureChannel == null) {
                        loge(TAG,
                                "No connected device or secure channel found when try to "
                                        + "associate.");
                        return;
                    }
                    ((AssociationSecureChannel) connectedDevice.mSecureChannel)
                            .setShowVerificationCodeListener(
                                    code -> {
                                        if (mAssociationCallback == null) {
                                            loge(TAG, "No valid callback for association.");
                                            return;
                                        }
                                        mAssociationCallback.onVerificationCodeAvailable(code);
                                    });
                }

                @Override
                public void onRemoteDeviceDisconnected(BluetoothDevice device) {
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice(device);
                    if (isAssociating()) {
                        mAssociationCallback.onAssociationError(
                                DEVICE_ERROR_UNEXPECTED_DISCONNECTION);
                    }
                    // Reset before invoking callbacks to avoid a race condition with reconnect
                    // logic.
                    reset();
                    if (connectedDevice != null && connectedDevice.mDeviceId != null) {
                        mCallbacks.invoke(callback -> callback.onDeviceDisconnected(
                                connectedDevice.mDeviceId));
                    }
                }
            };

    private final SecureChannel.Callback mSecureChannelCallback =
            new SecureChannel.Callback() {
                @Override
                public void onSecureChannelEstablished() {
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        disconnectWithError("Null device id found when secure channel "
                                + "established.");
                        return;
                    }
                    String deviceId = connectedDevice.mDeviceId;
                    if (mClientDeviceAddress == null) {
                        disconnectWithError("Null device address found when secure channel "
                                + "established.");
                        return;
                    }
                    if (isAssociating()) {
                        logd(TAG, "Secure channel established for un-associated device. Saving "
                                + "association of that device for current user.");
                        mStorage.addAssociatedDeviceForActiveUser(
                                new AssociatedDevice(deviceId, mClientDeviceAddress,
                                        mClientDeviceName, /* isConnectionEnabled= */ true));
                        if (mAssociationCallback != null) {
                            mAssociationCallback.onAssociationCompleted(deviceId);
                            mAssociationCallback = null;
                        }
                    }
                    mCallbacks.invoke(callback -> callback.onSecureChannelEstablished(deviceId));
                }

                @Override
                public void onEstablishSecureChannelFailure(int error) {
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        disconnectWithError("Null device id found when secure channel failed to "
                                + "establish.");
                        return;
                    }
                    String deviceId = connectedDevice.mDeviceId;
                    mCallbacks.invoke(callback -> callback.onSecureChannelError(deviceId));

                    if (isAssociating()) {
                        mAssociationCallback.onAssociationError(error);
                    }

                    disconnectWithError("Error while establishing secure connection.");
                }

                @Override
                public void onMessageReceived(DeviceMessage deviceMessage) {
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        disconnectWithError("Null device id found when message received.");
                        return;
                    }

                    logd(TAG, "Received new message from " + connectedDevice.mDeviceId
                            + " with " + deviceMessage.getMessage().length + " bytes in its "
                            + "payload. Notifying " + mCallbacks.size() + " callbacks.");
                    mCallbacks.invoke(
                            callback -> callback.onMessageReceived(connectedDevice.mDeviceId,
                                    deviceMessage));
                }

                @Override
                public void onMessageReceivedError(Exception exception) {
                    // TODO(b/143879960) Extend the message error from here to continue up the
                    // chain.
                    disconnectWithError("Error while receiving message.");
                }

                @Override
                public void onDeviceIdReceived(String deviceId) {
                    setDeviceIdAndNotifyCallbacks(deviceId);
                }
            };
}
