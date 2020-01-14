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

import static com.android.car.connecteddevice.util.SafeLog.logw;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.android.car.companiondevicesupport.api.external.CompanionDevice;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.ConversationNotification;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyle;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.PhoneToCarMessage;

import java.util.LinkedList;

/**
 * Represents a conversation notification's metadata that is shared between the conversation's
 * messages. Note, each {@link ConversationKey} should map to exactly one
 * ConversationNotificationInfo object.
 **/
public class ConversationNotificationInfo {
    private static final String TAG = "CMC.ConversationNotificationInfo";
    private static int sNextNotificationId = 0;
    final int mNotificationId = sNextNotificationId++;

    private final String mDeviceName;
    private final String mDeviceId;
    // This is always the sender name for SMS Messages from Bluetooth MAP.
    private final String mConvoTitle;
    private final boolean mIsGroupConvo;

    /** Only used for {@link NotificationMsg} conversations. **/
    @Nullable
    private final String mNotificationKey;
    @Nullable
    private final String mAppDisplayName;
    @Nullable
    private final String mUserDisplayName;
    private final int mAppSmallIconResId;

    public final LinkedList<MessageKey> mMessageKeys = new LinkedList<>();

    /**
     * Creates a ConversationNotificationInfo for a message received through Bluetooth MAP
     * profile.
     **/
    public static ConversationNotificationInfo createConversationNotificationInfo(Intent intent,
            int appSmallIconResId) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        return new ConversationNotificationInfo(device.getName(), device.getAddress(),
                intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME),
                /* isGroupConvo= */ false, /* notificationKey= */ null, /* appDisplayName= */ null,
                /* userDisplayName= */ null, appSmallIconResId);

    }

    /**
     * Creates a ConversationNotificationInfo for a {@link NotificationMsg}. Returns {@code null} if
     * the {@link ConversationNotification} is missing required fields.
     **/
    @Nullable
    public static ConversationNotificationInfo createConversationNotificationInfo(
            CompanionDevice device,
            ConversationNotification conversation, String notificationKey) {
        MessagingStyle messagingStyle = conversation.getMessagingStyle();

        if (!Utils.isValidConversationNotification(conversation, /* isShallowCheck= */ true)) {
            if (Log.isLoggable(TAG, Log.DEBUG) || Build.IS_DEBUGGABLE) {
                throw new IllegalArgumentException(
                        "ConversationNotificationInfo is missing required fields");
            } else {
                logw(TAG, "ConversationNotificationInfo is missing required fields");
                return null;
            }
        }

        return new ConversationNotificationInfo(device.getDeviceName(), device.getDeviceId(),
                messagingStyle.getConvoTitle(),
                messagingStyle.getIsGroupConvo(), notificationKey,
                conversation.getMessagingAppDisplayName(),
                messagingStyle.getUserDisplayName(), /* appSmallIconResId= */ 0);

    }

    private ConversationNotificationInfo(@Nullable String deviceName, String deviceId,
            String convoTitle, boolean isGroupConvo, @Nullable String notificationKey,
            @Nullable String appDisplayName, @Nullable String userDisplayName,
            int appSmallIconResId) {
        boolean missingDeviceId = (deviceId == null);
        boolean missingTitle = (convoTitle == null);
        if (missingDeviceId || missingTitle) {
            StringBuilder builder = new StringBuilder("Missing required fields:");
            if (missingDeviceId) {
                builder.append(" deviceId");
            }
            if (missingTitle) {
                builder.append(" convoTitle");
            }
            throw new IllegalArgumentException(builder.toString());
        }
        this.mDeviceName = deviceName;
        this.mDeviceId = deviceId;
        this.mConvoTitle = convoTitle;
        this.mIsGroupConvo = isGroupConvo;
        this.mNotificationKey = notificationKey;
        this.mAppDisplayName = appDisplayName;
        this.mUserDisplayName = userDisplayName;
        this.mAppSmallIconResId = appSmallIconResId;
    }

    /** Returns the id that should be used for this object's {@link android.app.Notification} **/
    public int getNotificationId() {
        return mNotificationId;
    }

    /** Returns the friendly name of the device that received the notification. **/
    public String getDeviceName() {
        return mDeviceName;
    }

    /** Returns the address of the device that received the notification. **/
    public String getDeviceId() {
        return mDeviceId;
    }

    /**
     * Returns the conversation title of this notification. If this notification came from MAP
     * profile, the title will be the Sender's name.
     */
    public String getConvoTitle() {
        return mConvoTitle;
    }

    /** Returns {@code true} if this message is in a group conversation **/
    public boolean isGroupConvo() {
        return mIsGroupConvo;
    }

    /**
     * Returns the key if this conversation is based on a {@link ConversationNotification}. Refer to
     * {@link PhoneToCarMessage#getNotificationKey()} for more info.
     */
    @Nullable
    public String getNotificationKey() {
        return mNotificationKey;
    }

    /**
     * Returns the display name of the application that posted this notification if this object is
     * based on a {@link ConversationNotification}.
     **/
    @Nullable
    public String getAppDisplayName() {
        return mAppDisplayName;
    }

    /**
     * Returns the User Display Name if this object is based on a @link ConversationNotification}.
     * This is needed for {@link android.app.Notification.MessagingStyle}.
     */
    @Nullable
    public String getUserDisplayName() {
        return mUserDisplayName;
    }


    /** Returns the icon's resource id of the application that posted this notification. **/
    public int getAppSmallIconResId() {
        return mAppSmallIconResId;
    }

    public MessageKey getLastMessageKey() {
        return mMessageKeys.getLast();
    }
}
