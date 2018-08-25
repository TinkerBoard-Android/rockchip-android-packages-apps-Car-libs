/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.car.media.common.source;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import com.android.car.media.common.playback.PlaybackStateAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is an abstractions over {@link MediaSessionManager} that provides information about the
 * currently "active" media session.
 * <p>
 * It automatically determines the foreground media app (the one that would normally receive
 * playback events) and exposes metadata and events from such app, or when a different app becomes
 * foreground.
 * <p>
 * This requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission to be held by the
 * calling app.
 */
class ActiveMediaControllerLiveData extends LiveData<MediaController> {
    private static final String TAG = "ActiveSourceManager";

    private static final String PLAYBACK_MODEL_SHARED_PREFS =
            "com.android.car.media.PLAYBACK_MODEL";
    private static final String PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY =
            "active_packagename";

    private final MediaSessionManager mMediaSessionManager;
    private final MediaSessionUpdater mMediaSessionUpdater = new MediaSessionUpdater();
    private final SharedPreferences mSharedPreferences;

    /**
     * Temporary work-around to bug b/76017849. MediaSessionManager is not notifying media session
     * priority changes. As a work-around we subscribe to playback state changes on all controllers
     * to detect potential priority changes. This might cause a few unnecessary checks, but
     * selecting the top-most controller is a cheap operation.
     */
    private class MediaSessionUpdater {
        private List<MediaController> mControllers = new ArrayList<>();

        private MediaController.Callback mCallback = new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                selectMediaController(mMediaSessionManager.getActiveSessions(null));
            }

            @Override
            public void onSessionDestroyed() {
                selectMediaController(mMediaSessionManager.getActiveSessions(null));
            }
        };

        void registerCallbacks(List<MediaController> newControllers) {
            for (MediaController oldController : mControllers) {
                oldController.unregisterCallback(mCallback);
            }
            for (MediaController newController : newControllers) {
                newController.registerCallback(mCallback);
            }
            mControllers.clear();
            mControllers.addAll(newControllers);
        }
    }

    private MediaSessionManager.OnActiveSessionsChangedListener mSessionChangeListener =
            this::selectMediaController;

    ActiveMediaControllerLiveData(Context context) {
        this(Objects.requireNonNull(context.getSystemService(MediaSessionManager.class)),
                context.getSharedPreferences(PLAYBACK_MODEL_SHARED_PREFS, Context.MODE_PRIVATE));
    }

    @VisibleForTesting
    ActiveMediaControllerLiveData(@NonNull MediaSessionManager mediaSessionManager,
            @NonNull SharedPreferences preferences) {
        mMediaSessionManager = mediaSessionManager;
        mSharedPreferences = preferences;
    }

    /**
     * Selects one of the provided controllers as the "currently playing" one.
     */
    private void selectMediaController(List<MediaController> controllers) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            dump("Selecting a media controller from: ", controllers);
        }
        changeMediaController(getTopMostController(controllers), true);
        mMediaSessionUpdater.registerCallbacks(controllers);
    }

    private void dump(@SuppressWarnings("SameParameterValue") String title,
            List<MediaController> controllers) {
        Log.d(TAG, title + " (total: " + controllers.size() + ")");
        for (MediaController controller : controllers) {
            String stateName = getStateName(controller.getPlaybackState() != null
                    ? controller.getPlaybackState().getState()
                    : PlaybackState.STATE_NONE);
            Log.d(TAG, String.format("\t%s: %s",
                    controller.getPackageName(),
                    stateName));
        }
    }

    private String getStateName(@PlaybackStateAnnotations.State int state) {
        switch (state) {
            case PlaybackState.STATE_NONE:
                return "NONE";
            case PlaybackState.STATE_STOPPED:
                return "STOPPED";
            case PlaybackState.STATE_PAUSED:
                return "PAUSED";
            case PlaybackState.STATE_PLAYING:
                return "PLAYING";
            case PlaybackState.STATE_FAST_FORWARDING:
                return "FORWARDING";
            case PlaybackState.STATE_REWINDING:
                return "REWINDING";
            case PlaybackState.STATE_BUFFERING:
                return "BUFFERING";
            case PlaybackState.STATE_ERROR:
                return "ERROR";
            case PlaybackState.STATE_CONNECTING:
                return "CONNECTING";
            case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                return "SKIPPING_TO_PREVIOUS";
            case PlaybackState.STATE_SKIPPING_TO_NEXT:
                return "SKIPPING_TO_NEXT";
            case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                return "SKIPPING_TO_QUEUE_ITEM";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Returns the controller most likely to be the currently active one, out of the list of active
     * controllers reported by {@link MediaSessionManager}. It does so by picking the first one (in
     * order of priority) with an active state as reported by
     * {@link MediaController#getPlaybackState()}
     */
    @Nullable
    private MediaController getTopMostController(List<MediaController> controllers) {
        if (controllers != null && controllers.size() > 0) {
            for (MediaController candidate : controllers) {
                @PlaybackStateAnnotations.State int state = candidate.getPlaybackState() != null
                        ? candidate.getPlaybackState().getState()
                        : PlaybackState.STATE_NONE;
                if (state == PlaybackState.STATE_BUFFERING
                        || state == PlaybackState.STATE_CONNECTING
                        || state == PlaybackState.STATE_FAST_FORWARDING
                        || state == PlaybackState.STATE_PLAYING
                        || state == PlaybackState.STATE_REWINDING
                        || state == PlaybackState.STATE_SKIPPING_TO_NEXT
                        || state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS
                        || state == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM) {
                    return candidate;
                }
            }
            // If no source is active, we go for the last known source
            String packageName = getLastKnownActivePackageName();
            if (packageName != null) {
                for (MediaController candidate : controllers) {
                    if (candidate.getPackageName().equals(packageName)) {
                        return candidate;
                    }
                }
            }
            return controllers.get(0);
        }
        return null;
    }

    private void changeMediaController(MediaController mediaController, boolean persist) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "New media controller: " + (mediaController != null
                    ? mediaController.getPackageName() : null));
        }
        if ((mediaController == null && getValue() == null)
                || (mediaController != null && getValue() != null
                && mediaController.getPackageName().equals(getValue().getPackageName()))) {
            // If no change, do nothing.
            return;
        }
        postValue(mediaController);
        if (persist) {
            setLastKnownActivePackageName(mediaController != null
                    ? mediaController.getPackageName()
                    : null);
        }
    }

    @Override
    protected void onActive() {
        mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionChangeListener, null);
        selectMediaController(mMediaSessionManager.getActiveSessions(null));
    }

    @Override
    protected void onInactive() {
        mMediaSessionUpdater.registerCallbacks(new ArrayList<>());
        mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionChangeListener);
        changeMediaController(null, false);
    }


    private String getLastKnownActivePackageName() {
        return mSharedPreferences.getString(PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY, null);
    }

    private void setLastKnownActivePackageName(String packageName) {
        mSharedPreferences.edit()
                .putString(PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY, packageName)
                .apply();
    }

    /**
     * Returns the {@link MediaController} corresponding to the given package name, or NULL if no
     * active session exists for it.
     */
    @Nullable
    MediaController getControllerForPackage(String packageName) {
        List<MediaController> controllers = mMediaSessionManager.getActiveSessions(null);
        for (MediaController controller : controllers) {
            if (controller.getPackageName().equals(packageName)) {
                return controller;
            }
        }
        return null;
    }

}
