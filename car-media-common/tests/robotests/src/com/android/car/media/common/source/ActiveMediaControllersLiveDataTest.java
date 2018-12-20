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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.lifecycle.Lifecycle;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.InstantTaskExecutorRule;
import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.TestConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ActiveMediaControllersLiveDataTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public MediaSessionManager mMediaSessionManager;
    @Mock
    public MediaController mFirstMediaController;
    @Mock
    public MediaController mSecondMediaController;
    @Mock
    public MediaControllerCompat mFirstMediaControllerCompat;
    @Mock
    public MediaControllerCompat mSecondMediaControllerCompat;
    @Captor
    public ArgumentCaptor<OnActiveSessionsChangedListener> mSessionChangeListenerCaptor;
    @Captor
    public ArgumentCaptor<MediaControllerCompat.Callback> mFirstControllerCallbackCaptor;

    private List<MediaController> mMediaControllerList;

    private ActiveMediaControllersLiveData mLiveData;

    @Before
    public void setUp() {
        mMediaControllerList = new ArrayList<>();
        mMediaControllerList.add(mFirstMediaController);
        mMediaControllerList.add(mSecondMediaController);

        doNothing().when(mFirstMediaControllerCompat)
                .registerCallback(mFirstControllerCallbackCaptor.capture());
        doNothing().when(mMediaSessionManager).addOnActiveSessionsChangedListener(
                mSessionChangeListenerCaptor.capture(), any());
        doAnswer(invocation -> {
            OnActiveSessionsChangedListener argument = invocation.getArgument(0);
            if (argument == mSessionChangeListenerCaptor.getValue()) {
                mSessionChangeListenerCaptor =
                        ArgumentCaptor.forClass(OnActiveSessionsChangedListener.class);
            }
            return null;
        }).when(mMediaSessionManager).removeOnActiveSessionsChangedListener(any());


        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(mMediaControllerList);

        mLiveData = new ActiveMediaControllersLiveData(mMediaSessionManager) {
            @Override
            protected MediaControllerCompat fromMediaController(MediaController mediaController) {
                if (mediaController == mFirstMediaController) {
                    return mFirstMediaControllerCompat;
                } else if (mediaController == mSecondMediaController) {
                    return mSecondMediaControllerCompat;
                }
                return null;
            }
        };
    }

    @Test
    public void testFetchOnActive() {
        CaptureObserver<List<MediaControllerCompat>> observer = new CaptureObserver<>();

        mLiveData.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        List<MediaControllerCompat> mediaControllerCompatList = new ArrayList<>();
        mediaControllerCompatList.add(mFirstMediaControllerCompat);
        mediaControllerCompatList.add(mSecondMediaControllerCompat);
        assertThat(observer.getObservedValue()).isEqualTo(mediaControllerCompatList);
    }


    @Test
    public void testPlaybackStateChangedTriggersUpdate() {
        CaptureObserver<List<MediaControllerCompat>> observer = new CaptureObserver<>();
        mLiveData.observe(mLifecycleOwner, observer);
        observer.reset();

        mFirstControllerCallbackCaptor.getValue()
                .onPlaybackStateChanged(mFirstMediaControllerCompat.getPlaybackState());

        assertThat(observer.hasBeenNotified()).isTrue();
    }

    @Test
    public void testSessionDestroyedTriggersUpdate() {
        CaptureObserver<List<MediaControllerCompat>> observer = new CaptureObserver<>();
        mLiveData.observe(mLifecycleOwner, observer);
        observer.reset();

        mFirstControllerCallbackCaptor.getValue().onSessionDestroyed();

        assertThat(observer.hasBeenNotified()).isTrue();
    }

    @Test
    public void testUnregisterOnInactive() {
        CaptureObserver<List<MediaControllerCompat>> observer = new CaptureObserver<>();
        mLiveData.observe(mLifecycleOwner, observer);
        observer.reset();

        // Need to hold reference to captor since it will be swapped out when listener is
        // unregistered.
        ArgumentCaptor<OnActiveSessionsChangedListener> oldArgumentCaptor =
                mSessionChangeListenerCaptor;
        mLifecycleOwner.markState(Lifecycle.State.DESTROYED);

        verify(mMediaSessionManager)
                .removeOnActiveSessionsChangedListener(oldArgumentCaptor.getValue());
    }
}
