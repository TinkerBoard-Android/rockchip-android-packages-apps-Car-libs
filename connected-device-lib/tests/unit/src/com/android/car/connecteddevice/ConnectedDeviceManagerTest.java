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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.connecteddevice.ble.CarBleCentralManager;
import com.android.car.connecteddevice.ble.CarBlePeripheralManager;
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ConnectedDeviceManagerTest {

    @Mock
    private CarCompanionDeviceStorage mMockStorage;

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
    public void connectToActiveUserDevice_startsAdvertisingWithDeviceId() {
        UUID deviceId = UUID.randomUUID();
        when(mMockStorage.getTrustedDevicesForUser(anyInt())).thenReturn(
                Collections.singletonList(deviceId.toString()));
        mConnectedDeviceManager.connectToActiveUserDevice();
        verify(mMockPeripheralManager).connectToDevice(deviceId);
    }
}
