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

package com.android.car.media.common.browse;

import static androidx.lifecycle.Transformations.map;

import static com.android.car.arch.common.LiveDataFunctions.pair;
import static com.android.car.arch.common.LiveDataFunctions.split;
import static com.android.car.arch.common.LoadingSwitchMap.loadingSwitchMap;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UiThread;
import android.app.Application;
import android.support.v4.media.MediaBrowserCompat;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.LoadingSwitchMap;
import com.android.car.arch.common.switching.SwitchingLiveData;
import com.android.car.media.common.MediaItemMetadata;

import java.util.List;

/**
 * Contains observable data needed for displaying playback and browse UI
 */

public class MediaBrowserViewModel extends AndroidViewModel {

    /**
     * Possible states of the application UI
     */
    public enum BrowseState {
        /** There is no content to show */
        EMPTY,
        /** We are still in the process of obtaining data */
        LOADING,
        /** Data has been loaded */
        LOADED,
        /** The content can't be shown due an error */
        ERROR
    }

    private final SwitchingLiveData<MediaBrowserCompat> mMediaBrowserSwitch =
            SwitchingLiveData.newInstance();

    private final LiveData<MediaBrowserCompat> mConnectedMediaBrowser =
            map(mMediaBrowserSwitch.asLiveData(),
                    MediaBrowserViewModel::requireConnected);

    private final MutableLiveData<String> mCurrentBrowseId = new MutableLiveData<>();

    private final LoadingSwitchMap<List<MediaItemMetadata>> mCurrentMediaItems =
            loadingSwitchMap(pair(mConnectedMediaBrowser, mCurrentBrowseId),
                    split((connectedMediaBrowser, browseId) ->
                            connectedMediaBrowser == null
                                    ? null
                                    : new BrowsedMediaItems(connectedMediaBrowser, browseId)));
    private final LiveData<BrowseState> mBrowseState = new MediatorLiveData<BrowseState>() {
        {
            setValue(BrowseState.EMPTY);
            addSource(mCurrentMediaItems.isLoading(), isLoading -> update());
            addSource(mCurrentMediaItems.getOutput(), items -> update());
        }

        private void update() {
            setValue(getState());
        }

        private BrowseState getState() {
            Boolean isLoading = mCurrentMediaItems.isLoading().getValue();
            if (isLoading == null) {
                // Uninitialized
                return BrowseState.EMPTY;
            }
            if (isLoading) {
                return BrowseState.LOADING;
            }
            List<MediaItemMetadata> items = mCurrentMediaItems.getOutput().getValue();
            if (items == null) {
                // Normally this could be null if it hasn't been initialized, but in that case
                // isLoading would not be false, so this means it must have encountered an error.
                return BrowseState.ERROR;
            }
            if (items.isEmpty()) {
                return BrowseState.EMPTY;
            }
            return BrowseState.LOADED;
        }
    };

    public MediaBrowserViewModel(@NonNull Application application) {
        super(application);
    }

    private static MediaBrowserCompat requireConnected(@Nullable MediaBrowserCompat mediaBrowser) {
        if (mediaBrowser != null && !mediaBrowser.isConnected()) {
            throw new IllegalStateException(
                    "Only connected MediaBrowsers may be provided to MediaBrowserViewModel.");
        }
        return mediaBrowser;
    }

    /**
     * Set the source {@link MediaBrowserCompat} to use for browsing. If {@code mediaBrowser} emits
     * non-null, the MediaBrowser emitted must already be in a connected state.
     */
    public void setConnectedMediaBrowser(@Nullable LiveData<MediaBrowserCompat> mediaBrowser) {
        mMediaBrowserSwitch.setSource(mediaBrowser);
    }

    /**
     * Set the current item to be browsed. If available, the list of items will be emitted by {@link
     * #getBrowsedMediaItems()}.
     */
    @UiThread
    public void setCurrentBrowseId(@Nullable String browseId) {
        mCurrentBrowseId.setValue(browseId);
    }

    public String getCurrentBrowseId() {
        return mCurrentBrowseId.getValue();
    }

    public LiveData<BrowseState> getBrowseState() {
        return mBrowseState;
    }

    public LiveData<Boolean> isLoading() {
        return mCurrentMediaItems.isLoading();
    }

    /**
     * Fetches the MediaItemMetadatas for the current browsed id. A MediaSource must be selected and
     * its MediaBrowser connected, otherwise this will emit {@code null}.
     *
     * @return a LiveData that emits the MediaItemMetadatas for the current browsed id or {@code
     * null} if unavailable.
     * @see #setCurrentBrowseId(String)
     */
    public LiveData<List<MediaItemMetadata>> getBrowsedMediaItems() {
        return mCurrentMediaItems.getOutput();
    }

}
