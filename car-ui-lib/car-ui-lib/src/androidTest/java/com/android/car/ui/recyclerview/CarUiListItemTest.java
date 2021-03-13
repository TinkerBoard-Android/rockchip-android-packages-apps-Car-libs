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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.rule.ActivityTestRule;

import com.android.car.ui.CarUiText;
import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link CarUiListItem}.
 */
public class CarUiListItemTest {
    private static final CharSequence LONG_CHAR_SEQUENCE =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
                    + "incididunt ut labore et dolore magna aliqua. Netus et malesuada fames ac "
                    + "turpis egestas maecenas pharetra convallis. At urna condimentum mattis "
                    + "pellentesque id nibh tortor. Purus in mollis nunc sed id semper risus in. "
                    + "Turpis massa tincidunt dui ut ornare lectus sit amet. Porttitor lacus "
                    + "luctus accumsan tortor posuere ac. Augue eget arcu dictum varius. Massa "
                    + "tempor nec feugiat nisl pretium fusce id velit ut. Fames ac turpis egestas"
                    + " sed tempus urna et pharetra pharetra. Tellus orci ac auctor augue mauris "
                    + "augue neque gravida. Purus viverra accumsan in nisl nisi scelerisque eu. "
                    + "Ut lectus arcu bibendum at varius vel pharetra. Penatibus et magnis dis "
                    + "parturient montes nascetur ridiculus mus. Suspendisse sed nisi lacus sed "
                    + "viverra tellus in hac habitasse.";
    private static final String ELLIPSIS = "…";

    private CarUiRecyclerView mCarUiRecyclerView;

    @Rule
    public ActivityTestRule<CarUiRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(CarUiRecyclerViewTestActivity.class);

    @Before
    public void setUp() {
        mCarUiRecyclerView = mActivityRule.getActivity().requireViewById(R.id.list);
    }

