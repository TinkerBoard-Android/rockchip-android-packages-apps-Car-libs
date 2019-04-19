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
    private final Model mModel;

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
     * Note: when the text of a TextView is empty, its visibility will be set to View.INVISIBLE
     * instead of View.GONE. Thus the views stay in the same position, and the constraint chains of
     * the layout won't be disrupted.
     *
     * @param lifecycleOwner The lifecycle scope for the Views provided to this controller.
     * @param model          The Model to provide metadata for display.
     * @param title          Displays the track's title. Must not be {@code null}.
     * @param artist         Displays the track's artist. May be {@code null}.
     * @param albumTitle     Displays the track's album title. May be {@code null}.
     * @param outerSeparator Displays the separator between the album title and time. May be {@code
     *                       null}.
     * @param currentTime    Displays the track's current position as text. May be {@code null}.
     * @param innerSeparator Displays the separator between the currentTime and the maxTime. May be
     *                       {@code null}.
     * @param maxTime        Displays the track's duration as text. May be {@code null}.
     * @param seekBar        Displays the track's progress visually. May be {@code null}.
     * @param albumArt       Displays the track's album art. May be {@code null}.
     */
    public MetadataController(@NonNull LifecycleOwner lifecycleOwner, @NonNull Model model,
            @NonNull TextView title, @Nullable TextView artist, @Nullable TextView albumTitle,
            @Nullable TextView outerSeparator, @Nullable TextView currentTime,
            @Nullable TextView innerSeparator, @Nullable TextView maxTime,
            @Nullable SeekBar seekBar, @Nullable ImageView albumArt, int albumArtSizePx) {
        mModel = model;
        mModel.getPlaybackViewModel().getPlaybackController().observe(lifecycleOwner,
                controller -> mController = controller);

        mModel.getTitle().observe(lifecycleOwner, title::setText);

        if (albumTitle != null) {
            mModel.getAlbumTitle().observe(lifecycleOwner, albumName -> {
                albumTitle.setText(albumName);
                ViewHelper.setInvisible(albumTitle, TextUtils.isEmpty(albumName));
            });
        }

        if (artist != null) {
            mModel.getArtist().observe(lifecycleOwner, artistName -> {
                artist.setText(artistName);
                ViewHelper.setInvisible(artist, TextUtils.isEmpty(artistName));
            });
        }

        if (albumArt != null) {
            mModel.setAlbumArtSize(albumArtSizePx);
            mModel.getAlbumArt().observe(lifecycleOwner, bitmap -> {
                if (bitmap == null) {
                    albumArt.setImageDrawable(new ColorDrawable(title.getContext().getResources()
                            .getColor(R.color.album_art_placeholder_color, null)));
                } else {
                    albumArt.setImageBitmap(bitmap);
                }
            });
        }

        if (outerSeparator != null) {
            mModel.showOuterSeparator().observe(lifecycleOwner,
                    // The text of outerSeparator is not empty. when albumTitle is empty,
                    // the visibility of outerSeparator should be View.GONE instead of
                    // View.INVISIBLE so that currentTime can be aligned to the left .
                    visible -> ViewHelper.setVisible(outerSeparator, visible));
        }

        mModel.hasTime().observe(lifecycleOwner,
                visible -> {
                    ViewHelper.setInvisible(currentTime, !visible);
                    ViewHelper.setInvisible(innerSeparator, !visible);
                    ViewHelper.setInvisible(maxTime, !visible);
                });

        if (currentTime != null) {
            mModel.getCurrentTimeText().observe(lifecycleOwner,
                    timeText -> currentTime.setText(timeText));
        }

        if (maxTime != null) {
            mModel.getMaxTimeText().observe(lifecycleOwner,
                    timeText -> maxTime.setText(timeText));
        }

        if (seekBar != null) {
            mModel.hasTime().observe(lifecycleOwner,
                    visible -> seekBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
            mModel.getMaxProgress().observe(lifecycleOwner,
                    maxProgress -> seekBar.setMax(maxProgress.intValue()));
            mModel.getProgress().observe(lifecycleOwner,
                    progress -> {
                        if (!mTrackingTouch) {
                            seekBar.setProgress(progress.intValue());
                        }
                    });

            mModel.isSeekToEnabled().observe(lifecycleOwner,
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
            mModel.getPlaybackViewModel().getPlaybackStateWrapper().observe(lifecycleOwner,
                    state -> mTrackingTouch = false);
        }
    }

    /**
     * Model to convert playback metadata to formatted text.
     */
    public static class Model {

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
        private final LiveData<Boolean> mShowOuterSeparator;

        private final MutableLiveData<Integer> mAlbumArtSize = new MutableLiveData<>();

        private final PlaybackViewModel mPlaybackViewModel;

        /**
         * Creates a Model for current track's metadata.
         *
         * @param playbackViewModel The ViewModel to provide metadata for display
         * @param pauseUpdates      Views will not update while this LiveData emits {@code true}
         */
        public Model(@NonNull PlaybackViewModel playbackViewModel,
                @Nullable LiveData<Boolean> pauseUpdates) {
            mPlaybackViewModel = playbackViewModel;
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

            mShowOuterSeparator = combine(mAlbumTitle, mHasTime,
                    (albumName, hasTime) -> !TextUtils.isEmpty(albumName) && hasTime);

            mIsSeekToEnabled = distinct(freezable(pauseUpdates,
                    map(playbackViewModel.getPlaybackStateWrapper(),
                            state -> state != null && state.isSeekToEnabled())));
        }

        /*
         * Gets the PlaybackViewModel.
         */
        public PlaybackViewModel getPlaybackViewModel() {
            return mPlaybackViewModel;
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

        /**
         * Returns a LiveData that emits formatted text indicating the current playback progress.
         */
        public LiveData<CharSequence> getCurrentTimeText() {
            return mCurrentTimeText;
        }

        /**
         * Returns a LiveData that emits formatted text indicating the duration of the media item.
         */
        public LiveData<CharSequence> getMaxTimeText() {
            return mMaxTimeText;
        }

        /**
         * Returns a LiveData that emits a boolean value indicating whether the media item has
         * playback progress.
         */
        public LiveData<Boolean> hasTime() {
            return mHasTime;
        }

        LiveData<Boolean> isSeekToEnabled() {
            return mIsSeekToEnabled;
        }

        LiveData<Boolean> showOuterSeparator() {
            return mShowOuterSeparator;
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
