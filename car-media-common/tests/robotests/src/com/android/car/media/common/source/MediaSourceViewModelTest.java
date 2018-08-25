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

import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.testing.CaptureObserver;
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

    private static final String SESSION_CONTROLLER_PACKAGE_NAME = "session";
    private static final String PACKAGE_CONTROLLER_PACKAGE_NAME = "package";
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public SimpleMediaSource mMediaSource;
    @Mock
    public MediaBrowser mMediaBrowser;
    @Mock
    public MediaController mMediaControllerForPackage;
    @Mock
    public MediaController mMediaControllerForSession;

    private final MutableLiveData<List<SimpleMediaSource>> mMediaSources = new MutableLiveData<>();
    private final MutableLiveData<MediaBrowserState> mMediaBrowserState = new MutableLiveData<>();
    private final MutableLiveData<MediaController> mActiveMediaController = new MutableLiveData<>();

    private MediaSourceViewModel mViewModel;

    private ComponentName mRequestedBrowseService;

    @Before
    public void setUp() {
        when(mMediaControllerForPackage.getPackageName()).thenReturn(
                PACKAGE_CONTROLLER_PACKAGE_NAME);
        when(mMediaControllerForSession.getPackageName()).thenReturn(
                SESSION_CONTROLLER_PACKAGE_NAME);
        mRequestedBrowseService = null;
        mViewModel = new MediaSourceViewModel(application, new MediaSourceViewModel.InputFactory() {
            @Override
            public LiveData<List<SimpleMediaSource>> createMediaSources() {
                return mMediaSources;
            }

            @Override
            public LiveData<MediaBrowserState> createMediaBrowserConnector(
                    @NonNull ComponentName browseService) {
                mRequestedBrowseService = browseService;
                return mMediaBrowserState;
            }

            @Override
            public LiveData<MediaController> createActiveMediaController() {
                return mActiveMediaController;
            }

            @Override
            public MediaController getControllerForPackage(String packageName) {
                return mMediaControllerForPackage;
            }

            @Override
            public MediaController getControllerForSession(@Nullable MediaSession.Token token) {
                return mMediaControllerForSession;
            }
        });
        mViewModel.setSelectedMediaSource(mMediaSource);
    }


    @Test
    public void testGetMediaSources() {
        assertThat(mViewModel.getMediaSources()).isSameAs(mMediaSources);
    }

    @Test
    public void testHasMediaSources() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        mMediaSources.setValue(Collections.singletonList(mMediaSource));

        mViewModel.hasMediaSources().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isTrue();
        observer.reset();

        mMediaSources.setValue(Collections.emptyList());

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isFalse();
        observer.reset();

        mMediaSources.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isFalse();
    }

    @Test
    public void testGetSelectedMediaSource() {
        CaptureObserver<SimpleMediaSource> observer = new CaptureObserver<>();

        mViewModel.getSelectedMediaSource().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaSource);
    }

    @Test
    public void testGetMediaController_fromActiveSession() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaControllerForPackage);
        assertThat(mRequestedBrowseService).isNull();
    }

    @Test
    public void testGetMediaController_noActiveSession() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        mMediaControllerForPackage = null;
        ComponentName testComponent = new ComponentName("test", "test");
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(testComponent);
        mMediaBrowserState.setValue(new MediaBrowserState(mMediaBrowser,
                MediaBrowserConnector.ConnectionState.CONNECTED));

        mViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isSameAs(mMediaControllerForSession);
        assertThat(mRequestedBrowseService).isEqualTo(testComponent);
    }

    @Test
    public void testGetMediaController_noActiveSession_noBrowseService() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        mMediaControllerForPackage = null;
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
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        mMediaControllerForPackage = null;
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
        assertThat(mViewModel.getActiveMediaController()).isSameAs(mActiveMediaController);
    }

    @Test
    public void testIsCurrentMediaSourcePlaying() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        ComponentName testComponent = new ComponentName("test", "test");
        when(mMediaSource.getBrowseServiceComponentName()).thenReturn(testComponent);
        when(mMediaSource.getPackageName()).thenReturn(SESSION_CONTROLLER_PACKAGE_NAME);
        mMediaBrowserState.setValue(new MediaBrowserState(mMediaBrowser,
                MediaBrowserConnector.ConnectionState.CONNECTED));
        mActiveMediaController.setValue(mMediaControllerForSession);

        mViewModel.isCurrentMediaSourcePlaying().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isTrue();

        mActiveMediaController.setValue(mMediaControllerForPackage);

        assertThat(observer.getObservedValue()).isFalse();
    }
}
