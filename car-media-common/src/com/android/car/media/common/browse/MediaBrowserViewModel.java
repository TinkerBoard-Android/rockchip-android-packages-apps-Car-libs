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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UiThread;
import android.app.Application;
import android.media.browse.MediaBrowser;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.arch.common.switching.SwitchingLiveData;
import com.android.car.media.common.MediaItemMetadata;

import java.util.List;

/**
 * Contains observable data needed for displaying playback and browse UI
 */

public class MediaBrowserViewModel extends AndroidViewModel {

    private final SwitchingLiveData<MediaBrowser> mMediaBrowserSwitch =
            SwitchingLiveData.newInstance();

    private final LiveData<MediaBrowser> mConnectedMediaBrowser =
            map(mMediaBrowserSwitch.asLiveData(),
                    MediaBrowserViewModel::requireConnected);

    private final MutableLiveData<String> mCurrentBrowseId = new MutableLiveData<>();

    private final LiveData<List<MediaItemMetadata>> mCurrentMediaItems =
            Transformations.switchMap(pair(mConnectedMediaBrowser, mCurrentBrowseId),
                    (pair) -> pair == null || pair.first == null
                            ? null
                            : new BrowsedMediaItems(pair.first, pair.second));


    public MediaBrowserViewModel(@NonNull Application application) {
        super(application);
    }

    private static MediaBrowser requireConnected(@Nullable MediaBrowser mediaBrowser) {
        if (mediaBrowser != null && !mediaBrowser.isConnected()) {
            throw new IllegalStateException(
                    "Only connected MediaBrowsers may be provided to MediaBrowserViewModel.");
        }
        return mediaBrowser;
    }

    /**
     * Set the source {@link MediaBrowser} to use for browsing. If {@code mediaBrowser} emits
     * non-null, the MediaBrowser emitted must already be in a connected state.
     */
    public void setConnectedMediaBrowser(@Nullable LiveData<MediaBrowser> mediaBrowser) {
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

    /**
     * Fetches the MediaItemMetadatas for the current browsed id. A MediaSource must be selected and
     * its MediaBrowser connected, otherwise this will emit {@code null}.
     *
     * @return a LiveData that emits the MediaItemMetadatas for the current browsed id or {@code
     * null} if unavailable.
     * @see #setCurrentBrowseId(String)
     */
    public LiveData<List<MediaItemMetadata>> getBrowsedMediaItems() {
        return mCurrentMediaItems;
    }

}
