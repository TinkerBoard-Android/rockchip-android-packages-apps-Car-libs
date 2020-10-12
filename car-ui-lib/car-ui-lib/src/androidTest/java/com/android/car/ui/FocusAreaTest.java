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

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_BOTTOM_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_LEFT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_RIGHT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_TOP_BOUND_OFFSET;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.test.R;

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
    private TestFocusArea mFocusArea2;
    private View mChild;
    private View mDefaultFocus;
    private View mNonChild;
    private View mChild1;
    private View mChild2;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mFocusArea = mActivity.findViewById(R.id.focus_area);
        mFocusArea.enableForegroundHighlight();
        mFocusArea2 = mActivity.findViewById(R.id.focus_area2);
        mChild = mActivity.findViewById(R.id.child);
        mDefaultFocus = mActivity.findViewById(R.id.default_focus);
        mNonChild = mActivity.findViewById(R.id.non_child);
        mChild1 = mActivity.findViewById(R.id.child1);
        mChild2 = mActivity.findViewById(R.id.child2);
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
        CountDownLatch latch = new CountDownLatch(1);
        mFocusArea.post(() -> {
            mFocusArea.performAccessibilityAction(ACTION_FOCUS, bundle);
            latch.countDown();
        });
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mDefaultFocus.isFocused()).isTrue();
    }

    @Test
    public void testBoundsOffset() {
        assertThat(mFocusArea.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);

        // FocusArea's bounds offset specified in layout file:
        // 10dp(start), 20dp(end), 30dp(top), 40dp(bottom).
        int left = dp2Px(10);
        int right = dp2Px(20);
        int top = dp2Px(30);
        int bottom = dp2Px(40);
        AccessibilityNodeInfo node = mFocusArea.createAccessibilityNodeInfo();
        assertBoundsOffset(node, left, top, right, bottom);
        node.recycle();
    }

    @Test
    public void testBoundsOffsetWithRtl() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        mFocusArea.post(() -> {
            mFocusArea.setLayoutDirection(LAYOUT_DIRECTION_RTL);
            latch.countDown();
        });
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mFocusArea.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_RTL);

        // FocusArea highlight padding specified in layout file:
        // 10dp(start), 20dp(end), 30dp(top), 40dp(bottom).
        int left = dp2Px(20);
        int right = dp2Px(10);
        int top = dp2Px(30);
        int bottom = dp2Px(40);
        AccessibilityNodeInfo node = mFocusArea.createAccessibilityNodeInfo();
        assertBoundsOffset(node, left, top, right, bottom);
        node.recycle();
    }

    @Test
    public void testSetBoundsOffset() {
        mFocusArea.setBoundsOffset(50, 60, 70, 80);
        AccessibilityNodeInfo node = mFocusArea.createAccessibilityNodeInfo();
        assertBoundsOffset(node, 50, 60, 70, 80);
        node.recycle();
    }

    @Test
    public void testHighlightPadding() {
        assertThat(mFocusArea2.getLayoutDirection()).isEqualTo(LAYOUT_DIRECTION_LTR);

        int left = dp2Px(50);
        int right = dp2Px(10);
        int top = dp2Px(40);
        int bottom = dp2Px(20);
        AccessibilityNodeInfo node = mFocusArea2.createAccessibilityNodeInfo();
        assertBoundsOffset(node, left, top, right, bottom);
        node.recycle();
    }

    @Test
    public void testLastFocusedViewRemoved() {
        mChild1.post(() -> {
            // Focus on mChild1 in mFocusArea2, then mChild in mFocusArea .
            mChild1.requestFocus();
            assertThat(mChild1.isFocused()).isTrue();
            mChild.requestFocus();
            assertThat(mChild.isFocused()).isTrue();

            // Remove mChild1 in mFocusArea2, then Perform ACTION_FOCUS on mFocusArea2.
            mFocusArea2.removeView(mChild1);
            mFocusArea2.performAccessibilityAction(ACTION_FOCUS, null);

            // mChild2 in mFocusArea2 should get focused.
            assertThat(mChild2.isFocused()).isTrue();
        });
    }

    private void assertBoundsOffset(
            @NonNull AccessibilityNodeInfo node, int leftPx, int topPx, int rightPx, int bottomPx) {
        Bundle extras = node.getExtras();
        assertThat(extras.getInt(FOCUS_AREA_LEFT_BOUND_OFFSET)).isEqualTo(leftPx);
        assertThat(extras.getInt(FOCUS_AREA_RIGHT_BOUND_OFFSET)).isEqualTo(rightPx);
        assertThat(extras.getInt(FOCUS_AREA_TOP_BOUND_OFFSET)).isEqualTo(topPx);
        assertThat(extras.getInt(FOCUS_AREA_BOTTOM_BOUND_OFFSET)).isEqualTo(bottomPx);
    }

    /** Converts dp unit to equivalent pixels. */
    private int dp2Px(int dp) {
        return (int) (dp * mActivity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void assertDrawMethodsCalled(CountDownLatch latch) throws Exception {
        latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        assertThat(mFocusArea.onDrawCalled()).isTrue();
        assertThat(mFocusArea.drawCalled()).isTrue();
    }
}
