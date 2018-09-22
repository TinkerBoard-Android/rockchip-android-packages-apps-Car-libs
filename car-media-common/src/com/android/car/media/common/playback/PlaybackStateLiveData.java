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

package com.android.car.media.common.playback;

import android.annotation.NonNull;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.lifecycle.LiveData;

/**
 * Watches the {@link PlaybackStateCompat} for the session controlled by given a
 * {@link MediaControllerCompat}.
 *
 * @see MediaControllerCompat#getPlaybackState()
 */
class PlaybackStateLiveData extends LiveData<PlaybackStateCompat> {

    private final MediaControllerCompat mMediaController;
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat playbackState) {
            setValue(playbackState);
        }
    };

    PlaybackStateLiveData(@NonNull MediaControllerCompat mediaController) {
        mMediaController = mediaController;
    }

    @Override
    protected void onActive() {
        setValue(mMediaController.getPlaybackState());
        mMediaController.registerCallback(mCallback);
    }

    @Override
    protected void onInactive() {
        mMediaController.unregisterCallback(mCallback);
    }
}
