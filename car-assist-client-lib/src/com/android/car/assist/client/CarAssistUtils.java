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

import static android.app.Notification.Action.SEMANTIC_ACTION_MARK_AS_READ;
import static android.app.Notification.Action.SEMANTIC_ACTION_REPLY;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.car.assist.CarVoiceInteractionSession;
import com.android.car.assist.client.tts.TextToSpeechHelper;
import com.android.internal.app.AssistUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Util class providing helper methods to interact with the current active voice service,
 * while ensuring that the active voice service has the required permissions.
 */
public class CarAssistUtils {
    public static final String TAG = "CarAssistUtils";
    private static final List<Integer> REQUIRED_SEMANTIC_ACTIONS = Collections.unmodifiableList(
            Arrays.asList(
                    SEMANTIC_ACTION_MARK_AS_READ
            )
    );

    private static final List<Integer> SUPPORTED_SEMANTIC_ACTIONS = Collections.unmodifiableList(
            Arrays.asList(
                    SEMANTIC_ACTION_MARK_AS_READ,
                    SEMANTIC_ACTION_REPLY
            )
    );

    private final TextToSpeechHelper.Listener mListener = new TextToSpeechHelper.Listener() {
        @Override
        public void onTextToSpeechStarted() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTextToSpeechStarted");
            }
        }

        @Override
        public void onTextToSpeechStopped(boolean error) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTextToSpeechStopped");
            }
            if (error) {
                Toast.makeText(mContext, mErrorMessage, Toast.LENGTH_LONG).show();
            }
        }
    };

    private final Context mContext;
    private final AssistUtils mAssistUtils;
    private final FallbackAssistant mFallbackAssistant;
    private final String mErrorMessage;

    public CarAssistUtils(Context context) {
        mContext = context;
        mAssistUtils = new AssistUtils(context);
        mFallbackAssistant = new FallbackAssistant(new TextToSpeechHelper(context));
        mErrorMessage = context.getString(R.string.assist_action_failed_toast);
    }

    /**
     * Returns true if the current active assistant has notification listener permissions.
     */
    public boolean assistantIsNotificationListener() {
        final String activeComponent = mAssistUtils.getActiveServiceComponentName()
                .flattenToString();
        int slashIndex = activeComponent.indexOf("/");
        final String activePackage = activeComponent.substring(0, slashIndex);
        // TODO: remove this log
        Log.d(TAG, "assistantIsNotificationListener: activeComponent: " + activeComponent
                + " activePackage: " + activePackage + " active user: "
                + mContext.getContentResolver().getUserId());

        final String listeners = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);

        if (listeners != null) {
            // TODO: remove this log
            Log.d(TAG, " Listeners are: " + listeners);
            for (String listener : Arrays.asList(listeners.split(":"))) {
                if (listener.contains(activePackage)) {
                    Log.d(TAG, "Active assistant has notification listener: " + listener);
                    return true;
                }
            }
        }
        Log.w(TAG, "No notification listeners found for assistant: " + activeComponent);
        return false;
    }

    /**
     * Checks whether the notification is a car-compatible messaging notification.
     *
     * @param sbn The notification being checked.
     * @return true if the notification is a car-compatible messaging notification.
     */
    public static boolean isCarCompatibleMessagingNotification(StatusBarNotification sbn) {
        return hasMessagingStyle(sbn)
                && hasRequiredAssistantCallbacks(sbn)
                && replyCallbackHasRemoteInput(sbn)
                && assistantCallbacksShowNoUi(sbn);
    }

    /** Returns true if the semantic action provided can be supported. */
    public static boolean isSupportedSemanticAction(int semanticAction) {
        return SUPPORTED_SEMANTIC_ACTIONS.contains(semanticAction);
    }

    /**
     * Returns true if the notification has a messaging style.
     * <p/>
     * This is the case if the notification in question was provided an instance of
     * {@link Notification.MessagingStyle} (or an instance of
     * {@link NotificationCompat.MessagingStyle} if {@link NotificationCompat} was used).
     */
    private static boolean hasMessagingStyle(StatusBarNotification sbn) {
        return NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(sbn.getNotification()) != null;
    }

    /**
     * Returns true if the notification has the required Assistant callbacks to be considered
     * a car-compatible messaging notification. The callbacks must be unambiguous, therefore false
     * is returned if multiple callbacks exist for any semantic action that is supported.
     */
    private static boolean hasRequiredAssistantCallbacks(StatusBarNotification sbn) {
        List<Integer> semanticActionList = Arrays.stream(sbn.getNotification().actions)
                .map(Notification.Action::getSemanticAction)
                .filter(REQUIRED_SEMANTIC_ACTIONS::contains)
                .collect(Collectors.toList());
        Set<Integer> semanticActionSet = new HashSet<>(semanticActionList);

        return semanticActionList.size() == semanticActionSet.size()
                && semanticActionSet.containsAll(REQUIRED_SEMANTIC_ACTIONS);
    }

    /**
     * Returns true if the reply callback has at least one {@link RemoteInput}.
     * <p/>
     * Precondition: There exists only one reply callback.
     */
    private static boolean replyCallbackHasRemoteInput(StatusBarNotification sbn) {
        return Arrays.stream(sbn.getNotification().actions)
                .filter(action -> action.getSemanticAction() == SEMANTIC_ACTION_REPLY)
                .map(Notification.Action::getRemoteInputs)
                .filter(Objects::nonNull)
                .anyMatch(remoteInputs -> remoteInputs.length > 0);
    }

    /** Returns true if all Assistant callbacks indicate that they show no UI, false otherwise. */
    private static boolean assistantCallbacksShowNoUi(StatusBarNotification sbn) {
        final Notification notification = sbn.getNotification();
        return IntStream.range(0, notification.actions.length)
                .mapToObj(i -> NotificationCompat.getAction(notification, i))
                .filter(Objects::nonNull)
                .filter(action -> SUPPORTED_SEMANTIC_ACTIONS.contains(action.getSemanticAction()))
                .noneMatch(NotificationCompat.Action::getShowsUserInterface);
    }

    /**
     * Requests a given action from the current active Assistant.
     *
     * @param sbn            the notification payload to deliver to assistant
     * @param semanticAction the semantic action that is to be requested
     * @return true if the request was successful
     */
    public boolean requestAssistantVoiceAction(StatusBarNotification sbn, int semanticAction) {
        if (!isCarCompatibleMessagingNotification(sbn)) {
            Log.w(TAG, "Assistant action requested for non-compatible notification.");
            return false;
        }

        switch (semanticAction) {
            case SEMANTIC_ACTION_MARK_AS_READ:
                return readMessageNotification(sbn);
            case SEMANTIC_ACTION_REPLY:
                return replyMessageNotification(sbn);
            default:
                return false;
        }
    }

    /**
     * Requests a read action for the notification from the current active Assistant.
     * If the Assistant is cannot handle the request, a fallback implementation will attempt to
     * handle it.
     *
     * @param sbn the notification to deliver as the payload
     * @return true if the read request was handled successfully
     */
    private boolean readMessageNotification(StatusBarNotification sbn) {
        Log.i(TAG, " in readMessageNotification");
        return requestAction(BundleBuilder.buildAssistantReadBundle(sbn))
                || mFallbackAssistant.handleReadAction(sbn, mListener);
    }

    /**
     * Requests a reply action for the notification from the current active Assistant.
     * If the Assistant is cannot handle the request, a fallback implementation will attempt to
     * handle it.
     *
     * @param sbn the notification to deliver as the payload
     * @return true if the reply request was handled successfully
     */
    private boolean replyMessageNotification(StatusBarNotification sbn) {
        return requestAction(BundleBuilder.buildAssistantReplyBundle(sbn))
                || mFallbackAssistant.handleErrorMessage(mErrorMessage, mListener);
    }

    private boolean requestAction(Bundle payloadArguments) {
        if (assistantIsNotificationListener()) {
            if (mAssistUtils.showSessionForActiveService(payloadArguments,
                    CarVoiceInteractionSession.SHOW_SOURCE_NOTIFICATION, null, null)) {
                Log.d(TAG, " Successfully showed session, returning true");
                return true;
            } else {
                Log.d(TAG, "Session could not be shown for active service");
            }
        }
        return false;
    }
}
