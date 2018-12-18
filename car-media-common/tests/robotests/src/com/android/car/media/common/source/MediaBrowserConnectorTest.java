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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.InstantTaskExecutorRule;
import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.TestConfig;
import com.android.car.media.common.source.MediaBrowserConnector.ConnectionState;
import com.android.car.media.common.source.MediaBrowserConnector.MediaBrowserState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MediaBrowserConnectorTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public MediaBrowserCompat mMediaBrowser;

    private MediaBrowserConnector mLiveData;
    private MediaBrowserCompat.ConnectionCallback mConnectionCallback;

    @Before
    public void setUp() {
        mLiveData = new MediaBrowserConnector(application, new ComponentName("", "")) {
            @Override
            protected MediaBrowserCompat createMediaBrowser(@NonNull Context context,
                    @NonNull ComponentName browseService,
                    @NonNull MediaBrowserCompat.ConnectionCallback callback) {
                mConnectionCallback = callback;
                return mMediaBrowser;
            }
        };
    }

    @Test
    public void testConnectOnActive() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(false);

        mLiveData.observe(mLifecycleOwner, observer);

        verify(mMediaBrowser).connect();
        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.CONNECTING);
    }

    @Test
    public void testAlreadyConnectedOnActive() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(true);

        mLiveData.observe(mLifecycleOwner, observer);

        verify(mMediaBrowser, never()).connect();
        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mMediaBrowser).isSameAs(mMediaBrowser);
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.CONNECTED);
    }

    @Test
    public void testExceptionOnConnect() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(false);
        setConnectionAction(() -> {
            throw new IllegalStateException("expected");
        });

        mLiveData.observe(mLifecycleOwner, observer);

        verify(mMediaBrowser).connect();
        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.CONNECTING);
    }

    @Test
    public void testConnectionCallback_onConnected() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(false);
        setConnectionAction(() -> {
            observer.reset();
            mConnectionCallback.onConnected();
        });

        mLiveData.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mMediaBrowser).isSameAs(mMediaBrowser);
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.CONNECTED);
    }

    @Test
    public void testConnectionCallback_onConnectionFailed() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(false);
        setConnectionAction(() -> {
            observer.reset();
            mConnectionCallback.onConnectionFailed();
        });

        mLiveData.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.CONNECTION_FAILED);
    }

    @Test
    public void testConnectionCallback_onConnectionSuspended() {
        CaptureObserver<MediaBrowserState> observer = new CaptureObserver<>();
        when(mMediaBrowser.isConnected()).thenReturn(false);
        setConnectionAction(() -> {
            mConnectionCallback.onConnected();
            observer.reset();
            mConnectionCallback.onConnectionSuspended();
        });

        mLiveData.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        MediaBrowserState observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.mConnectionState).isEqualTo(ConnectionState.DISCONNECTED);
    }

    private void setConnectionAction(@NonNull Runnable action) {
        doAnswer(invocation -> {
            action.run();
            return null;
        }).when(mMediaBrowser).connect();
    }


}
