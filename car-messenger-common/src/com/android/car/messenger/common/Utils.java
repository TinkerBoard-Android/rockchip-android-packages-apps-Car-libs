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

import static com.android.car.connecteddevice.util.SafeLog.logd;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.android.car.messenger.NotificationMsgProto.NotificationMsg;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.ConversationNotification;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyle;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyleMessage;

/** Utils methods for the car-messenger-common lib. **/
public class Utils {
    private static final String TAG = "CMC.Utils";
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

    /**
     * Ensure the {@link ConversationNotification} object has all the required fields.
     *
     * @param isShallowCheck should be {@code true} if the caller only wants to verify the
     *                       notification and its {@link MessagingStyle} is valid, without checking
     *                       all of the notification's {@link MessagingStyleMessage}s.
     **/
    public static boolean isValidConversationNotification(ConversationNotification notification,
            boolean isShallowCheck) {
        if (notification == null) {
            logd(TAG, "ConversationNotification is null");
            return false;
        } else if (!notification.hasMessagingStyle()) {
            logd(TAG, "ConversationNotification is missing required field: messagingStyle");
            return false;
        } else if (notification.getMessagingAppDisplayName() == null) {
            logd(TAG, "ConversationNotification is missing required field: appDisplayName");
            return false;
        } else if (notification.getMessagingAppPackageName() == null) {
            logd(TAG, "ConversationNotification is missing required field: appPackageName");
            return false;
        }
        return isValidMessagingStyle(notification.getMessagingStyle(), isShallowCheck);
    }

    /**
     * Ensure the {@link MessagingStyle} object has all the required fields.
     **/
    private static boolean isValidMessagingStyle(MessagingStyle messagingStyle,
            boolean isShallowCheck) {
        if (messagingStyle == null) {
            logd(TAG, "MessagingStyle is null");
            return false;
        } else if (messagingStyle.getConvoTitle() == null) {
            logd(TAG, "MessagingStyle is missing required field: convoTitle");
            return false;
        } else if (messagingStyle.getUserDisplayName() == null) {
            logd(TAG, "MessagingStyle is missing required field: userDisplayName");
            return false;
        } else if (messagingStyle.getMessagingStyleMsgCount() == 0) {
            logd(TAG, "MessagingStyle is missing required field: messagingStyleMsg");
            return false;
        }
        if (!isShallowCheck) {
            for (MessagingStyleMessage message : messagingStyle.getMessagingStyleMsgList()) {
                if (!isValidMessagingStyleMessage(message)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Ensure the {@link MessagingStyleMessage} object has all the required fields.
     **/
    public static boolean isValidMessagingStyleMessage(MessagingStyleMessage message) {
        if (message == null) {
            logd(TAG, "MessagingStyleMessage is null");
            return false;
        } else if (message.getTextMessage() == null) {
            logd(TAG, "MessagingStyleMessage is missing required field: textMessage");
            return false;
        } else if (!message.hasSender()) {
            logd(TAG, "MessagingStyleMessage is missing required field: sender");
            return false;
        }
        return true;
    }
}
