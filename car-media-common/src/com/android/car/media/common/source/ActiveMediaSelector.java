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
import android.media.session.MediaSessionManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.car.media.common.playback.PlaybackStateAnnotations;

import java.util.List;

/**
 * Provides functions for selecting a {@link MediaControllerCompat} from a list of controllers.
 */
class ActiveMediaSelector {
    private static final String TAG = "ActiveSourceManager";

    private static final String PLAYBACK_MODEL_SHARED_PREFS =
            "com.android.car.media.PLAYBACK_MODEL";
    private static final String PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY =
            "active_packagename";

    private final SharedPreferences mSharedPreferences;

    ActiveMediaSelector(Context context) {
        this(context.getSharedPreferences(PLAYBACK_MODEL_SHARED_PREFS, Context.MODE_PRIVATE));
    }

    @VisibleForTesting
    ActiveMediaSelector(@NonNull SharedPreferences preferences) {
        mSharedPreferences = preferences;
    }

    /**
     * Searches through {@code controllers} to find the MediaControllerCompat with the same package
     * name as {@code mediaSource}.
     *
     * @param controllers The List of MediaControllers to search through.
     * @param mediaSource The MediaSource to match.
     * @return The MediaControllerCompat whose package name matches or {@code null} if no match is
     * found.
     */
    @Nullable
    MediaControllerCompat getControllerForSource(@NonNull List<MediaControllerCompat> controllers,
            MediaSource mediaSource) {
        if (mediaSource != null) {
            return getControllerForPackage(controllers, mediaSource.getPackageName());
        }
        return null;
    }

    /**
     * Searches through {@code controllers} to find the MediaController with the specified {@code
     * packageName}
     *
     * @param controllers The List of MediaControllers to search through.
     * @param packageName The package name to find.
     * @return The MediaController whose package name matches or {@code null} if no match is found.
     */
    @Nullable
    MediaControllerCompat getControllerForPackage(@NonNull List<MediaControllerCompat> controllers,
            @NonNull String packageName) {
        for (MediaControllerCompat controller : controllers) {
            if (controller != null && packageName.equals(controller.getPackageName())) {
                return controller;
            }
        }
        return null;
    }

    /**
     * Returns one of the provided controllers as the "currently playing" one. If {@code previous}
     * is equivalent, will return that instance.
     */
    MediaControllerCompat getTopMostMediaController(
            @NonNull List<MediaControllerCompat> controllers,
            @Nullable MediaControllerCompat previous) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            dump("Selecting a media controller from: ", controllers);
        }
        MediaControllerCompat topMostController = pickTopMostController(controllers);
        if ((topMostController == null && previous == null)
                || (topMostController != null && previous != null
                && topMostController.getPackageName().equals(previous.getPackageName()))) {
            // If no change, do nothing.
            return previous;
        }
        setLastKnownActivePackageName(
                topMostController == null ? null : topMostController.getPackageName());
        return topMostController;
    }

    private void dump(@SuppressWarnings("SameParameterValue") String title,
            List<MediaControllerCompat> controllers) {
        Log.d(TAG, title + " (total: " + controllers.size() + ")");
        for (MediaControllerCompat controller : controllers) {
            String stateName = getStateName(controller.getPlaybackState() != null
                    ? controller.getPlaybackState().getState()
                    : PlaybackStateCompat.STATE_NONE);
            Log.d(TAG, String.format("\t%s: %s",
                    controller.getPackageName(),
                    stateName));
        }
    }

    private String getStateName(@PlaybackStateAnnotations.State int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_NONE:
                return "NONE";
            case PlaybackStateCompat.STATE_STOPPED:
                return "STOPPED";
            case PlaybackStateCompat.STATE_PAUSED:
                return "PAUSED";
            case PlaybackStateCompat.STATE_PLAYING:
                return "PLAYING";
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
                return "FORWARDING";
            case PlaybackStateCompat.STATE_REWINDING:
                return "REWINDING";
            case PlaybackStateCompat.STATE_BUFFERING:
                return "BUFFERING";
            case PlaybackStateCompat.STATE_ERROR:
                return "ERROR";
            case PlaybackStateCompat.STATE_CONNECTING:
                return "CONNECTING";
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                return "SKIPPING_TO_PREVIOUS";
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                return "SKIPPING_TO_NEXT";
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                return "SKIPPING_TO_QUEUE_ITEM";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Returns the controller most likely to be the currently active one, out of the list of active
     * controllers reported by {@link MediaSessionManager}. It does so by picking the first one (in
     * order of priority) with an active state as reported by
     * {@link MediaControllerCompat#getPlaybackState()}
     */
    @Nullable
    private MediaControllerCompat pickTopMostController(List<MediaControllerCompat> controllers) {
        if (controllers != null && controllers.size() > 0) {
            for (MediaControllerCompat candidate : controllers) {
                @PlaybackStateAnnotations.State int state = candidate.getPlaybackState() != null
                        ? candidate.getPlaybackState().getState()
                        : PlaybackStateCompat.STATE_NONE;
                if (state == PlaybackStateCompat.STATE_BUFFERING
                        || state == PlaybackStateCompat.STATE_CONNECTING
                        || state == PlaybackStateCompat.STATE_FAST_FORWARDING
                        || state == PlaybackStateCompat.STATE_PLAYING
                        || state == PlaybackStateCompat.STATE_REWINDING
                        || state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
                        || state == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
                        || state == PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM) {
                    return candidate;
                }
            }
            // If no source is active, we go for the last known source
            String packageName = getLastKnownActivePackageName();
            if (packageName != null) {
                for (MediaControllerCompat candidate : controllers) {
                    if (candidate.getPackageName().equals(packageName)) {
                        return candidate;
                    }
                }
            }
            return controllers.get(0);
        }
        return null;
    }

    private String getLastKnownActivePackageName() {
        return mSharedPreferences.getString(PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY, null);
    }

    private void setLastKnownActivePackageName(String packageName) {
        mSharedPreferences.edit()
                .putString(PLAYBACK_MODEL_ACTIVE_PACKAGE_NAME_KEY, packageName)
                .apply();
    }

}
