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

package com.android.car.ui.toolbar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.car.ui.actions.ViewActions.waitForView;
import static com.android.car.ui.matchers.ViewMatchers.doesNotExistOrIsNotDisplayed;
import static com.android.car.ui.matchers.ViewMatchers.withDrawable;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertEquals;

import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.ui.core.CarUi;
import com.android.car.ui.sharedlibrarysupport.SharedLibraryFactorySingleton;
import com.android.car.ui.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Unit test for {@link ToolbarController}. */
@SuppressWarnings("AndroidJdkLibsChecker")
@RunWith(Parameterized.class)
public class ToolbarTest {

    @Parameterized.Parameters(name = "With shared library? {0}")
    public static Object[] data() {
        // It's important to do no shared library first, so that the shared library will
        // still be enabled when this test finishes
        return new Object[] { false, true };
    }

    public ToolbarTest(boolean sharedLibEnabled) {
        SharedLibraryFactorySingleton.setSharedLibEnabled(sharedLibEnabled);
    }

    @Rule
    public final ActivityScenarioRule<ToolbarTestActivity> mScenarioRule =
            new ActivityScenarioRule<>(ToolbarTestActivity.class);

    @Test
    public void test_setTitle_displaysTitle() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setTitle("Test title"));

        onView(withText("Test title")).check(matches(isDisplayed()));
    }

    /**
     * This is somewhat of a bug, but various tests in other apps rely on this functionality.
     */
    @Test
    public void test_setTitle_null_returns_nonNull() throws Throwable {
        CharSequence[] getTitleResult = new CharSequence[] {"Something obviously incorrect"};
        runWithToolbar((toolbar) -> {
            toolbar.setTitle(null);
            getTitleResult[0] = toolbar.getTitle();
        });

        assertEquals("", getTitleResult[0]);
    }

    /**
     * This is somewhat of a bug, but various tests in other apps rely on this functionality.
     */
    @Test
    public void test_setSubtitle_null_returns_nonNull() throws Throwable {
        CharSequence[] getTitleResult = new CharSequence[] {"Something obviously incorrect"};
        runWithToolbar((toolbar) -> {
            toolbar.setSubtitle(null);
            getTitleResult[0] = toolbar.getSubtitle();
        });

        assertEquals("", getTitleResult[0]);
    }

    @Test
    public void test_setSubtitle_displaysSubtitle() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setSubtitle("Test subtitle"));

        onView(withText("Test subtitle")).check(matches(isDisplayed()));
    }

    @Test
    public void test_setSearchHint_isDisplayed() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setSearchHint("Test search hint");
            toolbar.setState(Toolbar.State.SEARCH);
        });

        onView(withHint("Test search hint")).check(matches(isDisplayed()));
    }

    @Test
    public void setters_and_getters_test() throws Throwable {
        runWithToolbar((toolbar) -> {
            toolbar.setTitle("Foo");
            toolbar.setSearchHint("Foo2");
            toolbar.setShowMenuItemsWhileSearching(true);
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);

            assertThat(toolbar.getTitle().toString()).isEqualTo("Foo");
            assertThat(toolbar.getSearchHint().toString()).isEqualTo("Foo2");
            assertThat(toolbar.getShowMenuItemsWhileSearching()).isEqualTo(true);
            assertThat(toolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.SUBPAGE);
            assertThat(toolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                    Toolbar.NavButtonMode.CLOSE);
        });
    }

    @Test
    public void test_setLogo_displaysLogo() throws Throwable {
        runWithToolbar((toolbar) -> toolbar.setLogo(R.drawable.ic_launcher));

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        onView(withDrawable(context, R.drawable.ic_launcher)).check(matches(isDisplayed()));
    }

    @Test
    public void pressBack_withoutListener_callsActivityOnBack() throws Throwable {
        ToolbarTestActivity[] savedActivity = new ToolbarTestActivity[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            savedActivity[0] = activity;
        });

        onView(withContentDescription("Back")).perform(click());

        assertEquals(1, savedActivity[0].getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withListenerThatReturnsFalse_callsActivityOnBack() throws Throwable {
        ToolbarTestActivity[] savedActivity = new ToolbarTestActivity[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.registerOnBackListener(() -> false);
            savedActivity[0] = activity;
        });

        onView(withContentDescription("Back")).perform(click());

        assertEquals(1, savedActivity[0].getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withListenerThatReturnsTrue_doesntCallActivityOnBack() throws Throwable {
        ToolbarTestActivity[] savedActivity = new ToolbarTestActivity[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            toolbar.registerOnBackListener(() -> true);
            savedActivity[0] = activity;
        });

        onView(withContentDescription("Back")).perform(click());

        assertEquals(0, savedActivity[0].getTimesOnBackPressed());
    }

    @Test
    public void pressBack_withUnregisteredListener_doesntCallActivityOnBack() throws Throwable {
        ToolbarTestActivity[] savedActivity = new ToolbarTestActivity[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            toolbar.setState(Toolbar.State.SUBPAGE);
            Toolbar.OnBackListener listener = () -> true;
            toolbar.registerOnBackListener(listener);
            toolbar.registerOnBackListener(listener);
            toolbar.unregisterOnBackListener(listener);
            savedActivity[0] = activity;
        });

        onView(withContentDescription("Back")).perform(click());

        assertEquals(1, savedActivity[0].getTimesOnBackPressed());
    }

    @Test
    public void menuItems_setId_shouldWork() throws Throwable {
        runWithActivityAndToolbar((activity, toolbar) -> {
            MenuItem item = MenuItem.builder(activity).build();

            assertThat(item.getId()).isEqualTo(View.NO_ID);

            item.setId(7);

            assertThat(item.getId()).isEqualTo(7);
        });
    }

    @Test
    public void menuItems_whenClicked_shouldCallListener() throws Throwable {
        MenuItem.OnClickListener callback = mock(MenuItem.OnClickListener.class);
        MenuItem[] menuItem = new MenuItem[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            menuItem[0] = MenuItem.builder(activity)
                    .setTitle("Button!")
                    .setOnClickListener(callback)
                    .build();
            toolbar.setMenuItems(Collections.singletonList(menuItem[0]));
        });

        waitForViewWithText("Button!");

        onView(withText("Button!")).perform(click());

        verify(callback).onClick(menuItem[0]);
    }

    @Test
    public void menuItems_null_shouldRemoveExistingMenuItems() throws Throwable {
        runWithActivityAndToolbar((activity, toolbar) ->
                toolbar.setMenuItems(Arrays.asList(
                        MenuItem.builder(activity)
                                .setTitle("Button!")
                                .build(),
                        MenuItem.builder(activity)
                                .setTitle("Button2!")
                                .build()
                )));
        waitForViewWithText("Button!");
        waitForViewWithText("Button2!");

        onView(withText("Button!")).check(matches(isDisplayed()));
        onView(withText("Button2!")).check(matches(isDisplayed()));

        runWithToolbar((toolbar) -> toolbar.setMenuItems(null));

        onView(withText("Button!")).check(doesNotExist());
        onView(withText("Button2!")).check(doesNotExist());
    }

    @Test
    public void menuItems_setVisibility_shouldHide() throws Throwable {
        MenuItem[] menuItem = new MenuItem[] { null };
        runWithActivityAndToolbar((activity, toolbar) -> {
            menuItem[0] = MenuItem.builder(activity)
                    .setTitle("Button!")
                    .build();
            toolbar.setMenuItems(Collections.singletonList(menuItem[0]));
        });

        waitForViewWithText("Button!");
        onView(withText("Button!")).check(matches(isDisplayed()));

        runWithToolbar((toolbar) -> menuItem[0].setVisible(false));

        onView(withText("Button!")).check(matches(not(isDisplayed())));
    }

    @Test
    public void menuItems_searchScreen_shouldHideMenuItems() throws Throwable {
        runWithActivityAndToolbar((activity, toolbar) -> {
            toolbar.setMenuItems(Arrays.asList(
                    MenuItem.builder(activity)
                            .setToSearch()
                            .build(),
                    MenuItem.builder(activity)
                            .setTitle("Button!")
                            .build()));
            toolbar.setShowMenuItemsWhileSearching(true);
            toolbar.setState(Toolbar.State.SEARCH);
        });

        waitForViewWithText("Button!");

        // Even if not hiding MenuItems while searching, the search MenuItem should still be hidden
        onView(withText("Button!")).check(matches(isDisplayed()));
        onView(withContentDescription(R.string.car_ui_toolbar_menu_item_search_title))
                .check(doesNotExistOrIsNotDisplayed());

        runWithToolbar((toolbar) -> toolbar.setShowMenuItemsWhileSearching(false));

        // All menuitems should be hidden if we're hiding menuitems while searching
        onView(withText("Button!")).check(doesNotExistOrIsNotDisplayed());
        onView(withContentDescription(R.string.car_ui_toolbar_menu_item_search_title))
                .check(doesNotExistOrIsNotDisplayed());

    }

    private void runWithToolbar(Consumer<ToolbarController> toRun) throws Throwable {
        mScenarioRule.getScenario().onActivity(activity -> {
            ToolbarController toolbar = CarUi.requireToolbar(activity);
            toRun.accept(toolbar);
        });
    }

    private void runWithActivityAndToolbar(BiConsumer<ToolbarTestActivity, ToolbarController> toRun)
            throws Throwable {
        mScenarioRule.getScenario().onActivity(activity -> {
            ToolbarController toolbar = CarUi.requireToolbar(activity);
            toRun.accept(activity, toolbar);
        });
    }

    private void waitForViewWithText(String text) {
        onView(isRoot()).perform(waitForView(withText(text), 500));
    }
}
