/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.ui;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.FOCUS_ACTION_TYPE;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_DEFAULT;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_FIRST;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.view.View;

import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.tests.unit.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link FocusArea}. */
public class FocusAreaTest {
    private static final long WAIT_TIME_MS = 3000;

    @Rule
    public ActivityTestRule<FocusAreaTestActivity> mActivityRule =
            new ActivityTestRule<>(FocusAreaTestActivity.class);

    private FocusAreaTestActivity mActivity;
    private TestFocusArea mFocusArea;
    private View mChild;
    private View mDefaultFocus;
    private View mNonChild;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mFocusArea = mActivity.findViewById(R.id.focus_area);
        mChild = mActivity.findViewById(R.id.child);
        mDefaultFocus = mActivity.findViewById(R.id.default_focus);
        mNonChild = mActivity.findViewById(R.id.non_child);
    }

    @Test
    public void testLoseFocus() throws Exception {
        mChild.post(() -> {
            mChild.requestFocus();
        });
        mFocusArea.setOnDrawCalled(false);
        mFocusArea.setDrawCalled(false);

        // FocusArea lost focus.
        CountDownLatch latch = new CountDownLatch(1);
        mNonChild.post(() -> {
            mNonChild.requestFocus();
            mNonChild.post(() -> {
                latch.countDown();
            });
        });
        assertDrawMethodsCalled(latch);
    }

    @Test
    public void testGetFocus() throws Exception {
        mNonChild.post(() -> {
            mNonChild.requestFocus();
        });
        mFocusArea.setOnDrawCalled(false);
        mFocusArea.setDrawCalled(false);

        // FocusArea got focus.
        CountDownLatch latch = new CountDownLatch(1);
        mChild.post(() -> {
            mChild.requestFocus();
            mChild.post(() -> {
                latch.countDown();
            });
        });
        assertDrawMethodsCalled(latch);
    }

    @Test
    public void testFocusOnDefaultFocus() throws Exception {
        assertThat(mDefaultFocus.isFocused()).isFalse();

        Bundle bundle = new Bundle();
        bundle.putInt(FOCUS_ACTION_TYPE, FOCUS_DEFAULT);
        CountDownLatch latch = new CountDownLatch(1);
        mFocusArea.post(() -> {
            mFocusArea.performAccessibilityAction(ACTION_FOCUS, bundle);
            latch.countDown();
        });
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mDefaultFocus.isFocused()).isTrue();
    }

    @Test
    public void testFocusOnFirstFocusable() throws Exception {
        assertThat(mChild.isFocused()).isFalse();

        Bundle bundle = new Bundle();
        bundle.putInt(FOCUS_ACTION_TYPE, FOCUS_FIRST);
        CountDownLatch latch = new CountDownLatch(1);
        mFocusArea.post(() -> {
            mFocusArea.performAccessibilityAction(ACTION_FOCUS, bundle);
            latch.countDown();
        });
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mChild.isFocused()).isTrue();
    }

    private void assertDrawMethodsCalled(CountDownLatch latch) throws Exception {
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mFocusArea.onDrawCalled()).isTrue();
        assertThat(mFocusArea.drawCalled()).isTrue();
    }
}
