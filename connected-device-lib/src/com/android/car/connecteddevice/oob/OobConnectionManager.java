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

package com.android.car.connecteddevice.oob;

import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.security.keystore.KeyProperties;

import com.android.internal.annotations.VisibleForTesting;

import com.google.common.primitives.Bytes;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is a class that manages a token--{@link OobConnectionManager#mEncryptionKey}-- passed via
 * an out of band {@link Channel} that is distinct from the channel that is currently being secured.
 *
 * <p>Intended usage as client:
 * {@link OobConnectionManager#forClient(Channel)}
 *
 * <pre>{@code When a message is received:
 *   verificationCode = OobConnectionManager#decryptVerificationCode(byte[])
 *   check that verification code is valid
 *   if it is:
 *     encryptedMessage =  OobConnectionManager#encryptVerificationCode(byte[])
 *     send encryptedMessage
 *     verify handshake
 *   otherwise:
 *     fail handshake
 * }</pre>
 *
 * <p>Intended usage as server:
 * {@link OobConnectionManager#forServer(Channel)}
 *
 * <pre>{@code encryptedMessage = OobConnectionManager#encryptVerificationCode(byte[])
 * sendMessage
 * when a message is received:
 *   verificationCode = OobConnectionManager#decryptVerificationCode(byte[])
 *   check that verification code is valid
 *   if it is:
 *     verify handshake
 *   otherwise:
 *     fail handshake
 * }</pre>
 */
class OobConnectionManager {
    private static final String TAG = "OobConnectionManager";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // The nonce length is chosen to be consistent with the standard specification:
    // Section 8.2 of https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
    @VisibleForTesting
    static final int NONCE_LENGTH_BYTES = 12;

    private final Cipher mCipher;
    @VisibleForTesting
    byte[] mEncryptionIv = new byte[NONCE_LENGTH_BYTES];
    @VisibleForTesting
    byte[] mDecryptionIv = new byte[NONCE_LENGTH_BYTES];
    @VisibleForTesting
    SecretKey mEncryptionKey;

    /**
     * Static initializer for client device.
     *
     * @param oobChannel to send out of band data to server
     */
    @Nullable
    public static OobConnectionManager forClient(@NonNull Channel oobChannel) {
        try {
            return new OobConnectionManager(oobChannel, /* isClient= */ true);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            loge(TAG, "Creation of OobConnectionManager failed, returning null", e);
        }
        return null;
    }

    /**
     * Static initializer for server device
     *
     * @param oobChannel to listen for out of band data to be sent from client
     */
    @Nullable
    public static OobConnectionManager forServer(@NonNull Channel oobChannel) {
        try {
            return new OobConnectionManager(oobChannel, /* isClient= */ false);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            loge(TAG, "Creation of OobConnectionManager failed, returning null", e);
        }
        return null;
    }

    private OobConnectionManager(Channel oobChannel, boolean isClient)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        mCipher = Cipher.getInstance(ALGORITHM);

        if (isClient) {
            initAsClient(oobChannel);
        } else {
            initAsServer(oobChannel);
        }
    }

    /**
     * Encrypts {@param verificationCode} using {@link OobConnectionManager#mEncryptionKey}
     */
    @NonNull
    public byte[] encryptVerificationCode(@NonNull byte[] verificationCode)
            throws InvalidAlgorithmParameterException,
            BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        mCipher.init(Cipher.ENCRYPT_MODE, mEncryptionKey, new IvParameterSpec(mEncryptionIv));
        return mCipher.doFinal(verificationCode);
    }

    /**
     * Decrypts {@param encryptedMessage} using {@link OobConnectionManager#mEncryptionKey}
     */
    @NonNull
    public byte[] decryptVerificationCode(@NonNull byte[] encryptedMessage)
            throws InvalidAlgorithmParameterException, BadPaddingException, InvalidKeyException,
            IllegalBlockSizeException {
        mCipher.init(Cipher.DECRYPT_MODE, mEncryptionKey, new IvParameterSpec(mDecryptionIv));
        return mCipher.doFinal(encryptedMessage);
    }

    private void initAsClient(@NonNull Channel oobChannel) throws NoSuchAlgorithmException {
        if (oobChannel == null) {
            logw(TAG, "OOB channel is null, cannot send data.");
            return;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
        mEncryptionKey = keyGenerator.generateKey();

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(mEncryptionIv);
        secureRandom.nextBytes(mDecryptionIv);

        oobChannel.sendOobData(
                Bytes.concat(mDecryptionIv, mEncryptionIv, mEncryptionKey.getEncoded()));
    }

    private void initAsServer(@NonNull Channel oobChannel) {
        oobChannel.mOobDataReceivedListener = (oobData) -> {
            mEncryptionIv = Arrays.copyOfRange(oobData, 0, NONCE_LENGTH_BYTES);
            mDecryptionIv = Arrays.copyOfRange(oobData, NONCE_LENGTH_BYTES,
                    NONCE_LENGTH_BYTES * 2);
            mEncryptionKey = new SecretKeySpec(
                    Arrays.copyOfRange(oobData, NONCE_LENGTH_BYTES * 2, oobData.length),
                    KeyProperties.KEY_ALGORITHM_AES);
        };
    }

    /**
     * An abstract class that represents the out of band channel and can be used to exchange the
     * token before the handshake begins.
     */
    public abstract static class Channel {
        @VisibleForTesting
        Consumer<byte[]> mOobDataReceivedListener;

        /**
         * Callback to be invoked when {@param oobData} is received via the out of band channel.
         */
        public final void onOobDataReceived(@NonNull byte[] oobData) {
            if (mOobDataReceivedListener == null) {
                loge(TAG, "OobDataReceivedListener is null, returning");
                return;
            }
            mOobDataReceivedListener.accept(oobData);
        }

        /**
         * Sends {@param oobData} over the out of band channel.
         */
        public abstract void sendOobData(@NonNull byte[] oobData);
    }
}
