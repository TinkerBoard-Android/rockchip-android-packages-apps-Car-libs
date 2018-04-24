package com.android.car.media.common;

import android.view.ViewGroup;

/**
 * Custom view that can be used to display playback controls. It accepts a {@link PlaybackModel}
 * as its data source, automatically reacting to changes in playback state.
 */
public interface PlaybackControls {
    /**
     * Sets the {@link PlaybackModel} to use as the view model for this view.
     */
    void setModel(PlaybackModel model);

    /**
     * Collapses the playback controls if they were expanded.
     */
    void close();

    /**
     * Defines the root {@link ViewGroup} used to animate the expand/collapse layout transitions.
     * If this method is not used, only this view will be animated.
     * If other elements of the screen have a layout relative to this view, their container
     * layout should be passed to this method.
     */
    void setAnimationViewGroup(ViewGroup animationViewGroup);
}
