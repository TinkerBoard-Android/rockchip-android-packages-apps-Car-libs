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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UiThread;
import android.support.v4.media.MediaBrowserCompat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.media.common.MediaItemMetadata;
import com.android.car.media.common.source.MediaSourceViewModel;

import java.util.List;

/**
 * Contains observable data needed for displaying playback and browse UI. Instances can be obtained
 * via {@link MediaBrowserViewModel.Factory}
 */
public interface MediaBrowserViewModel {

    /**
     * Possible states of the application UI
     */
    enum BrowseState {
        /** There is no content to show */
        EMPTY,
        /** We are still in the process of obtaining data */
        LOADING,
        /** Data has been loaded */
        LOADED,
        /** The content can't be shown due an error */
        ERROR
    }

    /**
     * Returns a LiveData that emits the current {@link BrowseState}
     */
    LiveData<BrowseState> getBrowseState();

    /**
     * Returns a LiveData that emits {@code true} when loading new items
     */
    LiveData<Boolean> isLoading();

    /**
     * Fetches the MediaItemMetadatas for the current browsed id. A MediaSource must be selected and
     * its MediaBrowser connected, otherwise this will emit {@code null}.
     *
     * @return a LiveData that emits the MediaItemMetadatas for the current browsed id or {@code
     * null} if unavailable.
     * @see WithMutableBrowseId#setCurrentBrowseId(String)
     */
    LiveData<List<MediaItemMetadata>> getBrowsedMediaItems();

    /**
     * A {@link MediaBrowserViewModel} whose selected browse ID may be changed.
     */
    interface WithMutableBrowseId extends MediaBrowserViewModel {

        /**
         * Set the current item to be browsed. If available, the list of items will be emitted by
         * {@link #getBrowsedMediaItems()}.
         */
        @UiThread
        void setCurrentBrowseId(@Nullable String browseId);

        /**
         * Set the current item to be searched for. If available, the list of items will be emitted
         * by {@link #getBrowsedMediaItems()}.
         */
        @UiThread
        void search(@Nullable String query);
    }

    /**
     * Creates and/or fetches {@link MediaBrowserViewModel} instances.
     */
    class Factory {

        private static final String KEY_BROWSER_ROOT =
                "com.android.car.media.common.browse.MediaBrowserViewModel.Factory.browserRoot";

        /**
         * Returns an initialized {@link MediaBrowserViewModel.WithMutableBrowseId}, and fetches a
         * {@link MediaSourceViewModel} from {@code viewModelProvider} to provide the connected
         * media browser.
         */
        @NonNull
        public static MediaBrowserViewModel.WithMutableBrowseId getInstance(
                @NonNull ViewModelProvider viewModelProvider) {
            MediaBrowserViewModelImpl viewModel = viewModelProvider.get(
                    MediaBrowserViewModelImpl.class);
            initMediaBrowser(fetchConnectedMediaBrowser(viewModelProvider), viewModel);
            return viewModel;
        }

        /**
         * Fetch an initialized {@link MediaBrowserViewModel.WithMutableBrowseId}. It will get its
         * media browser from the {@link MediaSourceViewModel} provided by {@code
         * viewModelProvider}.
         *
         * @param viewModelProvider the ViewModelProvider to load ViewModels from.
         * @param key               a key to decide which instance of the ViewModel to fetch.
         *                          Subsequent calls with the same key will return the same
         *                          instance.
         * @return an initialized MediaBrowserViewModel.WithMutableBrowseId for the given key.
         * @see ViewModelProvider#get(String, Class)
         */
        @NonNull
        public static MediaBrowserViewModel.WithMutableBrowseId getInstanceForKey(
                @NonNull ViewModelProvider viewModelProvider,
                @NonNull String key) {
            MediaBrowserViewModelImpl viewModel = viewModelProvider.get(key,
                    MediaBrowserViewModelImpl.class);
            initMediaBrowser(fetchConnectedMediaBrowser(viewModelProvider), viewModel);
            return viewModel;
        }

        /**
         * Fetch an initialized {@link MediaBrowserViewModel}. It will get its media browser from
         * the {@link MediaSourceViewModel} provided by {@code viewModelProvider}. It will already
         * be configured to browse {@code browseId}.
         *
         * @param viewModelProvider the ViewModelProvider to load ViewModels from.
         * @param browseId          the browseId to browse. This will also serve as the key for
         *                          fetching the ViewModel.
         * @return an initialized MediaBrowserViewModel configured to browse the specified browseId.
         */
        @NonNull
        public static MediaBrowserViewModel getInstanceForBrowseId(
                @NonNull ViewModelProvider viewModelProvider,
                @NonNull String browseId) {
            MediaBrowserViewModel.WithMutableBrowseId viewModel =
                    getInstanceForKey(viewModelProvider, browseId);
            viewModel.setCurrentBrowseId(browseId);
            return viewModel;
        }

        /**
         * Fetch an initialized {@link MediaBrowserViewModel}. It will get its media browser from
         * the {@link MediaSourceViewModel} provided by {@code viewModelProvider}. It will already
         * be configured to browse the root of the browser.
         *
         * @param viewModelProvider the ViewModelProvider to load ViewModels from.
         * @return an initialized MediaBrowserViewModel configured to browse the specified browseId.
         */
        @NonNull
        public static MediaBrowserViewModel getInstanceForBrowseRoot(
                @NonNull ViewModelProvider viewModelProvider) {
            MediaBrowserViewModel.WithMutableBrowseId viewModel =
                    getInstanceForKey(viewModelProvider, KEY_BROWSER_ROOT);
            viewModel.setCurrentBrowseId(null);
            return viewModel;
        }

        private static void initMediaBrowser(
                @NonNull LiveData<MediaBrowserCompat> connectedMediaBrowser,
                MediaBrowserViewModelImpl viewModel) {
            if (viewModel.getMediaBrowserSource() != connectedMediaBrowser) {
                viewModel.setConnectedMediaBrowser(connectedMediaBrowser);
            }
        }

        private static LiveData<MediaBrowserCompat> fetchConnectedMediaBrowser(
                @NonNull ViewModelProvider viewModelProvider) {
            return viewModelProvider.get(MediaSourceViewModel.class).getConnectedMediaBrowser();
        }
    }
}
