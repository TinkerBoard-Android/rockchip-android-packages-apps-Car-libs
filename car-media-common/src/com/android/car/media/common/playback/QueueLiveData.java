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
import android.annotation.Nullable;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.lifecycle.LiveData;

import java.util.List;


/**
 * Watches the queue for the session controlled by given a {@link MediaControllerCompat}.
 *
 * @see MediaControllerCompat#getQueue()
 */
class QueueLiveData extends LiveData<List<MediaSessionCompat.QueueItem>> {

    private final MediaControllerCompat mMediaController;
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onQueueChanged(@Nullable List<MediaSessionCompat.QueueItem> metadata) {
            setValue(metadata);
        }
    };

    QueueLiveData(@NonNull MediaControllerCompat mediaController) {
        mMediaController = mediaController;
    }

    @Override
    protected void onActive() {
        setValue(mMediaController.getQueue());
        mMediaController.registerCallback(mCallback);
    }

    @Override
    protected void onInactive() {
        mMediaController.unregisterCallback(mCallback);
    }
}
