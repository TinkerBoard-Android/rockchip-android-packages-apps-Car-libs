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

package com.android.car.connecteddevice.connection.ble;

import static com.android.car.connecteddevice.ConnectedDeviceManager.DEVICE_ERROR_UNEXPECTED_DISCONNECTION;
import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;

import com.android.car.connecteddevice.AssociationCallback;
import com.android.car.connecteddevice.connection.AssociationSecureChannel;
import com.android.car.connecteddevice.connection.CarBluetoothManager;
import com.android.car.connecteddevice.connection.OobAssociationSecureChannel;
import com.android.car.connecteddevice.connection.ReconnectSecureChannel;
import com.android.car.connecteddevice.connection.SecureChannel;
import com.android.car.connecteddevice.oob.OobChannel;
import com.android.car.connecteddevice.oob.OobConnectionManager;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.car.connecteddevice.util.ByteUtils;
import com.android.car.connecteddevice.util.EventLog;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Communication manager that allows for targeted connections to a specific device in the car.
 */
public class CarBlePeripheralManager extends CarBluetoothManager {

    private static final String TAG = "CarBlePeripheralManager";

    // Attribute protocol bytes attached to message. Available write size is MTU size minus att
    // bytes.
    private static final int ATT_PROTOCOL_BYTES = 3;

    // Arbitrary delay time for a retry of association advertising if bluetooth adapter name change
    // fails.
    private static final long ASSOCIATE_ADVERTISING_DELAY_MS = 10L;

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int SALT_BYTES = 8;

    private static final int TOTAL_AD_DATA_BYTES = 16;

    private static final int TRUNCATED_BYTES = 3;

    private static final String TIMEOUT_HANDLER_THREAD_NAME = "peripheralThread";

