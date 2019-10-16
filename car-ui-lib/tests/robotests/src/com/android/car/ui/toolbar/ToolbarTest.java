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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.ui.CarUiRobolectricTestRunner;
import com.android.car.ui.R;
import com.android.car.ui.utils.ShadowTypeface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(CarUiRobolectricTestRunner.class)
@Config(qualifiers = "land", shadows = ShadowTypeface.class)
public class ToolbarTest {

    private Context mContext;
    private ActivityController<TestActivity> mActivityController;
    private TestActivity mActivity;
    private Toolbar mToolbar;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mActivityController = Robolectric.buildActivity(TestActivity.class);
        mActivityController.setup();

        mActivity = mActivityController.get();
        mToolbar = mActivity.findViewById(R.id.toolbar);
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
    public void configurationChange_shouldNotLoseProperties() {
        mToolbar.setTitle("Foo");
        mToolbar.setSearchHint("Foo2");
        mToolbar.setBackgroundShown(false);
        mToolbar.setShowMenuItemsWhileSearching(true);
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);

        // TODO this is supposed to change the configuration, but doesn't
        RuntimeEnvironment.setQualifiers("+port");
        mActivityController.configurationChange();
        mActivity = mActivityController.get();
        mToolbar = mActivity.findViewById(R.id.toolbar);

