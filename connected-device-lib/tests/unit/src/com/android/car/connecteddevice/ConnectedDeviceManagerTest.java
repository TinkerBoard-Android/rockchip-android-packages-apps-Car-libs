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

import static com.android.car.connecteddevice.ConnectedDeviceManager.DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED;
import static com.android.car.connecteddevice.ConnectedDeviceManager.DEVICE_ERROR_INVALID_SECURITY_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.connecteddevice.ConnectedDeviceManager.ConnectionCallback;
import com.android.car.connecteddevice.ConnectedDeviceManager.DeviceCallback;
import com.android.car.connecteddevice.ble.CarBleCentralManager;
import com.android.car.connecteddevice.ble.CarBleManager;
import com.android.car.connecteddevice.ble.CarBlePeripheralManager;
import com.android.car.connecteddevice.ble.DeviceMessage;
import com.android.car.connecteddevice.model.ConnectedDevice;
import com.android.car.connecteddevice.storage.ConnectedDeviceStorage;
import com.android.car.connecteddevice.util.ByteUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ConnectedDeviceManagerTest {

    private final Executor mCallbackExecutor = Executors.newSingleThreadExecutor();

    private final UUID mRecipientId = UUID.randomUUID();

    @Mock
    private ConnectedDeviceStorage mMockStorage;

    @Mock
    private CarBlePeripheralManager mMockPeripheralManager;

    @Mock
    private CarBleCentralManager mMockCentralManager;

    private ConnectedDeviceManager mConnectedDeviceManager;

    private MockitoSession mMockingSession;

    @Before
    public void setUp() {
        mMockingSession = mockitoSession()
                .initMocks(this)
                .strictness(Strictness.LENIENT)
                .startMocking();
        mConnectedDeviceManager = new ConnectedDeviceManager(mMockStorage, mMockCentralManager,
            mMockPeripheralManager);
        mConnectedDeviceManager.start();
    }

    @After
    public void tearDown() {
        if (mMockingSession != null) {
            mMockingSession.finishMocking();
        }
    }

    @Test
    public void getActiveUserConnectedDevices_initiallyShouldReturnEmptyList() {
        assertThat(mConnectedDeviceManager.getActiveUserConnectedDevices()).isEmpty();
    }

    @Test
    public void getActiveUserConnectedDevices_includesNewlyConnectedDevice() {
        String deviceId = connectNewDevice(mMockCentralManager);
        List<ConnectedDevice> activeUserDevices =
                mConnectedDeviceManager.getActiveUserConnectedDevices();
        ConnectedDevice expectedDevice = new ConnectedDevice(deviceId, /* deviceName = */ null,
                /* belongsToActiveUser = */ true, /* hasSecureChannel = */ false);
        assertThat(activeUserDevices).containsExactly(expectedDevice);
    }

    @Test
    public void getActiveUserConnectedDevices_excludesDevicesNotBelongingToActiveUser() {
        String deviceId = UUID.randomUUID().toString();
        String otherUserDeviceId = UUID.randomUUID().toString();
        when(mMockStorage.getActiveUserAssociatedDeviceIds()).thenReturn(
                Collections.singletonList(otherUserDeviceId));
        mConnectedDeviceManager.addConnectedDevice(deviceId, mMockCentralManager);
        assertThat(mConnectedDeviceManager.getActiveUserConnectedDevices()).isEmpty();
    }

    @Test
    public void getActiveUserConnectedDevices_reflectsSecureChannelEstablished() {
        String deviceId = connectNewDevice(mMockCentralManager);
        mConnectedDeviceManager.onSecureChannelEstablished(deviceId, mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        assertThat(connectedDevice.getHasSecureChannel()).isTrue();
    }

    @Test
    public void getActiveUserConnectedDevices_excludesDisconnectedDevice() {
        String deviceId = connectNewDevice(mMockCentralManager);
        mConnectedDeviceManager.removeConnectedDevice(deviceId, mMockCentralManager);
        assertThat(mConnectedDeviceManager.getActiveUserConnectedDevices()).isEmpty();
    }

    @Test
    public void getActiveUserConnectedDevices_unaffectedByOtherManagerDisconnect() {
        String deviceId = connectNewDevice(mMockCentralManager);
        mConnectedDeviceManager.removeConnectedDevice(deviceId, mMockPeripheralManager);
        assertThat(mConnectedDeviceManager.getActiveUserConnectedDevices()).hasSize(1);
    }

    @Test
    public void connectToActiveUserDevice_startsAdvertisingWithDeviceId()
            throws InterruptedException {
        UUID deviceId = UUID.randomUUID();
        when(mMockStorage.getActiveUserAssociatedDeviceIds()).thenReturn(
                Collections.singletonList(deviceId.toString()));
        mConnectedDeviceManager.connectToActiveUserDevice();
        Thread.sleep(100); // Below verify call is made on a background thread.
        verify(mMockPeripheralManager).connectToDevice(deviceId);
    }

    @Test(expected = IllegalStateException.class)
    public void sendMessageSecurely_throwsIllegalStateExceptionIfNoSecureChannel() {
        connectNewDevice(mMockCentralManager);
        ConnectedDevice device = mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        UUID recipientId = UUID.randomUUID();
        byte[] message = ByteUtils.randomBytes(10);
        mConnectedDeviceManager.sendMessageSecurely(device, recipientId, message);
    }

    @Test
    public void sendMessageSecurely_sendsEncryptedMessage() {
        String deviceId = connectNewDevice(mMockCentralManager);
        mConnectedDeviceManager.onSecureChannelEstablished(deviceId, mMockCentralManager);
        ConnectedDevice device = mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        UUID recipientId = UUID.randomUUID();
        byte[] message = ByteUtils.randomBytes(10);
        mConnectedDeviceManager.sendMessageSecurely(device, recipientId, message);
        ArgumentCaptor<DeviceMessage> messageCaptor = ArgumentCaptor.forClass(DeviceMessage.class);
        verify(mMockCentralManager).sendMessage(eq(deviceId), messageCaptor.capture());
        assertThat(messageCaptor.getValue().isMessageEncrypted()).isTrue();
    }

    @Test
    public void sendMessageSecurely_doesNotSendIfDeviceDisconnected() {
        String deviceId = connectNewDevice(mMockCentralManager);
        ConnectedDevice device = mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        mConnectedDeviceManager.removeConnectedDevice(deviceId, mMockCentralManager);
        UUID recipientId = UUID.randomUUID();
        byte[] message = ByteUtils.randomBytes(10);
        mConnectedDeviceManager.sendMessageSecurely(device, recipientId, message);
        verify(mMockCentralManager, times(0)).sendMessage(eq(deviceId), any(DeviceMessage.class));
    }

    @Test
    public void sendMessageUnsecurely_sendsMessageWithoutEncryption() {
        String deviceId = connectNewDevice(mMockCentralManager);
        ConnectedDevice device = mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        UUID recipientId = UUID.randomUUID();
        byte[] message = ByteUtils.randomBytes(10);
        mConnectedDeviceManager.sendMessageUnsecurely(device, recipientId, message);
        ArgumentCaptor<DeviceMessage> messageCaptor = ArgumentCaptor.forClass(DeviceMessage.class);
        verify(mMockCentralManager).sendMessage(eq(deviceId), messageCaptor.capture());
        assertThat(messageCaptor.getValue().isMessageEncrypted()).isFalse();
    }

    @Test
    public void connectionCallback_onDeviceConnectedInvokedForNewlyConnectedDevice()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        String deviceId = connectNewDevice(mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isTrue();
        ArgumentCaptor<ConnectedDevice> deviceCaptor =
                ArgumentCaptor.forClass(ConnectedDevice.class);
        verify(connectionCallback).onDeviceConnected(deviceCaptor.capture());
        ConnectedDevice connectedDevice = deviceCaptor.getValue();
        assertThat(connectedDevice.getDeviceId()).isEqualTo(deviceId);
        assertThat(connectedDevice.getHasSecureChannel()).isFalse();
    }

    @Test
    public void connectionCallback_onDeviceConnectedNotInvokedDeviceConnectedForDifferentUser()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        String deviceId = UUID.randomUUID().toString();
        String otherUserDeviceId = UUID.randomUUID().toString();
        when(mMockStorage.getActiveUserAssociatedDeviceIds()).thenReturn(
                Collections.singletonList(otherUserDeviceId));
        mConnectedDeviceManager.addConnectedDevice(deviceId, mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void connectionCallback_onDeviceConnectedNotInvokedForDifferentBleManager()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        String deviceId = connectNewDevice(mMockPeripheralManager);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        mConnectedDeviceManager.addConnectedDevice(deviceId, mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void connectionCallback_onDeviceDisconnectedInvokedForActiveUserDevice()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        String deviceId = connectNewDevice(mMockCentralManager);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        mConnectedDeviceManager.removeConnectedDevice(deviceId, mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isTrue();
        ArgumentCaptor<ConnectedDevice> deviceCaptor =
                ArgumentCaptor.forClass(ConnectedDevice.class);
        verify(connectionCallback).onDeviceDisconnected(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().getDeviceId()).isEqualTo(deviceId);
    }

    @Test
    public void connectionCallback_onDeviceDisconnectedNotInvokedDeviceForDifferentUser()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        String deviceId = UUID.randomUUID().toString();
        mConnectedDeviceManager.addConnectedDevice(deviceId, mMockCentralManager);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        mConnectedDeviceManager.removeConnectedDevice(deviceId, mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void unregisterConnectionCallback_removesCallbackAndNotInvoked()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        mConnectedDeviceManager.unregisterConnectionCallback(connectionCallback);
        connectNewDevice(mMockCentralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void registerDeviceCallback_blacklistsDuplicateRecipientId()
            throws InterruptedException {
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        Semaphore firstSemaphore = new Semaphore(0);
        Semaphore secondSemaphore = new Semaphore(0);
        Semaphore thirdSemaphore = new Semaphore(0);
        DeviceCallback firstDeviceCallback = createDeviceCallback(firstSemaphore);
        DeviceCallback secondDeviceCallback = createDeviceCallback(secondSemaphore);
        DeviceCallback thirdDeviceCallback = createDeviceCallback(thirdSemaphore);

        // Register three times for following chain of events:
        // 1. First callback registered without issue.
        // 2. Second callback with same recipientId triggers blacklisting both callbacks and issues
        //    error callbacks on both. Both callbacks should be unregistered at this point.
        // 3. Third callback gets rejected at registration and issues error callback.

        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                firstDeviceCallback, mCallbackExecutor);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                secondDeviceCallback, mCallbackExecutor);
        DeviceMessage message = new DeviceMessage(mRecipientId, false, new byte[10]);
        mConnectedDeviceManager.onMessageReceived(connectedDevice.getDeviceId(), message);
        assertThat(tryAcquire(firstSemaphore)).isTrue();
        assertThat(tryAcquire(secondSemaphore)).isTrue();
        verify(firstDeviceCallback)
                .onDeviceError(connectedDevice, DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED);
        verify(secondDeviceCallback)
                .onDeviceError(connectedDevice, DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED);
        verify(firstDeviceCallback, times(0)).onMessageReceived(any(), any());
        verify(secondDeviceCallback, times(0)).onMessageReceived(any(), any());

        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                thirdDeviceCallback, mCallbackExecutor);
        assertThat(tryAcquire(thirdSemaphore)).isTrue();
        verify(thirdDeviceCallback)
                .onDeviceError(connectedDevice, DEVICE_ERROR_INSECURE_RECIPIENT_ID_DETECTED);
    }

    @Test
    public void deviceCallback_onSecureChannelEstablishedInvoked() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        mConnectedDeviceManager.onSecureChannelEstablished(connectedDevice.getDeviceId(),
                mMockCentralManager);
        connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        assertThat(tryAcquire(semaphore)).isTrue();
        verify(deviceCallback).onSecureChannelEstablished(connectedDevice);
    }

    @Test
    public void deviceCallback_onSecureChannelEstablishedNotInvokedWithSecondBleManager()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        mConnectedDeviceManager.onSecureChannelEstablished(connectedDevice.getDeviceId(),
                mMockCentralManager);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        mConnectedDeviceManager.onSecureChannelEstablished(connectedDevice.getDeviceId(),
                mMockPeripheralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void deviceCallback_onMessageReceivedInvokedForSameRecipientId()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        byte[] payload = ByteUtils.randomBytes(10);
        DeviceMessage message = new DeviceMessage(mRecipientId, false, payload);
        mConnectedDeviceManager.onMessageReceived(connectedDevice.getDeviceId(), message);
        assertThat(tryAcquire(semaphore)).isTrue();
        verify(deviceCallback).onMessageReceived(connectedDevice, payload);
    }

    @Test
    public void deviceCallback_onMessageReceivedNotInvokedForDifferentRecipientId()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        byte[] payload = ByteUtils.randomBytes(10);
        DeviceMessage message = new DeviceMessage(UUID.randomUUID(), false, payload);
        mConnectedDeviceManager.onMessageReceived(connectedDevice.getDeviceId(), message);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void deviceCallback_onDeviceErrorInvokedOnChannelError() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        mConnectedDeviceManager.deviceErrorOccurred(connectedDevice.getDeviceId());
        assertThat(tryAcquire(semaphore)).isTrue();
        verify(deviceCallback).onDeviceError(connectedDevice, DEVICE_ERROR_INVALID_SECURITY_KEY);
    }

    @Test
    public void unregisterDeviceCallback_removesCallbackAndNotInvoked()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        mConnectedDeviceManager.unregisterDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback);
        mConnectedDeviceManager.onSecureChannelEstablished(connectedDevice.getDeviceId(),
                mMockPeripheralManager);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void registerDeviceCallback_sendsMissedMessageAfterRegistration()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        byte[] payload = ByteUtils.randomBytes(10);
        DeviceMessage message = new DeviceMessage(mRecipientId, false, payload);
        mConnectedDeviceManager.onMessageReceived(connectedDevice.getDeviceId(), message);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        assertThat(tryAcquire(semaphore)).isTrue();
        verify(deviceCallback).onMessageReceived(connectedDevice, payload);
    }

    @Test
    public void registerDeviceCallback_doesNotSendMissedMessageForDifferentRecipient()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        ConnectedDevice connectedDevice =
                mConnectedDeviceManager.getActiveUserConnectedDevices().get(0);
        byte[] payload = ByteUtils.randomBytes(10);
        DeviceMessage message = new DeviceMessage(UUID.randomUUID(), false, payload);
        mConnectedDeviceManager.onMessageReceived(connectedDevice.getDeviceId(), message);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void registerDeviceCallback_doesNotSendMissedMessageForDifferentDevice()
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        connectNewDevice(mMockCentralManager);
        connectNewDevice(mMockCentralManager);
        List<ConnectedDevice> connectedDevices =
                mConnectedDeviceManager.getActiveUserConnectedDevices();
        ConnectedDevice connectedDevice = connectedDevices.get(0);
        ConnectedDevice otherDevice = connectedDevices.get(1);
        byte[] payload = ByteUtils.randomBytes(10);
        DeviceMessage message = new DeviceMessage(mRecipientId, false, payload);
        mConnectedDeviceManager.onMessageReceived(otherDevice.getDeviceId(), message);
        DeviceCallback deviceCallback = createDeviceCallback(semaphore);
        mConnectedDeviceManager.registerDeviceCallback(connectedDevice, mRecipientId,
                deviceCallback, mCallbackExecutor);
        assertThat(tryAcquire(semaphore)).isFalse();
    }

    @Test
    public void onAssociationCompleted_disconnectsOriginalDeviceAndReconnectsAsActiveUser()
            throws InterruptedException {
        String deviceId = UUID.randomUUID().toString();
        mConnectedDeviceManager.addConnectedDevice(deviceId, mMockPeripheralManager);
        Semaphore semaphore = new Semaphore(0);
        ConnectionCallback connectionCallback = createConnectionCallback(semaphore);
        mConnectedDeviceManager.registerActiveUserConnectionCallback(connectionCallback,
                mCallbackExecutor);
        when(mMockStorage.getActiveUserAssociatedDeviceIds()).thenReturn(
                Collections.singletonList(deviceId));
        mConnectedDeviceManager.onAssociationCompleted(deviceId);
        assertThat(tryAcquire(semaphore)).isTrue();
    }

    private boolean tryAcquire(Semaphore semaphore) throws InterruptedException {
        return semaphore.tryAcquire(100, TimeUnit.MILLISECONDS);
    }

    @NonNull
    private String connectNewDevice(@NonNull CarBleManager carBleManager) {
        String deviceId = UUID.randomUUID().toString();
        when(mMockStorage.getActiveUserAssociatedDeviceIds()).thenReturn(
                Collections.singletonList(deviceId));
        mConnectedDeviceManager.addConnectedDevice(deviceId, carBleManager);
        return deviceId;
    }

    @NonNull
    private ConnectionCallback createConnectionCallback(@NonNull final Semaphore semaphore) {
        return spy(new ConnectionCallback() {
            @Override
            public void onDeviceConnected(ConnectedDevice device) {
                semaphore.release();
            }

            @Override
            public void onDeviceDisconnected(ConnectedDevice device) {
                semaphore.release();
            }
        });
    }

    @NonNull
    private DeviceCallback createDeviceCallback(@NonNull final Semaphore semaphore) {
        return spy(new DeviceCallback() {
            @Override
            public void onSecureChannelEstablished(ConnectedDevice device) {
                semaphore.release();
            }

            @Override
            public void onMessageReceived(ConnectedDevice device, byte[] message) {
                semaphore.release();
            }

            @Override
            public void onDeviceError(ConnectedDevice device, int error) {
                semaphore.release();
            }
        });
    }
}
