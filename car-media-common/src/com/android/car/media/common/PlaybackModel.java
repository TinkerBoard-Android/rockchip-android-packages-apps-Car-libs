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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * View-model for playback UI components. This abstractions provides a simplified view of
 * {@link MediaSession} and {@link MediaSessionManager} data and events.
 * <p>
 * It automatically determines the foreground media app (the one that would normally
 * receive playback events) and exposes metadata and events from such app, or when a different app
 * becomes foreground.
 * <p>
 * This requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL
 * permission be held by the calling app.
 */
public class PlaybackModel {
    private static final String TAG = "PlaybackModel";

    private final MediaSessionManager mMediaSessionManager;
    @Nullable
    private MediaController mMediaController;
    private Context mContext;
    private List<PlaybackObserver> mObservers = new ArrayList<>();
    private final MediaSessionUpdater mMediaSessionUpdater = new MediaSessionUpdater();

    /**
     * Temporary work-around to bug b/76017849.
     * MediaSessionManager is not notifying media session priority changes.
     * As a work-around we subscribe to playback state changes on all controllers to detect
     * potential priority changes.
     * This might cause a few unnecessary checks, but selecting the top-most controller is a
     * cheap operation.
     */
    private class MediaSessionUpdater {
        private Map<String, MediaController> mControllersByPackageName = new HashMap<>();

