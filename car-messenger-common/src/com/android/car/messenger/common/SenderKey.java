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

package com.android.car.messenger.common;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * {@link CompositeKey} subclass used to identify Notification info for a sender;
 * it uses a combination of senderContactUri and senderContactName as the secondary key.
 */
public class SenderKey extends CompositeKey implements Parcelable {

    private SenderKey(String deviceAddress, String key) {
        super(deviceAddress, key);
    }

    @Override
    public String toString() {
        return String.format("SenderKey: %s -- %s", getDeviceAddress(), getSubKey());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getDeviceAddress());
        dest.writeString(getSubKey());
    }

    /** Creates {@link SenderKey} instances from {@link Parcel} sources. */
    public static final Parcelable.Creator<SenderKey> CREATOR =
            new Parcelable.Creator<SenderKey>() {
                @Override
                public SenderKey createFromParcel(Parcel source) {
                    return new SenderKey(source.readString(), source.readString());
                }

                @Override
                public SenderKey[] newArray(int size) {
                    return new SenderKey[size];
                }
            };

}
