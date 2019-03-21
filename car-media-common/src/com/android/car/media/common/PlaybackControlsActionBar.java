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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.android.car.apps.common.ControlBar;
import com.android.car.media.common.playback.PlaybackViewModel;
import com.android.car.media.common.source.MediaSourceColors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PlaybackControls} that uses the {@link ControlBar}
 */
public class PlaybackControlsActionBar extends ControlBar implements PlaybackControls {
    private static final String TAG = "PlaybackView";

    private static final float ALPHA_ENABLED = 1.0F;
    private static final float ALPHA_DISABLED = 0.5F;

    private PlayPauseStopImageView mPlayPauseStopImageView;
    private View mPlayPauseStopImageContainer;
    private ProgressBar mSpinner;
    private Context mContext;
    private ImageButton mSkipPrevButton;
    private ImageButton mSkipNextButton;
    private ImageButton mOverflowButton;
    private ColorStateList mIconsColor;
    private boolean mSkipNextAdded;
    private boolean mSkipPrevAdded;

    private PlaybackViewModel mModel;
    private PlaybackViewModel.PlaybackController mController;

    /** Creates a {@link PlaybackControlsActionBar} view */
    public PlaybackControlsActionBar(Context context) {
        this(context, null, 0, 0);
    }

    /** Creates a {@link PlaybackControlsActionBar} view */
    public PlaybackControlsActionBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    /** Creates a {@link PlaybackControlsActionBar} view */
    public PlaybackControlsActionBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /** Creates a {@link PlaybackControlsActionBar} view */
    public PlaybackControlsActionBar(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPlayPauseStopImageContainer = inflate(context, R.layout.play_pause_stop_button_layout,
                null);
        mPlayPauseStopImageContainer.setOnClickListener(this::onPlayPauseStopClicked);
        mPlayPauseStopImageView = mPlayPauseStopImageContainer.findViewById(R.id.play_pause_stop);
        mPlayPauseStopImageView.setVisibility(View.INVISIBLE);
        mSpinner = mPlayPauseStopImageContainer.findViewById(R.id.spinner);
        mSpinner.setVisibility(View.INVISIBLE);
        mPlayPauseStopImageView.setAction(PlayPauseStopImageView.ACTION_DISABLED);
        mPlayPauseStopImageView.setOnClickListener(this::onPlayPauseStopClicked);

        mIconsColor = context.getResources().getColorStateList(R.color.playback_control_color,
                null);

        mSkipPrevButton = createIconButton(context.getDrawable(R.drawable.ic_skip_previous));
        mSkipPrevButton.setId(R.id.skip_prev);
        mSkipPrevButton.setVisibility(VISIBLE);
        mSkipPrevButton.setOnClickListener(this::onPrevClicked);

        mSkipNextButton = createIconButton(context.getDrawable(R.drawable.ic_skip_next));
        mSkipNextButton.setId(R.id.skip_next);
        mSkipNextButton.setVisibility(VISIBLE);
        mSkipNextButton.setOnClickListener(this::onNextClicked);

        mOverflowButton = createIconButton(context.getDrawable(R.drawable.ic_overflow));
        mOverflowButton.setId(R.id.overflow);

        resetInitialViews();
    }

    private void resetInitialViews() {
        setViews(new View[0]);
        setView(mPlayPauseStopImageContainer, ControlBar.SLOT_MAIN);
        setView(null, ControlBar.SLOT_LEFT);
        setView(null, ControlBar.SLOT_RIGHT);
        mSkipNextAdded = false;
        mSkipPrevAdded = false;
        setExpandCollapseView(mOverflowButton);
    }

    @Override
    protected ImageButton createIconButton(Drawable icon) {
        ImageButton button = super.createIconButton(icon);
        button.setImageTintList(mIconsColor);
        button.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        return button;
    }

    @Override
    public void setModel(@NonNull PlaybackViewModel model, @NonNull LifecycleOwner owner) {
        if (mModel != null) {
            Log.w(TAG, "PlaybackViewModel set more than once. Ignoring subsequent call.");
        }
        mModel = model;

        model.getPlaybackController().observe(owner, controller -> {
            if (mController != controller) {
                mController = controller;
                resetInitialViews();
            }
        });
        mPlayPauseStopImageView.setVisibility(View.VISIBLE);
        model.getMediaSourceColors().observe(owner, this::applyColors);
        model.getPlaybackStateWrapper().observe(owner, this::onPlaybackStateChanged);
    }

