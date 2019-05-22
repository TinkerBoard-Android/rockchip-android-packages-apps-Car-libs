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

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.car.assist.CarVoiceInteractionSession;
import com.android.internal.app.AssistUtils;

import java.util.ArrayList;
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

    private final Context mContext;
    private final AssistUtils mAssistUtils;
    private final FallbackAssistant mFallbackAssistant;
    private final String mErrorMessage;

    public CarAssistUtils(Context context) {
        mContext = context;
        mAssistUtils = new AssistUtils(context);
        mFallbackAssistant = new FallbackAssistant(context);
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

        final String listeners = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS, ActivityManager.getCurrentUser());

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Current user: " + ActivityManager.getCurrentUser()
                    + " has active voice service: " + activePackage + " and enabled notification "
                    + " listeners: " + listeners);
        }

        if (listeners != null) {
            for (String listener : Arrays.asList(listeners.split(":"))) {
                if (listener.contains(activePackage)) {
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
        List<Integer> semanticActionList = getAllActions(sbn.getNotification())
                .stream()
                .map(NotificationCompat.Action::getSemanticAction)
                .filter(REQUIRED_SEMANTIC_ACTIONS::contains)
                .collect(Collectors.toList());
        Set<Integer> semanticActionSet = new HashSet<>(semanticActionList);
        return semanticActionList.size() == semanticActionSet.size()
                && semanticActionSet.containsAll(REQUIRED_SEMANTIC_ACTIONS);
    }

    /** Retrieves all visible and invisible {@link Action}s from the {@link #notification}. */
    public static List<NotificationCompat.Action> getAllActions(Notification notification) {
        List<NotificationCompat.Action> actions = new ArrayList<>();
        actions.addAll(NotificationCompat.getInvisibleActions(notification));
        for (int i = 0; i < NotificationCompat.getActionCount(notification); i++) {
            actions.add(NotificationCompat.getAction(notification, i));
        }
        return actions;
    }

    /**
     * Retrieves the {@link NotificationCompat.Action} containing the
     * {@link NotificationCompat.Action#SEMANTIC_ACTION_MARK_AS_READ} semantic action.
     */
    @Nullable
    public static NotificationCompat.Action getMarkAsReadAction(Notification notification) {
        for (NotificationCompat.Action action : getAllActions(notification)) {
            if (action.getSemanticAction()
                    == NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ) {
                return action;
            }
        }
        return null;
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
     * @param sbn         the notification payload to deliver to assistant
     * @param voiceAction must be a valid {@link CarVoiceInteractionSession} VOICE_ACTION
     * @return true if the request was successful
     */
    public boolean requestAssistantVoiceAction(StatusBarNotification sbn, String voiceAction) {
        if (!isCarCompatibleMessagingNotification(sbn)) {
            Log.w(TAG, "Assistant action requested for non-compatible notification.");
            return false;
        }

        switch (voiceAction) {
            case CarVoiceInteractionSession.VOICE_ACTION_READ_NOTIFICATION:
                return readMessageNotification(sbn);
            case CarVoiceInteractionSession.VOICE_ACTION_REPLY_NOTIFICATION:
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
        return requestAction(BundleBuilder.buildAssistantReadBundle(sbn))
                || mFallbackAssistant.handleReadAction(sbn);
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
                || mFallbackAssistant.handleErrorMessage(mErrorMessage);
    }

    private boolean requestAction(Bundle payloadArguments) {
        if (assistantIsNotificationListener()) {
            if (mAssistUtils.showSessionForActiveService(payloadArguments,
                    CarVoiceInteractionSession.SHOW_SOURCE_NOTIFICATION, null, null)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, " Successfully showed session, returning true");
                }
                return true;
            } else {
                Log.w(TAG, "Session could not be shown for active service");
            }
        }
        return false;
    }
}
