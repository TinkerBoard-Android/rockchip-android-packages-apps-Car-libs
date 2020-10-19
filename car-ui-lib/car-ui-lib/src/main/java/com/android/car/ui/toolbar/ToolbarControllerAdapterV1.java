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

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1;
import com.android.car.ui.toolbar.TabLayout.Tab;
import com.android.car.ui.toolbar.Toolbar.NavButtonMode;
import com.android.car.ui.toolbar.Toolbar.OnBackListener;
import com.android.car.ui.toolbar.Toolbar.OnHeightChangedListener;
import com.android.car.ui.toolbar.Toolbar.OnSearchCompletedListener;
import com.android.car.ui.toolbar.Toolbar.OnSearchListener;
import com.android.car.ui.toolbar.Toolbar.OnTabSelectedListener;
import com.android.car.ui.toolbar.Toolbar.State;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Adapts a {@link com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1}
 * into a {@link ToolbarController}
 */
public final class ToolbarControllerAdapterV1 implements ToolbarController {

    private static final String TAG = ToolbarControllerAdapterV1.class.getName();

    private ToolbarControllerOEMV1 mOemToolbar;
    private Context mContext;

    private final List<TabAdapterV1> mTabs = new ArrayList<>();
    private State mState = State.HOME;
    private CharSequence mTitle = null;
    private CharSequence mSubtitle = null;
    private Drawable mLogo = null;
    private boolean mShowTabsInSubpage = false;
    private final Set<OnTabSelectedListener> mOnTabSelectedListeners = new HashSet<>();
    private final Set<OnBackListener> mOnBackListeners = new HashSet<>();
    private final Set<OnSearchListener> mOnSearchListeners = new HashSet<>();
    private final Set<OnSearchCompletedListener> mOnSearchCompletedListeners = new HashSet<>();
    private final ProgressBarControllerAdapterV1 mProgressBar;

    public ToolbarControllerAdapterV1(@NonNull ToolbarControllerOEMV1 oemToolbar) {
        mOemToolbar = oemToolbar;
        mProgressBar = new ProgressBarControllerAdapterV1(mOemToolbar.getProgressBar());
        mContext = oemToolbar.getContext();

        Activity activity = CarUiUtils.getActivity(mContext);

        oemToolbar.setBackListener(() -> {
            boolean handled = false;
            for (OnBackListener listener : mOnBackListeners) {
                handled |= listener.onBack();
            }
            if (!handled && activity != null) {
                activity.onBackPressed();
            }
        });

        oemToolbar.setSearchListener(query -> {
            for (OnSearchListener listener : mOnSearchListeners) {
                listener.onSearch(query.toString());
            }
        });
        oemToolbar.setSearchCompletedListener(() -> {
            for (OnSearchCompletedListener listener : mOnSearchCompletedListeners) {
                listener.onSearchCompleted();
            }
        });
    }

    @Override
    public boolean isTabsInSecondRow() {
        Log.w(TAG, "Unsupported operation isTabsInSecondRow() called, ignoring");
        return false;
    }

    @Override
    public void setTitle(int title) {
        setTitle(mContext.getString(title));
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (stateHasLogoTitleOrSubtitle(mState)) {
            mOemToolbar.setTitle(title);
        }
    }

    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    @Override
    public void setSubtitle(int title) {
        setSubtitle(mContext.getString(title));
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        if (stateHasLogoTitleOrSubtitle(mState)) {
            mOemToolbar.setSubtitle(subtitle);
        }
    }

    @Override
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    @Override
    public int getTabCount() {
        return mTabs.size();
    }

