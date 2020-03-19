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

import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;

import java.util.List;

/**
 * An interface for accessing a Chassis Toolbar, regardless of how the underlying
 * views are represented.
 */
public interface ToolbarController {

    /**
     * Returns {@code true} if a two row layout in enabled for the toolbar.
     */
    boolean isTabsInSecondRow();

    /**
     * Sets the title of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    void setTitle(@StringRes int title);

    /**
     * Sets the title of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    void setTitle(CharSequence title);

    /**
     * Gets the current toolbar title.
     */
    CharSequence getTitle();

    /**
     * Gets the {@link TabLayout} for this toolbar.
     */
    TabLayout getTabLayout();

    /**
     * Adds a tab to this toolbar. You can listen for when it is selected via
     * {@link #registerOnTabSelectedListener(Toolbar.OnTabSelectedListener)}.
     */
    void addTab(TabLayout.Tab tab);

    /** Removes all the tabs. */
    void clearAllTabs();

    /**
     * Gets a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    TabLayout.Tab getTab(int position);

    /**
     * Selects a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    void selectTab(int position);

    /**
     * Sets whether or not tabs should also be shown in the SUBPAGE {@link Toolbar.State}.
     */
    void setShowTabsInSubpage(boolean showTabs);

    /**
     * Gets whether or not tabs should also be shown in the SUBPAGE {@link Toolbar.State}.
     */
    boolean getShowTabsInSubpage();

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    void setLogo(@DrawableRes int resId);

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    void setLogo(Drawable drawable);

    /** Sets the hint for the search bar. */
    void setSearchHint(@StringRes int resId);

    /** Sets the hint for the search bar. */
    void setSearchHint(CharSequence hint);

    /** Gets the search hint */
    CharSequence getSearchHint();

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    void setSearchIcon(@DrawableRes int resId);

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    void setSearchIcon(Drawable d);


    /** Sets the {@link Toolbar.NavButtonMode} */
    void setNavButtonMode(Toolbar.NavButtonMode style);

    /** Gets the {@link Toolbar.NavButtonMode} */
    Toolbar.NavButtonMode getNavButtonMode();

    /** Show/hide the background. When hidden, the toolbar is completely transparent. */
    void setBackgroundShown(boolean shown);

    /** Returns true is the toolbar background is shown */
    boolean getBackgroundShown();

    /**
     * Sets the {@link MenuItem Menuitems} to display.
     */
    void setMenuItems(@Nullable List<MenuItem> items);

    /**
     * Sets the {@link MenuItem Menuitems} to display to a list defined in XML.
     *
     * <p>If this method is called twice with the same argument (and {@link #setMenuItems(List)}
     * wasn't called), nothing will happen the second time, even if the MenuItems were changed.
     *
     * <p>The XML file must have one <MenuItems> tag, with a variable number of <MenuItem>
     * child tags. See CarUiToolbarMenuItem in CarUi's attrs.xml for a list of available attributes.
     *
     * Example:
     * <pre>
     * <MenuItems>
     *     <MenuItem
     *         app:title="Foo"/>
     *     <MenuItem
     *         app:title="Bar"
     *         app:icon="@drawable/ic_tracklist"
     *         app:onClick="xmlMenuItemClicked"/>
     *     <MenuItem
     *         app:title="Bar"
     *         app:checkable="true"
     *         app:uxRestrictions="FULLY_RESTRICTED"
     *         app:onClick="xmlMenuItemClicked"/>
     * </MenuItems>
     * </pre>
     *
     * @return The MenuItems that were loaded from XML.
     * @see #setMenuItems(List)
     */
    List<MenuItem> setMenuItems(@XmlRes int resId);

    /** Gets the {@link MenuItem MenuItems} currently displayed */
    @NonNull
    List<MenuItem> getMenuItems();

    /** Gets a {@link MenuItem} by id. */
    @Nullable
    MenuItem findMenuItemById(int id);

    /** Gets a {@link MenuItem} by id. Will throw an IllegalArgumentException if not found. */
    @NonNull
    MenuItem requireMenuItemById(int id);

    /**
     * Set whether or not to show the {@link MenuItem MenuItems} while searching. Default false.
     * Even if this is set to true, the {@link MenuItem} created by
     * {@link MenuItem.Builder#setToSearch()} will still be hidden.
     */
    void setShowMenuItemsWhileSearching(boolean showMenuItems);

    /** Returns if {@link MenuItem MenuItems} are shown while searching */
    boolean getShowMenuItemsWhileSearching();

    /**
     * Sets the search query.
     */
    void setSearchQuery(String query);

    /**
     * Sets the state of the toolbar. This will show/hide the appropriate elements of the toolbar
     * for the desired state.
     */
    void setState(Toolbar.State state);

    /** Gets the current {@link Toolbar.State} of the toolbar. */
    Toolbar.State getState();

    /**
     * Registers a new {@link Toolbar.OnHeightChangedListener} to the list of listeners. Register a
     * {@link com.android.car.ui.recyclerview.CarUiRecyclerView} only if there is a toolbar at
     * the top and a {@link com.android.car.ui.recyclerview.CarUiRecyclerView} in the view and
     * nothing else. {@link com.android.car.ui.recyclerview.CarUiRecyclerView} will
     * automatically adjust its height according to the height of the Toolbar.
     */
    void registerToolbarHeightChangeListener(Toolbar.OnHeightChangedListener listener);

    /** Unregisters an existing {@link Toolbar.OnHeightChangedListener} from the list of
     * listeners. */
    boolean unregisterToolbarHeightChangeListener(Toolbar.OnHeightChangedListener listener);

    /** Registers a new {@link Toolbar.OnTabSelectedListener} to the list of listeners. */
    void registerOnTabSelectedListener(Toolbar.OnTabSelectedListener listener);

    /** Unregisters an existing {@link Toolbar.OnTabSelectedListener} from the list of listeners. */
    boolean unregisterOnTabSelectedListener(Toolbar.OnTabSelectedListener listener);

    /** Registers a new {@link Toolbar.OnSearchListener} to the list of listeners. */
    void registerOnSearchListener(Toolbar.OnSearchListener listener);

    /** Unregisters an existing {@link Toolbar.OnSearchListener} from the list of listeners. */
    boolean unregisterOnSearchListener(Toolbar.OnSearchListener listener);

    /** Registers a new {@link Toolbar.OnSearchCompletedListener} to the list of listeners. */
    void registerOnSearchCompletedListener(Toolbar.OnSearchCompletedListener listener);

    /** Unregisters an existing {@link Toolbar.OnSearchCompletedListener} from the list of
     * listeners. */
    boolean unregisterOnSearchCompletedListener(Toolbar.OnSearchCompletedListener listener);

    /** Registers a new {@link Toolbar.OnBackListener} to the list of listeners. */
    void registerOnBackListener(Toolbar.OnBackListener listener);

    /** Unregisters an existing {@link Toolbar.OnBackListener} from the list of listeners. */
    boolean unregisterOnBackListener(Toolbar.OnBackListener listener);

    /** Shows the progress bar */
    void showProgressBar();

    /** Hides the progress bar */
    void hideProgressBar();

    /** Returns the progress bar */
    ProgressBar getProgressBar();
}
