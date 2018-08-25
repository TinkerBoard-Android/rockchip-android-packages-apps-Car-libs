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

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A LiveData that emits a MediaBrowserState for the current connection status. Attempts to maintain
 * a connection while active.
 */

class MediaBrowserConnector extends LiveData<MediaBrowserConnector.MediaBrowserState> {

    /**
     * Contains the {@link MediaBrowser} for a {@link MediaBrowserConnector} and its associated
     * connection status.
     */
    public static class MediaBrowserState {
        public final MediaBrowser mMediaBrowser;

        @ConnectionState
        public final int mConnectionState;

        MediaBrowserState(MediaBrowser mediaBrowser, @ConnectionState int connectionState) {
            mMediaBrowser = mediaBrowser;
            mConnectionState = connectionState;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {ConnectionState.DISCONNECTED, ConnectionState.CONNECTING,
            ConnectionState.CONNECTED, ConnectionState.CONNECTION_FAILED})
    public @interface ConnectionState {
        int DISCONNECTED = 0;
        int CONNECTING = 1;
        int CONNECTED = 2;
        int CONNECTION_FAILED = 3;
    }

    private final MediaBrowser mBrowser;

    /**
     * Create a new MediaBrowserConnector for the specified component.
     *
     * @param context       The Context with which to build the MediaBrowser.
     * @param browseService The ComponentName of the media browser service.
     * @see MediaBrowser#MediaBrowser(Context, ComponentName, MediaBrowser.ConnectionCallback,
     * android.os.Bundle)
     */
    MediaBrowserConnector(@NonNull Context context,
            @NonNull ComponentName browseService) {
        mBrowser = createMediaBrowser(context, browseService,
                new MediaBrowser.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        setValue(new MediaBrowserState(mBrowser, ConnectionState.CONNECTED));
                    }

                    @Override
                    public void onConnectionFailed() {
                        setValue(
                                new MediaBrowserState(mBrowser, ConnectionState.CONNECTION_FAILED));
                    }

                    @Override
                    public void onConnectionSuspended() {
                        setValue(new MediaBrowserState(mBrowser, ConnectionState.DISCONNECTED));
                    }
                });
    }

    /**
     * Instantiate the MediaBrowser this MediaBrowserConnector will connect with.
     */
    @VisibleForTesting()
    protected MediaBrowser createMediaBrowser(@NonNull Context context,
            @NonNull ComponentName browseService,
            @NonNull MediaBrowser.ConnectionCallback callback) {
        return new MediaBrowser(context, browseService, callback, null);
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (mBrowser.isConnected()) {
            setValue(new MediaBrowserState(mBrowser, ConnectionState.CONNECTED));
        } else {
            try {
                connect();
            } catch (IllegalStateException ex) {
                // Ignore: MediaBrowse could be in an intermediate state (not connected, but not
                // disconnected either.). In this situation, trying to connect again can throw
                // this exception, but there is no way to know without trying.
            }
        }
    }

    private void connect() {
        setValue(new MediaBrowserState(mBrowser, ConnectionState.CONNECTING));
        mBrowser.connect();
    }

    @Override
    protected void onInactive() {
        // TODO(b/77640010): Review MediaBrowse disconnection.
        // Some media sources are not responding correctly to MediaBrowser#disconnect(). We
        // are keeping the connection going.
        //   mBrowser.disconnect();
    }
}
