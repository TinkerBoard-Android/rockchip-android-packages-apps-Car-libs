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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.security.keystore.KeyProperties;

import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import com.android.internal.annotations.VisibleForTesting;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

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
    private static final int NONCE_LENGTH_BYTES = 12;

    private final Channel mOobChannel;
    private final Cipher mCipher;
    private final IvParameterSpec mIvParameterSpec;

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
        mOobChannel = oobChannel;

        byte[] iv = new byte[NONCE_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        mIvParameterSpec = new IvParameterSpec(iv);

        if (isClient) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            mEncryptionKey = keyGenerator.generateKey();
            if (mOobChannel == null) {
                logw(TAG, "OOB channel is null, cannot send data.");
                return;
            }
            mOobChannel.sendOobData(mEncryptionKey);
        } else {
            mOobChannel.mOobDataReceivedListener = (oobData) -> {
                mEncryptionKey = oobData;
            };
        }
    }

    /**
     * Encrypts {@param verificationCode} using {@link OobConnectionManager#mEncryptionKey}
     */
    @NonNull
    public byte[] encryptVerificationCode(@NonNull byte[] verificationCode)
            throws InvalidAlgorithmParameterException,
            BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        mCipher.init(Cipher.ENCRYPT_MODE, mEncryptionKey, mIvParameterSpec);
        return mCipher.doFinal(verificationCode);
    }

    /**
     * Decrypts {@param encryptedMessage} using {@link OobConnectionManager#mEncryptionKey}
     */
    @NonNull
    public byte[] decryptVerificationCode(@NonNull byte[] encryptedMessage)
            throws InvalidAlgorithmParameterException, BadPaddingException, InvalidKeyException,
            IllegalBlockSizeException {
        mCipher.init(Cipher.DECRYPT_MODE, mEncryptionKey, mIvParameterSpec);
        return mCipher.doFinal(encryptedMessage);
    }

    /**
     * An abstract class that represents the out of band channel and can be used to exchange the
     * token before the handshake begins.
     */
    public abstract static class Channel {
        @VisibleForTesting
        Consumer<SecretKey> mOobDataReceivedListener;

        /**
         * Callback to be invoked when {@param oobData} is received via the out of band channel.
         */
        public final void onOobDataReceived(@NonNull SecretKey oobData) {
            if (mOobDataReceivedListener == null) {
                loge(TAG, "OobDataReceivedListener is null, returning");
                return;
            }
            mOobDataReceivedListener.accept(oobData);
        }

        /**
         * Sends {@param oobData} over the out of band channel.
         */
        public abstract void sendOobData(@NonNull SecretKey oobData);
    }
}
