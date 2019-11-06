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

package com.android.car.connecteddevice;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;

import com.android.car.connecteddevice.ble.BleCentralManager;
import com.android.car.connecteddevice.ble.BlePeripheralManager;
import com.android.car.connecteddevice.ble.CarBleCentralManager;
import com.android.car.connecteddevice.ble.CarBleManager;
import com.android.car.connecteddevice.ble.CarBlePeripheralManager;
import com.android.car.connecteddevice.ble.DeviceMessage;
import com.android.car.connecteddevice.model.ConnectedDevice;
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage;
import com.android.car.connecteddevice.util.ThreadSafeCallbacks;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/** Manager of devices connected to the car. */
public class ConnectedDeviceManager {

    private static final String TAG = "ConnectedDeviceManager";

    private final CarCompanionDeviceStorage mStorage;

    private final CarBleCentralManager mCentralManager;

    private final CarBlePeripheralManager mPeripheralManager;

    private final ThreadSafeCallbacks<ConnectionCallback> mActiveUserConnectionCallbacks =
            new ThreadSafeCallbacks<>();

    private final ThreadSafeCallbacks<ConnectionCallback> mAllUserConnectionCallbacks =
            new ThreadSafeCallbacks<>();

    // deviceId -> (recipientId -> callbacks)
    private final Map<String, Map<UUID, ThreadSafeCallbacks<DeviceCallback>>> mDeviceCallbacks =
            new ConcurrentHashMap<>();

    // deviceId -> device
    private final Map<String, InternalConnectedDevice> mConnectedDevices =
            new ConcurrentHashMap<>();

    // Recipient ids that received multiple callback registrations indicate that the recipient id
    // has been compromised. Another party now has access the messages intended for that recipient.
    // As a safeguard, that recipient id will be added to this list and blocked from further
    // callback notifications.
    private final Set<UUID> mBlacklistedRecipients = new CopyOnWriteArraySet<>();

    private final Lock mLock = new ReentrantLock();
    @GuardedBy("mLock")
    private volatile boolean mIsConnectingToUserDevice = false;

    @Retention(SOURCE)
    @IntDef(prefix = { "DEVICE_ERROR_" },
            value = {
                    DEVICE_ERROR_INVALID_SECURITY_KEY,
                    DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED
            }
    )
    public @interface DeviceError {}
    public static final int DEVICE_ERROR_INVALID_SECURITY_KEY = 0;
    public static final int DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED = 1;

    public ConnectedDeviceManager(@NonNull Context context) {
        this(context, new CarCompanionDeviceStorage(context), new BleCentralManager(context),
                new BlePeripheralManager(context),
                UUID.fromString(context.getString(R.string.car_service_uuid)),
                context.getString(R.string.car_bg_mask),
                UUID.fromString(context.getString(R.string.car_secure_write_uuid)),
                UUID.fromString(context.getString(R.string.car_secure_read_uuid)));
    }

    private ConnectedDeviceManager(
            @NonNull Context context,
            @NonNull CarCompanionDeviceStorage storage,
            @NonNull BleCentralManager bleCentralManager,
            @NonNull BlePeripheralManager blePeripheralManager,
            @NonNull UUID serviceUuid,
            @NonNull String bgMask,
            @NonNull UUID writeCharacteristicUuid,
            @NonNull UUID readCharacteristicUuid) {
        this(storage,
                new CarBleCentralManager(context, bleCentralManager, storage, serviceUuid, bgMask,
                        writeCharacteristicUuid, readCharacteristicUuid),
                new CarBlePeripheralManager(blePeripheralManager, storage, writeCharacteristicUuid,
                        readCharacteristicUuid));
    }

    @VisibleForTesting
    ConnectedDeviceManager(
            @NonNull CarCompanionDeviceStorage storage,
            @NonNull CarBleCentralManager centralManager,
            @NonNull CarBlePeripheralManager peripheralManager) {
        Executor callbackExecutor = Executors.newSingleThreadExecutor();
        mStorage = storage;
        mCentralManager = centralManager;
        mPeripheralManager = peripheralManager;
        mCentralManager.registerCallback(generateCarBleCallback(centralManager), callbackExecutor);
        mPeripheralManager.registerCallback(generateCarBleCallback(peripheralManager),
                callbackExecutor);
    }

