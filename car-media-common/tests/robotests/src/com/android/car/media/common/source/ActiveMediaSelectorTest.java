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

import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.TestConfig;
import com.android.car.media.common.playback.PlaybackStateAnnotations;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class ActiveMediaSelectorTest {

    private static final String TEST_PACKAGE_1 = "package1";
    private static final String TEST_PACKAGE_2 = "package2";
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public MediaController mFirstMediaController;
    @Mock
    public MediaController mSecondMediaController;

    private List<MediaController> mMediaControllerList;
    private SharedPreferences mSharedPreferences;
    private ActiveMediaSelector mSelector;


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
        setControllerState(mSecondMediaController, PlaybackState.STATE_PLAYING);
        when(mSecondMediaController.getPackageName()).thenReturn(TEST_PACKAGE_2);


        mSelector = new ActiveMediaSelector(mSharedPreferences);
    }

    @Test
    public void testChooseFirstController() {
        assertThat(mSelector.getTopMostMediaController(mMediaControllerList, null))
                .isSameAs(mFirstMediaController);
    }

    @Test
    public void testPickPlayingController() {
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);

        assertThat(mSelector.getTopMostMediaController(mMediaControllerList, null))
                .isSameAs(mSecondMediaController);
    }

    @Test
    public void testUsedLastWhenAllPaused() {
        setControllerState(mFirstMediaController, PlaybackState.STATE_PAUSED);
        setControllerState(mSecondMediaController, PlaybackState.STATE_PAUSED);
        setLastObservedController(mSecondMediaController);

        assertThat(mSelector.getTopMostMediaController(mMediaControllerList, null))
                .isSameAs(mSecondMediaController);
    }

    @Test
    public void testGetControllerForPackage() {
        assertThat(mSelector.getControllerForPackage(mMediaControllerList, TEST_PACKAGE_2))
                .isSameAs(mSecondMediaController);
    }

    @Test
    public void testGetControllerForPackage_noMatch() {
        assertThat(mSelector.getControllerForPackage(mMediaControllerList, "")).isNull();
    }

    private void setLastObservedController(@Nullable MediaController mediaController) {
        List<MediaController> mediaControllers =
                mediaController == null ? Collections.emptyList()
                        : Collections.singletonList(mediaController);

        // Use another instance of ActiveMediaControllerLiveData (not the one under test) to
        // inject the desired value into SharedPreferences.
        ActiveMediaSelector injectorData =
                new ActiveMediaSelector(mSharedPreferences);

        injectorData.getTopMostMediaController(mediaControllers, null);
    }

    private void setControllerState(MediaController mediaController,
            @PlaybackStateAnnotations.State int state) {
        when(mediaController.getPlaybackState())
                .thenReturn(
                        new PlaybackState.Builder().setState(state, 0, 0).build());
    }
}
