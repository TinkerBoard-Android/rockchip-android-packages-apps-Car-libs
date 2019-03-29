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
import static androidx.lifecycle.Transformations.switchMap;

import static com.android.car.arch.common.LiveDataFunctions.combine;
import static com.android.car.arch.common.LiveDataFunctions.distinct;
import static com.android.car.arch.common.LiveDataFunctions.falseLiveData;
import static com.android.car.arch.common.LiveDataFunctions.freezable;
import static com.android.car.arch.common.LiveDataFunctions.mapNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.apps.common.util.ViewHelper;
import com.android.car.media.common.playback.AlbumArtLiveData;
import com.android.car.media.common.playback.PlaybackViewModel;

import java.util.concurrent.TimeUnit;

/**
 * Common controller for displaying current track's metadata.
 */
public class MetadataController {
    private PlaybackViewModel.PlaybackController mController;

    private boolean mTrackingTouch;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Do nothing.
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mTrackingTouch = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mTrackingTouch && mController != null) {
                        mController.seekTo(seekBar.getProgress());
                    }
                    mTrackingTouch = false;
                }
            };

    /**
     * Create a new MetadataController that operates on the provided Views
     *
     * @param lifecycleOwner The lifecycle scope for the Views provided to this controller
     * @param viewModel      The ViewModel to provide metadata for display
     * @param pauseUpdates   Views will not update while this LiveData emits {@code true}
     * @param title          Displays the track's title. Must not be {@code null}.
     * @param albumTitle     Displays the track's album title. May be {@code null}.
     * @param artist         Displays the track's artist. Must not be {@code null}.
     * @param timeExtras     Regroups views for time display (eg: separators). May be {@code null}.
     * @param currentTime    Displays the track's current position as text. May be {@code null}.
     * @param maxTime        Displays the track's duration as text. May be {@code null}.
     * @param seekBar        Displays the track's progress visually. May be {@code null}.
     * @param albumArt       Displays the track's album art. May be {@code null}.
     */
    public MetadataController(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull PlaybackViewModel viewModel, @Nullable LiveData<Boolean> pauseUpdates,
            @NonNull TextView title, @Nullable TextView albumTitle, @NonNull TextView artist,
            @Nullable View timeExtras, @Nullable TextView currentTime, @Nullable TextView maxTime,
            @Nullable SeekBar seekBar, @Nullable ImageView albumArt, int albumArtSizePx) {
        viewModel.getPlaybackController().observe(lifecycleOwner,
                controller -> mController = controller);

        Model model = new Model(viewModel, pauseUpdates);

        model.getTitle().observe(lifecycleOwner, title::setText);

        if (albumTitle != null) {
            model.getAlbumTitle().observe(lifecycleOwner, albumName -> {
                if (!TextUtils.isEmpty(albumName)) {
                    albumTitle.setText(albumName);
                    albumTitle.setVisibility(View.VISIBLE);
                } else {
                    albumTitle.setVisibility(View.GONE);
                }
            });
        }

        model.getArtist().observe(lifecycleOwner, artist::setText);

        if (albumArt != null) {
            model.setAlbumArtSize(albumArtSizePx);
            model.getAlbumArt().observe(lifecycleOwner, bitmap -> {
                if (bitmap == null) {
                    albumArt.setImageDrawable(new ColorDrawable(title.getContext().getResources()
                            .getColor(R.color.album_art_placeholder_color, null)));
                } else {
                    albumArt.setImageBitmap(bitmap);
                }
            });
        }

        model.hasTime().observe(lifecycleOwner,
                visible -> {
                    // Current and Max time are not necessarily children of timeExtras.
                    ViewHelper.setVisible(timeExtras, visible);
                    ViewHelper.setVisible(currentTime, visible);
                    ViewHelper.setVisible(maxTime, visible);
                });

        if (currentTime != null) {
            model.getCurrentTimeText().observe(lifecycleOwner,
                    timeText -> currentTime.setText(timeText));
        }

        if (maxTime != null) {
            model.getMaxTimeText().observe(lifecycleOwner,
                    timeText -> maxTime.setText(timeText));
        }

        if (seekBar != null) {
            model.hasTime().observe(lifecycleOwner,
                    visible -> seekBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
            model.getMaxProgress().observe(lifecycleOwner,
                    maxProgress -> seekBar.setMax(maxProgress.intValue()));
            model.getProgress().observe(lifecycleOwner,
                    progress -> {
                        if (!mTrackingTouch) {
                            seekBar.setProgress(progress.intValue());
                        }
                    });

            model.isSeekToEnabled().observe(lifecycleOwner,
                    enabled -> {
                        mTrackingTouch = false;
                        if (seekBar.getThumb() != null) {
                            seekBar.getThumb().mutate().setAlpha(enabled ? 255 : 0);
                        }
                        final boolean shouldHandleTouch = seekBar.getThumb() != null && enabled;
                        seekBar.setOnTouchListener(
                                (v, event) -> !shouldHandleTouch /* consumeEvent */);
                    });

            seekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
            viewModel.getPlaybackStateWrapper().observe(lifecycleOwner, state -> {
                mTrackingTouch = false;
            });
        }
    }

    static class Model {

        private final LiveData<CharSequence> mTitle;
        private final LiveData<CharSequence> mAlbumTitle;
        private final LiveData<CharSequence> mArtist;
        private final LiveData<Bitmap> mAlbumArt;
        private final LiveData<Long> mProgress;
        private final LiveData<Long> mMaxProgress;
        private final LiveData<CharSequence> mCurrentTimeText;
        private final LiveData<CharSequence> mMaxTimeText;
        private final LiveData<Boolean> mHasTime;
        private final LiveData<Boolean> mIsSeekToEnabled;

        private final MutableLiveData<Integer> mAlbumArtSize = new MutableLiveData<>();

        Model(@NonNull PlaybackViewModel playbackViewModel,
                @Nullable LiveData<Boolean> pauseUpdates) {
            if (pauseUpdates == null) {
                pauseUpdates = falseLiveData();
            }
            mTitle = freezable(pauseUpdates,
                    mapNonNull(playbackViewModel.getMetadata(), MediaItemMetadata::getTitle));
            mAlbumTitle = freezable(pauseUpdates,
                    mapNonNull(playbackViewModel.getMetadata(), MediaItemMetadata::getAlbumTitle));
            mArtist = freezable(pauseUpdates,
                    mapNonNull(playbackViewModel.getMetadata(), MediaItemMetadata::getArtist));
            mAlbumArt = freezable(pauseUpdates,
                    switchMap(mAlbumArtSize,
                            size -> AlbumArtLiveData.getAlbumArt(
                                    playbackViewModel.getApplication(),
                                    size, size, true,
                                    playbackViewModel.getMetadata())));
            mProgress = freezable(pauseUpdates, playbackViewModel.getProgress());
            mMaxProgress = freezable(pauseUpdates,
                    map(playbackViewModel.getPlaybackStateWrapper(),
                            state -> state != null ? state.getMaxProgress() : 0L));

            mCurrentTimeText = combine(mProgress, mMaxProgress, (progress, maxProgress) -> {
                boolean showHours = TimeUnit.MILLISECONDS.toHours(maxProgress) > 0;
                return formatTime(progress, showHours);
            });

            mMaxTimeText = combine(mProgress, mMaxProgress, (progress, maxProgress) -> {
                boolean showHours = TimeUnit.MILLISECONDS.toHours(maxProgress) > 0;
                return formatTime(maxProgress, showHours);
            });

            mHasTime = combine(mProgress, mMaxProgress,
                    (progress, maxProgress) ->
                            maxProgress > 0 && progress != PlaybackState.PLAYBACK_POSITION_UNKNOWN);

            mIsSeekToEnabled = distinct(freezable(pauseUpdates,
                    map(playbackViewModel.getPlaybackStateWrapper(),
                            state -> state != null && state.isSeekToEnabled())));
        }


        void setAlbumArtSize(int size) {
            mAlbumArtSize.setValue(size);
        }

        LiveData<CharSequence> getTitle() {
            return mTitle;
        }

        LiveData<CharSequence> getAlbumTitle() {
            return mAlbumTitle;
        }

        LiveData<CharSequence> getArtist() {
            return mArtist;
        }

        LiveData<Bitmap> getAlbumArt() {
            return mAlbumArt;
        }

        LiveData<Long> getProgress() {
            return mProgress;
        }

        LiveData<Long> getMaxProgress() {
            return mMaxProgress;
        }

        LiveData<CharSequence> getCurrentTimeText() {
            return mCurrentTimeText;
        }

        LiveData<CharSequence> getMaxTimeText() {
            return mMaxTimeText;
        }

        LiveData<Boolean> hasTime() {
            return mHasTime;
        }

        LiveData<Boolean> isSeekToEnabled() {
            return mIsSeekToEnabled;
        }

        @SuppressLint("DefaultLocale")
        private static String formatTime(long millis, boolean showHours) {
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1);
            if (showHours) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%d:%02d", minutes, seconds);
            }
        }
    }
}