    /**
     * Start internal processes and begin discovering devices. Must be called before any
     * connections can be made using {@link #connectToActiveUserDevice()}.
     */
    public void start() {
        logd(TAG, "Starting ConnectedDeviceManager.");
        mCentralManager.start();
        mPeripheralManager.start();
    }

    /** Clean up internal processes and disconnect any active connections. */
    public void cleanup() {
        logd(TAG, "Cleaning up ConnectedDeviceManager.");
        mCentralManager.stop();
        mPeripheralManager.stop();
        mDeviceCallbacks.clear();
        mActiveUserConnectionCallbacks.clear();
        mAllUserConnectionCallbacks.clear();
    }

    /** Returns {@link List<ConnectedDevice>} of devices currently connected. */
    @NonNull
    public List<ConnectedDevice> getActiveUserConnectedDevices() {
        List<ConnectedDevice> activeUserConnectedDevices = new ArrayList<>();
        for (InternalConnectedDevice device : mConnectedDevices.values()) {
            if (device.mConnectedDevice.getBelongsToActiveUser()) {
                activeUserConnectedDevices.add(device.mConnectedDevice);
            }
        }
        return activeUserConnectedDevices;
    }

    /**
     * Register a callback for manager triggered connection events for only the currently active
     * user's devices.
     *
     * @param callback {@link ConnectionCallback} to register.
     * @param executor {@link Executor} to execute triggers on.
     */
    public void registerActiveUserConnectionCallback(@NonNull ConnectionCallback callback,
            @NonNull @CallbackExecutor Executor executor) {
        mActiveUserConnectionCallbacks.add(callback, executor);
    }

    /**
     * Unregister a connection callback from manager.
     *
     * @param callback {@link ConnectionCallback} to unregister.
     */
    public void unregisterConnectionCallback(ConnectionCallback callback) {
        mActiveUserConnectionCallbacks.remove(callback);
        mAllUserConnectionCallbacks.remove(callback);
    }

