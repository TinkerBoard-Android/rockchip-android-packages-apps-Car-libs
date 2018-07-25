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

import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import java.util.function.Supplier;

/**
 * Updates current progress from a given {@link PlaybackState} while active
 */
class ProgressLiveData extends LiveData<Long> {

    /** How long this LiveData should wait between progress updates */
    @VisibleForTesting
    static final long UPDATE_INTERVAL_MS = 500;

    private final PlaybackState mPlaybackState;
    private final long mMaxProgress;
    private final Handler mTimerHandler = new Handler(Looper.getMainLooper());
    private final Supplier<Long> mElapsedRealtime;

    ProgressLiveData(@NonNull PlaybackState playbackState, long maxProgress) {
        this(playbackState, maxProgress, SystemClock::elapsedRealtime);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ProgressLiveData(
            @NonNull PlaybackState playbackState, long maxProgress,
            Supplier<Long> elapsedRealtime) {
        mPlaybackState = playbackState;
        mMaxProgress = maxProgress;
        mElapsedRealtime = elapsedRealtime;
    }

    private void updateProgress() {
        setValue(getProgress());
        mTimerHandler.postDelayed(this::updateProgress, UPDATE_INTERVAL_MS);
    }

    private long getProgress() {
        if (mPlaybackState.getPosition() == PlaybackState.PLAYBACK_POSITION_UNKNOWN) {
            return PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        }
        long timeDiff = mElapsedRealtime.get() - mPlaybackState.getLastPositionUpdateTime();
        float speed = mPlaybackState.getPlaybackSpeed();
        if (mPlaybackState.getState() == PlaybackState.STATE_PAUSED
                || mPlaybackState.getState() == PlaybackState.STATE_STOPPED) {
            // This guards against apps who don't keep their playbackSpeed to spec (b/62375164)
            speed = 0f;
        }
        long posDiff = (long) (timeDiff * speed);
        return Math.min(posDiff + mPlaybackState.getPosition(), mMaxProgress);
    }

    @Override
    protected void onActive() {
        super.onActive();
        updateProgress();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mTimerHandler.removeCallbacksAndMessages(null);
    }
}
