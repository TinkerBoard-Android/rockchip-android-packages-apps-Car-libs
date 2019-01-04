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

import android.app.Notification;
import android.app.Notification.MessagingStyle.Message;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;

import com.android.car.assist.client.tts.TextToSpeechHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles Assistant request fallbacks in the case that Assistant cannot fulfill the request for
 * any given reason.
 * <p/>
 * Simply reads out the notification messages for read requests, and speaks out
 * an error message for other requests.
 */
public class FallbackAssistant {

    private final TextToSpeechHelper mTextToSpeechHelper;

    public FallbackAssistant(TextToSpeechHelper ttsHelper) {
        mTextToSpeechHelper = ttsHelper;
    }

    /**
     * Handles a fallback read action by reading all messages in the notification.
     *
     * @param sbn the payload notification from which to extract messages from
     * @return true if successful
     */
    public boolean handleReadAction(StatusBarNotification sbn,
            TextToSpeechHelper.Listener listener) {
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

        return mTextToSpeechHelper.requestPlay(messages, listener);
    }

    /**
     * Handles generic (non-read) actions by reading out an error message.
     *
     * @param errorMessage the error message to read out
     * @return true if successful
     */
    public boolean handleErrorMessage(CharSequence errorMessage,
            TextToSpeechHelper.Listener listener) {
        if (mTextToSpeechHelper.isSpeaking()) {
            mTextToSpeechHelper.requestStop();
        }

        return mTextToSpeechHelper.requestPlay(Collections.singletonList(errorMessage), listener);
    }
}
