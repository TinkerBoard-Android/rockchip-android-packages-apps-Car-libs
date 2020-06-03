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

package com.android.car.ui.toolbar;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.ui.CarUiRobolectricTestRunner;
import com.android.car.ui.CarUiTestUtil;
import com.android.car.ui.R;
import com.android.car.ui.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(CarUiRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ExtendedShadowTypeface.class, ShadowAsyncLayoutInflater.class})
public class ToolbarTest {
    private Context mContext;
    private Resources mResources;
    private Toolbar mToolbar;

    @Before
    public void setUp() {
        mContext = CarUiTestUtil.getMockContext();
        mResources = mContext.getResources();
        mToolbar = new Toolbar(mContext);
    }

    @Test
    public void getters_nochanges_shouldReturnDefaults() {
        assertThat(mToolbar.getBackgroundShown()).isEqualTo(true);
        assertThat(mToolbar.getShowMenuItemsWhileSearching()).isEqualTo(false);
        assertThat(mToolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.HOME);
        assertThat(mToolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                Toolbar.NavButtonMode.BACK);
    }

    @Test
    public void setState_subpage_shouldCauseGetStateToReturnSubpage() {
        mToolbar.setState(Toolbar.State.SUBPAGE);

        assertThat(mToolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.SUBPAGE);
    }

    @Test
    public void setters_and_getters_test() {
        mToolbar.setTitle("Foo");
        mToolbar.setSearchHint("Foo2");
        mToolbar.setBackgroundShown(false);
        mToolbar.setShowMenuItemsWhileSearching(true);
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);

