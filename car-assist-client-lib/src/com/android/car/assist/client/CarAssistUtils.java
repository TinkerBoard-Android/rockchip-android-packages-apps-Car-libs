/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.car.assist.CarVoiceInteractionSession;
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
    private static final List<Integer> sRequiredSemanticActions = Collections.unmodifiableList(
            Arrays.asList(
                    Notification.Action.SEMANTIC_ACTION_REPLY,
                    Notification.Action.SEMANTIC_ACTION_MARK_AS_READ
            )
    );

    // Currently, all supported semantic actions are required.
    private static final List<Integer> sSupportedSemanticActions = sRequiredSemanticActions;

    private final Context mContext;
    private final AssistUtils mAssistUtils;

    public CarAssistUtils(Context context) {
        mAssistUtils = new AssistUtils(context);
        mContext = context;
    }

    /**
     * Returns true if the current active assistant has notification listener permissions.
     */
    public boolean assistantIsNotificationListener() {
        final String activeComponent = mAssistUtils.getActiveServiceComponentName()
                .flattenToString();
        final String listeners = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);

        return listeners != null
                && Arrays.asList(listeners.split(":")).contains(activeComponent);
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
        return sSupportedSemanticActions.contains(semanticAction);
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
                .filter(sRequiredSemanticActions::contains)
                .collect(Collectors.toList());
        Set<Integer> semanticActionSet = new HashSet<>(semanticActionList);

        return semanticActionList.size() == semanticActionSet.size()
                && semanticActionSet.containsAll(sRequiredSemanticActions);
    }

    /**
     * Returns true if the reply callback has exactly one RemoteInput.
     * <p/>
     * Precondition: There exists only one reply callback.
     */
    private static boolean replyCallbackHasRemoteInput(StatusBarNotification sbn) {
        return Arrays.stream(sbn.getNotification().actions)
                .filter(action ->
                        action.getSemanticAction() == Notification.Action.SEMANTIC_ACTION_REPLY)
                .map(Notification.Action::getRemoteInputs)
                .anyMatch(remoteInputs -> remoteInputs != null && remoteInputs.length == 1);
    }

    /** Returns true if all Assistant callbacks indicate that they show no UI, false otherwise. */
    private static boolean assistantCallbacksShowNoUi(StatusBarNotification sbn) {
        final Notification notification = sbn.getNotification();
        return IntStream.range(0, notification.actions.length)
                .mapToObj(i -> NotificationCompat.getAction(notification, i))
                .filter(Objects::nonNull)
                .filter(action -> sRequiredSemanticActions.contains(action.getSemanticAction()))
                .noneMatch(NotificationCompat.Action::getShowsUserInterface);
    }

    /**
     * Requests a given action from the current active assistant.
     *
     * @param sbn the notification payload to deliver to assistant
     * @param semanticAction the semantic action that is to be requested
     * @return true if the request was successful
     */
    public boolean requestAssistantAction(StatusBarNotification sbn, int semanticAction) {
        switch (semanticAction) {
            case Notification.Action.SEMANTIC_ACTION_MARK_AS_READ:
                return readMessageNotification(sbn);
            case Notification.Action.SEMANTIC_ACTION_REPLY:
                return replyMessageNotification(sbn);
            default:
                Log.w(TAG, "Unhanded semanticAction");
        }

        return false;
    }

    /**
     * Requests a read action for the notification from the current active Assistant.
     *
     * @param sbn the notification to deliver as the payload
     * @return true if the read request to Assistant was successful
     */
    private boolean readMessageNotification(StatusBarNotification sbn) {
        return requestAction(sbn, BundleBuilder.buildAssistantReadBundle(sbn));
    }

    /**
     * Requests a reply action for the notification from the current active Assistant.
     *
     * @param sbn the notification to deliver as the payload
     * @return true if the reply request to Assistant was successful
     */
    private boolean replyMessageNotification(StatusBarNotification sbn) {
        return requestAction(sbn, BundleBuilder.buildAssistantReplyBundle(sbn));
    }

    private boolean requestAction(StatusBarNotification sbn, Bundle payloadArguments) {
        return isCarCompatibleMessagingNotification(sbn)
                && assistantIsNotificationListener()
                && mAssistUtils.showSessionForActiveService(payloadArguments,
                CarVoiceInteractionSession.SHOW_SOURCE_NOTIFICATION, null, null);
    }
}
