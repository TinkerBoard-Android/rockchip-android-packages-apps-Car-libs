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

package com.android.car.media.common.source;

import static androidx.lifecycle.Transformations.map;
import static androidx.lifecycle.Transformations.switchMap;

import static com.android.car.arch.common.LiveDataFunctions.coalesceNull;
import static com.android.car.arch.common.LiveDataFunctions.combine;
import static com.android.car.arch.common.LiveDataFunctions.mapNonNull;
import static com.android.car.arch.common.LiveDataFunctions.nullLiveData;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UiThread;
import android.app.Application;
import android.content.ComponentName;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;

import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.media.common.source.MediaBrowserConnector.ConnectionState;
import com.android.car.media.common.source.MediaBrowserConnector.MediaBrowserState;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Contains observable data needed for displaying playback and browse UI
 */
public class MediaSourceViewModel extends AndroidViewModel {

    private final LiveData<List<SimpleMediaSource>> mMediaSources;

    private final LiveData<Boolean> mHasMediaSources;

    private final MutableLiveData<SimpleMediaSource> mSelectedMediaSource = new MutableLiveData<>();

    private final LiveData<MediaBrowser> mConnectedMediaBrowser;

    // Media controller for selected media source.
    private final LiveData<MediaController> mMediaController;

    // Media controller for active media source, may not be the same as selected media source.
    private final LiveData<MediaController> mTopActiveMediaController;

    private final LiveData<Boolean> mIsCurrentMediaSourcePlaying;

    /**
     * Factory for creating dependencies. Can be swapped out for testing.
     */
    @VisibleForTesting
    interface InputFactory {
        LiveData<List<SimpleMediaSource>> createMediaSources();

        LiveData<MediaBrowserState> createMediaBrowserConnector(
                @NonNull ComponentName browseService);

        ActiveMediaSelector createActiveMediaSelector();

        LiveData<List<MediaController>> createActiveMediaControllerData();

        MediaController getControllerForSession(@Nullable MediaSession.Token session);
    }

    /**
     * Create a new instance of MediaSourceViewModel
     *
     * @see AndroidViewModel
     */
    public MediaSourceViewModel(@NonNull Application application) {
        this(application, new InputFactory() {

            @Override
            public LiveData<List<SimpleMediaSource>> createMediaSources() {
                return new MediaSourcesLiveData(application);
            }

            @Override
            public LiveData<MediaBrowserState> createMediaBrowserConnector(
                    @NonNull ComponentName browseService) {
                return new MediaBrowserConnector(application, browseService);
            }

            @Override
            public LiveData<List<MediaController>> createActiveMediaControllerData() {
                return new ActiveMediaControllersLiveData(application);
            }

            @Override
            public ActiveMediaSelector createActiveMediaSelector() {
                return new ActiveMediaSelector(application);
            }

            @Override
            public MediaController getControllerForSession(@Nullable MediaSession.Token token) {
                if (token == null) return null;
                return new MediaController(application, token);
            }
        });
    }

    @VisibleForTesting
    MediaSourceViewModel(@NonNull Application application, @NonNull InputFactory inputFactory) {
        super(application);

        LiveData<List<MediaController>> activeMediaControllers =
                inputFactory.createActiveMediaControllerData();
        ActiveMediaSelector mediaSelector = inputFactory.createActiveMediaSelector();
        mTopActiveMediaController = map(activeMediaControllers,
                new LoopbackFunction<>(mediaSelector::getTopMostMediaController));

        mMediaSources = inputFactory.createMediaSources();
        mHasMediaSources = map(mMediaSources, sources -> sources != null && !sources.isEmpty());

        LiveData<MediaBrowserState> mediaBrowserState = switchMap(mSelectedMediaSource,
                (mediaSource) -> {
                    if (mediaSource == null) {
                        return nullLiveData();
                    }
                    ComponentName browseService = mediaSource.getBrowseServiceComponentName();
                    if (browseService == null) {
                        return nullLiveData();
                    }
                    return inputFactory.createMediaBrowserConnector(browseService);
                });
        mConnectedMediaBrowser = map(mediaBrowserState,
                state -> state != null && (state.mConnectionState == ConnectionState.CONNECTED)
                        ? state.mMediaBrowser : null);

        LiveData<MediaController> controllerFromActiveList =
                combine(activeMediaControllers, mSelectedMediaSource,
                        mediaSelector::getControllerForSource);
        LiveData<MediaController> controllerFromMediaBrowser = mapNonNull(mConnectedMediaBrowser,
                browser -> inputFactory.getControllerForSession(browser.getSessionToken()));
        // Prefer fetching MediaController from MediaSessionManager's active controller
        // list. Otherwise use controller from MediaBrowser (which requires connecting to it).
        mMediaController = coalesceNull(controllerFromActiveList, controllerFromMediaBrowser);

        mIsCurrentMediaSourcePlaying = combine(mTopActiveMediaController, mSelectedMediaSource,
                (mediaController, mediaSource) ->
                        mediaController != null && mediaSource != null
                                && Objects.equals(mediaController.getPackageName(),
                                mediaSource.getPackageName()));
    }

    /**
     * Returns a live list of all MediaSources that can be selected for playback
     */
    @NonNull
    public LiveData<List<SimpleMediaSource>> getMediaSources() {
        return mMediaSources;
    }

    /**
     * Returns a LiveData that emits whether there are any media sources that can be selected for
     * playback
     */
    public LiveData<Boolean> hasMediaSources() {
        return mHasMediaSources;
    }

    /**
     * Returns a LiveData that emits the MediaSource that is to be browsed or displayed.
     */
    public LiveData<SimpleMediaSource> getSelectedMediaSource() {
        return mSelectedMediaSource;
    }

    /**
     * Set the MediaSource that is to be browsed or displayed. If a browse service is available, a
     * connection may be made and provided through {@link #getConnectedMediaBrowser()}.
     */
    @UiThread
    public void setSelectedMediaSource(@Nullable SimpleMediaSource mediaSource) {
        mSelectedMediaSource.setValue(mediaSource);
    }

    /**
     * Returns a LiveData that emits the currently connected MediaBrowser. Emits {@code null} if no
     * MediaSource is set, if the MediaSource does not support browsing, or if the MediaBrowser is
     * not connected. Observing the LiveData will attempt to connect to a media browse session if
     * possible.
     */
    public LiveData<MediaBrowser> getConnectedMediaBrowser() {
        return mConnectedMediaBrowser;
    }

    /**
     * Returns a LiveData that emits a {@link MediaController} that allows controlling this media
     * source, or emits {@code null} if the media source doesn't support browsing or the browser is
     * not connected.
     */
    public LiveData<MediaController> getMediaController() {
        return mMediaController;
    }

    /**
     * Returns a LiveData that emits a {@link MediaController} for the primary active media source.
     * Note that this may not be from the selected media source.
     */
    public LiveData<MediaController> getTopActiveMediaController() {
        return mTopActiveMediaController;
    }

    /**
     * Emits {@code true} iff the selected media source is the active media source
     */
    public LiveData<Boolean> isCurrentMediaSourcePlaying() {
        return mIsCurrentMediaSourcePlaying;
    }

    private static class LoopbackFunction<P, V> implements Function<P, V> {

        private final BiFunction<P, V, V> mFunction;
        private V mLastValue;

        private LoopbackFunction(BiFunction<P, V, V> function) {
            mFunction = function;
        }

        @Override
        public V apply(P param) {
            mLastValue = mFunction.apply(param, mLastValue);
            return mLastValue;
        }
    }
}