        assertThat(mToolbar.getTitle().toString()).isEqualTo("Foo");
        assertThat(mToolbar.getSearchHint().toString()).isEqualTo("Foo2");
        assertThat(mToolbar.getBackgroundShown()).isEqualTo(false);
        assertThat(mToolbar.getShowMenuItemsWhileSearching()).isEqualTo(true);
        assertThat(mToolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.SUBPAGE);
        assertThat(mToolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                Toolbar.NavButtonMode.CLOSE);
    }

    @Test
    public void showLogo_whenSet_andStateIsHome() {
        mToolbar.setState(Toolbar.State.HOME);
        mToolbar.setLogo(R.drawable.test_ic_launcher);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_nav_icon_container).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void hideLogo_andTitleLogo_whenSet_andStateIsHome_andLogoIsDisabled() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_show_logo)).thenReturn(false);

        Toolbar toolbar = new Toolbar(mContext);
        toolbar.setState(Toolbar.State.HOME);
        toolbar.setLogo(R.drawable.test_ic_launcher);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_nav_icon_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void showTitleLogo_whenSet_andStateIsNotHome() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setLogo(R.drawable.test_ic_launcher);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_nav_icon_container).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo_container).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void hideLogo_andTitleLogo_whenNotSet_andStateIsHome() {
        mToolbar.setState(Toolbar.State.HOME);
        mToolbar.setLogo(0);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_nav_icon_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);    }

    @Test
    public void hideLogo_andTitleLogo_whenNotSet_andStateIsNotHome() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setLogo(0);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_title_logo_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void registerOnBackListener_whenBackIsPressed_shouldCallListener() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        Mutable<Integer> timesBackPressed = new Mutable<>(0);
        Toolbar.OnBackListener listener = () -> {
            timesBackPressed.value++;
            return false;
        };

        mToolbar.registerOnBackListener(listener);
        pressBack();

        assertThat(timesBackPressed.value).isEqualTo(1);
    }

    @Test
    public void testState_twoRow_withTitle_withTabs() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(true);

        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isTrue();

        // Set title and tabs for toolbar.
        toolbar.setTitle("Test title");
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));

        // Toolbar should display two rows, showing both title and tabs.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void testState_twoRow_withTitle() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(true);

        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isTrue();

        toolbar.setTitle("Test title");

        // Toolbar should display two rows, but no tabs are set so they should not be visible.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isNotEqualTo(
                View.VISIBLE);
    }

    @Test
    public void testState_twoRow_withTabs() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(true);

        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isTrue();
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));

        // Toolbar should display two rows with an empty title and tabs.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void testState_oneRow_withTitle_withTabs() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(false);

        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isFalse();

        // Set title and tabs for toolbar.
        toolbar.setTitle("Test title");
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));

        // With only one row available, toolbar will only show tabs and not the title.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isNotEqualTo(
                View.VISIBLE);
    }

    @Test
    public void testState_oneRow_withTitle() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(false);

        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isFalse();

        toolbar.setTitle("Test title");

        // Toolbar should display one row with the title and no tabs.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isNotEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void testState_oneRow_withTabs() {
        when(mResources.getBoolean(R.bool.car_ui_toolbar_tabs_on_second_row)).thenReturn(false);


        Toolbar toolbar = new Toolbar(mContext);
        assertThat(toolbar.isTabsInSecondRow()).isFalse();

        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));
        toolbar.addTab(new TabLayout.Tab(mContext.getDrawable(R.drawable.test_ic_launcher), "Foo"));

        // Toolbar should display one row with only tabs.
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_tabs).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(toolbar.findViewById(R.id.car_ui_toolbar_title).getVisibility()).isNotEqualTo(
                View.VISIBLE);
    }

    @Test
    public void registerOnBackListener_whenListenerRegisteredTwice_shouldntCallListenerTwice() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        Mutable<Integer> timesBackPressed = new Mutable<>(0);
        Toolbar.OnBackListener listener = () -> {
            timesBackPressed.value++;
            return false;
        };

        // Registering a second time shouldn't do anything
        mToolbar.registerOnBackListener(listener);
        mToolbar.registerOnBackListener(listener);
        pressBack();

        assertThat(timesBackPressed.value).isEqualTo(1);
    }

    @Test
    public void unregisterOnBackListener_previouslyRegisteredListener_shouldUnregister() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        Mutable<Integer> timesBackPressed = new Mutable<>(0);
        Toolbar.OnBackListener listener = () -> {
            timesBackPressed.value++;
            return false;
        };

        mToolbar.registerOnBackListener(listener);
        mToolbar.unregisterOnBackListener(listener);
        pressBack();

        assertThat(timesBackPressed.value).isEqualTo(0);
    }

    @Test
    public void menuItems_builder_id() {
        MenuItem item = MenuItem.builder(mContext)
                .setId(5)
                .build();

        assertThat(item.getId()).isEqualTo(5);
    }

    @Test
    public void menuItems_setId_shouldWork() {
        MenuItem item = MenuItem.builder(mContext).build();

        assertThat(item.getId()).isEqualTo(View.NO_ID);

        item.setId(7);

        assertThat(item.getId()).isEqualTo(7);
    }

    @Test
    public void menuItems_whenClicked_shouldCallListener() {
        assertThat(getMenuItemCount()).isEqualTo(0);

        Mutable<Boolean> button1Clicked = new Mutable<>(false);
        Mutable<Boolean> button2Clicked = new Mutable<>(false);
        mToolbar.setMenuItems(Arrays.asList(
                createMenuItem(i -> button1Clicked.value = true),
                createMenuItem(i -> button2Clicked.value = true)));

        assertThat(getMenuItemCount()).isEqualTo(2);

        getMenuItemView(0).performClick();

        assertThat(button1Clicked.value).isTrue();

        getMenuItemView(1).performClick();

        assertThat(button2Clicked.value).isTrue();
    }

    @Test
    public void menuItems_null_shouldRemoveExistingMenuItems() {
        mToolbar.setMenuItems(Arrays.asList(
                createMenuItem(i -> {
                }),
                createMenuItem(i -> {
                })));

        assertThat(getMenuItemCount()).isEqualTo(2);

        mToolbar.setMenuItems(null);

        assertThat(getMenuItemCount()).isEqualTo(0);
    }

    @Test
    public void menuItems_setVisibility_shouldDefaultToShown() {
        MenuItem item = createMenuItem(i -> {
        });
        mToolbar.setMenuItems(Collections.singletonList(item));

        assertThat(getMenuItemView(0).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void menuItems_setVisibility_shouldHide() {
        MenuItem item = createMenuItem(i -> {
        });
        mToolbar.setMenuItems(Collections.singletonList(item));

        item.setVisible(false);
        assertThat(getMenuItemView(0).getVisibility()).isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void menuItems_setVisibility_shouldReshowAfterHiding() {
        MenuItem item = createMenuItem(i -> {
        });
        mToolbar.setMenuItems(Collections.singletonList(item));

        item.setVisible(false);
        item.setVisible(true);
        assertThat(getMenuItemView(0).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void menuItems_equalItems_shouldntRecreateViews() {
        List<MenuItem> menuItems = Arrays.asList(
                createMenuItem(i -> {
                }),
                createMenuItem(i -> {
                }));
        mToolbar.setMenuItems(menuItems);

        assertThat(getMenuItemCount()).isEqualTo(2);

        View firstMenuItemView = getMenuItemView(0);

        mToolbar.setMenuItems(menuItems);

        assertThat(firstMenuItemView).isSameAs(getMenuItemView(0));
    }

    @Test
    public void menuItems_searchScreen_shouldHideMenuItems() {
        mToolbar.setMenuItems(Arrays.asList(
                MenuItem.builder(mContext).setToSearch().build(),
                createMenuItem(i -> {
                })));

        mToolbar.setShowMenuItemsWhileSearching(false);
        mToolbar.setState(Toolbar.State.SEARCH);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_menu_items_container).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void menuItems_showMenuItemsWhileSearching() {
        mToolbar.setMenuItems(Arrays.asList(
                MenuItem.builder(mContext).setToSearch().build(),
                createMenuItem(i -> {
                })));

        mToolbar.setShowMenuItemsWhileSearching(true);
        mToolbar.setState(Toolbar.State.SEARCH);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_menu_items_container).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(getMenuItemView(0).getVisibility()).isNotEqualTo(View.VISIBLE);
        assertThat(getMenuItemView(1).getVisibility()).isEqualTo(View.VISIBLE);
    }

    private MenuItem createMenuItem(MenuItem.OnClickListener listener) {
        return MenuItem.builder(mContext)
                .setTitle("Button!")
                .setOnClickListener(listener)
                .build();
    }

    private int getMenuItemCount() {
        return mToolbar.getMenuItems().size();
    }

    private View getMenuItemView(int index) {
        return ((ViewGroup) mToolbar
                .findViewById(R.id.car_ui_toolbar_menu_items_container))
                .getChildAt(index);
    }

    private void pressBack() {
        mToolbar.findViewById(R.id.car_ui_toolbar_nav_icon_container).performClick();
    }

    private static class Mutable<T> {
        public T value;

        Mutable(T value) {
            this.value = value;
        }
    }

}