    private void onPlaybackStateChanged(@Nullable PlaybackViewModel.PlaybackStateWrapper state) {

        boolean hasState = (state != null);
        mPlayPauseStopImageView.setAction(convertMainAction(state));
        mSpinner.setVisibility(hasState && state.isLoading() ? View.VISIBLE : View.INVISIBLE);

        // If prev/next is reserved, but not enabled, the icon is displayed as disabled (inactive
        // or grayed out). For example some apps only allow a certain number of skips in a given
        // time.

        boolean skipPreviousReserved = hasState && state.iSkipPreviousReserved();
        boolean skipPreviousEnabled = hasState && state.isSkipPreviousEnabled();

        if (skipPreviousReserved || skipPreviousEnabled) {
            if (!mSkipPrevAdded) {
                setView(mSkipPrevButton, ControlBar.SLOT_LEFT);
                mSkipPrevAdded = true;
            }
        } else {
            setView(null, ControlBar.SLOT_LEFT);
            mSkipPrevAdded = false;
        }

        if (skipPreviousEnabled) {
            mSkipPrevButton.setAlpha(ALPHA_ENABLED);
        } else {
            mSkipPrevButton.setAlpha(ALPHA_DISABLED);
        }


        boolean skipNextReserved = hasState && state.isSkipNextReserved();
        boolean skipNextEnabled = hasState && state.isSkipNextEnabled();

        if (skipNextReserved || skipNextEnabled) {
            if (!mSkipNextAdded) {
                setView(mSkipNextButton, ControlBar.SLOT_RIGHT);
                mSkipNextAdded = true;
            }
        } else {
            setView(null, ControlBar.SLOT_RIGHT);
            mSkipNextAdded = false;
        }

        if (skipNextEnabled) {
            mSkipNextButton.setAlpha(ALPHA_ENABLED);
        } else {
            mSkipNextButton.setAlpha(ALPHA_DISABLED);
        }

        updateCustomActions(state);
    }

    @PlayPauseStopImageView.Action
    private int convertMainAction(@Nullable PlaybackViewModel.PlaybackStateWrapper state) {
        @PlaybackViewModel.Action int action =
                (state != null) ? state.getMainAction() : PlaybackViewModel.ACTION_DISABLED;
        switch (action) {
            case PlaybackViewModel.ACTION_DISABLED:
                return PlayPauseStopImageView.ACTION_DISABLED;
            case PlaybackViewModel.ACTION_PLAY:
                return PlayPauseStopImageView.ACTION_PLAY;
            case PlaybackViewModel.ACTION_PAUSE:
                return PlayPauseStopImageView.ACTION_PAUSE;
            case PlaybackViewModel.ACTION_STOP:
                return PlayPauseStopImageView.ACTION_STOP;
        }
        Log.w(TAG, "Unknown action: " + action);
        return PlayPauseStopImageView.ACTION_DISABLED;
    }

    private void applyColors(MediaSourceColors colors) {
        int color = getMediaSourceColor(colors);
        int tintColor = ColorChecker.getTintColor(mContext, color);
        mPlayPauseStopImageView.setPrimaryActionColor(color, tintColor);
        mSpinner.setIndeterminateTintList(ColorStateList.valueOf(color));
    }

    private int getMediaSourceColor(@Nullable MediaSourceColors colors) {
        int defaultColor = mContext.getResources().getColor(android.R.color.background_dark, null);
        return colors != null ? colors.getAccentColor(defaultColor) : defaultColor;
    }

    private void updateCustomActions(@Nullable PlaybackViewModel.PlaybackStateWrapper state) {
        List<ImageButton> imageButtons = new ArrayList<>();
        if (state != null) {
            imageButtons.addAll(state.getCustomActions()
                    .stream()
                    .map(rawAction -> rawAction.fetchDrawable(getContext()))
                    .map(action -> {
                        ImageButton button = createIconButton(action.mIcon);
                        button.setOnClickListener(view ->
                                mController.doCustomAction(action.mAction, action.mExtras));
                        return button;
                    })
                    .collect(Collectors.toList()));
        }
        setViews(imageButtons.toArray(new ImageButton[0]));
    }

    private void onPlayPauseStopClicked(View view) {
        if (mController == null) {
            return;
        }
        switch (mPlayPauseStopImageView.getAction()) {
            case PlayPauseStopImageView.ACTION_PLAY:
                mController.play();
                break;
            case PlayPauseStopImageView.ACTION_PAUSE:
                mController.pause();
                break;
            case PlayPauseStopImageView.ACTION_STOP:
                mController.stop();
                break;
            default:
                Log.i(TAG, "Play/Pause/Stop clicked on invalid state");
                break;
        }
    }

    private void onNextClicked(View view) {
        PlaybackViewModel.PlaybackStateWrapper state = getPlaybackState();
        if ((mController != null) && (state != null) && (state.isSkipNextEnabled())) {
            mController.skipToNext();
        }
    }

    private void onPrevClicked(View view) {
        PlaybackViewModel.PlaybackStateWrapper state = getPlaybackState();
        if ((mController != null) && (state != null) && (state.isSkipPreviousEnabled())) {
            mController.skipToPrevious();
        }
    }

    private PlaybackViewModel.PlaybackStateWrapper getPlaybackState() {
        if (mModel != null) {
            return mModel.getPlaybackStateWrapper().getValue();
        }
        return null;
    }

    @Override
    public void close() {
        // TODO(b/77242566): This will be implemented once the corresponding change is published in
        // Car Support Library.
    }

    @Override
    public void setAnimationViewGroup(ViewGroup animationViewGroup) {
        // TODO(b/77242566): This will be implemented once the corresponding change is published in
        // Car Support Library.
    }
}
