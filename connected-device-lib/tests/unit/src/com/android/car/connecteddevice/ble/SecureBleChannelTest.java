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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.encryptionrunner.DummyEncryptionRunner;
import android.car.encryptionrunner.EncryptionRunnerFactory;
import android.car.encryptionrunner.Key;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.connecteddevice.ble.BleDeviceMessageStream.MessageReceivedListener;
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage;
import com.android.car.connecteddevice.util.ByteUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class SecureBleChannelTest {
    private static final UUID CLIENT_DEVICE_ID =
            UUID.fromString("a5645523-3280-410a-90c1-582a6c6f4969");
    private static final UUID SERVER_DEVICE_ID =
            UUID.fromString("a29f0c74-2014-4b14-ac02-be6ed15b545a");

    private SecureBleChannel mChannel;
    private MessageReceivedListener mMessageReceivedListener;

    @Mock private BleDeviceMessageStream mStreamMock;
    @Mock private CarCompanionDeviceStorage mStorageMock;
    @Mock private SecureBleChannel.ShowVerificationCodeListener mShowVerificationCodeListenerMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mStorageMock.getUniqueId()).thenReturn(SERVER_DEVICE_ID);
        when(mStorageMock.saveEncryptionKey(anyString(), any(byte[].class))).thenReturn(true);
    }

    @Test
    public void testEncryptionHandshake_Association() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ChannelCallback callbackSpy = spy(new ChannelCallback(semaphore));
        setUpSecureBleChannel_Association(callbackSpy);
        ArgumentCaptor<String> deviceIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DeviceMessage> messageCaptor =
                ArgumentCaptor.forClass(DeviceMessage.class);

        sendDeviceId();
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();
        verify(callbackSpy).onDeviceIdReceived(deviceIdCaptor.capture());
        verify(mStreamMock).writeMessage(messageCaptor.capture(), any());
        byte[] deviceIdMessage = messageCaptor.getValue().getMessage();
        assertThat(deviceIdMessage).isEqualTo(ByteUtils.uuidToBytes(SERVER_DEVICE_ID));
        assertThat(deviceIdCaptor.getValue()).isEqualTo(CLIENT_DEVICE_ID.toString());

        initHandshakeMessage();
        verify(mStreamMock, times(2)).writeMessage(messageCaptor.capture(), any());
        byte[] response = messageCaptor.getValue().getMessage();
        assertThat(response).isEqualTo(DummyEncryptionRunner.INIT_RESPONSE.getBytes());

        respondToContinueMessage();
        verify(mShowVerificationCodeListenerMock).showVerificationCode(anyString());

        mChannel.notifyOutOfBandAccepted();
        verify(mStreamMock, times(3)).writeMessage(messageCaptor.capture(), any());
        byte[] confirmMessage = messageCaptor.getValue().getMessage();
        assertThat(confirmMessage).isEqualTo(SecureBleChannelKt.getCONFIRMATION_SIGNAL());
        verify(mStorageMock).addAssociatedDeviceForActiveUser(eq(CLIENT_DEVICE_ID.toString()));
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();
        verify(callbackSpy).onSecureChannelEstablished(any());
    }

    @Test
    public void testEncryptionHandshake_Association_wrongInitHandshakeMessage()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ChannelCallback callbackSpy = spy(new ChannelCallback(semaphore));
        setUpSecureBleChannel_Association(callbackSpy);

        sendDeviceId();
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();

        // Wrong init handshake message
        respondToContinueMessage();
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();
        verify(callbackSpy).onEstablishSecureChannelFailure(
                eq(SecureBleChannel.CHANNEL_ERROR_INVALID_HANDSHAKE)
        );
    }

    @Test
    public void testEncryptionHandshake_Association_wrongRespondToContinueMessage()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ChannelCallback callbackSpy = spy(new ChannelCallback(semaphore));
        setUpSecureBleChannel_Association(callbackSpy);

        sendDeviceId();
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();

        initHandshakeMessage();

        // Wrong respond to continue message
        initHandshakeMessage();
        assertThat(semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)).isTrue();
        verify(callbackSpy).onEstablishSecureChannelFailure(
                eq(SecureBleChannel.CHANNEL_ERROR_INVALID_HANDSHAKE)
        );
    }

    private void setUpSecureBleChannel_Association(ChannelCallback callback) {
        mChannel = new SecureBleChannel(
                mStreamMock,
                mStorageMock,
                callback,
                /* isReconnect = */ false,
                EncryptionRunnerFactory.newDummyRunner()
        );
        mChannel.setShowVerificationCodeListener(mShowVerificationCodeListenerMock);
        ArgumentCaptor<MessageReceivedListener> listenerCaptor =
                ArgumentCaptor.forClass(MessageReceivedListener.class);
        verify(mStreamMock).setMessageReceivedListener(listenerCaptor.capture());
        mMessageReceivedListener = listenerCaptor.getValue();
    }

    private void sendDeviceId() {
        DeviceMessage message = new DeviceMessage(
                /* recipient = */ null,
                /* isMessageEncrypted = */ false,
                ByteUtils.uuidToBytes(CLIENT_DEVICE_ID)
        );
        mMessageReceivedListener.onMessageReceived(message);
    }

    private void initHandshakeMessage() {
        DeviceMessage message = new DeviceMessage(
                /* recipient = */ null,
                /* isMessageEncrypted = */ false,
                DummyEncryptionRunner.INIT.getBytes()
        );
        mMessageReceivedListener.onMessageReceived(message);
    }

    private void respondToContinueMessage() {
        DeviceMessage message = new DeviceMessage(
                /* recipient = */ null,
                /* isMessageEncrypted = */ false,
                DummyEncryptionRunner.CLIENT_RESPONSE.getBytes()
        );
        mMessageReceivedListener.onMessageReceived(message);
    }

    /**
     * Add the thread control logic into {@link SecureBleChannel.Callback} only for spy purpose.
     *
     * <p>The callback will release the semaphore which hold by one test when this callback
     * is called, telling the test that it can verify certain behaviors which will only occurred
     * after the callback is notified. This is needed mainly because of the callback is notified
     * in a different thread.
     */
    class ChannelCallback implements SecureBleChannel.Callback {
        private final Semaphore mSemaphore;
        ChannelCallback(Semaphore semaphore) {
            mSemaphore = semaphore;
        }
        @Override
        public void onSecureChannelEstablished(Key encryptionKey) {
            mSemaphore.release();
        }

        @Override
        public void onEstablishSecureChannelFailure(int error) {
            mSemaphore.release();
        }

        @Override
        public void onMessageReceived(DeviceMessage deviceMessage) {
            mSemaphore.release();
        }

        @Override
        public void onMessageReceivedError(Exception exception) {
            mSemaphore.release();
        }

        @Override
        public void onDeviceIdReceived(String deviceId) {
            mSemaphore.release();
        }
    }
}
