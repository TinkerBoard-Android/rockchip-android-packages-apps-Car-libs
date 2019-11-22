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


import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.android.car.messenger.NotificationMsgProto.NotificationMsg;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.ConversationNotification;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyle;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyleMessage;

/** Utils methods for the car-messenger-common lib. **/
public class Utils {
    /**
     * Represents the maximum length of a message substring to be used when constructing the
     * message's unique handle/key.
     */
    private static final int MAX_SUB_MESSAGE_LENGTH = 5;

    /** Gets the latest message for a {@link NotificationMsg} Conversation. **/
    public static MessagingStyleMessage getLatestMessage(
            ConversationNotification notification) {
        MessagingStyle messagingStyle = notification.getMessagingStyle();
        long latestTime = 0;
        MessagingStyleMessage latestMessage = null;

        for (MessagingStyleMessage message : messagingStyle.getMessagingStyleMsgList()) {
            if (message.getTimestamp() > latestTime) {
                latestTime = message.getTimestamp();
                latestMessage = message;
            }
        }
        return latestMessage;
    }

    /**
     * Helper method to create a unique handle/key for this message. This is used as this Message's
     * {@link MessageKey#getSubKey()}.
     */
    public static String createMessageHandle(MessagingStyleMessage message) {
        String textMessage = message.getTextMessage();
        String subMessage = textMessage.substring(
                Math.min(MAX_SUB_MESSAGE_LENGTH, textMessage.length()));
        return message.getTimestamp() + "/" + message.getSender().getName() + "/" + subMessage;
    }

    /**
     * Extracts the {@link BluetoothDevice}'s address from an intent sent from
     * {@link android.bluetooth.BluetoothMapClient}.
     */
    public static String getBluetoothDeviceAddress(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        return device.getAddress();
    }
}
