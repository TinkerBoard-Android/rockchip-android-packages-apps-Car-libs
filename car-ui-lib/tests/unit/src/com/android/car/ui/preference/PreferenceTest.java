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

package com.android.car.ui.preference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.car.ui.matchers.ViewMatchers.withIndex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Resources;

import androidx.preference.Preference;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.tests.unit.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/** Unit tests for {@link CarUiPreference}. */
public class PreferenceTest {

    private PreferenceTestActivity mActivity;
    private String[] mEntries;
    private String[] mEntriesValues;

    @Rule
    public ActivityTestRule<PreferenceTestActivity> mActivityRule =
            new ActivityTestRule<>(PreferenceTestActivity.class);

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        Resources resources = mActivity.getResources();
        mEntries = resources.getStringArray(R.array.entries);
        mEntriesValues = resources.getStringArray(R.array.entry_values);
    }

    @Test
    public void testListPreference() {
        // Display full screen list preference.
        onView(withText(R.string.title_list_preference)).perform(click());

        Preference.OnPreferenceChangeListener mockListener = mock(
                Preference.OnPreferenceChangeListener.class);
        when(mockListener.onPreferenceChange(any(), any())).thenReturn(true);
        mActivity.setOnPreferenceChangeListener("list", mockListener);

        // Check that no option is initially selected.
        onView(withIndex(withId(R.id.radio_button_widget), 0)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 2)).check(matches(isNotChecked()));

        // Select first option.
        onView(withText(mEntries[0])).perform(click());
        // Check that first option is selected.
        onView(withIndex(withId(R.id.radio_button_widget), 0)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 2)).check(matches(isNotChecked()));

        // Press back to save selection.
        onView(withId(R.id.car_ui_toolbar_nav_icon)).perform(click());
        // Verify preference value was updated.
        verify(mockListener, times(1)).onPreferenceChange(any(), eq(mEntriesValues[0]));

        onView(withText(R.string.title_list_preference)).perform(click());

        // Check that first option is selected.
        onView(withIndex(withId(R.id.radio_button_widget), 0)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 2)).check(matches(isNotChecked()));

        // Select second option.
        onView(withText(mEntries[1])).perform(click());
        // Check that second option is selected.
        onView(withIndex(withId(R.id.radio_button_widget), 0)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 1)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 2)).check(matches(isNotChecked()));

        // Press back to save selection.
        onView(withId(R.id.car_ui_toolbar_nav_icon)).perform(click());
        // Verify preference value was updated.
        verify(mockListener, times(1)).onPreferenceChange(any(), eq(mEntriesValues[1]));
        // Return to list preference screen.
        onView(withText(R.string.title_list_preference)).perform(click());

        // Check that second option is selected.
        onView(withIndex(withId(R.id.radio_button_widget), 0)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 1)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.radio_button_widget), 2)).check(matches(isNotChecked()));
    }

    @Test
    public void testMultiSelectListPreference() {
        // Display full screen list preference.
        onView(withText(R.string.title_multi_list_preference)).perform(click());

        Preference.OnPreferenceChangeListener mockListener = mock(
                Preference.OnPreferenceChangeListener.class);
        when(mockListener.onPreferenceChange(any(), any())).thenReturn(true);
        mActivity.setOnPreferenceChangeListener("multi_select_list", mockListener);

        // Check that no option is initially selected.
        onView(withIndex(withId(R.id.checkbox_widget), 0)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 2)).check(matches(isNotChecked()));

        // Select options 1 and 3.
        onView(withText(mEntries[0])).perform(click());
        onView(withText(mEntries[2])).perform(click());

        // Check that selections are correctly reflected.
        onView(withIndex(withId(R.id.checkbox_widget), 0)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 2)).check(matches(isChecked()));

        // Press back to save selection.
        onView(withId(R.id.car_ui_toolbar_nav_icon)).perform(click());
        Set<String> expectedUpdate = new HashSet<>();
        expectedUpdate.add(mEntriesValues[0]);
        expectedUpdate.add(mEntriesValues[2]);
        // Verify preference value was updated.
        verify(mockListener, times(1)).onPreferenceChange(any(), eq(expectedUpdate));

        // Return to multi-select list preference screen.
        onView(withText(R.string.title_multi_list_preference)).perform(click());

        // Check that selections are correctly reflected.
        onView(withIndex(withId(R.id.checkbox_widget), 0)).check(matches(isChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 1)).check(matches(isNotChecked()));
        onView(withIndex(withId(R.id.checkbox_widget), 2)).check(matches(isChecked()));
    }
}
