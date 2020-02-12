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

import com.android.car.messenger.NotificationMsgProto.NotificationMsg;

/**
 * {@link CompositeKey} subclass used to give each contact on all the connected devices a
 * unique Key.
 */
public class SenderKey extends CompositeKey {
    /** Creates a senderkey for SMS, MMS, and {@link NotificationMsg}. **/
    protected SenderKey(String deviceId, String senderName, String contactUri) {
        // Use a combination of senderName and senderContactUri for key. Ideally we would use
        // only senderContactUri (which is encoded phone no.). However since some phones don't
        // provide these, we fall back to senderName. Since senderName may not be unique, we
        // include senderContactUri also to provide uniqueness in cases it is available.
        super(deviceId, senderName + "/" + contactUri);
    }
}
