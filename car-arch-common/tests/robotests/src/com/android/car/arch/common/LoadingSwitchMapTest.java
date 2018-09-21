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

package com.android.car.arch.common;

import static com.google.common.truth.Truth.assertThat;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LoadingSwitchMapTest {

    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    private MutableLiveData<Object> mTrigger;
    private MutableLiveData<Integer> mOutput;
    private CaptureObserver<Boolean> mLoadingObserver;
    private CaptureObserver<Integer> mOutputObserver;

    @Before
    public void setUp() {
        mTrigger = new MutableLiveData<>();
        mOutput = new MutableLiveData<>();
        mLoadingObserver = new CaptureObserver<>();
        mOutputObserver = new CaptureObserver<>();
    }

    @Test
    public void testIsLoading_uninitialized() {
        LoadingSwitchMap<Integer> underTest = LoadingSwitchMap.loadingSwitchMap(mTrigger,
                (data) -> mOutput);
        underTest.isLoading().observe(mLifecycleOwner, mLoadingObserver);
        underTest.getOutput().observe(mLifecycleOwner, mOutputObserver);

        assertThat(mLoadingObserver.hasBeenNotified()).isFalse();
        assertThat(mOutputObserver.hasBeenNotified()).isFalse();
    }

    @Test
    public void testIsLoading_initializedTrigger() {
        mTrigger.setValue(new Object());

        LoadingSwitchMap<Integer> underTest = LoadingSwitchMap.loadingSwitchMap(mTrigger,
                (data) -> mOutput);
        underTest.isLoading().observe(mLifecycleOwner, mLoadingObserver);
        underTest.getOutput().observe(mLifecycleOwner, mOutputObserver);

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isTrue();
        assertThat(mOutputObserver.hasBeenNotified()).isFalse();
    }

    @Test
    public void testIsLoading_alreadyLoaded() {
        mTrigger.setValue(new Object());
        mOutput.setValue(1);

        LoadingSwitchMap<Integer> underTest = LoadingSwitchMap.loadingSwitchMap(mTrigger,
                (data) -> mOutput);
        underTest.isLoading().observe(mLifecycleOwner, mLoadingObserver);
        underTest.getOutput().observe(mLifecycleOwner, mOutputObserver);

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isFalse();
        assertThat(mOutputObserver.hasBeenNotified()).isTrue();
        assertThat(mOutputObserver.getObservedValue()).isEqualTo(1);
    }

    @Test
    public void testIsLoading_normalFlow() {
        LoadingSwitchMap<Integer> underTest = LoadingSwitchMap.loadingSwitchMap(mTrigger,
                (data) -> mOutput);
        underTest.isLoading().observe(mLifecycleOwner, mLoadingObserver);
        underTest.getOutput().observe(mLifecycleOwner, mOutputObserver);

        mTrigger.setValue(new Object());

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isTrue();
        assertThat(mOutputObserver.hasBeenNotified()).isFalse();

        mOutput.setValue(1);

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isFalse();
        assertThat(mOutputObserver.hasBeenNotified()).isTrue();
        assertThat(mOutputObserver.getObservedValue()).isEqualTo(1);
    }

    @Test
    public void testIsLoading_secondLoad() {
        mTrigger.setValue(new Object());
        mOutput.setValue(1);

        LoadingSwitchMap<Integer> underTest = LoadingSwitchMap.loadingSwitchMap(mTrigger,
                (data) -> mOutput);
        underTest.isLoading().observe(mLifecycleOwner, mLoadingObserver);
        underTest.getOutput().observe(mLifecycleOwner, mOutputObserver);

        mTrigger.setValue(new Object());

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isTrue();
        assertThat(mOutputObserver.hasBeenNotified()).isTrue();
        assertThat(mOutputObserver.getObservedValue()).isEqualTo(1);

        mOutput.setValue(2);

        assertThat(mLoadingObserver.hasBeenNotified()).isTrue();
        assertThat(mLoadingObserver.getObservedValue()).isFalse();
        assertThat(mOutputObserver.hasBeenNotified()).isTrue();
        assertThat(mOutputObserver.getObservedValue()).isEqualTo(2);
    }
}