    @Override
    public int getTabPosition(Tab tab) {
        for (int i = 0; i < mTabs.size(); i++) {
            if (mTabs.get(i).getClientTab() == tab) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addTab(Tab clientTab) {
        mTabs.add(new TabAdapterV1(mContext, clientTab, () -> {
            for (OnTabSelectedListener listener : mOnTabSelectedListeners) {
                listener.onTabSelected(clientTab);
            }
        }));
        setState(getState());
    }

    @Override
    public void clearAllTabs() {
        mTabs.clear();
        setState(getState());
    }

    @Override
    public Tab getTab(int position) {
        TabAdapterV1 tab = mTabs.get(position);
        return tab.getClientTab();
    }

    @Override
    public void selectTab(int position) {
        mOemToolbar.selectTab(position);
    }

    @Override
    public void setShowTabsInSubpage(boolean showTabs) {
        mShowTabsInSubpage = showTabs;
        setState(getState());
    }

    @Override
    public boolean getShowTabsInSubpage() {
        return mShowTabsInSubpage;
    }

    @Override
    public void setLogo(int resId) {
        setLogo(mContext.getDrawable(resId));
    }

    @Override
    public void setLogo(Drawable drawable) {
        mLogo = drawable;
        if (stateHasLogoTitleOrSubtitle(mState)) {
            mOemToolbar.setLogo(drawable);
        }
    }

    @Override
    public void setSearchHint(int resId) {
        setSearchHint(mContext.getString(resId));
    }

    @Override
    public void setSearchHint(CharSequence hint) {
        mOemToolbar.setSearchHint(hint);
    }

    @Override
    public CharSequence getSearchHint() {
        return mOemToolbar.getSearchHint();
    }

    @Override
    public void setSearchIcon(int resId) {
        setSearchIcon(mContext.getDrawable(resId));
    }

    @Override
    public void setSearchIcon(Drawable d) {
        mOemToolbar.setSearchIcon(d);
    }

    @Override
    public void setNavButtonMode(NavButtonMode style) {
        switch (style) {
            case BACK:
                mOemToolbar.setNavButtonMode(ToolbarControllerOEMV1.NAV_BUTTON_MODE_BACK);
                break;
            case CLOSE:
                mOemToolbar.setNavButtonMode(ToolbarControllerOEMV1.NAV_BUTTON_MODE_CLOSE);
                break;
            case DOWN:
                mOemToolbar.setNavButtonMode(ToolbarControllerOEMV1.NAV_BUTTON_MODE_DOWN);
                break;
        }
    }

    @Override
    public NavButtonMode getNavButtonMode() {
        int mode = mOemToolbar.getNavButtonMode();
        switch (mode) {
            case ToolbarControllerOEMV1.NAV_BUTTON_MODE_CLOSE:
                return NavButtonMode.CLOSE;
            case ToolbarControllerOEMV1.NAV_BUTTON_MODE_DOWN:
                return NavButtonMode.DOWN;
            default:
                return NavButtonMode.BACK;
        }
    }

    @Override
    public void setBackgroundShown(boolean shown) {
        Log.w(TAG, "Unsupported operation setBackgroundShown() called, ignoring");
    }

    @Override
    public boolean getBackgroundShown() {
        return true;
    }

    @Override
    public void setMenuItems(@Nullable List<MenuItem> items) {
        setMenuItemsInternal(items);
    }

    @Override
    public List<MenuItem> setMenuItems(int resId) {
        List<MenuItem> menuItems = MenuItemRenderer.readMenuItemList(mContext, resId);
        setMenuItemsInternal(menuItems);
        return menuItems;
    }

    private void setMenuItemsInternal(@Nullable List<MenuItem> items) {
        mOemToolbar.setMenuItems(convertList(items, MenuItemAdapterV1::new));
    }

    @NonNull
    @Override
    public List<MenuItem> getMenuItems() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public MenuItem findMenuItemById(int id) {
        for (MenuItem item : getMenuItems()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public MenuItem requireMenuItemById(int id) {
        MenuItem result = findMenuItemById(id);

        if (result == null) {
            throw new IllegalArgumentException("ID does not reference a MenuItem on this Toolbar");
        }

        return result;
    }

    @Override
    public void setShowMenuItemsWhileSearching(boolean showMenuItems) {
        mOemToolbar.setShowMenuItemsWhileSearching(showMenuItems);
    }

    @Override
    public boolean getShowMenuItemsWhileSearching() {
        return mOemToolbar.isShowingMenuItemsWhileSearching();
    }

    @Override
    public void setSearchQuery(String query) {
        mOemToolbar.setSearchQuery(query);
    }

    @Override
    public void setState(State state) {
        boolean gainingLogoTitleOrSubtitle =
                stateHasLogoTitleOrSubtitle(state) && !stateHasLogoTitleOrSubtitle(mState);
        boolean losingLogoTitleOrSubtitle =
                !stateHasLogoTitleOrSubtitle(state) && stateHasLogoTitleOrSubtitle(mState);
        mState = state;

        if (gainingLogoTitleOrSubtitle) {
            mOemToolbar.setLogo(mLogo);
            mOemToolbar.setTitle(mTitle);
            mOemToolbar.setSubtitle(mSubtitle);
        } else if (losingLogoTitleOrSubtitle) {
            mOemToolbar.setLogo(null);
            mOemToolbar.setTitle(null);
            mOemToolbar.setSubtitle(null);
        }

        if (state == State.SEARCH) {
            mOemToolbar.setSearchMode(ToolbarControllerOEMV1.SEARCH_MODE_SEARCH);
        } else if (state == State.EDIT) {
            mOemToolbar.setSearchMode(ToolbarControllerOEMV1.SEARCH_MODE_EDIT);
        } else {
            mOemToolbar.setSearchMode(ToolbarControllerOEMV1.SEARCH_MODE_DISABLED);
        }

        mOemToolbar.setBackButtonVisible(state != State.HOME);

        if (state == State.HOME || (state == State.SUBPAGE && mShowTabsInSubpage)) {
            mOemToolbar.setTabs(mTabs);
        } else {
            mOemToolbar.setTabs(Collections.emptyList());
        }
    }

    private boolean stateHasLogoTitleOrSubtitle(State state) {
        return state == State.HOME || state == State.SUBPAGE;
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public void registerToolbarHeightChangeListener(OnHeightChangedListener listener) {
        Log.w(TAG, "Unsupported operation registerToolbarHeightChangeListener() called, ignoring");
    }

    @Override
    public boolean unregisterToolbarHeightChangeListener(OnHeightChangedListener listener) {
        Log.w(TAG,
                "Unsupported operation unregisterToolbarHeightChangeListener() called, ignoring");
        return false;
    }

    @Override
    public void registerOnTabSelectedListener(OnTabSelectedListener listener) {
        mOnTabSelectedListeners.add(listener);
    }

    @Override
    public boolean unregisterOnTabSelectedListener(OnTabSelectedListener listener) {
        return mOnTabSelectedListeners.remove(listener);
    }

    @Override
    public void registerOnSearchListener(OnSearchListener listener) {
        mOnSearchListeners.add(listener);
    }

    @Override
    public boolean unregisterOnSearchListener(OnSearchListener listener) {
        return mOnSearchListeners.remove(listener);
    }

    @Override
    public boolean canShowSearchResultItems() {
        return mOemToolbar.canShowSearchResultItems();
    }

    @Override
    public boolean canShowSearchResultsView() {
        return mOemToolbar.canShowSearchResultsView();
    }

    @Override
    public void setSearchResultsView(View view) {
        mOemToolbar.setSearchResultsView(view);
    }

    @Override
    public void setSearchResultItems(List<? extends CarUiImeSearchListItem> searchItems) {
        mOemToolbar.setSearchResultItems(convertList(searchItems, SearchItemAdapterV1::new));
    }

    @Override
    public void registerOnSearchCompletedListener(OnSearchCompletedListener listener) {
        mOnSearchCompletedListeners.add(listener);
    }

    @Override
    public boolean unregisterOnSearchCompletedListener(OnSearchCompletedListener listener) {
        return mOnSearchCompletedListeners.add(listener);
    }

    @Override
    public void registerOnBackListener(OnBackListener listener) {
        mOnBackListeners.add(listener);
    }

    @Override
    public boolean unregisterOnBackListener(OnBackListener listener) {
        return mOnBackListeners.remove(listener);
    }

    @Override
    public ProgressBarController getProgressBar() {
        return mProgressBar;
    }

    /**
     * Given a list of T and a function to convert from T to U, return a list of U.
     *
     * This will create a new list.
     */
    private <T, U> List<U> convertList(List<T> list, Function<T, U> f) {
        if (list == null) {
            return null;
        }

        List<U> result = new ArrayList<>();
        for (T item : list) {
            result.add(f.apply(item));
        }
        return result;
    }
}