    /** Connect to a device for the active user if available. */
    public void connectToActiveUserDevice() {
        mLock.lock();
        try {
            if (mIsConnectingToUserDevice) {
                // Already connecting, no further action needed.
                return;
            }
            List<String> userDeviceIds = getActiveUserDeviceIds();
            if (userDeviceIds.isEmpty()) {
                logw(TAG, "No devices associated with active user. Ignoring.");
                return;
            }
            // Only currently support one device per user for fast association, so take the first
            // one.
            String userDeviceId  = userDeviceIds.get(0);
            if (mConnectedDevices.containsKey(userDeviceId)) {
                // Device has already connected, no further action needed.
                return;
            }
            mIsConnectingToUserDevice = true;
            mPeripheralManager.connectToDevice(UUID.fromString(userDeviceId));
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Register a callback for a specific device and recipient.
     *
     * @param device {@link ConnectedDevice} to register triggers on.
     * @param recipientId {@link UUID} to register as recipient of.
     * @param callback {@link DeviceCallback} to register.
     * @param executor {@link Executor} on which to execute callback.
     */
    public void registerDeviceCallback(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull DeviceCallback callback, @NonNull @CallbackExecutor Executor executor) {
        if (isRecipientBlacklisted(recipientId)) {
            notifyOfBlacklisting(device, recipientId, callback, executor);
            return;
        }
        logd(TAG, "New callback registered on device " + device.getDeviceId() + " for recipient "
                + recipientId);
        String deviceId = device.getDeviceId();
        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> recipientCallbacks =
                mDeviceCallbacks.computeIfAbsent(deviceId, key -> new HashMap<>());

        // Device already has a callback registered with this recipient UUID. For the
        // protection of the user, this UUID is now blacklisted from future subscriptions
        // and the original subscription is notified and removed.
        if (recipientCallbacks.containsKey(recipientId)) {
            blacklistRecipient(deviceId, recipientId);
            notifyOfBlacklisting(device, recipientId, callback, executor);
            return;
        }

        ThreadSafeCallbacks<DeviceCallback> newCallbacks = new ThreadSafeCallbacks<>();
        newCallbacks.add(callback, executor);
        recipientCallbacks.put(recipientId, newCallbacks);
    }

    private void notifyOfBlacklisting(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull DeviceCallback callback, @NonNull Executor executor) {
        loge(TAG, "Multiple callbacks registered for recipient " + recipientId + "! Your "
                + "recipient id is no longer secure and has been blocked from future use.");
        executor.execute(() ->
                callback.onDeviceError(device, DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED));
    }

    /**
     * Unregister callback from device events.
     *
     * @param device {@link ConnectedDevice} callback was registered on.
     * @param recipientId {@link UUID} callback was registered under.
     * @param callback {@link DeviceCallback} to unregister.
     */
    public void unregisterDeviceCallback(@NonNull ConnectedDevice device,
            @NonNull UUID recipientId, @NonNull DeviceCallback callback) {
        logd(TAG, "Device callback unregistered on device " + device.getDeviceId() + " for "
                + "recipient " + recipientId + ".");

        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> recipientCallbacks =
                mDeviceCallbacks.get(device.getDeviceId());
        if (recipientCallbacks == null) {
            return;
        }
        ThreadSafeCallbacks<DeviceCallback> callbacks = recipientCallbacks.get(recipientId);
        if (callbacks == null) {
            return;
        }

        callbacks.remove(callback);
        if (callbacks.size() == 0) {
            recipientCallbacks.remove(recipientId);
        }
    }

    /**
     * Securely send message to a device.
     *
     * @param device {@link ConnectedDevice} to send the message to.
     * @param recipientId Recipient {@link UUID}.
     * @param message Message to send.
     * @throws IllegalStateException Secure channel has not been established.
     */
    public void sendMessageSecurely(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull byte[] message) throws IllegalStateException {
        sendMessage(device, recipientId, message, /* isEncrypted = */ true);
    }

    /**
     * Send an unencrypted message to a device.
     *
     * @param device {@link ConnectedDevice} to send the message to.
     * @param recipientId Recipient {@link UUID}.
     * @param message Message to send.
     */
    public void sendMessageUnsecurely(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull byte[] message) {
        sendMessage(device, recipientId, message, /* isEncrypted = */ false);
    }

    private void sendMessage(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull byte[] message, boolean isEncrypted) throws IllegalStateException {
        String deviceId = device.getDeviceId();
        logd(TAG, "Sending new message to device $deviceId for " + recipientId + " containing "
                + message.length + ". Message will be sent securely: " + isEncrypted + ".");

        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice == null) {
            loge(TAG, "Attempted to send message to unknown device " + deviceId + ". Ignoring.");
            return;
        }

        if (isEncrypted && !connectedDevice.mConnectedDevice.getHasSecureChannel()) {
            throw new IllegalStateException("Cannot send a message securely to device that has not "
                    + "established a secure channel.");
        }

        connectedDevice.mCarBleManager.sendMessage(deviceId,
                new DeviceMessage(recipientId, isEncrypted, message));
    }

    private boolean isRecipientBlacklisted(UUID recipientId) {
        return mBlacklistedRecipients.contains(recipientId);
    }

    private void blacklistRecipient(@NonNull String deviceId, @NonNull UUID recipientId) {
        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> recipientCallbacks =
                mDeviceCallbacks.get(deviceId);
        if (recipientCallbacks == null) {
            // Should never happen, but null-safety check.
            return;
        }

        ThreadSafeCallbacks<DeviceCallback> existingCallback = recipientCallbacks.get(recipientId);
        if (existingCallback == null) {
            // Should never happen, but null-safety check.
            return;
        }

        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice != null) {
            recipientCallbacks.get(recipientId).invoke(
                    callback ->
                            callback.onDeviceError(connectedDevice.mConnectedDevice,
                                    DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED)
            );
        }

        recipientCallbacks.remove(recipientId);
        mBlacklistedRecipients.add(recipientId);
    }

