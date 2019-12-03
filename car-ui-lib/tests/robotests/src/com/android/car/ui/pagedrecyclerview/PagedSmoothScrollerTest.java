/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.ui.pagedrecyclerview;

import static androidx.recyclerview.widget.LinearSmoothScroller.SNAP_TO_START;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.car.ui.CarUiRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarUiRobolectricTestRunner.class)
public class PagedSmoothScrollerTest {

    private Context mContext;
    private PagedSmoothScroller mPagedSmoothScroller;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPagedSmoothScroller = new PagedSmoothScroller(mContext);
    }

    @Test
    public void calculateTimeForScrolling_shouldInitializeAllValues() {
        assertThat(mPagedSmoothScroller.mMillisecondsPerInch).isNotEqualTo(0);
        assertThat(mPagedSmoothScroller.mDecelerationTimeDivisor).isNotEqualTo(0);
        assertThat(mPagedSmoothScroller.mMillisecondsPerPixel).isNotEqualTo(0);
        assertThat(mPagedSmoothScroller.mInterpolator).isNotNull();
        assertThat(mPagedSmoothScroller.mDensityDpi).isNotEqualTo(0);
    }

    @Test
    public void getVerticalSnapPreference_shouldReturnSnapToStart() {
        assertThat(mPagedSmoothScroller.getVerticalSnapPreference()).isEqualTo(SNAP_TO_START);
    }

    @Test
    public void calculateTimeForScrolling_shouldReturnMultiplierOfMillisecondsPerPixel() {
        assertThat(mPagedSmoothScroller.calculateTimeForScrolling(20)).isEqualTo(
                (int) Math.ceil(Math.abs(20) * mPagedSmoothScroller.mMillisecondsPerPixel));
    }

    @Test
    public void calculateTimeForDeceleration_shouldReturnNotBeZero() {
        assertThat(mPagedSmoothScroller.calculateTimeForDeceleration(20)).isNotEqualTo(0);
    }
}
