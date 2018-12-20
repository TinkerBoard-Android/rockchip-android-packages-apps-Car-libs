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
package com.android.car.assist;

import android.annotation.NonNull;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.MessagingStyle;
import android.app.Notification.MessagingStyle.Message;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

import java.util.List;

/**
 * An active voice interaction session on the car, providing additional actions which assistant
 * should act on. Override the {@link #onShow(String, Bundle, int)} to received the action specified
 * by the voice session initiator.
 */
public abstract class CarVoiceInteractionSession extends VoiceInteractionSession {

    private static final String TAG = CarVoiceInteractionSession.class.getSimpleName();

    // TODO: Use the flag introduced in new API changes, once the changes are available.
    /**
     * Flag for use with {@link #onShow}: indicates that the voice interaction service was invoked
     * from a notification.
     */
    public static final int SHOW_SOURCE_NOTIFICATION = 1 << 6;

    /** The key used for the action {@link String} in the payload {@link Bundle}. */
    public static final String KEY_ACTION = "KEY_ACTION";

    /** The key used for the {@link StatusBarNotification} in the payload {@link Bundle}. */
    public static final String KEY_NOTIFICATION = "KEY_NOTIFICATION";

    /** Indicates to assistant that no action was specified. */
    public static final String ACTION_NO_ACTION = "ACTION_NO_ACTION";

    /** Indicates to assistant that a read action is being requested for a given payload. */
    public static final String ACTION_READ_NOTIFICATION = "ACTION_READ_NOTIFICATION";

    /** Indicates to assistant that a reply action is being requested for a given payload. */
    public static final String ACTION_REPLY_NOTIFICATION = "ACTION_REPLY_NOTIFICATION";

    /** This intent will hold additional data for retrieving remoteInput result */
    private Intent mAdditionalData = new Intent();

    public CarVoiceInteractionSession(Context context) {
        super(context);
    }

    public CarVoiceInteractionSession(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    public final void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        if (isCarNotificationSource(showFlags)) {
            String action = args.getString(KEY_ACTION);
            if (action != null && !ACTION_NO_ACTION.equals(action)) {
                onShow(action, args, showFlags);
            }
        } else {
            onShow(ACTION_NO_ACTION, args, showFlags);
        }
    }

    /**
     * Called when the session UI is going to be shown.  This is called after
     * {@link #onCreateContentView} (if the session's content UI needed to be created) and
     * immediately prior to the window being shown.  This may be called while the window
     * is already shown, if a show request has come in while it is shown, to allow you to
     * update the UI to match the new show arguments.
     *
     * @param action The action that is being requested for this session
     *               (e.g. {@link CarVoiceInteractionSession#ACTION_READ_NOTIFICATION},
     *               {@link CarVoiceInteractionSession#ACTION_REPLY_NOTIFICATION}).
     * @param args The arguments that were supplied to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     * @param flags The show flags originally provided to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     */
    protected abstract void onShow(String action, Bundle args, int flags);

    /**
     * Returns true if the request was initiated for a car notification.
     */
    private static boolean isCarNotificationSource(int flags) {
        return (flags & SHOW_SOURCE_NOTIFICATION) != 0;
    }

    /**
     * @return The action {@link String} provided in the args {@Bundle},
     * or {@link CarVoiceInteractionSession#ACTION_NO_ACTION} if no such string was provided.
     */
    protected static String getRequestedAction(Bundle args) {
        return args.getString(KEY_ACTION, ACTION_NO_ACTION);
    }

    /**
     * Returns the {@link Notification} of the {@link StatusBarNotification}
     * provided in the args {@link Bundle}.
     *
     * @return The {@link StatusBarNotification}'s {@link Notification}.
     * @throws IllegalArgumentException If no {@link StatusBarNotification} was found in the args
     * {@link Bundle}.
     */
    protected static Notification getNotification(Bundle args) throws IllegalArgumentException {
        StatusBarNotification sbn = args.getParcelable(KEY_NOTIFICATION);

        if (sbn == null) {
            String error = "Failed to get StatusBarNotification from args Bundle.";
            Log.e(TAG, error);
            throw new IllegalArgumentException(error);
        }

        return sbn.getNotification();
    }

