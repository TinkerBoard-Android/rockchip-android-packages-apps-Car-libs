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

package com.android.car.ui.tests.unit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.testing.TestableContext;
import android.testing.TestableResources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.recyclerview.CarUiRecyclerView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CarUiRecyclerViewTest {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    @Rule
    public TestableContext mTestableContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getTargetContext());

    private TestActivity mActivity;
    private TestableResources mTestableResources;


    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mTestableResources = mTestableContext.getOrCreateTestableResources();
    }

    @Test
    public void testIsScrollbarPresent_scrollbarEnabled() {
        mTestableResources.addOverride(R.bool.car_ui_scrollbar_enable, true);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(10));
        });
        carUiRecyclerView.setAdapter(new TestAdapter(10));

        onView(withId(R.id.car_ui_scroll_bar)).check(matches(isDisplayed()));
    }

    @Test
    public void testIsScrollbarPresent_scrollbarDisabled() {
        mTestableResources.addOverride(R.bool.car_ui_scrollbar_enable, false);
        CarUiRecyclerView carUiRecyclerView = new CarUiRecyclerView(mTestableContext);
        ViewGroup container = mActivity.findViewById(R.id.test_container);
        container.post(() -> {
            container.addView(carUiRecyclerView);
            carUiRecyclerView.setAdapter(new TestAdapter(10));
        });

        onView(withId(R.id.car_ui_scroll_bar)).check(doesNotExist());
    }

    /** A test adapter that handles inflating test views and binding data to it. */
    private static class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {
        private final List<String> mData;

        TestAdapter(int itemCount) {
            mData = new ArrayList<>(itemCount);

            for (int i = 0; i < itemCount; i++) {
                mData.add(String.format("Sample item #%d", i));
            }
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
