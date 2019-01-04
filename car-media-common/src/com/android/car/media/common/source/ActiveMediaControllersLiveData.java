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
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is an abstractions over {@link MediaSessionManager} that provides information about the
 * currently active media sessions.
 * <p>
 * This requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission to be held by the
 * calling app.
 *
 * @deprecated To be removed with b/121270620
 */
@Deprecated
class ActiveMediaControllersLiveData extends LiveData<List<MediaControllerCompat>> {

    private static final String TAG = "ActiveMedia";
    private final MediaSessionManager mMediaSessionManager;

    // TODO(b/121270620) Clean up MediaController fetching logic, remove this LiveData if not needed
    private final MediaSessionUpdater mMediaSessionUpdater = new MediaSessionUpdater();
    private Context mContext;

    /**
     * Temporary work-around to bug b/76017849. MediaSessionManager is not notifying media session
     * priority changes. As a work-around we subscribe to playback state changes on all controllers
     * to detect potential priority changes. This might cause a few unnecessary checks, but
     * selecting the top-most controller is a cheap operation.
     */
    private class MediaSessionUpdater {
        private List<MediaControllerCompat> mControllers = new ArrayList<>();

        private MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackStateCompat state) {
                List<MediaController> activeSessions =
                        mMediaSessionManager.getActiveSessions(null);
                update(activeSessions);
            }

            @Override
            public void onSessionDestroyed() {
                List<MediaController> activeSessions =
                        mMediaSessionManager.getActiveSessions(null);
                update(activeSessions);
            }
        };

        private void registerCallbacks(List<MediaControllerCompat> newControllers) {
            for (MediaControllerCompat oldController : mControllers) {
                oldController.unregisterCallback(mCallback);
            }
            for (MediaControllerCompat newController : newControllers) {
                newController.registerCallback(mCallback);
            }
            mControllers.clear();
            mControllers.addAll(newControllers);
        }
    }

    private MediaSessionManager.OnActiveSessionsChangedListener mSessionChangeListener =
            controllers -> setValue(convertCompat(controllers));

    ActiveMediaControllersLiveData(Context context) {
        this(Objects.requireNonNull(context.getSystemService(MediaSessionManager.class)));
        mContext = context;
    }

    @VisibleForTesting
    ActiveMediaControllersLiveData(@NonNull MediaSessionManager mediaSessionManager) {
        mMediaSessionManager = mediaSessionManager;
    }

    private void update(List<MediaController> activeSessions) {
        List<MediaControllerCompat> activeSessionsCompat = convertCompat(activeSessions);
        setValue(activeSessionsCompat);
        mMediaSessionUpdater.registerCallbacks(activeSessionsCompat);
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

    private List<MediaControllerCompat> convertCompat(List<MediaController> mediaControllers) {
        return mediaControllers.stream().map(
                controller -> fromMediaController(controller))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    MediaControllerCompat fromMediaController(MediaController mediaController) {
        // TODO(b/112161702): cache active MediaControllers
        MediaSessionCompat.Token token =
                MediaSessionCompat.Token.fromToken(mediaController.getSessionToken());
        MediaControllerCompat controllerCompat = null;
        try {
            controllerCompat = new MediaControllerCompat(mContext, token);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't get MediaController", e);
        }
        return controllerCompat;
    }
}
