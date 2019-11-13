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

package com.android.car.connecteddevice.storage;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.car.trust.TrustedDeviceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.android.car.connecteddevice.R;
import com.android.car.connecteddevice.model.AssociatedDevice;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/** Storage for Trusted Devices in a car. */
public class CarCompanionDeviceStorage {
    private static final String TAG = "CompanionStorage";

    private static final String UNIQUE_ID_KEY = "CTABM_unique_id";
    private static final String PREF_ENCRYPTION_KEY_PREFIX = "CTABM_encryption_key";
    private static final String KEY_ALIAS = "Ukey2Key";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String IV_SPEC_SEPARATOR = ";";
    // This delimiter separates deviceId and deviceInfo, so it has to differ from the
    // TrustedDeviceInfo delimiter. Once new API can be added, deviceId will be added to
    // TrustedDeviceInfo and this delimiter will be removed.
    private static final char DEVICE_INFO_DELIMITER = '#';

    // The length of the authentication tag for a cipher in GCM mode. The GCM specification states
    // that this length can only have the values {128, 120, 112, 104, 96}. Using the highest
    // possible value.
    private static final int GCM_AUTHENTICATION_TAG_LENGTH = 128;

    private final Context mContext;
    private SharedPreferences mSharedPreferences;
    private UUID mUniqueId;

    public CarCompanionDeviceStorage(@NonNull Context context) {
        mContext = context;
    }

    /** Return the car TrustedAgent {@link SharedPreferences}. */
    @NonNull
    public SharedPreferences getSharedPrefs() {
        // This should be called only after user 0 is unlocked.
        if (mSharedPreferences != null) {
            return mSharedPreferences;
        }
        mSharedPreferences =
                mContext.getSharedPreferences(
                        mContext.getString(R.string.connected_device_shared_preferences),
                        Context.MODE_PRIVATE);
        return mSharedPreferences;
    }

    /**
     * Get communication encryption key for the given device
     *
     * @param deviceId id of trusted device
     * @return encryption key, null if device id is not recognized
     */
    @Nullable
    public byte[] getEncryptionKey(@NonNull String deviceId) {
        SharedPreferences prefs = getSharedPrefs();
        String key = createSharedPrefKey(deviceId);
        if (!prefs.contains(key)) {
            return null;
        }

        // This value will not be "null" because we already checked via a call to contains().
        String[] values = prefs.getString(key, "").split(IV_SPEC_SEPARATOR, -1);

        if (values.length != 2) {
            return null;
        }

        byte[] encryptedKey = Base64.decode(values[0], Base64.DEFAULT);
        byte[] ivSpec = Base64.decode(values[1], Base64.DEFAULT);
        return decryptWithKeyStore(KEY_ALIAS, encryptedKey, ivSpec);
    }

    /**
     * Save encryption key for the given device
     *
     * @param deviceId      did of trusted device
     * @param encryptionKey encryption key
     * @return {@code true} if the operation succeeded
     */
    public boolean saveEncryptionKey(@NonNull String deviceId, @NonNull byte[] encryptionKey) {
        String encryptedKey = encryptWithKeyStore(KEY_ALIAS, encryptionKey);
        if (encryptedKey == null) {
            return false;
        }
        if (getSharedPrefs().contains(createSharedPrefKey(deviceId))) {
            clearEncryptionKey(deviceId);
        }

        getSharedPrefs().edit().putString(createSharedPrefKey(deviceId), encryptedKey).apply();
        return true;
    }

    /**
     * Clear the encryption key for the given device
     *
     * @param deviceId id of the peer device
     */
    public void clearEncryptionKey(@Nullable String deviceId) {
        if (deviceId == null) {
            return;
        }
        getSharedPrefs().edit().remove(createSharedPrefKey(deviceId)).apply();
    }