    /**
     * Retrieves all messages associated with the provided {@link StatusBarNotification} in the
     * args {@link Bundle}. These messages are provided through the notification's
     * {@link MessagingStyle}, using {@link MessagingStyle#addMessage(Message)}.
     *
     * @param args the payload delivered to the voice interaction session
     * @return all messages provided in the {@link MessagingStyle}
     */
    protected static List<Message> getMessages(Bundle args) {
        Notification notification = getNotification(args);
        Parcelable[] messages = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);

        return Message.getMessagesFromBundleArray(messages);
    }

    /**
     * Retrieves the corresponding {@link Action} from the notification's callback actions.
     *
     * @param args the payload delivered to the voice interaction session
     * @param semanticAction the {@link Action.SemanticAction} on which to select
     * @return the first action for which {@link Action#getSemanticAction()} returns semanticAction,
     * or null if no such action exists
     */
    protected static Action getAction(Bundle args, int semanticAction) {
        Notification notification = getNotification(args);

        for (Action action : notification.actions) {
            if (action.getSemanticAction() == semanticAction) return action;
        }

        Log.w(TAG, String.format("Semantic action not found: %d", semanticAction));
        return null;
    }

    /**
     * Retrieves the corresponding {@link PendingIntent} from the notification's callback actions.
     * @param semanticAction The {@link Action.SemanticAction} on which to select.
     * @return The {@link PendingIntent} of the first {@link Action} for which
     * {@link Action#getSemanticAction()} returns semanticAction,
     * or null if no such action exists.
     */
    protected static PendingIntent getPendingIntent(Bundle args, int semanticAction) {
        Action action = getAction(args, semanticAction);
        return (action == null) ? null : action.actionIntent;
    }

    /**
     * Fires the {@link PendingIntent} of the corresponding {@link Action}, ensuring that any
     * {@link RemoteInput}s corresponding to this action contain any addidional data.
     *
     * @param action The callback action to call.
     * @return true if the {@link PendingIntent} was sent successfully;
     * false if a {@link PendingIntent.CanceledException} was caught.
     */
    protected boolean sendActionIntent(@NonNull Action action) {
        try {
            action.actionIntent.send(getContext(), 0, mAdditionalData);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Writes the given reply to the {@link RemoteInput} of the reply action callback, if present.
     * Requires that a reply callback be included in the args {@link Bundle}, and that this
     * callback only contains one {@link RemoteInput}.
     *
     * @param reply - The reply that should be written to the {@link RemoteInput}.
     * @return true if it was able to find an unambiguous {@link RemoteInput} to write to,
     * false otherwise.
     */
    protected boolean writeReply(Bundle args, CharSequence reply) {

        Action replyCallback = getAction(args, Action.SEMANTIC_ACTION_REPLY);

        if (replyCallback == null) {
            Log.e(TAG, "No reply callback was provided.");
            return false;
        }

        RemoteInput[] remoteInputs = replyCallback.getRemoteInputs();
        if (remoteInputs == null || remoteInputs.length == 0) {
            Log.e(TAG, "No RemoteInputs were provided in the reply callback.");
            return false;
        }
        if (remoteInputs.length > 1) {
            Log.e(TAG, "Vague arguments. Multiple RemoteInputs were provided.");
            return false;
        }

        RemoteInput remoteInput = remoteInputs[0];
        if (remoteInput == null) {
            Log.e(TAG, "RemoteInput provided was null.");
            return false;
        }

        Bundle results = new Bundle();
        results.putCharSequence(remoteInput.getResultKey(), reply);
        RemoteInput.addResultsToIntent(remoteInputs, mAdditionalData, results);

        return true;
    }
}
