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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.InstantTaskExecutorRule;
import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.TestConfig;
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

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MediaSourceViewModelTest {

    private static final String BROWSER_CONTROLLER_PACKAGE_NAME = "browser";
    private static final String SESSION_MANAGER_CONTROLLER_PACKAGE_NAME = "mediaSessionManager";
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public MediaSource mMediaSource;
    @Mock
    public MediaBrowserCompat mMediaBrowser;
    @Mock
    public ActiveMediaSelector mActiveMediaSelector;
    @Mock
    public MediaControllerCompat mMediaControllerFromBrowser;
    @Mock
    public MediaControllerCompat mMediaControllerFromSessionManager;

    private final MutableLiveData<MediaBrowserState> mMediaBrowserState = new MutableLiveData<>();
    private final MutableLiveData<List<MediaControllerCompat>> mActiveMediaControllers =
            new MutableLiveData<>();

    private MediaSourceViewModel mViewModel;

    private ComponentName mRequestedBrowseService;

    @Before
    public void setUp() {
        when(mMediaControllerFromBrowser.getPackageName())
                .thenReturn(BROWSER_CONTROLLER_PACKAGE_NAME);
        when(mMediaControllerFromSessionManager.getPackageName())
                .thenReturn(SESSION_MANAGER_CONTROLLER_PACKAGE_NAME);

        mActiveMediaControllers.setValue(
                Collections.singletonList(mMediaControllerFromSessionManager));
        when(mActiveMediaSelector.getTopMostMediaController(any(), any()))
                .thenReturn(mMediaControllerFromSessionManager);

        mRequestedBrowseService = null;

        mViewModel = new MediaSourceViewModel(application, new MediaSourceViewModel.InputFactory() {

            @Override
            public LiveData<MediaBrowserState> createMediaBrowserConnector(
                    @NonNull ComponentName browseService) {
                mRequestedBrowseService = browseService;
                return mMediaBrowserState;
            }

            @Override
            public ActiveMediaSelector createActiveMediaSelector() {
                return mActiveMediaSelector;
            }

            @Override
            public LiveData<List<MediaControllerCompat>> createActiveMediaControllerData() {
                return mActiveMediaControllers;
            }

            @Override
            public MediaControllerCompat getControllerForSession(
                    @Nullable MediaSessionCompat.Token token) {
                return mMediaControllerFromBrowser;
            }

            @Override
            public MediaSource getSelectedSourceFromContentProvider() {
                return mMediaSource;
            }
        });
    }

    @Test
    public void testGetSelectedMediaSource() {
        CaptureObserver<MediaSource> observer = new CaptureObserver<>();

        mViewModel.getPrimaryMediaSource().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaSource);
    }

    @Test
    public void testGetMediaController_fromSessionManager() {
        CaptureObserver<MediaControllerCompat> observer = new CaptureObserver<>();
        when(mActiveMediaSelector.getControllerForSource(any(), any()))
                .thenReturn(mMediaControllerFromSessionManager);

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaControllerFromSessionManager);
        assertThat(mRequestedBrowseService).isNull();
    }

    @Test
    public void testGetMediaController_noActiveSession() {
        CaptureObserver<MediaControllerCompat> observer = new CaptureObserver<>();
        mMediaControllerFromSessionManager = null;
        ComponentName testComponent = new ComponentName("test", "test");
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(testComponent);
        mMediaBrowserState.setValue(new MediaBrowserState(mMediaBrowser,
                MediaBrowserConnector.ConnectionState.CONNECTED));

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaControllerFromBrowser);
        assertThat(mRequestedBrowseService).isEqualTo(testComponent);
    }

    @Test
    public void testGetMediaController_noActiveSession_noBrowseService() {
        CaptureObserver<MediaControllerCompat> observer = new CaptureObserver<>();
        mMediaControllerFromSessionManager = null;
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(null);
        mMediaBrowserState.setValue(new MediaBrowserState(mMediaBrowser,
                MediaBrowserConnector.ConnectionState.CONNECTED));

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        assertThat(mRequestedBrowseService).isEqualTo(null);
    }

    @Test
    public void testGetMediaController_noActiveSession_notConnected() {
        CaptureObserver<MediaControllerCompat> observer = new CaptureObserver<>();
        mMediaControllerFromSessionManager = null;
        ComponentName testComponent = new ComponentName("test", "test");
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(testComponent);
        mMediaBrowserState.setValue(new MediaBrowserState(mMediaBrowser,
                MediaBrowserConnector.ConnectionState.CONNECTING));

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        assertThat(mRequestedBrowseService).isEqualTo(testComponent);
    }

    @Test
    public void testGetActiveMediaController() {
        when(mActiveMediaSelector.getTopMostMediaController(any(), any()))
                .thenReturn(mMediaControllerFromBrowser);
        CaptureObserver<MediaControllerCompat> observer = new CaptureObserver<>();

        mViewModel.getTopActiveMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaControllerFromBrowser);
    }

    @Test
    public void testIsCurrentMediaSourcePlaying() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        ComponentName testComponent = new ComponentName("test", "test");
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(testComponent);
        when(mMediaSource.getPackageName()).thenReturn(BROWSER_CONTROLLER_PACKAGE_NAME);
        when(mActiveMediaSelector.getTopMostMediaController(any(), any()))
                .thenReturn(mMediaControllerFromBrowser);

        mViewModel.isCurrentMediaSourcePlaying().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isTrue();

        // make
        when(mActiveMediaSelector.getTopMostMediaController(any(), any()))
                .thenReturn(mMediaControllerFromSessionManager);
        mActiveMediaControllers.setValue(mActiveMediaControllers.getValue());

        assertThat(observer.getObservedValue()).isFalse();
    }
}
