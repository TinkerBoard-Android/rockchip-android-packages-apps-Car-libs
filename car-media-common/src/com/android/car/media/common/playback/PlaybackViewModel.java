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

import static androidx.lifecycle.Transformations.map;
import static androidx.lifecycle.Transformations.switchMap;

import static com.android.car.arch.common.LiveDataFunctions.combine;
import static com.android.car.arch.common.LiveDataFunctions.dataOf;
import static com.android.car.arch.common.LiveDataFunctions.distinct;
import static com.android.car.arch.common.LiveDataFunctions.nullLiveData;
import static com.android.car.arch.common.LiveDataFunctions.pair;
import static com.android.car.media.common.playback.PlaybackStateAnnotations.Actions;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.android.car.arch.common.switching.SwitchingLiveData;
import com.android.car.media.common.CustomPlaybackAction;
import com.android.car.media.common.MediaItemMetadata;
import com.android.car.media.common.R;
import com.android.car.media.common.source.MediaSourceColors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ViewModel for media playback.
 * <p>
 * Observes changes to the provided MediaController to expose playback state and metadata
 * observables. Clients should ensure {@link #setMediaController(LiveData)} has been called before
 * calling any other methods. {@link #setMediaController(LiveData)} may be called multiple times,
 * but it is expected that clients will only call it once on startup.
 */
public class PlaybackViewModel extends AndroidViewModel {
    private static final String TAG = "PlaybackViewModel";

    private static final String ACTION_SET_RATING =
            "com.android.car.media.common.ACTION_SET_RATING";
    private static final String EXTRA_SET_HEART = "com.android.car.media.common.EXTRA_SET_HEART";

    /**
     * Possible main actions.
     */
    @IntDef({ACTION_PLAY, ACTION_STOP, ACTION_PAUSE, ACTION_DISABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    /**
     * Main action is disabled. The source can't play media at this time
     */
    public static final int ACTION_DISABLED = 0;
    /**
     * Start playing
     */
    public static final int ACTION_PLAY = 1;
    /**
     * Stop playing
     */
    public static final int ACTION_STOP = 2;
    /**
     * Pause playing
     */
    public static final int ACTION_PAUSE = 3;

    private final SwitchingLiveData<MediaController>
            mMediaController = SwitchingLiveData.newInstance();
    private final LiveData<MediaController> mMediaControllerData = mMediaController.asLiveData();

    private final LiveData<MediaController> mDistinctMediaController =
            distinct(mMediaControllerData, this::haveSamePackageName);

    private final MediaSourceColors.Factory mColorsFactory;
    private final LiveData<MediaSourceColors> mColors;

    private final LiveData<MediaMetadata> mMetadata = switchMap(mMediaControllerData,
            mediaController -> mediaController == null ? nullLiveData()
                    : new MediaMetadataLiveData(mediaController));
    private final LiveData<MediaItemMetadata> mWrappedMetadata = distinct(map(mMetadata,
            metadata -> metadata == null ? null : new MediaItemMetadata(metadata)));

    private final LiveData<PlaybackState> mPlaybackState = switchMap(mMediaControllerData,
            mediaController -> mediaController == null ? nullLiveData()
                    : new PlaybackStateLiveData(mediaController));

    private final LiveData<List<MediaSession.QueueItem>> mQueue = switchMap(mMediaControllerData,
            mediaController -> mediaController == null ? nullLiveData()
                    : new QueueLiveData(mediaController));

    // Filters out queue items with no description or title and converts them to MediaItemMetadatas
    private final LiveData<List<MediaItemMetadata>> mSanitizedQueue = distinct(map(mQueue,
            queue -> queue == null ? Collections.emptyList()
                    : queue.stream()
                            .filter(item -> item.getDescription() != null
                                    && item.getDescription().getTitle() != null)
                            .map(MediaItemMetadata::new)
                            .collect(Collectors.toList())));

    private final LiveData<Boolean> mHasQueue = distinct(map(mQueue,
            queue -> queue != null && !queue.isEmpty()));

    private final LiveData<PlaybackController> mPlaybackControls = map(mDistinctMediaController,
            PlaybackController::new);

    private final LiveData<CombinedInfo> mCombinedInfo = combine(mMetadata, mPlaybackState,
            // getValue() on mMediaController is safe because mMetadata and mPlaybackState are
            // keeping it active. Don't pass through the values since they may be stale if the
            // MediaController is being updated.
            (mediaMetadata, playbackState) -> getCombinedInfo(
                    mMediaController.asLiveData().getValue())
    );

    @Nullable
    private CombinedInfo getCombinedInfo(@Nullable MediaController mediaController) {
        if (mediaController == null) return null;

        MediaMetadata metadata = mediaController.getMetadata();
        PlaybackState playbackState = mediaController.getPlaybackState();
        CombinedInfo oldInfo = mCombinedInfo.getValue();
        // Minimize object churn
        if (oldInfo == null
                || !Objects.equals(mediaController, oldInfo.mMediaController)
                // MediaMetadata.equals() doesn't check some relevant fields
                || !Objects.equals(playbackState, oldInfo.mPlaybackState)) {
            return new CombinedInfo(mediaController, metadata, playbackState);
        } else {
            return oldInfo;
        }
    }


    private final PlaybackInfo mPlaybackInfo = new PlaybackInfo();

    public PlaybackViewModel(Application application) {
        super(application);
        mColorsFactory = new MediaSourceColors.Factory(application);
        mColors = map(mMediaController.asLiveData(), controller ->
                controller == null ? null
                        : mColorsFactory.extractColors(controller.getPackageName())
        );
    }

    /**
     * Sets the MediaController source for this ViewModel. This should be called before other
     * methods on this ViewModel are set. This method may be called multiple times, and the
     * LiveDatas returned by other methods in this ViewModel will refer to the latest
     * MediaController.
     */
    public void setMediaController(@Nullable LiveData<MediaController> mediaControllerData) {
        mMediaController.setSource(mediaControllerData);
    }

    /**
     * Returns a LiveData that emits the currently set MediaController (may emit null).
     */
    @NonNull
    public LiveData<MediaController> getMediaController() {
        return mMediaControllerData;
    }

    /**
     * Returns a LiveData that emits the colors for the currently set media source.
     */
    public LiveData<MediaSourceColors> getMediaSourceColors() {
        return mColors;
    }

    /**
     * Returns a LiveData that emits a MediaItemMetadata of the current media item in the session
     * managed by the provided {@link MediaController}.
     */
    public LiveData<MediaItemMetadata> getMetadata() {
        return mWrappedMetadata;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    LiveData<PlaybackState> getPlaybackState() {
        return mPlaybackState;
    }

    /**
     * Returns a LiveData that emits the current queue as MediaItemMetadatas where items without a
     * title have been filtered out.
     */
    public LiveData<List<MediaItemMetadata>> getQueue() {
        return mSanitizedQueue;
    }

    /**
     * Returns a LiveData that emits whether the MediaController has a non-empty queue
     */
    public LiveData<Boolean> hasQueue() {
        return mHasQueue;
    }

    /**
     * Returns a LiveData that emits an object for controlling the currently selected
     * MediaController.
     */
    public LiveData<PlaybackController> getPlaybackController() {
        return mPlaybackControls;
    }

    /**
     * Returns an object for accessing data dependent on the current playback state.
     */
    public PlaybackInfo getPlaybackInfo() {
        return mPlaybackInfo;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    LiveData<CombinedInfo> getCombinedInfoForTesting() {
        return mCombinedInfo;
    }

    private boolean haveSamePackageName(MediaController left, MediaController right) {
        if (left == right) return true;
        return left != null && right != null
                && Objects.equals(left.getPackageName(), right.getPackageName());
    }

    /**
     * Contains LiveDatas related to the current PlaybackState. A single instance of this object is
     * created for each PlaybackViewModel.
     */
    public class PlaybackInfo {
        private LiveData<Integer> mMainAction = map(mPlaybackState, state -> {
            if (state == null) {
                return ACTION_DISABLED;
            }

            @Actions long actions = state.getActions();
            @Action int stopAction = ACTION_DISABLED;
            if ((actions & (PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0) {
                stopAction = ACTION_PAUSE;
            } else if ((actions & PlaybackState.ACTION_STOP) != 0) {
                stopAction = ACTION_STOP;
            }

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
                case PlaybackState.STATE_ERROR:
                    return (actions & PlaybackState.ACTION_PLAY) != 0 ? ACTION_PLAY
                            : ACTION_DISABLED;
                default:
                    Log.w(TAG, String.format("Unknown PlaybackState: %d", state.getState()));
                    return ACTION_DISABLED;
            }
        });

        private final LiveData<Long> mMaxProgress = map(mMetadata,
                metadata -> metadata == null ? 0
                        : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));

        private final LiveData<Long> mProgress =
                switchMap(pair(mPlaybackState, mMaxProgress),
                        pair -> pair.first == null ? dataOf(0L)
                                : new ProgressLiveData(pair.first, pair.second));

        private final LiveData<Boolean> mIsPlaying = map(mPlaybackState,
                state -> state != null && state.getState() == PlaybackState.STATE_PLAYING);

        private final LiveData<Boolean> mIsSkipNextEnabled = map(mPlaybackState,
                state -> state != null
                        && (state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);

        private final LiveData<Boolean> mIsSkipPreviousEnabled = map(mPlaybackState,
                state -> state != null
                        && (state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);

        // true if the media source is loading (e.g.: buffering, connecting, etc.)
        private final LiveData<Boolean> mIsLoading = map(mPlaybackState,
                playbackState -> {
                    if (playbackState == null) {
                        return false;
                    }

                    int state = playbackState.getState();
                    return state == PlaybackState.STATE_BUFFERING
                            || state == PlaybackState.STATE_CONNECTING
                            || state == PlaybackState.STATE_FAST_FORWARDING
                            || state == PlaybackState.STATE_REWINDING
                            || state == PlaybackState.STATE_SKIPPING_TO_NEXT
                            || state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS
                            || state == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
                });

        private final LiveData<CharSequence> mErrorMessage = map(mPlaybackState,
                state -> state == null ? null : state.getErrorMessage());

        private final LiveData<Long> mActiveQueueItemId = map(mPlaybackState,
                state -> state == null ? MediaSession.QueueItem.UNKNOWN_ID
                        : state.getActiveQueueItemId());

        private final LiveData<List<RawCustomPlaybackAction>> mCustomActions =
                distinct(map(mCombinedInfo, this::getCustomActions));

        private PlaybackInfo() {
        }

        @Action
        public LiveData<Integer> getMainAction() {
            return mMainAction;
        }

        /**
         * Returns a sorted list of custom actions available. Call {@link
         * RawCustomPlaybackAction#fetchDrawable(Context)} to get the appropriate icon Drawable.
         */
        public LiveData<List<RawCustomPlaybackAction>> getCustomActions() {
            return mCustomActions;
        }

        /**
         * Returns a LiveData that emits the duration of the media item, in milliseconds. The
         * current position in this duration can be obtained by calling {@link #getProgress()}.
         */
        public LiveData<Long> getMaxProgress() {
            return mMaxProgress;
        }

        /**
         * Returns a LiveData that emits the current playback progress, in milliseconds. This is a
         * value between 0 and {@link #getMaxProgress()} or
         * {@link PlaybackState#PLAYBACK_POSITION_UNKNOWN}
         * if the current position is unknown. This value will update on its own periodically (less
         * than a second) while active.
         */
        public LiveData<Long> getProgress() {
            return mProgress;
        }

        /**
         * Returns a LiveData that emits {@code true} iff the current media source is playing a
         * media item.
         */
        public LiveData<Boolean> isPlaying() {
            return mIsPlaying;
        }

        /**
         * Returns a LiveData that emits {@code true} iff the media source supports skipping to the
         * next item.
         */
        public LiveData<Boolean> isSkipNextEnabled() {
            return mIsSkipNextEnabled;
        }

        /**
         * Returns a LiveData that emits {@code true} iff the media source supports skipping to the
         * previous item.
         */
        public LiveData<Boolean> isSkipPreviousEnabled() {
            return mIsSkipPreviousEnabled;
        }

        /**
         * Returns a LiveData that emits {@code true} iff the media source is loading (e.g.:
         * buffering, connecting, etc.)
         */
        public LiveData<Boolean> isLoading() {
            return mIsLoading;
        }

        /**
         * Returns a LiveData that emits a human readable description of the error that cause the
         * media source to be in a non-playable state, or {@code null} if there is no error.
         */
        public LiveData<CharSequence> getErrorMessage() {
            return mErrorMessage;
        }

        /**
         * Returns a LiveData that emits the queue id of the currently playing queue item, or {@link
         * MediaSession.QueueItem#UNKNOWN_ID} if none of the items is currently playing.
         */
        public LiveData<Long> getActiveQueueItemId() {
            return mActiveQueueItemId;
        }

        private List<RawCustomPlaybackAction> getCustomActions(
                @Nullable CombinedInfo info) {
            List<RawCustomPlaybackAction> actions = new ArrayList<>();
            if (info == null) return actions;
            PlaybackState playbackState = info.mPlaybackState;
            if (playbackState == null) return actions;

            RawCustomPlaybackAction ratingAction = getRatingAction(info);
            if (ratingAction != null) actions.add(ratingAction);

            for (PlaybackState.CustomAction action : playbackState.getCustomActions()) {
                String packageName = info.mMediaController.getPackageName();
                actions.add(
                        new RawCustomPlaybackAction(action.getIcon(), packageName,
                                action.getAction(),
                                action.getExtras()));
            }
            return actions;
        }

        @Nullable
        private RawCustomPlaybackAction getRatingAction(@Nullable CombinedInfo info) {
            if (info == null) return null;
            PlaybackState playbackState = info.mPlaybackState;
            if (playbackState == null) return null;

            long stdActions = playbackState.getActions();
            if ((stdActions & PlaybackState.ACTION_SET_RATING) == 0) return null;

            int ratingType = info.mMediaController.getRatingType();
            if (ratingType != Rating.RATING_HEART) return null;

            MediaMetadata metadata = info.mMetadata;
            boolean hasHeart = false;
            if (metadata != null) {
                Rating rating = metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING);
                hasHeart = rating != null && rating.hasHeart();
            }

            int iconResource = hasHeart ? R.drawable.ic_star_filled : R.drawable.ic_star_empty;
            Bundle extras = new Bundle();
            extras.putBoolean(EXTRA_SET_HEART, !hasHeart);
            return new RawCustomPlaybackAction(iconResource, null, ACTION_SET_RATING,
                    extras);
        }
    }

    /**
     * Wraps the {@link android.media.session.MediaController.TransportControls TransportControls}
     * for a {@link MediaController} to send commands.
     */
    public static class PlaybackController {
        private final MediaController mMediaController;

        private PlaybackController(@Nullable MediaController mediaController) {
            mMediaController = mediaController;
        }

        /**
         * Sends a 'play' command to the media source
         */
        public void play() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().play();
            }
        }

        /**
         * Sends a 'skip previews' command to the media source
         */
        public void skipToPrevious() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().skipToPrevious();
            }
        }

        /**
         * Sends a 'skip next' command to the media source
         */
        public void skipToNext() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().skipToNext();
            }
        }

        /**
         * Sends a 'pause' command to the media source
         */
        public void pause() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().pause();
            }
        }

        /**
         * Sends a 'stop' command to the media source
         */
        public void stop() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().stop();
            }
        }

        /**
         * Sends a custom action to the media source
         *
         * @param action identifier of the custom action
         * @param extras additional data to send to the media source.
         */
        public void doCustomAction(String action, Bundle extras) {
            if (mMediaController == null) return;
            MediaController.TransportControls cntrl = mMediaController.getTransportControls();

            if (ACTION_SET_RATING.equals(action)) {
                boolean setHeart = extras != null && extras.getBoolean(EXTRA_SET_HEART, false);
                cntrl.setRating(Rating.newHeartRating(setHeart));
            } else {
                cntrl.sendCustomAction(action, extras);
            }
        }

        /**
         * Starts playing a given media item. This id corresponds to {@link
         * MediaItemMetadata#getId()}.
         */
        public void playItem(String mediaItemId) {
            if (mMediaController != null) {
                mMediaController.getTransportControls().playFromMediaId(mediaItemId, null);
            }
        }

        /**
         * Skips to a particular item in the media queue. This id is {@link
         * MediaItemMetadata#mQueueId} of the items obtained through {@link
         * PlaybackViewModel#getQueue()}.
         */
        public void skipToQueueItem(long queueId) {
            if (mMediaController != null) {
                mMediaController.getTransportControls().skipToQueueItem(queueId);
            }
        }

        /**
         * Prepares the current media source for playback.
         */
        public void prepare() {
            if (mMediaController != null) {
                mMediaController.getTransportControls().prepare();
            }
        }
    }

    /**
     * Abstract representation of a custom playback action. A custom playback action represents a
     * visual element that can be used to trigger playback actions not included in the standard
     * {@link PlaybackController} class. Custom actions for the current media source are exposed
     * through {@link PlaybackInfo#getCustomActions()}
     * <p>
     * Does not contain a {@link Drawable} representation of the icon. Instances of this object
     * should be converted to a {@link CustomPlaybackAction} via {@link
     * RawCustomPlaybackAction#fetchDrawable(Context)} for display.
     */
    public static class RawCustomPlaybackAction {
        // TODO (keyboardr): This class (and associtated translation code) will be merged with
        // CustomPlaybackAction in a future CL.
        /**
         * Icon to display for this custom action
         */
        public final int mIcon;
        /**
         * If true, use the resources from the this package to resolve the icon. If null use our own
         * resources.
         */
        @Nullable
        public final String mPackageName;
        /**
         * Action identifier used to request this action to the media service
         */
        @NonNull
        public final String mAction;
        /**
         * Any additional information to send along with the action identifier
         */
        @Nullable
        public final Bundle mExtras;

        /**
         * Creates a custom action
         */
        public RawCustomPlaybackAction(int icon, String packageName,
                @NonNull String action,
                @Nullable Bundle extras) {
            mIcon = icon;
            mPackageName = packageName;
            mAction = action;
            mExtras = extras;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RawCustomPlaybackAction that = (RawCustomPlaybackAction) o;

            return mIcon == that.mIcon
                    && Objects.equals(mPackageName, that.mPackageName)
                    && Objects.equals(mAction, that.mAction)
                    && Objects.equals(mExtras, that.mExtras);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mIcon, mPackageName, mAction, mExtras);
        }

        /**
         * Converts this {@link RawCustomPlaybackAction} into a {@link CustomPlaybackAction} by
         * fetching the appropriate drawable for the icon.
         *
         * @param context Context into which the icon will be drawn
         * @return the converted CustomPlaybackAction or null if appropriate {@link Resources}
         * cannot be obtained
         */
        @Nullable
        public CustomPlaybackAction fetchDrawable(@NonNull Context context) {
            Drawable icon;
            if (mPackageName == null) {
                icon = context.getDrawable(mIcon);
            } else {
                Resources resources = getResourcesForPackage(context, mPackageName);
                if (resources == null) {
                    return null;
                } else {
                    // the resources may be from another package. we need to update the
                    // configuration
                    // using the context from the activity so we get the drawable from the
                    // correct DPI
                    // bucket.
                    resources.updateConfiguration(context.getResources().getConfiguration(),
                            context.getResources().getDisplayMetrics());
                    icon = resources.getDrawable(mIcon, null);
                }
            }
            return new CustomPlaybackAction(icon, mAction, mExtras);
        }

        private Resources getResourcesForPackage(Context context, String packageName) {
            try {
                return context.getPackageManager().getResourcesForApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to get resources for " + packageName);
                return null;
            }
        }
    }

    static class CombinedInfo {
        final MediaController mMediaController;
        final MediaMetadata mMetadata;
        final PlaybackState mPlaybackState;

        private CombinedInfo(MediaController mediaController,
                MediaMetadata metadata, PlaybackState playbackState) {
            this.mMediaController = mediaController;
            this.mMetadata = metadata;
            this.mPlaybackState = playbackState;
        }
    }
}