    @VisibleForTesting
    void addConnectedDevice(@NonNull String deviceId, @NonNull CarBleManager bleManager) {
        if (mConnectedDevices.containsKey(deviceId)) {
            // Device already connected. No-op until secure channel established.
            return;
        }
        logd(TAG, "New device with id " + deviceId + " connected.");
        ConnectedDevice connectedDevice = new ConnectedDevice(deviceId, /* deviceName = */ null,
                getActiveUserDeviceIds().contains(deviceId), /* hasSecureChannel = */ false);

        mConnectedDevices.put(deviceId, new InternalConnectedDevice(connectedDevice, bleManager));
        invokeConnectionCallbacks(connectedDevice.getBelongsToActiveUser(),
                callback -> callback.onDeviceConnected(connectedDevice));
    }

    @VisibleForTesting
    void removeConnectedDevice(@NonNull String deviceId, @NonNull CarBleManager bleManager) {
        InternalConnectedDevice connectedDevice = getConnectedDeviceForManager(deviceId,
                bleManager);
        if (connectedDevice != null) {
            mConnectedDevices.remove(deviceId);
            invokeConnectionCallbacks(connectedDevice.mConnectedDevice.getBelongsToActiveUser(),
                    callback -> callback.onDeviceDisconnected(connectedDevice.mConnectedDevice));
        }

        // If disconnect happened on peripheral, open for future requests to connect.
        if (bleManager == mPeripheralManager) {
            mLock.lock();
            try {
                mIsConnectingToUserDevice = false;
            } finally {
                mLock.unlock();
            }
        }
    }

    @VisibleForTesting
    void onSecureChannelEstablished(@NonNull String deviceId,
            @NonNull CarBleManager bleManager) {
        if (mConnectedDevices.get(deviceId) == null) {
            loge(TAG, "Secure channel established on unknown device " + deviceId + ".");
            return;
        }
        ConnectedDevice connectedDevice = mConnectedDevices.get(deviceId).mConnectedDevice;
        ConnectedDevice updatedConnectedDevice = new ConnectedDevice(connectedDevice.getDeviceId(),
                connectedDevice.getDeviceName(), connectedDevice.getBelongsToActiveUser(),
                /* hasSecureChannel = */ true);

        boolean notifyCallbacks = getConnectedDeviceForManager(deviceId, bleManager) != null;

        // TODO (b/143088482) Implement interrupt
        // Ignore if central already holds the active device connection and interrupt the
        // connection.

        mConnectedDevices.put(deviceId,
                new InternalConnectedDevice(updatedConnectedDevice, bleManager));
        logd(TAG, "Secure channel established to $deviceId. Notifying callbacks: "
                + notifyCallbacks + ".");
        if (notifyCallbacks) {
            notifyAllDeviceCallbacks(deviceId,
                    callback -> callback.onSecureChannelEstablished(updatedConnectedDevice));
        }
    }

    @VisibleForTesting
    void onMessageReceived(@NonNull String deviceId, @NonNull DeviceMessage message) {
        logd(TAG, "New message received from device $deviceId intended for "
                + message.getRecipient() + "containing " + message.getMessage().length + " bytes.");

        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice == null) {
            logw(TAG, "Received message from unknown device " + deviceId + "or to unknown "
                    + "recipient " + message.getRecipient() + ".");
            return;
        }
        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> deviceCallbacks =
                mDeviceCallbacks.get(deviceId);
        if (deviceCallbacks == null) {
            return;
        }
        ThreadSafeCallbacks<DeviceCallback> recipientCallbacks =
                deviceCallbacks.get(message.getRecipient());
        if (recipientCallbacks == null) {
            return;
        }

