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

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Notification.MessagingStyle.Message;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.car.assist.client.tts.TextToSpeechHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Assistant request fallbacks in the case that Assistant cannot fulfill the request for
 * any given reason.
 * <p/>
 * Simply reads out the notification messages for read requests, and speaks out
 * an error message for other requests.
 */
public class FallbackAssistant {

    private static final String TAG = FallbackAssistant.class.getSimpleName();

    private final Context mContext;
    private final TextToSpeechHelper mTextToSpeechHelper;
    private final RequestIdGenerator mRequestIdGenerator;

    private Map<Long, StatusBarNotification> mPlayMessageRequestTracker = new HashMap<>();

    private final TextToSpeechHelper.Listener mListener = new TextToSpeechHelper.Listener() {
        @Override
        public void onTextToSpeechStarted(long requestId) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTextToSpeechStarted");
            }
        }

        @Override
        public void onTextToSpeechStopped(long requestId, boolean error) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTextToSpeechStopped");
            }
            if (error) {
                Toast.makeText(mContext, mContext.getString(R.string.assist_action_failed_toast),
                        Toast.LENGTH_LONG).show();
            } else if (mPlayMessageRequestTracker.containsKey(requestId)) {
                sendMarkAsReadIntent(mPlayMessageRequestTracker.get(requestId));
            }
            mPlayMessageRequestTracker.remove(requestId);
        }
    };

    public FallbackAssistant(Context context) {
        mContext = context;
        mTextToSpeechHelper = new TextToSpeechHelper(context, mListener);
        mRequestIdGenerator = new RequestIdGenerator();
    }

    /**
     * Handles a fallback read action by reading all messages in the notification.
     *
     * @param sbn the payload notification from which to extract messages from
     * @return true if successful
     */
    public boolean handleReadAction(StatusBarNotification sbn) {
        if (mTextToSpeechHelper.isSpeaking()) {
            mTextToSpeechHelper.requestStop();
        }

        Parcelable[] messagesBundle = sbn.getNotification().extras
                .getParcelableArray(Notification.EXTRA_MESSAGES);

        if (messagesBundle == null || messagesBundle.length == 0) {
            return false;
        }

        List<CharSequence> messages = new ArrayList<>();
        for (Message message : Message.getMessagesFromBundleArray(messagesBundle)) {
            messages.add(message.getText());
        }

        long requestId = mRequestIdGenerator.generateRequestId();
        mPlayMessageRequestTracker.put(requestId, sbn);

        return mTextToSpeechHelper.requestPlay(messages, requestId);
    }

    private void sendMarkAsReadIntent(StatusBarNotification sbn) {
        NotificationCompat.Action markAsReadAction = CarAssistUtils.getMarkAsReadAction(
                sbn.getNotification());
        boolean isDebugLoggable = Log.isLoggable(TAG, Log.DEBUG);

        if (markAsReadAction != null) {
            if (sendPendingIntent(markAsReadAction.getActionIntent(),
                    null /* resultIntent */) != ActivityManager.START_SUCCESS
                    && isDebugLoggable) {
                Log.d(TAG, "Could not relay mark as read event to the messaging app.");
            }
        } else if (isDebugLoggable) {
            Log.d(TAG, "Car compat message notification has no mark as read action: "
                    + sbn.getKey());
        }
    }

    private int sendPendingIntent(PendingIntent pendingIntent, Intent resultIntent) {
        try {
            return pendingIntent.sendAndReturnResult(/* context= */ mContext, /* code= */ 0,
                    /* intent= */ resultIntent, /* onFinished= */null,
                    /* handler= */ null, /* requiredPermissions= */ null,
                    /* options= */ null);
        } catch (PendingIntent.CanceledException e) {
            // Do not take down the app over this
            Log.w(TAG, "Sending contentIntent failed: " + e);
            return ActivityManager.START_ABORTED;
        }
    }

    /**
     * Handles generic (non-read) actions by reading out an error message.
     *
     * @param errorMessage the error message to read out
     * @return true if successful
     */
    public boolean handleErrorMessage(CharSequence errorMessage) {
        if (mTextToSpeechHelper.isSpeaking()) {
            mTextToSpeechHelper.requestStop();
        }

        return mTextToSpeechHelper.requestPlay(Collections.singletonList(errorMessage),
                mRequestIdGenerator.generateRequestId());
    }

    /** Helper class that generates unique IDs per TTS request. **/
    private class RequestIdGenerator {
        private long mCounter;

        RequestIdGenerator() {
            mCounter = 0;
        }

        public long generateRequestId() {
            return ++mCounter;
        }
    }
}
