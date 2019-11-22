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

import android.bluetooth.BluetoothMapClient;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * {@link CompositeKey} subclass used to give each conversation on all the connected devices a
 * unique Key.
 */
public class ConversationKey extends CompositeKey implements Parcelable {

    public ConversationKey(String deviceId, String key) {
        super(deviceId, key);
    }

    /**
     * Creates a ConversationKey for a MMS/SMS message from a {@link BluetoothMapClient} intent.
     **/
    public static ConversationKey createMmsSmsConversationKey(Intent intent) {
        // Use a combination of senderName and senderContactUri for key. Ideally we would use
        // only senderContactUri (which is encoded phone no.). However since some phones don't
        // provide these, we fall back to senderName. Since senderName may not be unique, we
        // include senderContactUri also to provide uniqueness in cases it is available.
        return new ConversationKey(Utils.getBluetoothDeviceAddress(intent),
                intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME)
                        + "/" + intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_URI));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getDeviceId());
        dest.writeString(getSubKey());
    }

    /** Creates {@link ConversationKey} instances from {@link Parcel} sources. */
    public static final Parcelable.Creator<ConversationKey> CREATOR =
            new Parcelable.Creator<ConversationKey>() {
                @Override
                public ConversationKey createFromParcel(Parcel source) {
                    return new ConversationKey(source.readString(), source.readString());
                }

                @Override
                public ConversationKey[] newArray(int size) {
                    return new ConversationKey[size];
                }
            };

}
