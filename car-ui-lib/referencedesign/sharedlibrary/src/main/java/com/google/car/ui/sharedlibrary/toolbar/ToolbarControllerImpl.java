/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.google.car.ui.sharedlibrary.toolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.MenuItemOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.ProgressBarControllerOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.SearchItemOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.TabOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1;

import com.google.car.ui.sharedlibrary.R;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("AndroidJdkLibsChecker")
class ToolbarControllerImpl implements ToolbarControllerOEMV1 {

    private final Context mSharedLibraryContext;
    private final ImageView mBackButtonView;
    private final TextView mTitleView;
    private final TextView mSubtitleView;
    private final ImageView mLogo;
    private final ImageView mLogoInNavIconSpace;
    private final ProgressBarController mProgressBar;
    private final TabLayout mTabContainer;
    private final ViewGroup mMenuItemsContainer;

    private Runnable mBackListener;
    private int mNavButtonMode = ToolbarControllerOEMV1.NAV_BUTTON_MODE_BACK;

    private boolean mBackButtonVisible = false;
    private boolean mHasLogo = false;
    private int mSearchMode = ToolbarControllerOEMV1.SEARCH_MODE_DISABLED;
    private final OverflowMenuItem mOverflowMenuItem;

    ToolbarControllerImpl(View view, Context sharedLibraryContext, Context activityContext) {
        mSharedLibraryContext = sharedLibraryContext;
        mBackButtonView = view.requireViewById(R.id.toolbar_nav_icon);
        mTitleView = view.requireViewById(R.id.toolbar_title);
        mSubtitleView = view.requireViewById(R.id.toolbar_subtitle);
        mLogo = view.requireViewById(R.id.toolbar_title_logo);
        mLogoInNavIconSpace = view.requireViewById(R.id.toolbar_logo);
        mProgressBar = new ProgressBarController(
                view.requireViewById(R.id.toolbar_progress_bar));
        mTabContainer = view.requireViewById(R.id.toolbar_tabs);
        mMenuItemsContainer = view.requireViewById(R.id.toolbar_menu_items_container);
        mOverflowMenuItem = new OverflowMenuItem(sharedLibraryContext, activityContext);
    }

    @Override
    public void setTitle(String title) {
        boolean hadTitle = !TextUtils.isEmpty(getTitle());
        mTitleView.setText(title);
        boolean hasTitle = !TextUtils.isEmpty(getTitle());

        if (hadTitle != hasTitle) {
            update();
        }
    }

    private CharSequence getTitle() {
        return mTitleView.getText();
    }

    @Override
    public void setSubtitle(String title) {
        boolean hadSubtitle = !TextUtils.isEmpty(getSubtitle());
        mSubtitleView.setText(title);
        boolean hasSubtitle = !TextUtils.isEmpty(getSubtitle());

        if (hadSubtitle != hasSubtitle) {
            update();
        }
    }

    private CharSequence getSubtitle() {
        return mSubtitleView.getText();
    }

    @Override
    public void setTabs(List<? extends TabOEMV1> tabs, int selectedTab) {
        if (tabs == null) {
            tabs = Collections.emptyList();
        }

        boolean hadTabs = mTabContainer.hasTabs();
        mTabContainer.setTabs(tabs, selectedTab);
        boolean hasTabs = mTabContainer.hasTabs();

        if (hadTabs != hasTabs) {
            update();
        }
    }

    @Override
    public void selectTab(int position) {
        mTabContainer.selectTab(position);
    }

    @Override
    public void setLogo(Drawable drawable) {
        mLogo.setImageDrawable(drawable);
        mLogoInNavIconSpace.setImageDrawable(drawable);

        boolean hasLogo = drawable != null;
        if (hasLogo != mHasLogo) {
            mHasLogo = hasLogo;
            update();
        }
    }

    @Override
    public void setSearchHint(String hint) {

    }

