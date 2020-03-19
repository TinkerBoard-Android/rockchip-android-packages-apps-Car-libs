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

package com.android.car.connecteddevice.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Table entity representing a key for an associated device. */
@Entity(tableName = "associated_device_keys")
public class AssociatedDeviceKeyEntity {

    /** Id of the device. */
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String encryptedKey;

    public AssociatedDeviceKeyEntity() { }

    public AssociatedDeviceKeyEntity(String deviceId, String encryptedKey) {
        id = deviceId;
        this.encryptedKey = encryptedKey;
    }
}