        private MediaController.Callback mCallback = new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                selectMediaController(mMediaSessionManager.getActiveSessions(null));
            }
        };

        void setControllersByPackageName(List<MediaController> newControllers) {
            Map<String, MediaController> newControllersMap = new HashMap<>();
            for (MediaController newController : newControllers) {
                if (!mControllersByPackageName.containsKey(newController.getPackageName())) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "New controller detected: "
                                + newController.getPackageName());
                    }
                    newController.registerCallback(mCallback);
                } else {
                    mControllersByPackageName.remove(newController.getPackageName());
                }
                newControllersMap.put(newController.getPackageName(), newController);
            }
            for (MediaController oldController : mControllersByPackageName.values()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Removed controller detected: "
                            + oldController.getPackageName());
                }
                oldController.unregisterCallback(mCallback);
            }
            mControllersByPackageName = newControllersMap;
        }
    }

    /**
     * An observer of this model
     */
    public abstract static class PlaybackObserver {
        /**
         * Called whenever the playback state of the current media item changes.
         */
        protected void onPlaybackStateChanged() {}

        /**
         * Called when the top source media app changes.
         */
        protected void onSourceChanged() {};

        /**
         * Called when the media item being played changes.
         */
        protected void onMetadataChanged() {};
    }

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPlaybackStateChanged: " + state);
            }
            PlaybackModel.this.notify(PlaybackObserver::onPlaybackStateChanged);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onMetadataChanged: " + metadata);
            }
            PlaybackModel.this.notify(PlaybackObserver::onMetadataChanged);
        }
    };

    private MediaSessionManager.OnActiveSessionsChangedListener mSessionChangeListener =
            this::selectMediaController;

    /**
     * Creates a {@link PlaybackModel}. By default this instance is going to be inactive until
     * {@link #start()} method is invoked.
     */
    public PlaybackModel(Context context) {
        mContext = context;
        mMediaSessionManager = mContext.getSystemService(MediaSessionManager.class);
    }

    private void selectMediaController(List<MediaController> controllers) {
        changeMediaController(controllers != null && controllers.size() > 0 ? controllers.get(0) :
                null);
        mMediaSessionUpdater.setControllersByPackageName(controllers);
    }

    private void changeMediaController(MediaController mediaController) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "New media controller: " + (mediaController != null
                    ? mediaController.getPackageName() : null));
        }
        if (mediaController == mMediaController) {
            // If no change, do nothing.
            return;
        }
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCallback);
        }
        mMediaController = mediaController;
        if (mMediaController != null) {
            mMediaController.registerCallback(mCallback);
        }
        notify(PlaybackObserver::onSourceChanged);
    }

    /**
     * Starts following changes on the list of active media sources. If any changes happen, all
     * observers registered through {@link #registerObserver(PlaybackObserver)} will be notified.
     * <p>
     * Calling this method might cause an immediate {@link PlaybackObserver#onSourceChanged()}
     * event in case the current media source is different than the last known one.
     */
    public void start() {
        mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionChangeListener, null);
        selectMediaController(mMediaSessionManager.getActiveSessions(null));
    }

    /**
     * Stops following changes on the list of active media sources. This method could cause an
     * immediate {@link PlaybackObserver#onSourceChanged()} event if a media source was already
     * connected.
     */
    public void stop() {
        mMediaSessionUpdater.setControllersByPackageName(new ArrayList<>());
        mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionChangeListener);
        changeMediaController(null);
    }

    private void notify(Consumer<PlaybackObserver> notification) {
        for (PlaybackObserver observer : mObservers) {
            notification.accept(observer);
        }
    }

    /**
     * @return the package name of the currently selected media source. Changes on this value will
     * be notified through {@link PlaybackObserver#onSourceChanged()}
     */
    @Nullable
    public String getPackageName() {
        if (mMediaController == null) {
            return null;
        }
        return mMediaController.getPackageName();
    }

    /**
     * @return {@link Action} selected as the main action for the current media item, based on the
     * current playback state and the available actions reported by the media source.
     * Changes on this value will be notified through
     * {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    @Action
    public int getMainAction() {
        return getMainAction(mMediaController != null ? mMediaController.getPlaybackState() : null);
    }

    /**
     * @return {@link MediaItemMetadata} of the currently selected media item in the media source.
     * Changes on this value will be notified through {@link PlaybackObserver#onMetadataChanged()}
     */
    @Nullable
    public MediaItemMetadata getMetadata() {
        if (mMediaController == null) {
            return null;
        }
        MediaMetadata metadata = mMediaController.getMetadata();
        if (metadata == null) {
            return null;
        }
        return new MediaItemMetadata(mContext, metadata);
    }

    /**
     * @return an integer representing the maximum value for the progress bar corresponding on the
     * current position in the media item, which can be obtained by calling {@link #getProgress()}.
     * Changes on this value will be notified through {@link PlaybackObserver#onMetadataChanged()}
     */
    public int getMaxProgress() {
        if (mMediaController == null || mMediaController.getMetadata() == null) {
            return 0;
        } else {
            return (int) mMediaController.getMetadata()
                    .getLong(MediaMetadata.METADATA_KEY_DURATION);
        }
    }

    /**
     * Sends a 'play' command to the media source
     */
    public void onPlay() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().play();
        }
    }

    /**
     * Sends a 'skip previews' command to the media source
     */
    public void onSkipPreviews() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().skipToPrevious();
        }

    }

    /**
     * Sends a 'skip next' command to the media source
     */
    public void onSkipNext() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().skipToNext();
        }
    }

    /**
     * Sends a 'pause' command to the media source
     */
    public void onPause() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().pause();
        }
    }

    /**
     * Sends a 'stop' command to the media source
     */
    public void onStop() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().stop();
        }
    }

    /**
     * Sends a custom action to the media source
     * @param action identifier of the custom action
     * @param extras additional data to send to the media source.
     */
    public void onCustomAction(String action, Bundle extras) {
        if (mMediaController != null) {
            mMediaController.getTransportControls().sendCustomAction(action, extras);
        }
    }

    /** Third-party defined application theme to use * */
    private static final String THEME_META_DATA_NAME =
            "com.google.android.gms.car.application.theme";

    /**
     * @return the accent color of the currently connected media source. Changes on this value will
     * be notified through {@link PlaybackObserver#onSourceChanged()}
     */
    public int getAccentColor() {
        if (mMediaController == null) {
            return mContext.getResources().getColor(android.R.color.background_dark, null);
        }
        return getAccentColor(getPackageName());
    }

    private int getAccentColor(String packageName) {
        int defaultColor = mContext.getResources().getColor(android.R.color.background_dark, null);
        TypedArray ta = null;
        try {
            ApplicationInfo applicationInfo =
                    mContext.getPackageManager().getApplicationInfo(packageName,
                            PackageManager.GET_META_DATA);
            // CharSequence title = applicationInfo.loadLabel(getContext().getPackageManager());
            Context packageContext = mContext.createPackageContext(packageName, 0);
            int appTheme = applicationInfo.metaData != null
                    ? applicationInfo.metaData.getInt(THEME_META_DATA_NAME)
                    : 0;
            appTheme = appTheme == 0
                    ? applicationInfo.theme
                    : appTheme;
            packageContext.setTheme(appTheme);
            Resources.Theme theme = packageContext.getTheme();
            ta = theme.obtainStyledAttributes(new int[] {
                    android.R.attr.colorAccent
            });
            return ta.getColor(0, defaultColor);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to obtain accent color from package: " + packageName);
            return defaultColor;
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
    }

    /**
     * Possible main actions.
     */
    @IntDef({ACTION_PLAY, ACTION_STOP, ACTION_PAUSE, ACTION_DISABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {}

    /** Main action is disabled. The source can't play media at this time */
    public static final int ACTION_DISABLED = 0;
    /** Start playing */
    public static final int ACTION_PLAY = 1;
    /** Stop playing */
    public static final int ACTION_STOP = 2;
    /** Pause playing */
    public static final int ACTION_PAUSE = 3;

    @Action
    private static int getMainAction(PlaybackState state) {
        if (state == null) {
            return ACTION_DISABLED;
        }
        int stopAction = ((state.getActions() & PlaybackState.ACTION_PAUSE) != 0)
                ? ACTION_PAUSE
                : ACTION_STOP;
        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
            case PlaybackState.STATE_FAST_FORWARDING:
            case PlaybackState.STATE_REWINDING:
            case PlaybackState.STATE_SKIPPING_TO_NEXT:
            case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                return stopAction;
            case PlaybackState.STATE_STOPPED:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_NONE:
                return ACTION_PLAY;
            case PlaybackState.STATE_ERROR:
                return ACTION_DISABLED;
            default:
                Log.w(TAG, String.format("Unknown PlaybackState: %d", state.getState()));
                return ACTION_DISABLED;
        }
    }

    /**
     * @return the current playback progress. This is a value between 0 and
     * {@link #getMaxProgress()}.
     */
    public int getProgress() {
        if (mMediaController == null) {
            return 0;
        }
        PlaybackState state = mMediaController.getPlaybackState();
        if (state == null) {
            return 0;
        }
        long timeDiff = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
        float speed = state.getPlaybackSpeed();
        if (state.getState() == PlaybackState.STATE_PAUSED
                || state.getState() == PlaybackState.STATE_STOPPED) {
            // This guards against apps who don't keep their playbackSpeed to spec (b/62375164)
            speed = 0f;
        }
        long posDiff = (long) (timeDiff * speed);
        return Math.min((int) (posDiff + state.getPosition()), getMaxProgress());
    }

    /**
     * @return true if the current media source is playing a media item. Changes on this value
     * would be notified through {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    public boolean isPlaying() {
        return mMediaController != null
                && mMediaController.getPlaybackState() != null
                && mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
    }

    /**
     * Registers an observer to be notified of media events.
     */
    public void registerObserver(PlaybackObserver observer) {
        mObservers.add(observer);
    }

    /**
     * Unregisters an observer previously registered using
     * {@link #registerObserver(PlaybackObserver)}
     */
    public void unregisterObserver(PlaybackObserver observer) {
        mObservers.remove(observer);
    }

    /**
     * @return true if the media source supports skipping to next item. Changes on this value
     * will be notified through {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    public boolean isSkipNextEnabled() {
        return mMediaController != null
                && mMediaController.getPlaybackState() != null
                && (mMediaController.getPlaybackState().getActions()
                    & PlaybackState.ACTION_SKIP_TO_NEXT) != 0;
    }

    /**
     * @return true if the media source supports skipping to previous item. Changes on this value
     * will be notified through {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    public boolean isSkipPreviewsEnabled() {
        return mMediaController != null
                && mMediaController.getPlaybackState() != null
                && (mMediaController.getPlaybackState().getActions()
                    & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0;
    }

    /**
     * @return true if the media source is buffering. Changes on this value would be notified
     * through {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    public boolean isBuffering() {
        return mMediaController != null
                && mMediaController.getPlaybackState() != null
                && mMediaController.getPlaybackState().getState() == PlaybackState.STATE_BUFFERING;
    }

    /**
     * @return a human readable description of the error that cause the media source to be in a
     * non-playable state, or null if there is no error. Changes on this value will be notified
     * through {@link PlaybackObserver#onPlaybackStateChanged()}
     */
    @Nullable
    public CharSequence getErrorMessage() {
        return mMediaController != null && mMediaController.getPlaybackState() != null
                ? mMediaController.getPlaybackState().getErrorMessage()
                : null;
    }

    /**
     * @return a sorted list of {@link MediaItemMetadata} corresponding to the queue of media items
     * as reported by the media source. Changes on this value will be notified through
     * {@link PlaybackObserver#onPlaybackStateChanged()}.
     */
    @NonNull
    public List<MediaItemMetadata> getQueue() {
        List<MediaSession.QueueItem> items = mMediaController.getQueue();
        if (items != null) {
            return items.stream()
                    .map(item -> new MediaItemMetadata(mContext, item))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * @return true if the media queue is not empty. Detailed information can be obtained by
     * calling to {@link #getQueue()}. Changes on this value will be notified through
     * {@link PlaybackObserver#onPlaybackStateChanged()}.
     */
    public boolean hasQueue() {
        List<MediaSession.QueueItem> items = mMediaController.getQueue();
        return items != null && !items.isEmpty();
    }

    /**
     * @return a sorted list of custom actions, as reported by the media source. Changes on this
     * value will be notified through
     * {@link PlaybackObserver#onPlaybackStateChanged()}.
     */
    public List<CustomPlaybackAction> getCustomActions() {
        List<CustomPlaybackAction> actions = new ArrayList<>();
        if (mMediaController == null || mMediaController.getPlaybackState() == null) {
            return actions;
        }
        for (PlaybackState.CustomAction action : mMediaController.getPlaybackState()
                .getCustomActions()) {
            Resources resources = getResourcesForPackage(mMediaController.getPackageName());
            if (resources == null) {
                actions.add(null);
            } else {
                // the resources may be from another package. we need to update the configuration
                // using the context from the activity so we get the drawable from the correct DPI
                // bucket.
                resources.updateConfiguration(mContext.getResources().getConfiguration(),
                        mContext.getResources().getDisplayMetrics());
                Drawable icon = resources.getDrawable(action.getIcon(), null);
                actions.add(new CustomPlaybackAction(icon, action.getAction(), action.getExtras()));
            }
        }
        return actions;
    }

    private Resources getResourcesForPackage(String packageName) {
        try {
            return mContext.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get resources for " + packageName);
            return null;
        }
    }
}
