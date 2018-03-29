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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.android.car.apps.common.ColorChecker;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ActionBar;

/**
 * Custom view that can be used to display playback controls. It accepts a {@link PlaybackModel}
 * as its data source, automatically reacting to changes in playback state.
 */
public class PlaybackControls extends ActionBar {
    private static final String TAG = "PlaybackView";

    private PlayPauseStopImageView mPlayPauseStopImageView;
    private View mPlayPauseStopImageContainer;
    private ProgressBar mSpinner;
    private Context mContext;
    private ImageButton mSkipPrevButton;
    private ImageButton mSkipNextButton;
    private List<ImageButton> mCustomActionButtons = new ArrayList<>();
    private PlaybackModel mModel;
    private PlaybackModel.PlaybackObserver mObserver = new PlaybackModel.PlaybackObserver() {
        @Override
        protected void onPlaybackStateChanged() {
            updateState();
            updateCustomActions();
        }

        @Override
        protected void onSourceChanged() {
            updateState();
            updateCustomActions();
            updateAccentColor();
        }
    };
    private ColorStateList mIconsColor;


    /** Creates a {@link PlaybackControls} view */
    public PlaybackControls(Context context) {
        this(context, null, 0, 0);
    }

    /** Creates a {@link PlaybackControls} view */
    public PlaybackControls(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    /** Creates a {@link PlaybackControls} view */
    public PlaybackControls(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /** Creates a {@link PlaybackControls} view */
    public PlaybackControls(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    /**
     * Sets the {@link PlaybackModel} to use as the view model for this view.
     */
    public void setModel(PlaybackModel model) {
        if (mModel != null) {
            mModel.unregisterObserver(mObserver);
        }
        mModel = model;
        if (mModel != null) {
            mModel.registerObserver(mObserver);
        }
    }

    private void init(Context context) {
        mContext = context;

        mPlayPauseStopImageContainer = inflate(context, R.layout.car_play_pause_stop_button_layout,
                null);
        mPlayPauseStopImageContainer.setOnClickListener(this::onPlayPauseStopClicked);
        mPlayPauseStopImageView = mPlayPauseStopImageContainer.findViewById(R.id.play_pause_stop);
        mSpinner = mPlayPauseStopImageContainer.findViewById(R.id.spinner);
        mPlayPauseStopImageView.setAction(PlayPauseStopImageView.ACTION_DISABLED);
        mPlayPauseStopImageView.setOnClickListener(this::onPlayPauseStopClicked);

        mIconsColor = context.getResources().getColorStateList(R.color.playback_control_color,
                null);

        mSkipPrevButton = createIconButton(mContext, mIconsColor,
                context.getDrawable(R.drawable.ic_skip_previous));
        mSkipPrevButton.setVisibility(INVISIBLE);
        mSkipPrevButton.setOnClickListener(v -> {
            if (mModel != null) {
                mModel.onSkipPreviews();
            }
        });
        mSkipNextButton = createIconButton(mContext, mIconsColor,
                context.getDrawable(R.drawable.ic_skip_next));
        mSkipNextButton.setVisibility(INVISIBLE);
        mSkipNextButton.setOnClickListener(v -> {
            if (mModel != null) {
                mModel.onSkipNext();
            }
        });

        ImageButton overflowButton = createIconButton(context, mIconsColor,
                context.getDrawable(androidx.car.R.drawable.ic_overflow));

        setView(mPlayPauseStopImageContainer, ActionBar.SLOT_MAIN);
        setView(mSkipPrevButton, ActionBar.SLOT_LEFT);
        setView(mSkipNextButton, ActionBar.SLOT_RIGHT);
        setExpandCollapseView(overflowButton);
    }

    private ImageButton createIconButton(Context context, ColorStateList csl, Drawable icon) {
        ImageButton button = new ImageButton(context, null, 0, R.style.PlaybackControl);
        button.setImageDrawable(icon);
        return button;
    }

    private void updateState() {
        mPlayPauseStopImageView.setAction(convertMainAction(mModel.getMainAction()));
        mSpinner.setVisibility(mModel.isBuffering() ? VISIBLE : INVISIBLE);
        mSkipPrevButton.setVisibility(mModel.isSkipPreviewsEnabled() ? VISIBLE : INVISIBLE);
        mSkipNextButton.setVisibility(mModel.isSkipNextEnabled() ? VISIBLE : INVISIBLE);
    }

    @PlayPauseStopImageView.Action
    private int convertMainAction(@PlaybackModel.Action int action) {
        switch (action) {
            case PlaybackModel.ACTION_DISABLED:
                return PlayPauseStopImageView.ACTION_DISABLED;
            case PlaybackModel.ACTION_PLAY:
                return PlayPauseStopImageView.ACTION_PLAY;
            case PlaybackModel.ACTION_PAUSE:
                return PlayPauseStopImageView.ACTION_PAUSE;
            case PlaybackModel.ACTION_STOP:
                return PlayPauseStopImageView.ACTION_STOP;
        }
        Log.w(TAG, "Unknown action: " + action);
        return PlayPauseStopImageView.ACTION_DISABLED;
    }

    private void updateAccentColor() {
        int color = mModel.getAccentColor();
        int tintColor = ColorChecker.getTintColor(mContext, color);
        mPlayPauseStopImageView.setPrimaryActionColor(color, tintColor);
        mSpinner.setIndeterminateTintList(ColorStateList.valueOf(color));
    }

    private void updateCustomActions() {
        List<CustomPlaybackAction> customActions = mModel.getCustomActions();

        if (customActions.size() > mCustomActionButtons.size()) {
            for (int i = mCustomActionButtons.size(); i < customActions.size(); i++) {
                mCustomActionButtons.add(createIconButton(getContext(), mIconsColor, null));
            }
            setViews(mCustomActionButtons.toArray(new View[mCustomActionButtons.size()]));
            Log.i(TAG, "Increasing buttons array: " + customActions.size());
        }
        if (customActions.size() < mCustomActionButtons.size()) {
            while (mCustomActionButtons.size() > customActions.size()) {
                mCustomActionButtons.remove(mCustomActionButtons.size() - 1);
            }
            setViews(mCustomActionButtons.toArray(new View[mCustomActionButtons.size()]));
            Log.i(TAG, "Decreasing buttons array: " + customActions.size());
        }

        for (int pos = 0; pos < mCustomActionButtons.size(); pos++) {
            ImageButton button = mCustomActionButtons.get(pos);
            if (customActions.size() > pos) {
                button.setVisibility(VISIBLE);
                button.setImageDrawable(customActions.get(pos).mIcon);
            } else {
                button.setVisibility(INVISIBLE);
            }
        }
    }

    private void onPlayPauseStopClicked(View view) {
        if (mModel == null) {
            return;
        }
        switch (mPlayPauseStopImageView.getAction()) {
            case PlayPauseStopImageView.ACTION_PLAY:
                mModel.onPlay();
                break;
            case PlayPauseStopImageView.ACTION_PAUSE:
                mModel.onPause();
                break;
            case PlayPauseStopImageView.ACTION_STOP:
                mModel.onStop();
                break;
            default:
                Log.i(TAG, "Play/Pause/Stop clicked on invalid state");
                break;
        }
    }

    /**
     * Collapses the playback controls if they were expanded.
     */
    public void close() {
        // TODO(b/77242566): This will be implemented once the corresponding change is published in
        // Car Support Library.
    }

    /**
     * Defines the root {@link ViewGroup} used to animate the expand/collapse layout transitions.
     * If this method is not used, only this view will be animated.
     * If other elements of the screen have a layout relative to this view, their container
     * layout should be passed to this method.
     */
    public void setAnimationViewGroup(ViewGroup animationViewGroup) {
        // TODO(b/77242566): This will be implemented once the corresponding change is published in
        // Car Support Library.
    }
}
