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

import static com.google.common.truth.Truth.assertThat;

import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.primitives.Bytes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testng.Assert;

import java.security.InvalidKeyException;
import java.security.SecureRandom;

import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class OobConnectionManagerTest {
    private static final byte[] TEST_MESSAGE = "testMessage".getBytes();
    private TestChannel mTestChannel;
    private SecretKey mTestKey;
    private byte[] mTestEncryptionIv = new byte[OobConnectionManager.NONCE_LENGTH_BYTES];
    private byte[] mTestDecryptionIv = new byte[OobConnectionManager.NONCE_LENGTH_BYTES];
    private byte[] mTestOobData;

    @Before
    public void setUp() throws Exception {
        mTestChannel = new TestChannel();
        mTestKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES).generateKey();

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(mTestEncryptionIv);
        secureRandom.nextBytes(mTestDecryptionIv);

        mTestOobData = Bytes.concat(mTestDecryptionIv, mTestEncryptionIv, mTestKey.getEncoded());
    }

    @Test
    public void testInitAsServer_listenerIsNotNull() {
        OobConnectionManager.forServer(mTestChannel);
        assertThat(mTestChannel.mOobDataReceivedListener).isNotNull();
    }

    @Test
    public void testInitAsServer_keyIsNull() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        assertThat(oobConnectionManager.mEncryptionKey).isNull();
    }

    @Test
    public void testServer_onOobDataReceived_setsKeyAndNonce() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        mTestChannel.onOobDataReceived(mTestOobData);
        assertThat(oobConnectionManager.mEncryptionKey).isEqualTo(mTestKey);
        // The decryption IV for the server is the encryption IV for the client and vice versa
        assertThat(oobConnectionManager.mDecryptionIv).isEqualTo(mTestEncryptionIv);
        assertThat(oobConnectionManager.mEncryptionIv).isEqualTo(mTestDecryptionIv);
    }

    @Test
    public void testInitAsClient_listenerIsNull() {
        OobConnectionManager.forClient(mTestChannel);
        assertThat(mTestChannel.mOobDataReceivedListener).isNull();
    }

    @Test
    public void testInitAsClient_keyAndNoncesAreNonNullAndSent() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forClient(mTestChannel);
        assertThat(oobConnectionManager.mEncryptionKey).isNotNull();
        assertThat(oobConnectionManager.mEncryptionIv).isNotNull();
        assertThat(oobConnectionManager.mDecryptionIv).isNotNull();
        assertThat(mTestChannel.mSentOobData).isEqualTo(Bytes.concat(
                oobConnectionManager.mDecryptionIv,
                oobConnectionManager.mEncryptionIv,
                oobConnectionManager.mEncryptionKey.getEncoded()
        ));
    }

    @Test
    public void testServerEncryptAndClientDecrypt() throws Exception {
        OobConnectionManager clientOobConnectionManager = OobConnectionManager.forClient(
                mTestChannel);
        OobConnectionManager serverOobConnectionManager = OobConnectionManager.forServer(
                mTestChannel);
        mTestChannel.mOobDataReceivedListener.accept(mTestChannel.mSentOobData);

        byte[] encryptedTestMessage = clientOobConnectionManager.encryptVerificationCode(
                TEST_MESSAGE);
        byte[] decryptedTestMessage = serverOobConnectionManager.decryptVerificationCode(
                encryptedTestMessage);

        assertThat(decryptedTestMessage).isEqualTo(TEST_MESSAGE);
    }

    @Test
    public void testClientEncryptAndServerDecrypt() throws Exception {
        OobConnectionManager clientOobConnectionManager = OobConnectionManager.forClient(
                mTestChannel);
        OobConnectionManager serverOobConnectionManager = OobConnectionManager.forServer(
                mTestChannel);
        mTestChannel.mOobDataReceivedListener.accept(mTestChannel.mSentOobData);

        byte[] encryptedTestMessage = serverOobConnectionManager.encryptVerificationCode(
                TEST_MESSAGE);
        byte[] decryptedTestMessage = clientOobConnectionManager.decryptVerificationCode(
                encryptedTestMessage);

        assertThat(decryptedTestMessage).isEqualTo(TEST_MESSAGE);
    }

    @Test
    public void testEncryptAndDecryptWithDifferentNonces_throwsAEADBadTagException()
            throws Exception {
        // The OobConnectionManager stores a different nonce for encryption and decryption, so it
        // can't decrypt messages that it encrypted itself. It can only send encrypted messages to
        // an OobConnectionManager on another device that share its nonces and encryption key.
        OobConnectionManager oobConnectionManager = OobConnectionManager.forClient(mTestChannel);
        byte[] encryptedMessage = oobConnectionManager.encryptVerificationCode(TEST_MESSAGE);
        Assert.assertThrows(AEADBadTagException.class,
                () -> oobConnectionManager.decryptVerificationCode(encryptedMessage));
    }

    @Test
    public void testDecryptWithShortMessage_throwsAEADBadTagException() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forClient(mTestChannel);
        Assert.assertThrows(AEADBadTagException.class,
                () -> oobConnectionManager.decryptVerificationCode("short".getBytes()));
    }

    @Test
    public void testEncryptWithNullKey_throwsInvalidKeyException() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        Assert.assertThrows(InvalidKeyException.class,
                () -> oobConnectionManager.encryptVerificationCode(TEST_MESSAGE));
    }

    @Test
    public void testDecryptWithNullKey_throwsInvalidKeyException() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        Assert.assertThrows(InvalidKeyException.class,
                () -> oobConnectionManager.decryptVerificationCode(TEST_MESSAGE));
    }

    private static class TestChannel extends OobConnectionManager.Channel {
        byte[] mSentOobData = null;

        @Override
        public void sendOobData(byte[] oobData) {
            mSentOobData = oobData;
        }
    }
}
