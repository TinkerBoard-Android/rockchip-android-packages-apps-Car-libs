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

package com.android.car.connecteddevice.connection.spp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.connecteddevice.StreamProtos.DeviceMessageProto.BleDeviceMessage;
import com.android.car.connecteddevice.StreamProtos.OperationProto;
import com.android.car.connecteddevice.StreamProtos.VersionExchangeProto;
import com.android.car.connecteddevice.connection.DeviceMessage;
import com.android.car.connecteddevice.connection.DeviceMessageStream;
import com.android.car.connecteddevice.util.ByteUtils;
import com.android.car.protobuf.ByteString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SppDeviceMessageStreamTest {
    private final byte[] mTestData = "testData".getBytes();
    private final UUID mTestUuid = UUID.randomUUID();
    private final OperationProto.OperationType mTestOperationType =
            OperationProto.OperationType.CLIENT_MESSAGE;
    private final boolean mIsEncrypted = false;
    private final DeviceMessage mTestDeviceMessage = new DeviceMessage(mTestUuid, mIsEncrypted,
            mTestData);
    private final BleDeviceMessage mBleDeviceMessage = BleDeviceMessage.newBuilder()
            .setOperation(mTestOperationType)
            .setIsPayloadEncrypted(mIsEncrypted)
            .setPayload(ByteString.copyFrom(mTestData))
            .setRecipient(ByteString.copyFrom(ByteUtils.uuidToBytes(mTestUuid)))
            .build();

    private final byte[] mVersionExchangeMessage =
            VersionExchangeProto.BleVersionExchange.newBuilder()
                    .setMinSupportedMessagingVersion(SppDeviceMessageStream.MESSAGING_VERSION)
                    .setMaxSupportedMessagingVersion(SppDeviceMessageStream.MESSAGING_VERSION)
                    .setMinSupportedSecurityVersion(SppDeviceMessageStream.SECURITY_VERSION)
                    .setMaxSupportedSecurityVersion(SppDeviceMessageStream.SECURITY_VERSION)
                    .build()
                    .toByteArray();
    @Mock
    private SppManager mMockSppManager;
    private DeviceMessageStream.MessageReceivedListener mMessageReceivedListener = spy(
            (deviceMessage, operationType) -> {
            });
    private DeviceMessageStream.MessageReceivedErrorListener mMessageReceivedErrorListener = spy(
            exception -> {
            }
    );
    private BluetoothDevice mBluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
            "00:11:22:33:44:55");
    private MockitoSession mMockingSession;
    private SppDeviceMessageStream mSppDeviceMessageStream;

    @Before
    public void setUp() {
        mMockingSession = mockitoSession()
                .initMocks(this)
                .strictness(Strictness.WARN)
                .startMocking();
        mSppDeviceMessageStream = new SppDeviceMessageStream(mMockSppManager, mBluetoothDevice);
    }

    @After
    public void tearDown() {
        if (mMockingSession != null) {
            mMockingSession.finishMocking();
        }
    }

    @Test
    public void testWriteMessage_CallSppManagerWriteMethod() {
        mSppDeviceMessageStream.writeMessage(mTestDeviceMessage, mTestOperationType);
        verify(mMockSppManager).write(mBleDeviceMessage.toByteArray());
    }

    @Test
    public void testOnMessageReceived_InformMessageReceivedListener() {
        mSppDeviceMessageStream.onMessageReceived(mBluetoothDevice, mVersionExchangeMessage);
        mSppDeviceMessageStream.setMessageReceivedListener(mMessageReceivedListener);
        mSppDeviceMessageStream.onMessageReceived(mBluetoothDevice,
                mBleDeviceMessage.toByteArray());
        verify(mMessageReceivedListener).onMessageReceived(mTestDeviceMessage, mTestOperationType);
    }

    @Test
    public void testOnMessageReceived_InformOnMessageReceivedErrorListener() {
        mSppDeviceMessageStream.onMessageReceived(mBluetoothDevice, mVersionExchangeMessage);
        mSppDeviceMessageStream.setMessageReceivedErrorListener(mMessageReceivedErrorListener);
        mSppDeviceMessageStream.onMessageReceived(mBluetoothDevice, mTestData);
        verify(mMessageReceivedErrorListener).onMessageReceivedError(any());
    }

}
