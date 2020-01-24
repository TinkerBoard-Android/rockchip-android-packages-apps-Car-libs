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

package com.android.car.connecteddevice.ble;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;

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
import android.car.encryptionrunner.EncryptionRunnerFactory;
import android.os.ParcelUuid;

import com.android.car.connecteddevice.AssociationCallback;
import com.android.car.connecteddevice.model.AssociatedDevice;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.internal.annotations.VisibleForTesting;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Communication manager that allows for targeted connections to a specific device in the car.
 */
public class CarBlePeripheralManager extends CarBleManager {

    private static final String TAG = "CarBlePeripheralManager";

    // Attribute protocol bytes attached to message. Available write size is MTU size minus att
    // bytes.
    private static final int ATT_PROTOCOL_BYTES = 3;

    // Arbitrary delay time for a retry of association advertising if bluetooth adapter name change
    // fails.
    private static final long ASSOCIATE_ADVERTISING_DELAY_MS = 10L;

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BluetoothGattDescriptor mDescriptor =
            new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG,
                    BluetoothGattDescriptor.PERMISSION_READ
                            | BluetoothGattDescriptor.PERMISSION_WRITE);

    private final ScheduledExecutorService mScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final BlePeripheralManager mBlePeripheralManager;

    private final UUID mAssociationServiceUuid;

    private final BluetoothGattCharacteristic mWriteCharacteristic;

    private final BluetoothGattCharacteristic mReadCharacteristic;

    // BLE default is 23, minus 3 bytes for ATT_PROTOCOL.
    private int mWriteSize = 20;

    private String mOriginalBluetoothName;

    private String mClientDeviceName;

    private String mClientDeviceAddress;

    private AssociationCallback mAssociationCallback;

    private AdvertiseCallback mAdvertiseCallback;

    /**
     * Initialize a new instance of manager.
     *
     * @param blePeripheralManager {@link BlePeripheralManager} for establishing connection.
     * @param connectedDeviceStorage Shared {@link ConnectedDeviceStorage} for companion features.
     * @param associationServiceUuid {@link UUID} of association service.
     * @param writeCharacteristicUuid {@link UUID} of characteristic the car will write to.
     * @param readCharacteristicUuid {@link UUID} of characteristic the device will write to.
     */
    public CarBlePeripheralManager(@NonNull BlePeripheralManager blePeripheralManager,
            @NonNull ConnectedDeviceStorage connectedDeviceStorage,
            @NonNull UUID associationServiceUuid, @NonNull UUID writeCharacteristicUuid,
            @NonNull UUID readCharacteristicUuid) {
        super(connectedDeviceStorage);
        mBlePeripheralManager = blePeripheralManager;
        mAssociationServiceUuid = associationServiceUuid;
        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mWriteCharacteristic = new BluetoothGattCharacteristic(writeCharacteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_READ);
        mReadCharacteristic = new BluetoothGattCharacteristic(readCharacteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        mReadCharacteristic.addDescriptor(mDescriptor);
    }

    @Override
    public void stop() {
        super.stop();
        reset();
    }

    private void reset() {
        resetBluetoothAdapterName();
        mClientDeviceAddress = null;
        mClientDeviceName = null;
        mAssociationCallback = null;
        mBlePeripheralManager.cleanup();
        mConnectedDevices.clear();
    }

    /** Connect to device with provided id. */
    public void connectToDevice(@NonNull UUID deviceId) {
        for (BleDevice device : mConnectedDevices) {
            if (UUID.fromString(device.mDeviceId).equals(deviceId)) {
                // Already connected to this device. Ignore requests to connect again.
                return;
            }
        }

        // Clear any previous session before starting a new one.
        reset();

        mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                logd(TAG, "Successfully started advertising for device " + deviceId + ".");
            }
        };
        mBlePeripheralManager.registerCallback(mReconnectPeripheralCallback);
        startAdvertising(deviceId, mAdvertiseCallback, /* includeDeviceName = */ false);
    }

    @Nullable
    private BleDevice getConnectedDevice() {
        if (mConnectedDevices.isEmpty()) {
            return null;
        }
        return mConnectedDevices.iterator().next();
    }

    /** Start the association with a new device */
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
        adapter.setName(nameForAssociation);
        logd(TAG, "Changing bluetooth adapter name from " + mOriginalBluetoothName + " to "
                + nameForAssociation + ".");
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

    /** Stop the association with any device. */
    public void stopAssociation(@NonNull AssociationCallback callback) {
        if (!isAssociating() || callback != mAssociationCallback) {
            return;
        }
        reset();
    }

    private void attemptAssociationAdvertising(@NonNull String adapterName,
            @NonNull AssociationCallback callback) {
        if (mOriginalBluetoothName != null
                && adapterName.equals(BluetoothAdapter.getDefaultAdapter().getName())) {
            startAdvertising(mAssociationServiceUuid, mAdvertiseCallback,
                    /* includeDeviceName = */ true);
            return;
        }

        ScheduledFuture future = mScheduler.schedule(
                () -> attemptAssociationAdvertising(adapterName, callback),
                ASSOCIATE_ADVERTISING_DELAY_MS, TimeUnit.MILLISECONDS);
        if (future.isCancelled()) {
            // Association failed to start.
            callback.onAssociationStartFailure();
            return;
        }
        logd(TAG, "Adapter name change has not taken affect prior to advertising attempt. Trying "
                + "again in " + ASSOCIATE_ADVERTISING_DELAY_MS + "  milliseconds.");
    }

    private void startAdvertising(@NonNull UUID serviceUuid, @NonNull AdvertiseCallback callback,
            boolean includeDeviceName) {
        BluetoothGattService gattService = new BluetoothGattService(serviceUuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        gattService.addCharacteristic(mWriteCharacteristic);
        gattService.addCharacteristic(mReadCharacteristic);

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(includeDeviceName)
                .addServiceUuid(new ParcelUuid(serviceUuid))
                .build();
        mBlePeripheralManager.startAdvertising(gattService, advertiseData, callback);
    }

    /** Notify that the user has accepted a pairing code or other out-of-band confirmation. */
    public void notifyOutOfBandAccepted() {
        if (getConnectedDevice() == null) {
            disconnectWithError("Null connected device found when out-of-band confirmation "
                    + "received.");
            return;
        }

        SecureBleChannel secureChannel = getConnectedDevice().mSecureChannel;
        if (secureChannel == null) {
            disconnectWithError("Null SecureBleChannel found for the current connected device "
                    + "when out-of-band confirmation received.");
            return;
        }

        secureChannel.notifyOutOfBandAccepted();
    }

    @VisibleForTesting
    @Nullable
    SecureBleChannel getConnectedDeviceChannel() {
        BleDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null) {
            return null;
        }

        return connectedDevice.mSecureChannel;
    }

    private void setDeviceId(@NonNull String deviceId) {
        logd(TAG, "Setting device id: " + deviceId);
        BleDevice connectedDevice = getConnectedDevice();
        if (connectedDevice == null) {
            disconnectWithError("Null connected device found when device id received.");
            return;
        }

        connectedDevice.mDeviceId = deviceId;
        mCallbacks.invoke(callback -> callback.onDeviceConnected(deviceId));
    }

    private void disconnectWithError(@NonNull String errorMessage) {
        loge(TAG, errorMessage);
        reset();
    }

    private void resetBluetoothAdapterName() {
        if (mOriginalBluetoothName == null) {
            return;
        }
        logd(TAG, "Changing bluetooth adapter name back to " + mOriginalBluetoothName + ".");
        BluetoothAdapter.getDefaultAdapter().setName(mOriginalBluetoothName);
        mOriginalBluetoothName = null;
    }

    private void addConnectedDevice(BluetoothDevice device, boolean isReconnect) {
        mBlePeripheralManager.stopAdvertising(mAdvertiseCallback);
        mClientDeviceAddress = device.getAddress();
        mClientDeviceName = device.getName();
        if (mClientDeviceName == null) {
            logd(TAG, "Device connected, but name is null; issuing request to retrieve device "
                    + "name.");
            mBlePeripheralManager.retrieveDeviceName(device);
        }

        BleDeviceMessageStream secureStream = new BleDeviceMessageStream(mBlePeripheralManager,
                device, mWriteCharacteristic, mReadCharacteristic);
        secureStream.setMaxWriteSize(mWriteSize);
        SecureBleChannel secureChannel = new SecureBleChannel(secureStream, mStorage, isReconnect,
                EncryptionRunnerFactory.newRunner());
        secureChannel.registerCallback(mSecureChannelCallback);
        BleDevice bleDevice = new BleDevice(device, /* gatt = */ null);
        bleDevice.mSecureChannel = secureChannel;
        addConnectedDevice(bleDevice);
    }

    private void setMtuSize(int mtuSize) {
        mWriteSize = mtuSize - ATT_PROTOCOL_BYTES;
        BleDevice connectedDevice = getConnectedDevice();
        if (connectedDevice != null
                && connectedDevice.mSecureChannel != null
                && connectedDevice.mSecureChannel.getStream() != null) {
            connectedDevice.mSecureChannel.getStream().setMaxWriteSize(mWriteSize);
        }
    }

    private boolean isAssociating() {
        return mAssociationCallback != null;
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
                    String deviceId = null;
                    BleDevice connectedDevice = getConnectedDevice(device);
                    if (connectedDevice != null) {
                        deviceId = connectedDevice.mDeviceId;
                    }
                    final String finalDeviceId = deviceId;
                    if (finalDeviceId != null) {
                        mCallbacks.invoke(callback -> callback.onDeviceDisconnected(finalDeviceId));
                    }
                    reset();
                }
            };

    private final BlePeripheralManager.Callback mAssociationPeripheralCallback =
            new BlePeripheralManager.Callback() {
                @Override
                public void onDeviceNameRetrieved(String deviceName) {
                    if (deviceName == null) {
                        return;
                    }
                    mClientDeviceName = deviceName;
                    BleDevice connectedDevice = getConnectedDevice();
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
                    addConnectedDevice(device, /* isReconnect = */ false);
                    BleDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mSecureChannel == null) {
                        return;
                    }
                    connectedDevice.mSecureChannel.setShowVerificationCodeListener(
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
                    BleDevice connectedDevice = getConnectedDevice(device);
                    if (connectedDevice != null && connectedDevice.mDeviceId != null) {
                        mCallbacks.invoke(callback -> callback.onDeviceDisconnected(
                                connectedDevice.mDeviceId));
                    }
                    reset();
                }
            };

    private final SecureBleChannel.Callback mSecureChannelCallback =
            new SecureBleChannel.Callback() {
                @Override
                public void onSecureChannelEstablished() {
                    BleDevice connectedDevice = getConnectedDevice();
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
                                        mClientDeviceName));
                        if (mAssociationCallback != null) {
                            mAssociationCallback.onAssociationCompleted(deviceId);
                            mAssociationCallback = null;
                        }
                    }
                    mCallbacks.invoke(callback -> callback.onSecureChannelEstablished(deviceId));
                }

                @Override
                public void onEstablishSecureChannelFailure(int error) {
                    BleDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        disconnectWithError("Null device id found when secure channel failed to "
                                + "establish.");
                        return;
                    }
                    String deviceId = connectedDevice.mDeviceId;
                    mCallbacks.invoke(callback -> callback.onSecureChannelError(deviceId));

                    if (isAssociating()) {
                        mAssociationCallback.onAssociationError(error);
                        disconnectWithError("Error while establishing secure connection.");
                    }
                }

                @Override
                public void onMessageReceived(DeviceMessage deviceMessage) {
                    BleDevice connectedDevice = getConnectedDevice();
                    if (connectedDevice == null || connectedDevice.mDeviceId == null) {
                        disconnectWithError("Null device id found when message received.");
                        return;
                    }

                    logd(TAG, "Received new message from " + connectedDevice.mDeviceId
                            + " with " + deviceMessage.getMessage().length + " in its payload. "
                            + "Notifying " + mCallbacks.size() + " callbacks.");
                    mCallbacks.invoke(
                            callback ->callback.onMessageReceived(connectedDevice.mDeviceId,
                                    deviceMessage));
                }

                @Override
                public void onMessageReceivedError(Exception exception) {
                    // TODO(b/143879960) Extend the message error from here to continue up the
                    // chain.
                }

                @Override
                public void onDeviceIdReceived(String deviceId) {
                    setDeviceId(deviceId);
                }
            };
}
