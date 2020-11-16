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

package com.android.car.ui.sharedlibrary.oemapis.toolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.List;
import java.util.function.Consumer;

/** The OEM interface for a Toolbar. */
@SuppressWarnings("AndroidJdkLibsChecker")
public interface ToolbarControllerOEMV1 {

    /** Gets the context used by the views of this toolbar */
    Context getContext();

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
     * Sets the subtitle of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example with one row layout with tabs.
     */
    void setSubtitle(CharSequence title);

    /**
     * Gets the current toolbar subtitle.
     */
    CharSequence getSubtitle();

    /**
     * Sets the tab to be shown. The implementation must copy the list once it's passed in,
     * or else the list could be modified from the app when the toolbar wasn't expecting it.
     *
     * @param tabs Nullable. Must not be mutated. List of tabs to show.
     */
    void setTabs(List<? extends TabOEMV1> tabs);

    /**
     * Selects a tab added to this toolbar. See
     * {@link #setTabs(List)}.
     */
    void selectTab(int position);

    /**
     * Sets the logo to display in this toolbar. If navigation icon is being displayed, this logo
     * will be displayed next to the title.
     */
    void setLogo(Drawable drawable);

    /** Sets the hint for the search bar. */
    void setSearchHint(CharSequence hint);

    /** Gets the search hint */
    CharSequence getSearchHint();

    /** Sets whether or not to display the back button */
    void setBackButtonVisible(boolean visible);

    /** Returns true if the back button is displayed to the user */
    boolean isBackButtonVisible();

    /**
     * Sets the icon to display in the search box.
     *
     * <p>The icon will be lost on configuration change, make sure to set it in onCreate() or
     * a similar place.
     */
    void setSearchIcon(Drawable d);

    /**
     * Sets the search query.
     */
    void setSearchQuery(CharSequence query);

    int SEARCH_MODE_DISABLED = 0;
    int SEARCH_MODE_SEARCH = 1;
    int SEARCH_MODE_EDIT = 2;

    /**
     * Sets the search mode, which is whether or not to display the search bar and how it should
     * look.
     */
    void setSearchMode(int searchMode);

    /** Sets whether or not to show MenuItems while the search bar is open */
    void setShowMenuItemsWhileSearching(boolean showMenuItemsWhileSearching);

    /** Returns whether or not to MenuItems are shown while the search bar is open */
    boolean isShowingMenuItemsWhileSearching();

    /**
     * Returns true if the toolbar can display search result items. One example of this is when the
     * system is configured to display search items in the IME instead of in the app.
     */
    boolean canShowSearchResultItems();

    /**
     * Returns true if the app is allowed to set search results view.
     */
    boolean canShowSearchResultsView();

    /**
     * Add a view within a container that will animate with the wide screen IME to display search
     * results.
     *
     * <p>Note: Apps can only call this method if the package name is allowed via OEM to render
     * their view.  To check if the application have the permission to do so or not first call
     * {@link #canShowSearchResultsView()}. If the app is not allowed this method will throw an
     * {@link IllegalStateException}
     *
     * @param view to be added in the container.
     */
    void setSearchResultsView(View view);

    /**
     * Sets list of search item {@link SearchItemOEMV1} to be displayed in the IMS
     * template. This method should be called when system is running in a wide screen mode. Apps
     * can check that by using {@link #canShowSearchResultItems()}
     * Else, this method will throw an {@link IllegalStateException}
     */
    void setSearchResultItems(List<? extends SearchItemOEMV1> searchItems);

    /** Display the nav button as a back button */
    int NAV_BUTTON_MODE_BACK = 0;
    /** Display the nav button as a close button */
    int NAV_BUTTON_MODE_CLOSE = 1;
    /**
     * Display the nav button as a "down" button.
     * This indicates that pressing it will slide a panel down to close it.
     */
    int NAV_BUTTON_MODE_DOWN = 2;

    /**
     * Sets the nav button mode, which is a certain style to display the nav button in.
     * These styles are all purely visual, and don't affect the behavior of clicking
     * the nav button.
     *
     * See {@link #NAV_BUTTON_MODE_BACK}, {@link #NAV_BUTTON_MODE_CLOSE},
     * and {@link #NAV_BUTTON_MODE_DOWN}.
     */
    void setNavButtonMode(int mode);

    /** Gets the nav button mode. See {@link #setNavButtonMode(int)} for more info. */
    int getNavButtonMode();

    /**
     * Sets the {@link MenuItemOEMV1 Menuitems} to display.
     */
    void setMenuItems(List<? extends MenuItemOEMV1> items);

    /** Gets the {@link MenuItemOEMV1 MenuItems} currently displayed */
    List<? extends MenuItemOEMV1> getMenuItems();

    /**
     * Sets a {@link Consumer<CharSequence>} to be called whenever the text in the search box
     * changes.
     *
     * Must accept {@code null} to unset the listener.
     */
    void setSearchListener(Consumer<CharSequence> listener);

    /**
     * Sets a {@link Runnable} to be called whenever the user indicates that they're done searching.
     * This can be by clicking the search/enter button on the keyboard, or a custom button
     * on the toolbar.
     *
     * Must accept {@code null} to unset the listener.
     */
    void setSearchCompletedListener(Runnable listener);

    /**
     * Sets a {@link Runnable} to be called whenever the back button is pressed.
     *
     * Must accept {@code null} to unset the listener.
     */
    void setBackListener(Runnable listener);

    /** Gets a {@link ProgressBarControllerOEMV1 ProgressBarController} */
    ProgressBarControllerOEMV1 getProgressBar();
}
