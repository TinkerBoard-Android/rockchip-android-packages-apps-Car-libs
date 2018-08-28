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
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is an abstractions over {@link MediaSessionManager} that provides information about the
 * currently active media sessions.
 * <p>
 * This requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission to be held by the
 * calling app.
 */
class ActiveMediaControllersLiveData extends LiveData<List<MediaController>> {

    private final MediaSessionManager mMediaSessionManager;
    private final MediaSessionUpdater mMediaSessionUpdater = new MediaSessionUpdater();

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
                List<MediaController> activeSessions = mMediaSessionManager.getActiveSessions(null);
                update(activeSessions);
            }

            @Override
            public void onSessionDestroyed() {
                List<MediaController> activeSessions = mMediaSessionManager.getActiveSessions(null);
                update(activeSessions);
            }
        };

        private void registerCallbacks(List<MediaController> newControllers) {
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
            this::setValue;

    ActiveMediaControllersLiveData(Context context) {
        this(Objects.requireNonNull(context.getSystemService(MediaSessionManager.class)));
    }

    @VisibleForTesting
    ActiveMediaControllersLiveData(@NonNull MediaSessionManager mediaSessionManager) {
        mMediaSessionManager = mediaSessionManager;
    }

    private void update(List<MediaController> activeSessions) {
        setValue(activeSessions);
        mMediaSessionUpdater.registerCallbacks(activeSessions);
    }

    @Override
    protected void onActive() {
        mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionChangeListener, null);
        update(mMediaSessionManager.getActiveSessions(null));
    }

    @Override
    protected void onInactive() {
        mMediaSessionUpdater.registerCallbacks(new ArrayList<>());
        mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionChangeListener);
    }

}