    /**
     * Encrypt value with designated key
     *
     * <p>The encrypted value is of the form:
     *
     * <p>key + IV_SPEC_SEPARATOR + ivSpec
     *
     * <p>The {@code ivSpec} is needed to decrypt this key later on.
     *
     * @param keyAlias KeyStore alias for key to use
     * @param value    a value to encrypt
     * @return encrypted value, null if unable to encrypt
     */
    @Nullable
    public String encryptWithKeyStore(@NonNull String keyAlias, @Nullable byte[] value) {
        if (value == null) {
            return null;
        }

        Key key = getKeyStoreKey(keyAlias);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(value), Base64.DEFAULT)
                    + IV_SPEC_SEPARATOR
                    + Base64.encodeToString(cipher.getIV(), Base64.DEFAULT);
        } catch (IllegalBlockSizeException
                | BadPaddingException
                | NoSuchAlgorithmException
                | NoSuchPaddingException
                | IllegalStateException
                | InvalidKeyException e) {
            loge(TAG, "Unable to encrypt value with key " + keyAlias, e);
            return null;
        }
    }

    /**
     * Decrypt value with designated key
     *
     * @param keyAlias KeyStore alias for key to use
     * @param value    encrypted value
     * @return decrypted value, null if unable to decrypt
     */
    @Nullable
    public byte[] decryptWithKeyStore(
            @NonNull String keyAlias, @Nullable byte[] value, @NonNull byte[] ivSpec) {
        if (value == null) {
            return null;
        }

        try {
            Key key = getKeyStoreKey(keyAlias);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LENGTH, ivSpec));
            return cipher.doFinal(value);
        } catch (IllegalBlockSizeException
                | BadPaddingException
                | NoSuchAlgorithmException
                | NoSuchPaddingException
                | IllegalStateException
                | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            loge(TAG, "Unable to decrypt value with key " + keyAlias, e);
            return null;
        }
    }

    private static Key getKeyStoreKey(@NonNull String keyAlias) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(keyAlias)) {
                KeyGenerator keyGenerator =
                        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                                KEYSTORE_PROVIDER);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(
                                keyAlias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .build());
                keyGenerator.generateKey();
            }
            return keyStore.getKey(keyAlias, null);

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | UnrecoverableKeyException
                | NoSuchProviderException
                | CertificateException
                | IOException
                | InvalidAlgorithmParameterException e) {
            loge(TAG, "Unable to retrieve key " + keyAlias + " from KeyStore.", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the unique id for head unit. Persists on device until factory reset. This should be
     * called
     * only after user 0 is unlocked.
     *
     * @return unique id
     */
    @NonNull
    public UUID getUniqueId() {
        if (mUniqueId != null) {
            return mUniqueId;
        }

        SharedPreferences prefs = getSharedPrefs();
        if (prefs.contains(UNIQUE_ID_KEY)) {
            mUniqueId = UUID.fromString(prefs.getString(UNIQUE_ID_KEY, null));
            logd(TAG,
                    "Found existing trusted unique id: " + prefs.getString(UNIQUE_ID_KEY, ""));
        }

        if (mUniqueId == null) {
            mUniqueId = UUID.randomUUID();
            prefs.edit().putString(UNIQUE_ID_KEY, mUniqueId.toString()).apply();
            logd(TAG,
                    "Generated new trusted unique id: " + prefs.getString(UNIQUE_ID_KEY, ""));
        }

        return mUniqueId;
    }

    /**
     * Returns a list of device ids of trusted devices for the given user.
     *
     * @param userId the user id for whom we want to know the device ids.
     * @return list of device ids
     */
    @NonNull
    public List<String> getTrustedDevicesForUser(int userId) {
        SharedPreferences sharedPrefs = getSharedPrefs();
        Set<String> deviceInfos = sharedPrefs.getStringSet(String.valueOf(userId), new HashSet<>());
        return deviceInfos.stream()
                .map(CarCompanionDeviceStorage::extractDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Currently, we store a map of userId -> a set of deviceId+deviceInfo strings This method
     * extracts deviceInfo from a device+deviceInfo string, which should be created by {@link
     * #serializeDeviceInfoWithId(TrustedDeviceInfo, String)}
     *
     * @param deviceInfoWithId deviceId+deviceInfo string
     */
    @Nullable
    public static TrustedDeviceInfo extractDeviceInfo(String deviceInfoWithId) {
        int delimiterIndex = deviceInfoWithId.indexOf(DEVICE_INFO_DELIMITER);
        if (delimiterIndex < 0) {
            return null;
        }
        return TrustedDeviceInfo.deserialize(deviceInfoWithId.substring(delimiterIndex + 1));
    }

    /**
     * Extract deviceId from a deviceId+deviceInfo string which should be created by {@link
     * #serializeDeviceInfoWithId(TrustedDeviceInfo, String)}
     *
     * @param deviceInfoWithId deviceId+deviceInfo string
     */
    @Nullable
    public static String extractDeviceId(String deviceInfoWithId) {
        int delimiterIndex = deviceInfoWithId.indexOf(DEVICE_INFO_DELIMITER);
        if (delimiterIndex < 0) {
            return null;
        }
        return deviceInfoWithId.substring(0, delimiterIndex);
    }

    /**
     * Create deviceId+deviceInfo string
     * @param info {@link TrustedDeviceInfo}
     * @param id Device id
     * @return Serialized String with device id and info
     */
    public static String serializeDeviceInfoWithId(TrustedDeviceInfo info, String id) {
        return id + DEVICE_INFO_DELIMITER + info.serialize();
    }

    /**
     * Add the associated device of the given deviceId for the given user.
     *
     * @param device New associated device to be added.
     */
    public void addAssociatedDeviceForActiveUser(@NonNull AssociatedDevice device) {
        int userId = ActivityManager.getCurrentUser();
        SharedPreferences sharedPrefs = getSharedPrefs();
        if (sharedPrefs.contains(device.getDeviceId())) {
            clearAssociatedDevice(userId, device.getDeviceId());
        }
        Set<String> devices = sharedPrefs.getStringSet(String.valueOf(userId), new HashSet<>());
        devices.add(device.serialize());
        sharedPrefs.edit()
            .putStringSet(String.valueOf(userId), devices)
            .apply();
    }

    /**
     * Get a list of associated devices for the given user.
     *
     * @param userId The identifier of the user.
     * @return Associated device list.
     */
    @NonNull
    public List<AssociatedDevice> getAssociatedDevicesForUser(@NonNull int userId) {
        SharedPreferences sharedPrefs = getSharedPrefs();
        Set<String> devices = sharedPrefs.getStringSet(String.valueOf(userId), new HashSet<>());
        return devices.stream()
                .map(AssociatedDevice.Companion::deserialize)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of associated devices for the current user.
     *
     * @return Associated device list.
     */
    @NonNull
    public List<AssociatedDevice> getActiveUserAssociatedDevices() {
        return getAssociatedDevicesForUser(ActivityManager.getCurrentUser());
    }

    /**
     * Returns a list of device ids of associated devices for the given user.
     *
     * @param userId The user id for whom we want to know the device ids.
     * @return List of device ids.
     */
    @NonNull
    public List<String> getAssociatedDeviceIdsForUser(@NonNull int userId) {
        SharedPreferences sharedPrefs = getSharedPrefs();
        Set<String> devices = sharedPrefs.getStringSet(String.valueOf(userId), new HashSet<>());
        List<String> deviceIds = new ArrayList<>();
        for (String device: devices) {
            deviceIds.add(AssociatedDevice.deserialize(device).getDeviceId());
        }
        return deviceIds;
    }

    /**
     * Returns a list of device ids of associated devices for the current user.
     *
     * @return List of device ids.
     */
    @NonNull
    public List<String> getActiveUserAssociatedDeviceIds() {
        return getAssociatedDeviceIdsForUser(ActivityManager.getCurrentUser());
    }

    /**
     * Clear the associated device of the given deviceId for the given user.
     *
     * @param userId The identifier of the user.
     * @param deviceId The identifier of the device to be cleared.
     */
    public void clearAssociatedDevice(int userId, @NonNull String deviceId) {
        SharedPreferences sharedPrefs = getSharedPrefs();
        clearEncryptionKey(deviceId);
        Set<String> deviceIds = sharedPrefs.getStringSet(String.valueOf(userId), new HashSet<>());
        deviceIds.remove(deviceId);
        sharedPrefs.edit()
            .putStringSet(String.valueOf(userId), deviceIds)
            .apply();
    }

    private static String createSharedPrefKey(@NonNull String deviceId) {
        return PREF_ENCRYPTION_KEY_PREFIX + deviceId;
    }
}
