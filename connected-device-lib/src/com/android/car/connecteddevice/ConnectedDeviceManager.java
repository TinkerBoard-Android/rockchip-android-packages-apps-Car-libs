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
import com.android.car.connecteddevice.model.AssociatedDevice;
import com.android.car.connecteddevice.model.ConnectedDevice;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.car.connecteddevice.util.ByteUtils;
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

    // Device name length is limited by available bytes in BLE advertisement data packet.
    //
    // BLE advertisement limits data packet length to 31
    // Currently we send:
    // - 18 bytes for 16 chars UUID: 16 bytes + 2 bytes for header;
    // - 3 bytes for advertisement being connectable;
    // which leaves 10 bytes.
    // Subtracting 2 bytes used by header, we have 8 bytes for device name.
    private static final int DEVICE_NAME_LENGTH_LIMIT = 8;

    private final ConnectedDeviceStorage mStorage;

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

    // recipientId -> (deviceId -> message bytes)
    private final Map<UUID, Map<String, byte[]>> mRecipientMissedMessages =
            new ConcurrentHashMap<>();

    // Recipient ids that received multiple callback registrations indicate that the recipient id
    // has been compromised. Another party now has access the messages intended for that recipient.
    // As a safeguard, that recipient id will be added to this list and blocked from further
    // callback notifications.
    private final Set<UUID> mBlacklistedRecipients = new CopyOnWriteArraySet<>();

    private final Lock mLock = new ReentrantLock();
    @GuardedBy("mLock")
    private volatile boolean mIsConnectingToUserDevice = false;

    @GuardedBy("mLock")
    private volatile boolean mIsStarted = false;

    // True if a request to connect came in before manager has started.
    @GuardedBy("mLock")
    private volatile boolean mHasDelayedConnectionRequest = false;

    private String mNameForAssociation;

    private AssociationCallback mAssociationCallback;

    @Retention(SOURCE)
    @IntDef(prefix = { "DEVICE_ERROR_" },
            value = {
                    DEVICE_ERROR_INVALID_HANDSHAKE,
                    DEVICE_ERROR_INVALID_MSG,
                    DEVICE_ERROR_INVALID_DEVICE_ID,
                    DEVICE_ERROR_INVALID_VERIFICATION,
                    DEVICE_ERROR_INVALID_CHANNEL_STATE,
                    DEVICE_ERROR_INVALID_ENCRYPTION_KEY,
                    DEVICE_ERROR_STORAGE_FAILURE,
                    DEVICE_ERROR_INVALID_SECURITY_KEY,
                    DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED
            }
    )
    public @interface DeviceError {}
    public static final int DEVICE_ERROR_INVALID_HANDSHAKE = 0;
    public static final int DEVICE_ERROR_INVALID_MSG = 1;
    public static final int DEVICE_ERROR_INVALID_DEVICE_ID = 2;
    public static final int DEVICE_ERROR_INVALID_VERIFICATION = 3;
    public static final int DEVICE_ERROR_INVALID_CHANNEL_STATE = 4;
    public static final int DEVICE_ERROR_INVALID_ENCRYPTION_KEY = 5;
    public static final int DEVICE_ERROR_STORAGE_FAILURE = 6;
    public static final int DEVICE_ERROR_INVALID_SECURITY_KEY = 7;
    public static final int DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED = 8;

    public ConnectedDeviceManager(@NonNull Context context) {
        this(context, new ConnectedDeviceStorage(context), new BleCentralManager(context),
                new BlePeripheralManager(context),
                UUID.fromString(context.getString(R.string.car_service_uuid)),
                UUID.fromString(context.getString(R.string.car_association_service_uuid)),
                context.getString(R.string.car_bg_mask),
                UUID.fromString(context.getString(R.string.car_secure_write_uuid)),
                UUID.fromString(context.getString(R.string.car_secure_read_uuid)));
    }

    private ConnectedDeviceManager(
            @NonNull Context context,
            @NonNull ConnectedDeviceStorage storage,
            @NonNull BleCentralManager bleCentralManager,
            @NonNull BlePeripheralManager blePeripheralManager,
            @NonNull UUID serviceUuid,
            @NonNull UUID associationServiceUuid,
            @NonNull String bgMask,
            @NonNull UUID writeCharacteristicUuid,
            @NonNull UUID readCharacteristicUuid) {
        this(storage,
                new CarBleCentralManager(context, bleCentralManager, storage, serviceUuid, bgMask,
                        writeCharacteristicUuid, readCharacteristicUuid),
                new CarBlePeripheralManager(blePeripheralManager, storage, associationServiceUuid,
                        writeCharacteristicUuid, readCharacteristicUuid));
    }

    @VisibleForTesting
    ConnectedDeviceManager(
            @NonNull ConnectedDeviceStorage storage,
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
        //mCentralManager.start();
        mPeripheralManager.start();
        boolean hasDelayedRequest = false;
        mLock.lock();
        try {
            mIsStarted = true;
            hasDelayedRequest = mHasDelayedConnectionRequest;
            mHasDelayedConnectionRequest = false;
        } finally {
            mLock.unlock();
        }

        if (hasDelayedRequest) {
            connectToActiveUserDevice();
        }
    }

    /** Clean up internal processes and disconnect any active connections. */
    public void cleanup() {
        logd(TAG, "Cleaning up ConnectedDeviceManager.");
        mLock.lock();
        try {
            mIsStarted = false;
            mHasDelayedConnectionRequest = false;
            mIsConnectingToUserDevice = false;
        } finally {
            mLock.unlock();
        }
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
        logd(TAG, "Returned " + activeUserConnectedDevices.size() + " active user devices.");
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
        Executors.defaultThreadFactory().newThread(() -> {
            logd(TAG, "Received request to connect to active user's device.");
            mLock.lock();
            try {
                if (!mIsStarted) {
                    mHasDelayedConnectionRequest = true;
                    logd(TAG,
                            "Manager has not been started yet. Deferring connection request until "
                                    + "after start.");
                    return;
                }
                if (mIsConnectingToUserDevice) {
                    logd(TAG, "A request has already been made to connect to this user's device. "
                            + "Ignoring redundant request.");
                    return;
                }
                List<String> userDeviceIds = mStorage.getActiveUserAssociatedDeviceIds();
                if (userDeviceIds.isEmpty()) {
                    logw(TAG, "No devices associated with active user. Ignoring.");
                    return;
                }

                // Only currently support one device per user for fast association, so take the
                // first one.
                String userDeviceId = userDeviceIds.get(0);
                byte[] key = mStorage.getEncryptionKey(userDeviceId);
                logd(TAG, "Found encryption key of value " + key);
                if (mConnectedDevices.containsKey(userDeviceId)) {
                    logd(TAG, "Device has already been connected. No need to attempt connection "
                            + "again.");
                    return;
                }
                mIsConnectingToUserDevice = true;
                mPeripheralManager.connectToDevice(UUID.fromString(userDeviceId));
            } catch (Exception e) {
                loge(TAG, "Exception while attempting connection with active user's device.", e);
            } finally {
                mLock.unlock();
            }
        }).start();
    }

    /**
     * Start the association with a new device.
     *
     * @param callback Callback for association events.
     */
    public void startAssociation(@NonNull AssociationCallback callback) {
        mAssociationCallback = callback;
        mPeripheralManager.startAssociation(getNameForAssociation(), mInternalAssociationCallback);
    }

    /** Stop the association with any device. */
    public void stopAssociation(@NonNull AssociationCallback callback) {
        if (mAssociationCallback != callback) {
            logd(TAG, "Stop association called with unrecognized callback. Ignoring.");
            return;
        }
        mAssociationCallback = null;
        mPeripheralManager.stopAssociation(mInternalAssociationCallback);
    }

    /**
     * Get a list of associated devices for the given user.
     *
     * @return Associated device list.
     */
    @NonNull
    public List<AssociatedDevice> getActiveUserAssociatedDevices() {
        return mStorage.getActiveUserAssociatedDevices();
    }

    /** Notify that the user has accepted a pairing code or any out-of-band confirmation. */
    public void notifyOutOfBandAccepted() {
        mPeripheralManager.notifyOutOfBandAccepted();
    }

    /**
     * Remove the associated device with the given device identifier for the current user.
     *
     * @param deviceId Device identifier.
     */
    public void removeActiveUserAssociatedDevice(@NonNull String deviceId) {
        if (mConnectedDevices.containsKey(deviceId)) {
            removeConnectedDevice(deviceId, mPeripheralManager);
            mPeripheralManager.stop();
        }
        mStorage.removeAssociatedDeviceForActiveUser(deviceId);
        //TODO(b/146504818): Notify features that an associated device has been removed.
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

        byte[] message = popMissedMessage(recipientId, device.getDeviceId());
        if (message != null) {
            newCallbacks.invoke(deviceCallback ->
                    deviceCallback.onMessageReceived(device, message));
        }
    }

    private void notifyOfBlacklisting(@NonNull ConnectedDevice device, @NonNull UUID recipientId,
            @NonNull DeviceCallback callback, @NonNull Executor executor) {
        loge(TAG, "Multiple callbacks registered for recipient " + recipientId + "! Your "
                + "recipient id is no longer secure and has been blocked from future use.");
        executor.execute(() ->
                callback.onDeviceError(device, DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED));
    }

    private void saveMissedMessage(@NonNull String deviceId, @NonNull UUID recipientId,
            @NonNull byte[] message) {
        // Store last message in case recipient registers callbacks in the future.
        logd(TAG, "No recipient registered for device " + deviceId + " and recipient "
                + recipientId + " combination. Saving message.");
        mRecipientMissedMessages.putIfAbsent(recipientId, new HashMap<>());
        mRecipientMissedMessages.get(recipientId).putIfAbsent(deviceId, message);
    }

    /**
     * Remove the last message sent for this device prior to a {@link DeviceCallback} being
     * registered.
     *
     * @param recipientId Recipient's id
     * @param deviceId Device id
     * @return The last missed {@code byte[]} of the message, or {@code null} if no messages were
     *         missed.
     */
    @Nullable
    private byte[] popMissedMessage(@NonNull UUID recipientId, @NonNull String deviceId) {
        Map<String, byte[]> missedMessages = mRecipientMissedMessages.get(recipientId);
        if (missedMessages == null) {
            return null;
        }

        return missedMessages.remove(deviceId);
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
        logd(TAG, "Sending new message to device " + deviceId + " for " + recipientId
                + " containing " + message.length + ". Message will be sent securely: "
                + isEncrypted + ".");

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
        ConnectedDevice connectedDevice = new ConnectedDevice(
                deviceId,
                /* deviceName = */ null,
                mStorage.getActiveUserAssociatedDeviceIds().contains(deviceId),
                /* hasSecureChannel = */ false
        );

        mConnectedDevices.put(deviceId, new InternalConnectedDevice(connectedDevice, bleManager));
        invokeConnectionCallbacks(connectedDevice.getBelongsToActiveUser(),
                callback -> callback.onDeviceConnected(connectedDevice));
    }

    @VisibleForTesting
    void removeConnectedDevice(@NonNull String deviceId, @NonNull CarBleManager bleManager) {
        logd(TAG, "Device " + deviceId + " disconnected from manager " + bleManager);
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
        logd(TAG, "Secure channel established to " + deviceId + " . Notifying callbacks: "
                + notifyCallbacks + ".");
        if (notifyCallbacks) {
            notifyAllDeviceCallbacks(deviceId,
                    callback -> callback.onSecureChannelEstablished(updatedConnectedDevice));
        }
    }

    @VisibleForTesting
    void onMessageReceived(@NonNull String deviceId, @NonNull DeviceMessage message) {
        logd(TAG, "New message received from device " + deviceId + " intended for "
                + message.getRecipient() + " containing " + message.getMessage().length
                + " bytes.");

        InternalConnectedDevice connectedDevice = mConnectedDevices.get(deviceId);
        if (connectedDevice == null) {
            logw(TAG, "Received message from unknown device " + deviceId + "or to unknown "
                    + "recipient " + message.getRecipient() + ".");
            return;
        }
        UUID recipientId = message.getRecipient();
        Map<UUID, ThreadSafeCallbacks<DeviceCallback>> deviceCallbacks =
                mDeviceCallbacks.get(deviceId);
        if (deviceCallbacks == null) {
            saveMissedMessage(deviceId, recipientId, message.getMessage());
            return;
        }
        ThreadSafeCallbacks<DeviceCallback> recipientCallbacks =
                deviceCallbacks.get(recipientId);
        if (recipientCallbacks == null) {
            saveMissedMessage(deviceId, recipientId, message.getMessage());
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

    @VisibleForTesting
    void onAssociationCompleted(@NonNull String deviceId) {
        InternalConnectedDevice connectedDevice =
                getConnectedDeviceForManager(deviceId, mPeripheralManager);
        if (connectedDevice == null) {
            return;
        }

        // The previous device is now obsolete and should be replaced with a new one properly
        // reflecting the state of belonging to the active user and notify features.
        removeConnectedDevice(deviceId, mPeripheralManager);
        addConnectedDevice(deviceId, mPeripheralManager);
    }

    @NonNull
    private List<String> getActiveUserDeviceIds() {
        return mStorage.getAssociatedDeviceIdsForUser(ActivityManager.getCurrentUser());
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

    /**
     * Returns the name that should be used for the device during enrollment of a trusted device.
     *
     * <p>The returned name will be a combination of a prefix sysprop and randomized digits.
     */
    @NonNull
    private String getNameForAssociation() {
        if (mNameForAssociation == null) {
            mNameForAssociation = ByteUtils.generateRandomNumberString(DEVICE_NAME_LENGTH_LIMIT);
        }
        return mNameForAssociation;
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

    private final AssociationCallback mInternalAssociationCallback = new AssociationCallback() {
        @Override
        public void onAssociationStartSuccess(String deviceName) {
            if (mAssociationCallback != null) {
                mAssociationCallback.onAssociationStartSuccess(deviceName);
            }
        }

        @Override
        public void onAssociationStartFailure() {
            if (mAssociationCallback != null) {
                mAssociationCallback.onAssociationStartFailure();
            }
        }

        @Override
        public void onAssociationError(int error) {
            if (mAssociationCallback != null) {
                mAssociationCallback.onAssociationError(error);
            }
        }

        @Override
        public void onVerificationCodeAvailable(String code) {
            if (mAssociationCallback != null) {
                mAssociationCallback.onVerificationCodeAvailable(code);
            }
        }

        @Override
        public void onAssociationCompleted(String deviceId) {
            if (mAssociationCallback != null) {
                mAssociationCallback.onAssociationCompleted(deviceId);
            }
            onAssociationCompleted(deviceId);
        }
    };

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