    @Override
    public void setBackButtonVisible(boolean visible) {
        if (visible != mBackButtonVisible) {
            mBackButtonVisible = visible;
            mBackButtonView.setOnClickListener(mBackButtonVisible ? v -> {
                if (mBackListener != null) {
                    mBackListener.run();
                }
            } : null);
            mBackButtonView.setClickable(mBackButtonVisible);
            update();
        }
    }

    @Override
    public boolean isBackButtonVisible() {
        return mBackButtonVisible;
    }

    @Override
    public void setSearchIcon(Drawable d) {

    }

    @Override
    public void setSearchQuery(String query) {

    }

    @Override
    public void setSearchMode(int searchMode) {
        if (mSearchMode != searchMode) {
            mSearchMode = searchMode;
            update();
        }
    }

    @Override
    public boolean canShowSearchResultItems() {
        return false;
    }

    @Override
    public boolean canShowSearchResultsView() {
        return false;
    }

    @Override
    public void setSearchResultsView(View view) {
        // Intentional no-op as canShowSearchResultsView returns false
    }

    @Override
    public void setSearchResultItems(List<? extends SearchItemOEMV1> searchItems) {
        // Intentional no-op as canShowSearchResultItems returns false
    }

    @Override
    public void setNavButtonMode(int mode) {
        mNavButtonMode = mode;
        switch (mNavButtonMode) {
            case ToolbarControllerOEMV1.NAV_BUTTON_MODE_CLOSE:
                mBackButtonView.setImageResource(R.drawable.icon_close);
                break;
            case ToolbarControllerOEMV1.NAV_BUTTON_MODE_DOWN:
                mBackButtonView.setImageResource(R.drawable.icon_down);
                break;
            default:
                mBackButtonView.setImageResource(R.drawable.icon_back);
                break;
        }
    }

    @Override
    public int getNavButtonMode() {
        return mNavButtonMode;
    }

    @Override
    public void setMenuItems(List<? extends MenuItemOEMV1> menuItems) {
        if (menuItems == null) {
            menuItems = Collections.emptyList();
        }

        List<? extends MenuItemOEMV1> overflowMenuItems = menuItems.stream()
                .filter(i -> i.getDisplayBehavior() != MenuItemOEMV1.DISPLAY_BEHAVIOR_ALWAYS)
                .collect(Collectors.toList());

        List<? extends MenuItemOEMV1> regularMenuItems = Stream.concat(
                menuItems.stream().filter(
                        i -> i.getDisplayBehavior() == MenuItemOEMV1.DISPLAY_BEHAVIOR_ALWAYS),
                Stream.of(mOverflowMenuItem))
                .collect(Collectors.toList());

        mOverflowMenuItem.setOverflowMenuItems(overflowMenuItems);

        mMenuItemsContainer.removeAllViews();
        for (MenuItemOEMV1 menuItem : regularMenuItems) {
            MenuItemView menuItemView = new MenuItemView(mSharedLibraryContext, menuItem);
            mMenuItemsContainer.addView(menuItemView,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    @Override
    public List<? extends MenuItemOEMV1> getMenuItems() {
        return null;
    }

    @Override
    public void setSearchListener(Consumer<String> listener) {

    }

    @Override
    public void setSearchCompletedListener(Runnable listener) {

    }

    @Override
    public void setBackListener(Runnable listener) {
        mBackListener = listener;
    }

    @Override
    public ProgressBarControllerOEMV1 getProgressBar() {
        return mProgressBar;
    }

    private void update() {
        setVisible(mBackButtonView, mBackButtonVisible);
        setVisible(mLogoInNavIconSpace, mHasLogo && !mBackButtonVisible);
        setVisible(mLogo, mHasLogo && mBackButtonVisible);
        boolean hasTabs = mTabContainer.hasTabs();
        setVisible(mTabContainer, hasTabs);
        setVisible(mTitleView, !TextUtils.isEmpty(getTitle()) && !hasTabs);
        setVisible(mSubtitleView, !TextUtils.isEmpty(getSubtitle()) && !hasTabs);
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