        assertThat(mToolbar.getTitle().toString()).isEqualTo("Foo");
        assertThat(mToolbar.getSearchHint().toString()).isEqualTo("Foo2");
        assertThat(mToolbar.getBackgroundShown()).isEqualTo(false);
        assertThat(mToolbar.getShowMenuItemsWhileSearching()).isEqualTo(true);
        assertThat(mToolbar.getState()).isEquivalentAccordingToCompareTo(Toolbar.State.SUBPAGE);
        assertThat(mToolbar.getNavButtonMode()).isEquivalentAccordingToCompareTo(
                Toolbar.NavButtonMode.CLOSE);
    }

    @Test
    public void setCustomView_shouldInflateViewIntoToolbar() {
        mToolbar.setCustomView(R.layout.test_custom_view);

        View v = mToolbar.findViewById(R.id.text_box_1);

        assertThat(v).isNotNull();
        assertThat(mToolbar.getState()).isEquivalentAccordingToCompareTo(
                Toolbar.State.SUBPAGE_CUSTOM);
    }

    @Test
    public void showLogo_whenSet_andStateIsHome() {
        mToolbar.setState(Toolbar.State.HOME);
        mToolbar.setLogo(R.drawable.test_ic_launcher);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).isShown()).isTrue();
    }

    @Test
    public void hideLogo_whenSet_andStateIsNotHome() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setLogo(R.drawable.test_ic_launcher);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).isShown()).isFalse();
    }

    @Test
    public void hideLogo_whenNotSet_andStateIsHome() {
        mToolbar.setState(Toolbar.State.HOME);
        mToolbar.setLogo(0);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).isShown()).isFalse();
    }

    @Test
    public void hideLogo_whenNotSet_andStateIsNotHome() {
        mToolbar.setState(Toolbar.State.SUBPAGE);
        mToolbar.setLogo(0);

        assertThat(mToolbar.findViewById(R.id.car_ui_toolbar_logo).isShown()).isFalse();
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
        assertThat(mActivity.getTimesBackPressed()).isEqualTo(1);
    }

    @Test
    public void registerOnBackListener_whenAListenerReturnsTrue_shouldSuppressBack() {
        mToolbar.setState(Toolbar.State.SUBPAGE);

        mToolbar.registerOnBackListener(() -> true);
        pressBack();
        mToolbar.registerOnBackListener(() -> false);
        pressBack();

        assertThat(mActivity.getTimesBackPressed()).isEqualTo(0);
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
    public void menuItems_whenClicked_shouldCallListener() {
        assertThat(getMenuItemViewCount()).isEqualTo(0);

        Mutable<Boolean> button1Clicked = new Mutable<>(false);
        Mutable<Boolean> button2Clicked = new Mutable<>(false);
        mToolbar.setMenuItems(Arrays.asList(
                createMenuItem(i -> button1Clicked.value = true),
                createMenuItem(i -> button2Clicked.value = true)));

        assertThat(getMenuItemViewCount()).isEqualTo(2);

        getMenuItemView(0).performClick();

        assertThat(button1Clicked.value).isTrue();

        getMenuItemView(1).performClick();

        assertThat(button2Clicked.value).isTrue();
    }

    @Test
    public void menuItems_null_shouldRemoveExistingMenuItems() {
        mToolbar.setMenuItems(Arrays.asList(
                createMenuItem(i -> { }),
                createMenuItem(i -> { })));

        assertThat(getMenuItemViewCount()).isEqualTo(2);

        mToolbar.setMenuItems(null);

        assertThat(getMenuItemViewCount()).isEqualTo(0);
    }

    @Test
    public void menuItems_setVisibility_shouldDefaultToShown() {
        MenuItem item = createMenuItem(i -> { });
        mToolbar.setMenuItems(Collections.singletonList(item));

        assertThat(getMenuItemView(0).isShown()).isTrue();
    }

    @Test
    public void menuItems_setVisibility_shouldHide() {
        MenuItem item = createMenuItem(i -> { });
        mToolbar.setMenuItems(Collections.singletonList(item));

        item.setVisible(false);
        assertThat(getMenuItemView(0).isShown()).isFalse();
    }

    @Test
    public void menuItems_setVisibility_shouldReshowAfterHiding() {
        MenuItem item = createMenuItem(i -> { });
        mToolbar.setMenuItems(Collections.singletonList(item));

        item.setVisible(false);
        item.setVisible(true);
        assertThat(getMenuItemView(0).isShown()).isTrue();
    }

    @Test
    public void menuItems_equalItems_shouldntRecreateViews() {
        List<MenuItem> menuItems = Arrays.asList(
                createMenuItem(i -> { }),
                createMenuItem(i -> { }));
        mToolbar.setMenuItems(menuItems);

        assertThat(getMenuItemViewCount()).isEqualTo(2);

        View firstMenuItemView = getMenuItemView(0);

        mToolbar.setMenuItems(menuItems);

        assertThat(firstMenuItemView).isSameAs(getMenuItemView(0));
    }

    @Test
    public void menuItems_searchScreen_shouldHideMenuItems() {
        mToolbar.setMenuItems(Arrays.asList(
                MenuItem.Builder.createSearch(mContext, i -> { }),
                createMenuItem(i -> { })));

        mToolbar.setShowMenuItemsWhileSearching(false);
        mToolbar.setState(Toolbar.State.SEARCH);

        assertThat(getMenuItemView(0).isShown()).isFalse();
        assertThat(getMenuItemView(1).isShown()).isFalse();
    }

    @Test
    public void menuItems_showMenuItemsWhileSearching() {
        mToolbar.setMenuItems(Arrays.asList(
                MenuItem.Builder.createSearch(mContext, i -> { }),
                createMenuItem(i -> { })));

        mToolbar.setShowMenuItemsWhileSearching(true);
        mToolbar.setState(Toolbar.State.SEARCH);

        assertThat(getMenuItemView(0).isShown()).isFalse();
        assertThat(getMenuItemView(1).isShown()).isTrue();
    }

    private MenuItem createMenuItem(MenuItem.OnClickListener listener) {
        return new MenuItem.Builder(mContext)
                .setTitle("Button!")
                .setOnClickListener(listener)
                .build();
    }

    private int getMenuItemViewCount() {
        return ((ViewGroup) mToolbar
                .findViewById(R.id.car_ui_toolbar_menu_items_container))
                .getChildCount();
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