        recipientCallbacks.invoke(
                callback -> callback.onMessageReceived(connectedDevice.mConnectedDevice,
                        message.getMessage()));
    }

    @VisibleForTesting
    void deviceErrorOccurred(@NonNull String deviceId) {
        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice == null) {
            logw(TAG, "Failed to establish secure channel on unknown device " + deviceId + ".");
            return;
        }

        notifyAllDeviceCallbacks(deviceId,
                callback -> callback.onDeviceError(connectedDevice.mConnectedDevice,
                        DEVICE_ERROR_INVALID_SECURITY_KEY));
    }

    @NonNull
    private List<String> getActiveUserDeviceIds() {
        return mStorage.getTrustedDevicesForUser(ActivityManager.getCurrentUser());
    }

    @Nullable
    private InternalConnectedDevice getConnectedDeviceForManager(@NonNull String deviceId,
            @NonNull CarBleManager bleManager) {
        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice != null && connectedDevice.mCarBleManager == bleManager) {
            return connectedDevice;
        }

        return null;
    }

    private void invokeConnectionCallbacks(boolean belongsToActiveUser,
            @NonNull Consumer<ConnectionCallback> notification) {
        logd(TAG, "Notifying connection callbacks for device belonging to active user "
                + belongsToActiveUser + ".");
        if (belongsToActiveUser) {
            mActiveUserConnectionCallbacks.invoke(notification);
        }
        mAllUserConnectionCallbacks.invoke(notification);
    }

    private void notifyAllDeviceCallbacks(@NonNull String deviceId,
            @NonNull Consumer<DeviceCallback> notification) {
        logd(TAG, "Notifying all device callbacks for device " + deviceId + ".");
        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> deviceCallbacks =
                mDeviceCallbacks.get(deviceId);
        if (deviceCallbacks == null) {
            return;
        }

        for (ThreadSafeCallbacks<DeviceCallback> callbacks : deviceCallbacks.values()) {
            callbacks.invoke(notification);
        }
    }

    @NonNull
    private CarBleManager.Callback generateCarBleCallback(@NonNull CarBleManager carBleManager) {
        return new CarBleManager.Callback() {
            @Override
            public void onDeviceConnected(String deviceId) {
                addConnectedDevice(deviceId, carBleManager);
            }

            @Override
            public void onDeviceDisconnected(String deviceId) {
                removeConnectedDevice(deviceId, carBleManager);
            }

            @Override
            public void onSecureChannelEstablished(String deviceId) {
                ConnectedDeviceManager.this.onSecureChannelEstablished(deviceId, carBleManager);
            }

            @Override
            public void onMessageReceived(String deviceId, DeviceMessage message) {
                ConnectedDeviceManager.this.onMessageReceived(deviceId, message);
            }

            @Override
            public void onSecureChannelError(String deviceId) {
                deviceErrorOccurred(deviceId);
            }
        };
    }

    /** Callback for triggered connection events from {@link ConnectedDeviceManager}. */
    public interface ConnectionCallback {
        /** Triggered when a new device has connected. */
        void onDeviceConnected(@NonNull ConnectedDevice device);

        /** Triggered when a device has disconnected. */
        void onDeviceDisconnected(@NonNull ConnectedDevice device);
    }

    /** Triggered device events for a connected device from {@link ConnectedDeviceManager}. */
    public interface DeviceCallback {
        /**
         * Triggered when secure channel has been established on a device. Encrypted messaging now
         * available.
         */
        void onSecureChannelEstablished(@NonNull ConnectedDevice device);

        /** Triggered when a new message is received from a device. */
        void onMessageReceived(@NonNull ConnectedDevice device, @NonNull byte[] message);

        /** Triggered when an error has occurred for a device. */
        void onDeviceError(@NonNull ConnectedDevice device, @DeviceError int error);
    }

    private static class InternalConnectedDevice {
        private final ConnectedDevice mConnectedDevice;
        private final CarBleManager mCarBleManager;

        InternalConnectedDevice(@NonNull ConnectedDevice connectedDevice,
                @NonNull CarBleManager carBleManager) {
            mConnectedDevice = connectedDevice;
            mCarBleManager = carBleManager;
        }
    }
}