    private final BluetoothGattDescriptor mDescriptor =
            new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG,
                    BluetoothGattDescriptor.PERMISSION_READ
                            | BluetoothGattDescriptor.PERMISSION_WRITE);

    private final ScheduledExecutorService mScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final BlePeripheralManager mBlePeripheralManager;

    private final UUID mAssociationServiceUuid;

    private final UUID mReconnectServiceUuid;

    private final UUID mReconnectDataUuid;

    private final BluetoothGattCharacteristic mWriteCharacteristic;

    private final BluetoothGattCharacteristic mReadCharacteristic;

    private HandlerThread mTimeoutHandlerThread;

    private Handler mTimeoutHandler;

    private final Duration mMaxReconnectAdvertisementDuration;

    private final int mDefaultMtuSize;

    private String mOriginalBluetoothName;

    private String mReconnectDeviceId;

    private byte[] mReconnectChallenge;

    private AdvertiseCallback mAdvertiseCallback;

    private OobConnectionManager mOobConnectionManager;

    private Future mBluetoothNameTask;

    private AssociationCallback mAssociationCallback;

    /**
     * Initialize a new instance of manager.
     *
     * @param blePeripheralManager    {@link BlePeripheralManager} for establishing connection.
     * @param connectedDeviceStorage  Shared {@link ConnectedDeviceStorage} for companion features.
     * @param associationServiceUuid  {@link UUID} of association service.
     * @param reconnectServiceUuid    {@link UUID} of reconnect service.
     * @param reconnectDataUuid       {@link UUID} key of reconnect advertisement data.
     * @param writeCharacteristicUuid {@link UUID} of characteristic the car will write to.
     * @param readCharacteristicUuid  {@link UUID} of characteristic the device will write to.
     * @param maxReconnectAdvertisementDuration Maximum duration to advertise for reconnect before
     *                                          restarting.
     * @param defaultMtuSize          Default MTU size for new channels.
     */
    public CarBlePeripheralManager(@NonNull BlePeripheralManager blePeripheralManager,
            @NonNull ConnectedDeviceStorage connectedDeviceStorage,
            @NonNull UUID associationServiceUuid,
            @NonNull UUID reconnectServiceUuid,
            @NonNull UUID reconnectDataUuid,
            @NonNull UUID writeCharacteristicUuid,
            @NonNull UUID readCharacteristicUuid,
            @NonNull Duration maxReconnectAdvertisementDuration,
            int defaultMtuSize) {
        super(connectedDeviceStorage);
        mBlePeripheralManager = blePeripheralManager;
        mAssociationServiceUuid = associationServiceUuid;
        mReconnectServiceUuid = reconnectServiceUuid;
        mReconnectDataUuid = reconnectDataUuid;
        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mWriteCharacteristic = new BluetoothGattCharacteristic(writeCharacteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_READ);
        mReadCharacteristic = new BluetoothGattCharacteristic(readCharacteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        mReadCharacteristic.addDescriptor(mDescriptor);
        mMaxReconnectAdvertisementDuration = maxReconnectAdvertisementDuration;
        mDefaultMtuSize = defaultMtuSize;
    }

    @Override
    public void start() {
        super.start();
        mTimeoutHandlerThread = new HandlerThread(TIMEOUT_HANDLER_THREAD_NAME);
        mTimeoutHandlerThread.start();
        mTimeoutHandler = new Handler(mTimeoutHandlerThread.getLooper());
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        mOriginalBluetoothName = mStorage.getStoredBluetoothName();
        if (mOriginalBluetoothName == null) {
            return;
        }
        if (mOriginalBluetoothName.equals(adapter.getName())) {
            mStorage.removeStoredBluetoothName();
            return;
        }

        logw(TAG, "Discovered mismatch in bluetooth adapter name. Resetting back to "
                + mOriginalBluetoothName + ".");
        mScheduler.schedule(
                () -> verifyBluetoothNameRestored(mOriginalBluetoothName),
                ASSOCIATE_ADVERTISING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        if (mTimeoutHandlerThread != null) {
            mTimeoutHandlerThread.quit();
        }
        reset();
    }

    @Override
    public void disconnectDevice(@NonNull String deviceId) {
        if (deviceId.equals(mReconnectDeviceId)) {
            logd(TAG, "Reconnection canceled for device " + deviceId + ".");
            reset();
            return;
        }
        ConnectedRemoteDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null || !deviceId.equals(connectedDevice.mDeviceId)) {
            return;
        }
        reset();
    }

    @Override
    public AssociationCallback getAssociationCallback() {
        return mAssociationCallback;
    }

    @Override
    public void setAssociationCallback(AssociationCallback callback) {
        mAssociationCallback = callback;
    }

    @Override
    public void reset() {
        super.reset();
        logd(TAG, "Resetting state.");
        resetBluetoothAdapterName();
        mBlePeripheralManager.cleanup();
        mReconnectDeviceId = null;
        mReconnectChallenge = null;
        mOobConnectionManager = null;
        mAssociationCallback = null;
        if (mBluetoothNameTask != null) {
            mBluetoothNameTask.cancel(true);
        }
        mBluetoothNameTask = null;
    }

    @Override
    public void connectToDevice(@NonNull UUID deviceId) {
        for (ConnectedRemoteDevice device : mConnectedDevices) {
            if (UUID.fromString(device.mDeviceId).equals(deviceId)) {
                logd(TAG, "Already connected to device " + deviceId + ".");
                // Already connected to this device. Ignore requests to connect again.
                return;
            }
        }

        // Clear any previous session before starting a new one.
        reset();
        mReconnectDeviceId = deviceId.toString();
        mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                mTimeoutHandler.postDelayed(mTimeoutRunnable,
                        mMaxReconnectAdvertisementDuration.toMillis());
                logd(TAG, "Successfully started advertising for device " + deviceId + ".");
            }
        };
        mBlePeripheralManager.unregisterCallback(mAssociationPeripheralCallback);
        mBlePeripheralManager.registerCallback(mReconnectPeripheralCallback);
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        byte[] advertiseData = createReconnectData(mReconnectDeviceId);
        if (advertiseData == null) {
            loge(TAG, "Unable to create advertisement data. Aborting reconnect.");
            return;
        }
        startAdvertising(mReconnectServiceUuid, mAdvertiseCallback, /* includeDeviceName= */ false,
                advertiseData, mReconnectDataUuid);
    }

    /**
     * Create data for reconnection advertisement.
     *
     * <p></p><p>Process:</p>
     * <ol>
     * <li>Generate random {@value SALT_BYTES} byte salt and zero-pad to
     * {@value TOTAL_AD_DATA_BYTES} bytes.
     * <li>Hash with stored challenge secret and truncate to {@value TRUNCATED_BYTES} bytes.
     * <li>Concatenate hashed {@value TRUNCATED_BYTES} bytes with salt and return.
     * </ol>
     */
    @Nullable
    private byte[] createReconnectData(String deviceId) {
        byte[] salt = ByteUtils.randomBytes(SALT_BYTES);
        byte[] zeroPadded = ByteUtils.concatByteArrays(salt,
                new byte[TOTAL_AD_DATA_BYTES - SALT_BYTES]);
        mReconnectChallenge = mStorage.hashWithChallengeSecret(deviceId, zeroPadded);
        if (mReconnectChallenge == null) {
            return null;
        }
        return ByteUtils.concatByteArrays(Arrays.copyOf(mReconnectChallenge, TRUNCATED_BYTES),
                salt);

    }

    @Override
    public void startAssociation(@NonNull String nameForAssociation,
            @NonNull AssociationCallback callback) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            loge(TAG, "Bluetooth is unavailable on this device. Unable to start associating.");
            return;
        }

        reset();
        mAssociationCallback = callback;
        if (mOriginalBluetoothName == null) {
            mOriginalBluetoothName = adapter.getName();
        }
        mStorage.storeBluetoothName(mOriginalBluetoothName);
        adapter.setName(nameForAssociation);
        logd(TAG, "Changing bluetooth adapter name from " + mOriginalBluetoothName + " to "
                + nameForAssociation + ".");
        mBlePeripheralManager.unregisterCallback(mReconnectPeripheralCallback);
        mBlePeripheralManager.registerCallback(mAssociationPeripheralCallback);
        mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                callback.onAssociationStartSuccess(nameForAssociation);
                logd(TAG, "Successfully started advertising for association.");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                callback.onAssociationStartFailure();
                logd(TAG, "Failed to start advertising for association. Error code: " + errorCode);
            }
        };
        attemptAssociationAdvertising(nameForAssociation, callback);
    }

    /** Start the association with a new device using out of band verification code exchange */
    @Override
    public void startOutOfBandAssociation(
            @NonNull String nameForAssociation,
            @NonNull OobChannel oobChannel,
            @NonNull AssociationCallback callback) {

        logd(TAG, "Starting out of band association.");
        startAssociation(nameForAssociation, new AssociationCallback() {
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

    private void attemptAssociationAdvertising(@NonNull String adapterName,
            @NonNull AssociationCallback callback) {
        if (mOriginalBluetoothName != null
                && adapterName.equals(BluetoothAdapter.getDefaultAdapter().getName())) {
            startAdvertising(mAssociationServiceUuid, mAdvertiseCallback,
                    /* includeDeviceName= */ true, /* serviceData= */ null,
                    /* serviceDataUuid= */ null);
            return;
        }

        if (mBluetoothNameTask != null) {
            mBluetoothNameTask.cancel(true);
        }
        mBluetoothNameTask = mScheduler.schedule(
                () -> attemptAssociationAdvertising(adapterName, callback),
                ASSOCIATE_ADVERTISING_DELAY_MS, TimeUnit.MILLISECONDS);
        if (mBluetoothNameTask.isCancelled()) {
            // Association failed to start.
            callback.onAssociationStartFailure();
            return;
        }
        logd(TAG, "Adapter name change has not taken affect prior to advertising attempt. Trying "
                + "again in " + ASSOCIATE_ADVERTISING_DELAY_MS + "  milliseconds.");
    }

    private void startAdvertising(@NonNull UUID serviceUuid, @NonNull AdvertiseCallback callback,
            boolean includeDeviceName, @Nullable byte[] serviceData,
            @Nullable UUID serviceDataUuid) {
        BluetoothGattService gattService = new BluetoothGattService(serviceUuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        gattService.addCharacteristic(mWriteCharacteristic);
        gattService.addCharacteristic(mReadCharacteristic);

        AdvertiseData.Builder builder = new AdvertiseData.Builder()
                .setIncludeDeviceName(includeDeviceName);
        ParcelUuid uuid = new ParcelUuid(serviceUuid);
        builder.addServiceUuid(uuid);
        if (serviceData != null) {
            ParcelUuid dataUuid = uuid;
            if (serviceDataUuid != null) {
                dataUuid = new ParcelUuid(serviceDataUuid);
            }
            builder.addServiceData(dataUuid, serviceData);
        }

        mBlePeripheralManager.startAdvertising(gattService, builder.build(), callback);
    }

    private void resetBluetoothAdapterName() {
        if (mOriginalBluetoothName == null) {
            return;
        }
        logd(TAG, "Changing bluetooth adapter name back to " + mOriginalBluetoothName + ".");
        BluetoothAdapter.getDefaultAdapter().setName(mOriginalBluetoothName);
    }

    private void verifyBluetoothNameRestored(@NonNull String expectedName) {
        String currentName = BluetoothAdapter.getDefaultAdapter().getName();
        if (expectedName.equals(currentName)) {
            logd(TAG, "Bluetooth adapter name restoration completed successfully. Removing stored "
                    + "adapter name.");
            mStorage.removeStoredBluetoothName();
            return;
        }
        // Attempting to set the name on the adapter before it is in state BT_ON will result in
        // repeated failures until a new name is attempted.
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            BluetoothAdapter.getDefaultAdapter().setName(expectedName);
        }
        logd(TAG, "Bluetooth adapter name restoration has not taken affect yet. Checking again in "
                + ASSOCIATE_ADVERTISING_DELAY_MS + " milliseconds.");
        if (mBluetoothNameTask != null) {
            mBluetoothNameTask.cancel(true);
        }
        mBluetoothNameTask = mScheduler.schedule(
                () -> verifyBluetoothNameRestored(expectedName),
                ASSOCIATE_ADVERTISING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void addConnectedDevice(BluetoothDevice device, boolean isReconnect) {
        addConnectedDevice(device, isReconnect, /* oobConnectionManager= */ null);
    }

    private void addConnectedDevice(@NonNull BluetoothDevice device, boolean isReconnect,
            @Nullable OobConnectionManager oobConnectionManager) {
        EventLog.onDeviceConnected();
        mBlePeripheralManager.stopAdvertising(mAdvertiseCallback);
        if (mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        }

        if (device.getName() == null) {
            logd(TAG, "Device connected, but name is null; issuing request to retrieve device "
                    + "name.");
            mBlePeripheralManager.retrieveDeviceName(device);
        } else {
            setClientDeviceName(device.getName());
        }
        setClientDeviceAddress(device.getAddress());

        BleDeviceMessageStream secureStream = new BleDeviceMessageStream(mBlePeripheralManager,
                device, mWriteCharacteristic, mReadCharacteristic,
                mDefaultMtuSize - ATT_PROTOCOL_BYTES);
        secureStream.setMessageReceivedErrorListener(
                exception -> {
                    disconnectWithError("Error occurred in stream: " + exception.getMessage());
                });
        SecureChannel secureChannel;
        if (isReconnect) {
            secureChannel = new ReconnectSecureChannel(secureStream, mStorage, mReconnectDeviceId,
                    mReconnectChallenge);
        } else if (oobConnectionManager != null) {
            secureChannel = new OobAssociationSecureChannel(secureStream, mStorage,
                    oobConnectionManager);
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
            mReconnectChallenge = null;
        }
    }

    private void setMtuSize(int mtuSize) {
        ConnectedRemoteDevice connectedDevice = getConnectedDevice();
        if (connectedDevice != null
                && connectedDevice.mSecureChannel != null
                && connectedDevice.mSecureChannel.getStream() != null) {
            ((BleDeviceMessageStream) connectedDevice.mSecureChannel.getStream())
                    .setMaxWriteSize(mtuSize - ATT_PROTOCOL_BYTES);
        }
    }

    private final BlePeripheralManager.Callback mReconnectPeripheralCallback =
            new BlePeripheralManager.Callback() {

                @Override
                public void onDeviceNameRetrieved(String deviceName) {
                    // Ignored.
                }

                @Override
                public void onMtuSizeChanged(int size) {
                    setMtuSize(size);
                }

                @Override
                public void onRemoteDeviceConnected(BluetoothDevice device) {
                    addConnectedDevice(device, /* isReconnect= */ true);
                }

                @Override
                public void onRemoteDeviceDisconnected(BluetoothDevice device) {
                    String deviceId = mReconnectDeviceId;
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice(device);
                    // Reset before invoking callbacks to avoid a race condition with reconnect
                    // logic.
                    reset();
                    if (connectedDevice != null) {
                        deviceId = connectedDevice.mDeviceId;
                    }
                    final String finalDeviceId = deviceId;
                    if (finalDeviceId == null) {
                        logw(TAG, "Callbacks were not issued for disconnect because the device id "
                                + "was null.");
                        return;
                    }
                    logd(TAG, "Connected device " + finalDeviceId + " disconnected.");
                    mCallbacks.invoke(callback -> callback.onDeviceDisconnected(finalDeviceId));
                }
            };

    private final BlePeripheralManager.Callback mAssociationPeripheralCallback =
            new BlePeripheralManager.Callback() {
                @Override
                public void onDeviceNameRetrieved(String deviceName) {
                    if (deviceName == null) {
                        return;
                    }
                    setClientDeviceName(deviceName);
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        return;
                    }
                    mStorage.updateAssociatedDeviceName(connectedDevice.mDeviceId, deviceName);
                }

                @Override
                public void onMtuSizeChanged(int size) {
                    setMtuSize(size);
                }

                @Override
                public void onRemoteDeviceConnected(BluetoothDevice device) {
                    resetBluetoothAdapterName();
                    addConnectedDevice(device, /* isReconnect= */ false, mOobConnectionManager);
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mSecureChannel == null) {
                        return;
                    }
                    ((AssociationSecureChannel) connectedDevice.mSecureChannel)
                            .setShowVerificationCodeListener(
                                    code -> {
                                        if (!isAssociating()) {
                                            loge(TAG, "No valid callback for association.");
                                            return;
                                        }
                                        mAssociationCallback.onVerificationCodeAvailable(code);
                                    });
                }

                @Override
                public void onRemoteDeviceDisconnected(BluetoothDevice device) {
                    logd(TAG, "Remote device disconnected.");
                    ConnectedRemoteDevice connectedDevice = getConnectedDevice(device);
                    if (isAssociating()) {
                        mAssociationCallback.onAssociationError(
                                DEVICE_ERROR_UNEXPECTED_DISCONNECTION);
                    }
                    // Reset before invoking callbacks to avoid a race condition with reconnect
                    // logic.
                    reset();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        logw(TAG, "Callbacks were not issued for disconnect.");
                        return;
                    }
                    mCallbacks.invoke(callback -> callback.onDeviceDisconnected(
                            connectedDevice.mDeviceId));
                }
            };

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            logd(TAG, "Timeout period expired without a connection. Restarting advertisement.");
            mBlePeripheralManager.stopAdvertising(mAdvertiseCallback);
            connectToDevice(UUID.fromString(mReconnectDeviceId));
        }
    };
}
