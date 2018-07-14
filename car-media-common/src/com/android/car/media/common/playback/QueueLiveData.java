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
import android.media.session.MediaController;
import android.media.session.MediaSession;

import androidx.lifecycle.LiveData;

import java.util.List;


/**
 * Watches the queue for the session controlled by given a {@link MediaController}.
 *
 * @see MediaController#getQueue()
 */
class QueueLiveData extends LiveData<List<MediaSession.QueueItem>> {

    private final MediaController mMediaController;
    private final MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onQueueChanged(@Nullable List<MediaSession.QueueItem> metadata) {
            setValue(metadata);
        }
    };

    QueueLiveData(@NonNull MediaController mediaController) {
        mMediaController = mediaController;
    }

    @Override
    protected void onActive() {
        mMediaController.registerCallback(mCallback);
    }

    @Override
    protected void onInactive() {
        mMediaController.unregisterCallback(mCallback);
    }
}
