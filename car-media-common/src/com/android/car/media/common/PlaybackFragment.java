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

package com.android.car.media.common;

import android.car.Car;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * {@link Fragment} that can be used to display and control the currently playing media item.
 * Its requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission be held by the
 * hosting application.
 */
public class PlaybackFragment extends Fragment {
    private PlaybackModel mModel;
    private ImageView mAlbumBackground;
    private PlaybackControls mPlaybackControls;
    private ImageView mAlbumArt;
    private TextView mTitle;
    private TextView mSubtitle;
    private SeekBar mSeekbar;

    private PlaybackModel.PlaybackObserver mObserver = new PlaybackModel.PlaybackObserver() {
        @Override
        public void onPlaybackStateChanged() {
            updateState();
        }

        @Override
        public void onSourceChanged() {
            updateState();
            updateMetadata();
            updateAccentColor();
        }

        @Override
        public void onMetadataChanged() {
            updateMetadata();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.car_playback_fragment, container, false);
        mModel = new PlaybackModel(getContext());
        mModel.registerObserver(mObserver);
        mAlbumBackground = view.findViewById(R.id.album_background);
        mPlaybackControls = view.findViewById(R.id.playback_controls);
        mPlaybackControls.setModel(mModel);
        mAlbumArt = view.findViewById(R.id.album_art);
        mTitle = view.findViewById(R.id.title);
        mSubtitle = view.findViewById(R.id.subtitle);
        mSeekbar = view.findViewById(R.id.seek_bar);

        mAlbumBackground.setOnClickListener(v -> {
            Intent intent = new Intent(Car.CAR_INTENT_ACTION_MEDIA_TEMPLATE);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(getActivity(), mAlbumArt,
                            getString(R.string.album_art));
            startActivity(intent, options.toBundle());
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mModel.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mModel.stop();
    }

    private void updateState() {
        long maxProgress = mModel.getMaxProgress();
        mSeekbar.setVisibility(maxProgress > 0 ? View.VISIBLE : View.INVISIBLE);
        mSeekbar.setMax((int) maxProgress);
        if (mModel.isPlaying()) {
            mSeekbar.post(mSeekBarRunnable);
        } else {
            mSeekbar.removeCallbacks(mSeekBarRunnable);
        }
    }

    private void updateMetadata() {
        MediaItemMetadata metadata = mModel.getMetadata();
        mTitle.setText(metadata != null ? metadata.getTitle() : null);
        mSubtitle.setText(metadata != null ? metadata.getSubtitle() : null);
        MediaItemMetadata.updateImageView(getContext(), metadata, mAlbumArt, 0);
        MediaItemMetadata.updateImageView(getContext(), metadata, mAlbumBackground, 0);
    }

    private void updateAccentColor() {
        int defaultColor = getResources().getColor(android.R.color.background_dark, null);
        int color = mModel.getMediaSource().getAccentColor(defaultColor);
        mSeekbar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private static final long SEEK_BAR_UPDATE_TIME_INTERVAL_MS = 500;

    private final Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mModel.isPlaying()) {
                return;
            }
            mSeekbar.setProgress((int) mModel.getProgress());
            mSeekbar.postDelayed(this, SEEK_BAR_UPDATE_TIME_INTERVAL_MS);
        }
    };
}
