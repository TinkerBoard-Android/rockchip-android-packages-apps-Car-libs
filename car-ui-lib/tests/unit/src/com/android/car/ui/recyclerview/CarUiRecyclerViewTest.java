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

package com.android.car.ui.recyclerview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.PositionAssertions.isBottomAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isLeftAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isRightAlignedWith;
import static androidx.test.espresso.assertion.PositionAssertions.isTopAlignedWith;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.TestActivity;
import com.android.car.ui.tests.unit.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for {@link CarUiRecyclerView}. */
public class CarUiRecyclerViewTest {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    private TestActivity mActivity;
    private Context mTestableContext;
    private Resources mTestableResources;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mTestableContext = spy(mActivity);
        mTestableResources = spy(mActivity.getResources());
        when(mTestableContext.getResources()).thenReturn(mTestableResources);
    }

    @Test
    public void testIsScrollbarPresent_scrollbarEnabled() {
        doReturn(true).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(100));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(matches(isDisplayed()));
    }

    @Test
    public void testIsScrollbarPresent_scrollbarDisabled() {
        doReturn(false).when(mTestableResources).getBoolean(R.bool.car_ui_scrollbar_enable);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(100));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(doesNotExist());
    }

    @Test
    public void testGridLayout() {
        TypedArray typedArray = spy(mActivity.getBaseContext().obtainStyledAttributes(
                null, R.styleable.CarUiRecyclerView));
        Context context = spy(InstrumentationRegistry.getInstrumentation().getTargetContext());

        doReturn(typedArray).when(context).obtainStyledAttributes(
                any(),
                eq(R.styleable.CarUiRecyclerView),
                anyInt(),
                anyInt());
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_layoutStyle), anyInt()))
                .thenReturn(CarUiRecyclerView.CarUiRecyclerViewLayout.GRID);
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_numOfColumns), anyInt()))
                .thenReturn(3);

        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(context);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        TestAdapter adapter = new TestAdapter(4);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(adapter);
        });

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof GridLayoutManager);

        // Check that all items in the first row are top-aligned.
        onView(withText(adapter.getItemText(0))).check(
                isTopAlignedWith(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isTopAlignedWith(withText(adapter.getItemText(2))));

        // Check that all items in the first row are bottom-aligned.
        onView(withText(adapter.getItemText(0))).check(
                isBottomAlignedWith(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isBottomAlignedWith(withText(adapter.getItemText(2))));

        // Check that items in second row are rendered correctly below the first row.
        onView(withText(adapter.getItemText(0))).check(
                isCompletelyAbove(withText(adapter.getItemText(3))));
        onView(withText(adapter.getItemText(0))).check(
                isLeftAlignedWith(withText(adapter.getItemText(3))));
        onView(withText(adapter.getItemText(0))).check(
                isRightAlignedWith(withText(adapter.getItemText(3))));
    }

    @Test
    public void testLinearLayout() {
        TypedArray typedArray = spy(mActivity.getBaseContext().obtainStyledAttributes(
                null, R.styleable.CarUiRecyclerView));
        Context context = spy(InstrumentationRegistry.getInstrumentation().getTargetContext());

        doReturn(typedArray).when(context).obtainStyledAttributes(
                any(),
                eq(R.styleable.CarUiRecyclerView),
                anyInt(),
                anyInt());
        when(typedArray.getInt(eq(R.styleable.CarUiRecyclerView_layoutStyle), anyInt()))
                .thenReturn(CarUiRecyclerView.CarUiRecyclerViewLayout.LINEAR);

        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(context);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        TestAdapter adapter = new TestAdapter(4);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(adapter);
        });

        assertTrue(carUiRecyclerView.getLayoutManager() instanceof LinearLayoutManager);

        // Check that item views are laid out linearly.
        onView(withText(adapter.getItemText(0))).check(
                isCompletelyAbove(withText(adapter.getItemText(1))));
        onView(withText(adapter.getItemText(1))).check(
                isCompletelyAbove(withText(adapter.getItemText(2))));
        onView(withText(adapter.getItemText(2))).check(
                isCompletelyAbove(withText(adapter.getItemText(3))));
    }

    @Test
    public void testOnHeightChanged_shouldAddTheValueToInitialTopValue() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(R.layout.car_ui_recycler_view_test_activity));

        onView(withId(R.id.list)).check(matches(isDisplayed()));

        CarUiRecyclerView carUiRecyclerView = mActivity.findViewById(R.id.list);

        assertEquals(carUiRecyclerView.getPaddingBottom(), 0);
        assertEquals(carUiRecyclerView.getPaddingTop(), 0);
        assertEquals(carUiRecyclerView.getPaddingStart(), 0);
        assertEquals(carUiRecyclerView.getPaddingEnd(), 0);

        mActivity.runOnUiThread(() -> carUiRecyclerView.onHeightChanged(10));
        onView(withId(R.id.list)).check(matches(isDisplayed()));

        assertEquals(carUiRecyclerView.getPaddingTop(), 10);
        assertEquals(carUiRecyclerView.getPaddingBottom(), 0);
        assertEquals(carUiRecyclerView.getPaddingStart(), 0);
        assertEquals(carUiRecyclerView.getPaddingEnd(), 0);
    }

    @Test
    public void testVisibility_goneAtInflationWithChangeToVisible() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(
                        R.layout.car_ui_recycler_view_gone_test_activity));

        onView(withId(R.id.list)).check(matches(not(isDisplayed())));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(3);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
            carUiRecyclerView.setVisibility(View.VISIBLE);
        });

        // Check that items in are displayed.
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(1))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(2))).check(matches(isDisplayed()));
    }

    @Test
    public void testVisibility_invisibleAtInflationWithChangeToVisible() {
        mActivity.runOnUiThread(
                () -> mActivity.setContentView(
                        R.layout.car_ui_recycler_view_invisible_test_activity));

        onView(withId(R.id.list)).check(matches(not(isDisplayed())));

        CarUiRecyclerView carUiRecyclerView = mActivity.requireViewById(R.id.list);
        TestAdapter adapter = new TestAdapter(3);
        mActivity.runOnUiThread(() -> {
            carUiRecyclerView.setAdapter(adapter);
            carUiRecyclerView.setVisibility(View.VISIBLE);
        });

        // Check that items in are displayed.
        onView(withText(adapter.getItemText(0))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(1))).check(matches(isDisplayed()));
        onView(withText(adapter.getItemText(2))).check(matches(isDisplayed()));
    }

    /** A test adapter that handles inflating test views and binding data to it. */
    private static class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {
        private final List<String> mData;

        TestAdapter(int itemCount) {
            mData = new ArrayList<>(itemCount);

            for (int i = 0; i < itemCount; i++) {
                mData.add(getItemText(i));
            }
        }

        String getItemText(int position) {
            if (position > mData.size()) {
                return null;
            }

            return String.format("Sample item #%d", position);
        }

        @NonNull
        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TestViewHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        TestViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.test_list_item, parent, false));
            mTextView = itemView.findViewById(R.id.text);
        }

        void bind(String text) {
            mTextView.setText(text);
        }
    }
}
