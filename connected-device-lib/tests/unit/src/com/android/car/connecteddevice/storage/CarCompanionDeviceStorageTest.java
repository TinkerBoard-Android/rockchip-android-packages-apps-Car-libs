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

import static com.google.common.truth.Truth.assertThat;

import android.car.trust.TrustedDeviceInfo;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class CarCompanionDeviceStorageTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CarCompanionDeviceStorage mCarCompanionDeviceStorage;

    @Before
    public void setUp() {
        mCarCompanionDeviceStorage = new CarCompanionDeviceStorage(mContext);
    }

    @After
    public void tearDown() {
        // Clearing the SharedPreferences. Even though a new CarcCmpanionDeviceStorage is created
        // per test, the underlying SharedPreferences being used should still be the same.
        mCarCompanionDeviceStorage.getSharedPrefs().edit().clear().apply();
    }

    @Test
    public void testExtractTrustedDeviceInfo() {
        TrustedDeviceInfo trustedDeviceInfo =
                new TrustedDeviceInfo(/* handle= */ 5L, /* address= */ "address",
                        /* name= */ "name");

        String deviceInfoWithId =
                CarCompanionDeviceStorage.serializeDeviceInfoWithId(trustedDeviceInfo,
                        /* id= */ "id");

        assertThat(CarCompanionDeviceStorage.extractDeviceInfo(deviceInfoWithId))
                .isEqualTo(trustedDeviceInfo);
    }

    @Test
    public void testExtractDeviceId() {
        TrustedDeviceInfo trustedDeviceInfo =
                new TrustedDeviceInfo(/* handle= */ 5L, /* address= */ "address",
                        /* name= */ "name");

        String deviceId = "deviceId";
        String deviceInfoWithId =
                CarCompanionDeviceStorage.serializeDeviceInfoWithId(trustedDeviceInfo, deviceId);

        assertThat(CarCompanionDeviceStorage.extractDeviceId(deviceInfoWithId)).isEqualTo(deviceId);
    }
}
