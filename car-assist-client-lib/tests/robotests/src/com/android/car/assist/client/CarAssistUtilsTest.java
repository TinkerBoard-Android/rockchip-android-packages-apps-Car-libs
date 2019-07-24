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
package com.android.car.assist.client;

import static com.google.common.truth.Truth.assertThat;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CarAssistUtilsTest {

    private Context mContext;
    private static final String PKG_1 = "package_1";
    private static final String OP_PKG = "OpPackage";
    private static final int ID = 1;
    private static final String TAG = "Tag";
    private static final int UID = 2;
    private static final int INITIAL_PID = 3;
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String CONTENT_TITLE = "CONTENT_TITLE";
    private static final String STATIC_USER_NAME = "STATIC_USER_NAME";
    private static final String SENDER_NAME = "Larry";
    private static final String SENDER_CONTACT_URI = "TEST_SENDER_URI";
    private static final String REMOTE_INPUT_KEY = "REMOTE_INPUT_KEY";
    private static final String REPLY_ACTION = "test.package.REPLY";
    private static final String READ_ACTION = "test.package.READ";
    private static final long POST_TIME = 12345L;
    private static final int ICON = android.R.drawable.ic_media_play;
    private static final String OVERRIDE_GROUP_KEY = "OVERRIDE_GROUP_KEY";
    private static final UserHandle USER_HANDLE = new UserHandle(12);

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testCarCompatMessagingNotification_qualifyingNotification() {
        assertThat(CarAssistUtils.isCarCompatibleMessagingNotification(
                buildStatusBarNotification(/* hasReplyAction */ true, /* hasMessagingStyle */
                        true))).isTrue();
    }

    @Test
    public void testCarCompatMessagingNotification_noReplyNotification() {
        assertThat(CarAssistUtils.isCarCompatibleMessagingNotification(
                buildStatusBarNotification(/* hasReplyAction */ false, /* hasMessagingStyle */
                        true))).isTrue();
    }

    @Test
    public void testCarCompatMessagingNotifcation_noMessagingStyleNotification() {
        assertThat(CarAssistUtils.isCarCompatibleMessagingNotification(
                buildStatusBarNotification(/* hasReplyAction */ true, /* hasMessagingStyle */
                        false))).isFalse();
    }

    private StatusBarNotification buildStatusBarNotification(boolean hasReplyAction,
            boolean hasMessagingStyle) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setContentTitle(CONTENT_TITLE)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .addAction(buildMarkAsReadAction())
                .setShowWhen(true);

        if (hasReplyAction) {
            builder.addAction(buildReplyAction());
        }

        if (hasMessagingStyle) {
            builder.setStyle(buildMessagingStyle());
        }

        return new StatusBarNotification(PKG_1, OP_PKG,
                ID, TAG, UID, INITIAL_PID, builder.build(), USER_HANDLE,
                OVERRIDE_GROUP_KEY, POST_TIME);
    }

    private Action buildMarkAsReadAction() {
        Intent intent = new Intent(mContext, this.getClass()).setAction(READ_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new Action.Builder(ICON, "read", pendingIntent)
                .setSemanticAction(Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build();
    }

    private Action buildReplyAction() {
        Intent intent = new Intent(mContext, this.getClass())
                .setAction(REPLY_ACTION);
        PendingIntent replyIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new Action.Builder(ICON,
                "reply", replyIntent)
                .setSemanticAction(Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .addRemoteInput(
                        new RemoteInput.Builder(REMOTE_INPUT_KEY)
                                .build()
                )
                .build();
    }

    private MessagingStyle buildMessagingStyle() {
        Person user = new Person.Builder()
                .setName(STATIC_USER_NAME)
                .build();
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(user);
        Person sender = new Person.Builder()
                .setName(SENDER_NAME)
                .setUri(SENDER_CONTACT_URI)
                .build();
        messagingStyle.addMessage("Hello World", POST_TIME, sender);
        return messagingStyle;
    }
}
