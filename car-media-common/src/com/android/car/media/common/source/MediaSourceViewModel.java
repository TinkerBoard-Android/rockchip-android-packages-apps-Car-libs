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

import static com.android.car.apps.common.util.CarAppsDebugUtils.idHash;
import static com.android.car.arch.common.LiveDataFunctions.dataOf;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.media.common.MediaConstants;

import java.util.Objects;

/**
 * Contains observable data needed for displaying playback and browse UI.
 * MediaSourceViewModel is a singleton tied to the application to provide a single source of truth.
 */
public class MediaSourceViewModel extends AndroidViewModel {
    private static final String TAG = "MediaSourceViewModel";

    private static MediaSourceViewModel sInstance;

    // Primary media source.
    private final MutableLiveData<MediaSource> mPrimaryMediaSource = dataOf(null);
    // Connected browser for the primary media source.
    private final MutableLiveData<MediaBrowserCompat> mConnectedMediaBrowser = dataOf(null);
    // Media controller for the connected browser.
    private final MutableLiveData<MediaControllerCompat> mMediaController = dataOf(null);

    /**
     * Factory for creating dependencies. Can be swapped out for testing.
     */
    @VisibleForTesting
    interface InputFactory {
        MediaBrowserConnector createMediaBrowserConnector(@NonNull Application application,
                @NonNull MediaBrowserConnector.Callback connectedBrowserCallback);

        MediaControllerCompat getControllerForSession(@Nullable MediaSessionCompat.Token session);

        MediaSource getSelectedSourceFromContentProvider();
    }

    /** Returns the MediaSourceViewModel singleton tied to the application. */
    public static MediaSourceViewModel get(@NonNull Application application) {
        if (sInstance == null) {
            sInstance = new MediaSourceViewModel(application);
        }
        return sInstance;
    }

    /**
     * Create a new instance of MediaSourceViewModel
     *
     * @see AndroidViewModel
     */
    private MediaSourceViewModel(@NonNull Application application) {
        this(application, new InputFactory() {
            @Override
            public MediaBrowserConnector createMediaBrowserConnector(
                    @NonNull Application application,
                    @NonNull MediaBrowserConnector.Callback connectedBrowserCallback) {
                return new MediaBrowserConnector(application, connectedBrowserCallback);
            }

            @Override
            public MediaControllerCompat getControllerForSession(
                    @Nullable MediaSessionCompat.Token token) {
                if (token == null) return null;
                try {
                    return new MediaControllerCompat(application, token);
                } catch (RemoteException e) {
                    Log.e(TAG, "Couldn't get MediaControllerCompat", e);
                    return null;
                }
            }

            @Override
            public MediaSource getSelectedSourceFromContentProvider() {
                Cursor cursor = application.getContentResolver().query(
                        MediaConstants.URI_MEDIA_SOURCE, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        return new MediaSource(application, cursor.getString(0));
                    }
                    return null;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    private final InputFactory mInputFactory;
    private final MediaBrowserConnector mBrowserConnector;
    private final MediaBrowserConnector.Callback mConnectedBrowserCallback;

    @VisibleForTesting
    MediaSourceViewModel(@NonNull Application application, @NonNull InputFactory inputFactory) {
        super(application);

        mInputFactory = inputFactory;

        mConnectedBrowserCallback = browser -> {
            mConnectedMediaBrowser.setValue(browser);
            if (browser != null) {
                if (!browser.isConnected()) {
                    Log.e(TAG, "Browser is NOT connected !! "
                            + mPrimaryMediaSource.getValue().getPackageName() + idHash(browser));
                    mMediaController.setValue(null);
                } else {
                    mMediaController.setValue(mInputFactory.getControllerForSession(
                            browser.getSessionToken()));
                }
            } else {
                mMediaController.setValue(null);
            }
        };
        mBrowserConnector = inputFactory.createMediaBrowserConnector(application,
                mConnectedBrowserCallback);

        updateModelState();
        application.getContentResolver().registerContentObserver(MediaConstants.URI_MEDIA_SOURCE,
                false, mMediaSourceObserver);

    }

    private final ContentObserver mMediaSourceObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateModelState();
        }
    };

    @VisibleForTesting
    ContentObserver getMediaSourceObserver() {
        return mMediaSourceObserver;
    }

    @VisibleForTesting
    MediaBrowserConnector.Callback getConnectedBrowserCallback() {
        return mConnectedBrowserCallback;
    }

    /**
     * Returns a LiveData that emits the MediaSource that is to be browsed or displayed.
     */
    public LiveData<MediaSource> getPrimaryMediaSource() {
        return mPrimaryMediaSource;
    }

    /**
     * Updates the primary media source, and notifies content provider of new source
     */
    public void setPrimaryMediaSource(MediaSource mediaSource) {
        ContentValues values = new ContentValues();
        values.put(MediaConstants.KEY_PACKAGE_NAME, mediaSource.getPackageName());
        getApplication().getContentResolver().update(
                MediaConstants.URI_MEDIA_SOURCE, values, null, null);
    }

    /**
     * Returns a LiveData that emits the currently connected MediaBrowser. Emits {@code null} if no
     * MediaSource is set, if the MediaSource does not support browsing, or if the MediaBrowser is
     * not connected.
     */
    public LiveData<MediaBrowserCompat> getConnectedMediaBrowser() {
        return mConnectedMediaBrowser;
    }

    /**
     * Returns a LiveData that emits a {@link MediaController} that allows controlling this media
     * source, or emits {@code null} if the media source doesn't support browsing or the browser is
     * not connected.
     */
    public LiveData<MediaControllerCompat> getMediaController() {
        return mMediaController;
    }

    private void updateModelState() {
        MediaSource oldMediaSource = mPrimaryMediaSource.getValue();
        MediaSource newMediaSource = mInputFactory.getSelectedSourceFromContentProvider();
        if (Objects.equals(oldMediaSource, newMediaSource)) {
            return;
        }

        // Reset dependent values to avoid propagating inconsistencies.
        mMediaController.setValue(null);
        mConnectedMediaBrowser.setValue(null);
        mBrowserConnector.connectTo(null);

        // Broadcast the new source
        mPrimaryMediaSource.setValue(newMediaSource);

        // Recompute dependent values
        if (newMediaSource == null) {
            return;
        }

        ComponentName browseService = newMediaSource.getBrowseServiceComponentName();
        if (browseService == null) {
            Log.e(TAG, "No browseService for source: " + newMediaSource.getPackageName());
        }
        mBrowserConnector.connectTo(browseService);
    }
}
