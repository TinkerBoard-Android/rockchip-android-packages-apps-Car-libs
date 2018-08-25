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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.TestConfig;
import com.android.car.media.common.playback.PlaybackStateAnnotations;

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
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ActiveMediaControllerLiveDataTest {

    private static final String TEST_PACKAGE_1 = "package1";
    private static final String TEST_PACKAGE_2 = "package2";
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
    @Captor
    public ArgumentCaptor<OnActiveSessionsChangedListener> mSessionChangeListenerCaptor;
    @Captor
    public ArgumentCaptor<MediaController.Callback> mFirstControllerCallbackCaptor;
    @Captor
    public ArgumentCaptor<MediaController.Callback> mSecondControllerCallbackCaptor;

    private List<MediaController> mMediaControllerList;
    private SharedPreferences mSharedPreferences;
    private ActiveMediaControllerLiveData mLiveData;


    @Before
    public void setUp() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                application);
        mSharedPreferences.edit().clear().apply();

        mMediaControllerList = new ArrayList<>();
        mMediaControllerList.add(mFirstMediaController);
        mMediaControllerList.add(mSecondMediaController);

        setControllerState(mFirstMediaController, PlaybackState.STATE_PLAYING);
        when(mFirstMediaController.getPackageName()).thenReturn(TEST_PACKAGE_1);
        doNothing().when(mFirstMediaController)
                .registerCallback(mFirstControllerCallbackCaptor.capture());
        setControllerState(mSecondMediaController, PlaybackState.STATE_PLAYING);
        when(mSecondMediaController.getPackageName()).thenReturn(TEST_PACKAGE_2);
        doNothing().when(mSecondMediaController)
                .registerCallback(mSecondControllerCallbackCaptor.capture());

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

        mLiveData = new ActiveMediaControllerLiveData(mMediaSessionManager, mSharedPreferences);
    }

    private void doOnActiveObservation(CaptureObserver<MediaController> observer) {
        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(mMediaControllerList);

        mLiveData.observe(mLifecycleOwner, observer);
    }

    private void doOnUpdateObservation(CaptureObserver<MediaController> observer) {
        mLiveData.observe(mLifecycleOwner, observer);
        observer.reset();
        mSessionChangeListenerCaptor.getValue().onActiveSessionsChanged(mMediaControllerList);
    }

    @Test
    public void testChooseFirstController_onActive() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();

        doOnActiveObservation(observer);

        assertObservedController(observer, mFirstMediaController);
    }

    @Test
    public void testChooseFirstController_onUpdate() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();

        doOnUpdateObservation(observer);

        assertObservedController(observer, mFirstMediaController);
    }

    @Test
    public void testPickPlayingController_onActive() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);

        doOnActiveObservation(observer);

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testPickPlayingController_onUpdate() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);

        doOnUpdateObservation(observer);

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testUsedLastWhenAllPaused_onActive() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);
        setControllerState(mSecondMediaController, PlaybackState.STATE_PAUSED);
        setLastObservedController(mSecondMediaController);

        doOnActiveObservation(observer);

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testUsedLastWhenAllPaused_onUpdate() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);
        setControllerState(mSecondMediaController, PlaybackState.STATE_PAUSED);
        setLastObservedController(mSecondMediaController);

        doOnUpdateObservation(observer);

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testPlaybackStateChangedTriggersUpdate() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        doOnActiveObservation(observer);
        observer.reset();
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);

        mFirstControllerCallbackCaptor.getValue()
                .onPlaybackStateChanged(mFirstMediaController.getPlaybackState());

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testSessionDestroyedTriggersUpdate() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        doOnActiveObservation(observer);
        observer.reset();
        setControllerState(mFirstMediaController, PlaybackState.STATE_NONE);

        mFirstControllerCallbackCaptor.getValue().onSessionDestroyed();

        assertObservedController(observer, mSecondMediaController);
    }

    @Test
    public void testGetControllerForPackage() {
        // Ensure LiveData is active
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        doOnActiveObservation(observer);

        assertThat(mLiveData.getControllerForPackage(TEST_PACKAGE_2)).isSameAs(
                mSecondMediaController);
    }

    @Test
    public void testGetControllerForPackage_noMatch() {
        // Ensure LiveData is active
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        doOnActiveObservation(observer);

        assertThat(mLiveData.getControllerForPackage("")).isNull();
    }

    @Test
    public void testUnregisterOnInactive() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        doOnActiveObservation(observer);

        // Need to hold reference to captor since it will be swapped out when listener is
        // unregistered.
        ArgumentCaptor<OnActiveSessionsChangedListener> oldArgumentCaptor =
                mSessionChangeListenerCaptor;
        mLifecycleOwner.markState(Lifecycle.State.DESTROYED);

        verify(mMediaSessionManager)
                .removeOnActiveSessionsChangedListener(oldArgumentCaptor.getValue());
    }

    private void setLastObservedController(@Nullable MediaController mediaController) {
        PlaybackState oldControllerState =
                mediaController == null ? null : mediaController.getPlaybackState();
        if (mediaController != null) {
            setControllerState(mediaController, PlaybackState.STATE_PLAYING);
        }

        MediaSessionManager mediaSessionManager = mock(MediaSessionManager.class);
        List<MediaController> mediaControllers =
                mediaController == null ? Collections.emptyList()
                        : Collections.singletonList(mediaController);
        when(mediaSessionManager.getActiveSessions(any())).thenReturn(mediaControllers);

        // Use another instance of ActiveMediaControllerLiveData (not the one under test) to
        // inject the desired value into SharedPreferences.
        ActiveMediaControllerLiveData injectorData =
                new ActiveMediaControllerLiveData(mediaSessionManager, mSharedPreferences);

        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        injectorData.observe(mLifecycleOwner, observer);
        injectorData.removeObserver(observer);

        if (mediaController != null) {
            when(mediaController.getPlaybackState()).thenReturn(oldControllerState);
        }
    }

    private void setControllerState(MediaController mediaController,
            @PlaybackStateAnnotations.State int state) {
        when(mediaController.getPlaybackState())
                .thenReturn(
                        new PlaybackState.Builder().setState(state, 0, 0).build());
    }

    private void assertObservedController(CaptureObserver<MediaController> observer,
            MediaController mediaController) {
        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(mediaController);
    }
}
