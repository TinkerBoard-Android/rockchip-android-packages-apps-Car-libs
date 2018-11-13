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

import static com.android.car.arch.common.LiveDataFunctions.pair;
import static com.android.car.arch.common.LiveDataFunctions.split;

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

import com.android.car.apps.common.CarActionBar;
import com.android.car.media.common.playback.PlaybackViewModel;
import com.android.car.media.common.source.MediaSourceColors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PlaybackControls} that uses the {@link CarActionBar}
 */
public class PlaybackControlsActionBar extends CarActionBar implements PlaybackControls {
    private static final String TAG = "PlaybackView";

    private static final float ALPHA_ENABLED = 1.0F;
    private static final float ALPHA_DISABLED = 0.5F;

    private PlayPauseStopImageView mPlayPauseStopImageView;
    private View mPlayPauseStopImageContainer;
    private ProgressBar mSpinner;
    private Context mContext;
    private ImageButton mSkipPrevButton;
    private ImageButton mSkipNextButton;
    private ImageButton mTrackListButton;
    private ImageButton mOverflowButton;
    private ColorStateList mIconsColor;
    private Listener mListener;

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
        mPlayPauseStopImageContainer = inflate(context,
                R.layout.car_play_pause_stop_button_layout,
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

        mSkipPrevButton = createIconButton(mContext,
                context.getDrawable(R.drawable.ic_skip_previous));
        mSkipPrevButton.setId(R.id.skip_prev);
        mSkipPrevButton.setVisibility(VISIBLE);
        mSkipPrevButton.setOnClickListener(this::onPrevClicked);

        mSkipNextButton = createIconButton(mContext,
                context.getDrawable(R.drawable.ic_skip_next));
        mSkipNextButton.setId(R.id.skip_next);
        mSkipNextButton.setVisibility(VISIBLE);
        mSkipNextButton.setOnClickListener(this::onNextClicked);

        mTrackListButton = createIconButton(mContext,
                context.getDrawable(R.drawable.ic_tracklist));
        mTrackListButton.setId(R.id.track_list);
        mTrackListButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onToggleQueue();
            }
        });

        mOverflowButton = createIconButton(context,
                context.getDrawable(androidx.car.R.drawable.ic_overflow));
        mOverflowButton.setId(R.id.overflow);

        resetInitialViews();
    }

    private void resetInitialViews() {
        setViews(new View[0]);
        setView(mPlayPauseStopImageContainer, CarActionBar.SLOT_MAIN);
        setView(null, CarActionBar.SLOT_LEFT);
        setView(null, CarActionBar.SLOT_RIGHT);
        setExpandCollapseView(mOverflowButton);
    }

    private ImageButton createIconButton(Context context, Drawable icon) {
        ImageButton button = new ImageButton(context, null, 0, R.style.PlaybackControl);
        button.setImageTintList(mIconsColor);
        button.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        button.setImageDrawable(icon);
        return button;
    }

    @Override
    public void setModel(@NonNull PlaybackViewModel model, @NonNull LifecycleOwner owner) {
        if (mModel != null) {
            Log.w(TAG, "PlaybackViewModel set more than once. Ignoring subsequent call.");
        }
        mModel = model;
        PlaybackViewModel.PlaybackInfo playbackInfo = model.getPlaybackInfo();

        model.getPlaybackController().observe(owner, controller -> {
            if (mController != controller) {
                mController = controller;
                resetInitialViews();
            }
        });
        mPlayPauseStopImageView.setVisibility(View.VISIBLE);
        playbackInfo.getMainAction().observe(owner,
                action -> mPlayPauseStopImageView.setAction(convertMainAction(action)));
        playbackInfo.isLoading().observe(owner,
                isLoading -> mSpinner.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE));

        playbackInfo.isSkipPreviousReserved().observe(owner,
                reserved -> {
                    Boolean enabled = playbackInfo.isSkipPreviousEnabled().getValue();
                    boolean reservedImplicitly = enabled != null && enabled;
                    if (reserved || reservedImplicitly) {
                        setView(mSkipPrevButton, CarActionBar.SLOT_LEFT);
                    } else {
                        setView(null, CarActionBar.SLOT_LEFT);
                    }
                });

        playbackInfo.isSkipNextReserved().observe(owner,
                reserved -> {
                    Boolean enabled = playbackInfo.isSkipNextEnabled().getValue();
                    boolean reservedImplicitly = enabled != null && enabled;
                    if (reserved || reservedImplicitly) {
                        setView(mSkipNextButton, CarActionBar.SLOT_RIGHT);
                    } else {
                        setView(null, CarActionBar.SLOT_RIGHT);
                    }
                });

        playbackInfo.isSkipPreviousEnabled().observe(owner,
                enabled -> {
                    if (enabled) {
                        mSkipPrevButton.setAlpha(ALPHA_ENABLED);
                    } else {
                        mSkipPrevButton.setAlpha(ALPHA_DISABLED);
                    }
                });

        playbackInfo.isSkipNextEnabled().observe(owner,
                enabled -> {
                    if (enabled) {
                        mSkipNextButton.setAlpha(ALPHA_ENABLED);
                    } else {
                        mSkipNextButton.setAlpha(ALPHA_DISABLED);
                    }
                });
        model.getMediaSourceColors().observe(owner, this::applyColors);
        pair(model.hasQueue(), playbackInfo.getCustomActions()).observe(owner,
                split(this::updateCustomActions));
    }

    @PlayPauseStopImageView.Action
    private int convertMainAction(@PlaybackViewModel.Action int action) {
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

    private void updateCustomActions(boolean hasQueue,
            List<PlaybackViewModel.RawCustomPlaybackAction> customActions) {
        List<ImageButton> combinedActions = new ArrayList<>();
        if (hasQueue) {
            combinedActions.add(mTrackListButton);
        }
        combinedActions.addAll(customActions
                .stream()
                .map(rawAction -> rawAction.fetchDrawable(getContext()))
                .map(action -> {
                    ImageButton button = createIconButton(getContext(), action.mIcon);
                    button.setOnClickListener(view ->
                            mController.doCustomAction(action.mAction, action.mExtras));
                    return button;
                })
                .collect(Collectors.toList()));
        setViews(combinedActions.toArray(new ImageButton[0]));
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
        PlaybackViewModel.PlaybackInfo playbackInfo = getPlaybackInfoInternal();
        if (playbackInfo != null && playbackInfo.isSkipNextEnabled() != null) {
            Boolean enabled = playbackInfo.isSkipNextEnabled().getValue();
            if (enabled != null && enabled) {
                mController.skipToNext();
            }
        }
    }

    private void onPrevClicked(View view) {
        PlaybackViewModel.PlaybackInfo playbackInfo = getPlaybackInfoInternal();
        if (playbackInfo != null && playbackInfo.isSkipPreviousEnabled() != null) {
            Boolean enabled = playbackInfo.isSkipPreviousEnabled().getValue();
            if (enabled != null && enabled) {
                mController.skipToPrevious();
            }
        }
    }

    private PlaybackViewModel.PlaybackInfo getPlaybackInfoInternal() {
        if (mController != null && mModel != null) {
            return mModel.getPlaybackInfo();
        }
        return null;
    }

    @Override
    public void setQueueVisible(boolean visible) {
        mTrackListButton.setActivated(visible);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
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