    @Test
    public void testItemVisibility_withTitle() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_body)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_icon_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItemVisibility_withBody() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody("Test body");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_body)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_title)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_icon_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItemVisibility_withChevron() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.CHEVRON);
        item.setTitle("Test item with chevron");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_body)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_icon_container)).check(matches(not(isDisplayed())));
        onView(withId(R.id.car_ui_list_item_action_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testItemVisibility_withTitle_withBodyAndIcon() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        item.setBody("Test body");
        item.setIcon(mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close));
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_body)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_icon_container)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_action_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testItem_withCheckbox() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnCheckedChangeListener mockOnCheckedChangeListener = mock(
                CarUiContentListItem.OnCheckedChangeListener.class);

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.CHECK_BOX);
        item.setTitle("Test item with checkbox");
        item.setOnCheckedChangeListener(mockOnCheckedChangeListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_checkbox_widget)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_action_divider)).check(matches(not(isDisplayed())));

        // List item with checkbox should be initially unchecked.
        onView(withId(R.id.car_ui_list_item_checkbox_widget)).check(matches(isNotChecked()));
        // Clicks anywhere on the item should toggle the checkbox
        onView(withId(R.id.car_ui_list_item_title)).perform(click());
        onView(withId(R.id.car_ui_list_item_checkbox_widget)).check(matches(isChecked()));
        // Check that onCheckChangedListener was invoked.
        verify(mockOnCheckedChangeListener, times(1)).onCheckedChanged(item, true);

        // Uncheck checkbox with click on the action container
        onView(withId(R.id.car_ui_list_item_action_container)).perform(click());
        onView(withId(R.id.car_ui_list_item_checkbox_widget)).check(matches(isNotChecked()));
        // Check that onCheckChangedListener was invoked.
        verify(mockOnCheckedChangeListener, times(1)).onCheckedChanged(item, false);
    }

    @Test
    public void testItem_withSwitch() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.SWITCH);
        item.setBody("Test item with switch");
        item.setChecked(true);
        item.setActionDividerVisible(true);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_body)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_switch_widget)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_action_divider)).check(matches(isDisplayed()));

        // List item with checkbox should be initially checked.
        onView(withId(R.id.car_ui_list_item_switch_widget)).check(matches(isChecked()));
        // Clicks anywhere on the item should toggle the switch
        onView(withId(R.id.car_ui_list_item_switch_widget)).perform(click());
        onView(withId(R.id.car_ui_list_item_switch_widget)).check(matches(isNotChecked()));
        // Uncheck checkbox with click on the action container
        onView(withId(R.id.car_ui_list_item_body)).perform(click());
        onView(withId(R.id.car_ui_list_item_switch_widget)).check(matches(isChecked()));
    }

    @Test
    public void testItem_withRadioButton() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.RADIO_BUTTON);
        item.setTitle("Test item with radio button");
        item.setChecked(false);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));
        onView(withId(R.id.car_ui_list_item_radio_button_widget)).check(matches(isDisplayed()));

        // List item with checkbox should be initially not checked.
        onView(withId(R.id.car_ui_list_item_radio_button_widget)).check(matches(isNotChecked()));
        // Clicks anywhere on the item should toggle the radio button.
        onView(withId(R.id.car_ui_list_item_radio_button_widget)).perform(click());
        onView(withId(R.id.car_ui_list_item_radio_button_widget)).check(matches(isChecked()));

        // Repeated clicks on a selected radio button should not toggle the element once checked.
        onView(withId(R.id.car_ui_list_item_title)).perform(click());
        onView(withId(R.id.car_ui_list_item_radio_button_widget)).check(matches(isChecked()));
    }

    @Test
    public void testItem_withListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener mockOnCheckedChangeListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.NONE);
        item.setIcon(mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close));
        item.setTitle("Test item with listener");
        item.setBody("Body text");
        item.setOnItemClickedListener(mockOnCheckedChangeListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item should toggle the listener
        onView(withId(R.id.car_ui_list_item_title)).perform(click());
        verify(mockOnCheckedChangeListener, times(1)).onClick(item);

        onView(withId(R.id.car_ui_list_item_body)).perform(click());
        verify(mockOnCheckedChangeListener, times(2)).onClick(item);

        onView(withId(R.id.car_ui_list_item_icon_container)).perform(click());
        verify(mockOnCheckedChangeListener, times(3)).onClick(item);
    }

    @Test
    public void testItem_withListenerAndSupplementalIconListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener clickListener = mock(
                CarUiContentListItem.OnClickListener.class);
        CarUiContentListItem.OnClickListener supplementalIconClickListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.ICON);
        item.setTitle("Test item with two listeners");
        item.setOnItemClickedListener(clickListener);
        item.setSupplementalIcon(
                mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close),
                supplementalIconClickListener);
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item (except supplemental icon) should trigger the item click
        // listener.
        onView(withId(R.id.car_ui_list_item_title)).perform(click());
        verify(clickListener, times(1)).onClick(item);
        verify(supplementalIconClickListener, times(0)).onClick(item);

        onView(withId(R.id.car_ui_list_item_supplemental_icon)).perform(click());
        // Check that icon is argument for single call to click listener.
        verify(supplementalIconClickListener, times(1)).onClick(item);

        // Verify that the standard click listener wasn't also fired.
        verify(clickListener, times(1)).onClick(item);
    }

    @Test
    public void testItem_withSupplementalIconAndIconOnClickListener() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem.OnClickListener mockedItemOnClickListener = mock(
                CarUiContentListItem.OnClickListener.class);
        CarUiContentListItem.OnClickListener mockedIconListener = mock(
                CarUiContentListItem.OnClickListener.class);

        CarUiContentListItem item = new CarUiContentListItem(
                CarUiContentListItem.Action.ICON);
        item.setSupplementalIcon(
                mActivityRule.getActivity().getDrawable(R.drawable.car_ui_icon_close),
                mockedIconListener);
        item.setOnItemClickedListener(mockedItemOnClickListener);
        item.setTitle("Test item with listeners");
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_title)).check(matches(isDisplayed()));

        // Clicks anywhere on the item (outside of the icon) should only invoke the item click
        // listener.
        onView(withId(R.id.car_ui_list_item_title)).perform(click());
        verify(mockedItemOnClickListener, times(1)).onClick(item);

        // Clicks anywhere on the icon should invoke both listeners.
        onView(withId(R.id.car_ui_list_item_action_container)).perform(click());
        verify(mockedItemOnClickListener, times(1)).onClick(item);
        verify(mockedIconListener, times(1)).onClick(item);
    }

    @Test
    public void testRadioButtonListItemAdapter() {
        List<CarUiRadioButtonListItem> items = new ArrayList<>();

        CarUiRadioButtonListItem itemOne = new CarUiRadioButtonListItem();
        String itemOneTitle = "Item 1";
        itemOne.setTitle(itemOneTitle);
        items.add(itemOne);

        CarUiRadioButtonListItem itemTwo = new CarUiRadioButtonListItem();
        String itemTwoTitle = "Item 2";
        itemTwo.setTitle(itemTwoTitle);
        items.add(itemTwo);

        CarUiRadioButtonListItem itemThree = new CarUiRadioButtonListItem();
        String itemThreeTitle = "Item 3";
        itemThree.setTitle(itemThreeTitle);
        items.add(itemThree);

        CarUiRadioButtonListItemAdapter adapter = new CarUiRadioButtonListItemAdapter(items);
        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(adapter));

        onView(withText(itemOneTitle)).check(matches(isDisplayed()));
        onView(withText(itemTwoTitle)).check(matches(isDisplayed()));
        onView(withText(itemThreeTitle)).check(matches(isDisplayed()));

        // All items are initially unchecked.
        assertFalse(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), -1);

        // Select first item.
        onView(withText(itemOneTitle)).perform(click());
        assertTrue(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 0);

        // Select second item.
        onView(withText(itemTwoTitle)).perform(click());
        assertFalse(itemOne.isChecked());
        assertTrue(itemTwo.isChecked());
        assertFalse(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 1);

        // Select third item.
        onView(withText(itemThreeTitle)).perform(click());
        assertFalse(itemOne.isChecked());
        assertFalse(itemTwo.isChecked());
        assertTrue(itemThree.isChecked());
        assertEquals(adapter.getSelectedItemPosition(), 2);
    }

    @Test
    public void testTextTruncation_twoShortLines() {
        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText("Short text string", 2));
        lines.add(new CarUiText("Second short string", 2));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        // Check for no manual truncation ellipsis.
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(not(withText(containsString(ELLIPSIS)))));
    }

    @Test
    public void testTextTruncation_oneLongOneShort_withMaxLines() {
        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, 2));
        lines.add(new CarUiText("Second short string", 2));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        // Check for manual truncation ellipsis.
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(withText(containsString(ELLIPSIS))));

        TextView bodyTextView = mCarUiRecyclerView.requireViewById(R.id.car_ui_list_item_body);
        assertEquals(3, bodyTextView.getLineCount());
    }

    @Test
    public void testTextTruncation_oneLongOneShort_noMaxLines() {
        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, Integer.MAX_VALUE));
        lines.add(new CarUiText("Second short string"));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        // Check for no manual truncation ellipsis.
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(not(withText(containsString(ELLIPSIS)))));
    }

    @Test
    public void testTextTruncation_twoLong_withMaxLines() {
        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, 3));
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, 3));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        // Check for manual truncation ellipsis.
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(withText(containsString(ELLIPSIS))));

        TextView bodyTextView = mCarUiRecyclerView.requireViewById(R.id.car_ui_list_item_body);
        assertEquals(6, bodyTextView.getLineCount());
    }

    @Test
    public void testTextTruncation_twoLong_differentMaxLines() {
        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, 1));
        lines.add(new CarUiText(LONG_CHAR_SEQUENCE, 4));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        // Check for manual truncation ellipsis.
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(withText(containsString(ELLIPSIS))));

        TextView bodyTextView = mCarUiRecyclerView.requireViewById(R.id.car_ui_list_item_body);
        assertEquals(5, bodyTextView.getLineCount());
    }

    @Test
    public void testMultipleBodyTextLines() {
        CharSequence line1 = "First short string";
        CharSequence line2 = "Second short string";
        CharSequence line3 = "Third short string";

        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(line1));
        lines.add(new CarUiText(line2));
        lines.add(new CarUiText(line3));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        String expectedText = line1 + "\n" + line2 + "\n" + line3;
        onView(withId(R.id.car_ui_list_item_body)).check(
                matches(withText(containsString(expectedText))));
    }

    @Test
    public void testBodyTextSpans() {
        int color = ContextCompat.getColor(mCarUiRecyclerView.getContext(),
                R.color.car_ui_color_accent);

        Spannable line1 = new SpannableString("This text contains color");
        line1.setSpan(new ForegroundColorSpan(color), 19, 24, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        List<CarUiText> lines = new ArrayList<>();
        lines.add(new CarUiText(line1, Integer.MAX_VALUE));

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setBody(lines);
        List<CarUiListItem> items = new ArrayList<>();
        items.add(item);

        mCarUiRecyclerView.post(
                () -> mCarUiRecyclerView.setAdapter(new CarUiListItemAdapter(items)));

        onView(withId(R.id.car_ui_list_item_body)).check(matches(isDisplayed()));
        TextView bodyTextView = mCarUiRecyclerView.requireViewById(R.id.car_ui_list_item_body);
        assertEquals(line1, bodyTextView.getText());
    }
}
