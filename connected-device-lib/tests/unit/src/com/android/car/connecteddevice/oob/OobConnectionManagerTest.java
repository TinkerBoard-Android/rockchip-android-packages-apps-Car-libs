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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testng.Assert;

import java.security.InvalidKeyException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class OobConnectionManagerTest {
    private static final byte[] TEST_MESSAGE = "testMessage".getBytes();
    private TestChannel mTestChannel;
    private SecretKey mTestKey;

    @Before
    public void setUp() throws Exception {
        mTestChannel = new TestChannel();
        mTestKey = KeyGenerator.getInstance("AES").generateKey();
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
    public void testServer_onOobDataReceived_setsKey() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        mTestChannel.onOobDataReceived(mTestKey);
        assertThat(oobConnectionManager.mEncryptionKey).isEqualTo(mTestKey);
    }

    @Test
    public void testInitAsClient_listenerIsNull() {
        OobConnectionManager.forClient(mTestChannel);
        assertThat(mTestChannel.mOobDataReceivedListener).isNull();
    }

    @Test
    public void testInitAsClient_keyIsNonNullAndSent() {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forClient(mTestChannel);
        assertThat(oobConnectionManager.mEncryptionKey).isNotNull();
        assertThat(mTestChannel.mSentOobData).isEqualTo(oobConnectionManager.mEncryptionKey);
    }

    @Test
    public void testClient_encryptAndDecrypt() throws Exception {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forClient(mTestChannel);
        byte[] encryptedMessage = oobConnectionManager.encryptVerificationCode(TEST_MESSAGE);
        byte[] decryptedMessage = oobConnectionManager.decryptVerificationCode(encryptedMessage);
        assertThat(decryptedMessage).isEqualTo(TEST_MESSAGE);
    }

    @Test
    public void testServer_encryptAndDecrypt() throws Exception {
        OobConnectionManager oobConnectionManager = OobConnectionManager.forServer(mTestChannel);
        mTestChannel.onOobDataReceived(mTestKey);

        byte[] encryptedMessage = oobConnectionManager.encryptVerificationCode(TEST_MESSAGE);
        byte[] decryptedMessage = oobConnectionManager.decryptVerificationCode(encryptedMessage);
        assertThat(decryptedMessage).isEqualTo(TEST_MESSAGE);
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
        SecretKey mSentOobData = null;

        @Override
        public void sendOobData(SecretKey oobData) {
            mSentOobData = oobData;
        }
    }
}
