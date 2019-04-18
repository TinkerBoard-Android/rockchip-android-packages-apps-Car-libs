/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.media.common;

import static androidx.lifecycle.Transformations.map;

import static com.android.car.arch.common.LiveDataFunctions.falseLiveData;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.android.car.apps.common.MinimizedControlBar;
import com.android.car.media.common.playback.PlaybackViewModel;

/**
 * This is a CarControlBar used for displaying Media content, including metadata for the currently
 * playing song and basic controls.
 */
public class MinimizedPlaybackControlBar extends MinimizedControlBar implements PlaybackControls {

    private static final String TAG = "Media.ControlBar";

    private MediaButtonController mMediaButtonController;
    private MetadataController mMetadataController;
    private ProgressBar mProgressBar;
    private PlaybackViewModel mPlaybackViewModel;
    private LiveData<Long> mMaxProgress;

    public MinimizedPlaybackControlBar(Context context) {
        this(context, null);
    }

    public MinimizedPlaybackControlBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MinimizedPlaybackControlBar(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs, R.layout.minimized_playback_control_bar);
        init(context);
    }

    private void init(Context context) {
        mMediaButtonController = new MediaButtonController(context, this);
        mProgressBar = findViewById(R.id.progress_bar);
    }

    @Override
    public void setModel(@NonNull PlaybackViewModel model, @NonNull LifecycleOwner owner) {
        mMediaButtonController.setModel(model, owner);
        mMetadataController = new MetadataController(owner, model,
                falseLiveData(), mTitle, mSubtitle, null, null, null, null, null, null,
                mContentTile, getContext().getResources().getDimensionPixelSize(
                R.dimen.minimized_control_bar_content_tile_size));

        mPlaybackViewModel = model;
        if (mProgressBar != null) {
            mPlaybackViewModel.getMediaSourceColors().observe(owner,
                    sourceColors -> {
                        int defaultColor = getContext().getResources().getColor(
                                R.color.minimized_progress_bar_highlight, null);
                        int color = sourceColors != null ? sourceColors.getAccentColor(defaultColor)
                                : defaultColor;
                        mProgressBar.setProgressTintList(ColorStateList.valueOf(color));
                    });

            // TODO(b/130566861): Get the progress and max progress through Model once Model is
            //  moved out to be a top-level class.
            mPlaybackViewModel.getProgress().observe(owner,
                    progress -> mProgressBar.setProgress(progress.intValue()));
            mMaxProgress = map(mPlaybackViewModel.getPlaybackStateWrapper(),
                    state -> state != null ? state.getMaxProgress() : 0L);
            mMaxProgress.observe(owner,
                    maxProgress -> mProgressBar.setMax(maxProgress.intValue()));
        }
    }
}
