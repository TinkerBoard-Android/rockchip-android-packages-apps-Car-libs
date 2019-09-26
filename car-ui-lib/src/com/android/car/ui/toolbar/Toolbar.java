/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.car.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A toolbar for Android Automotive OS apps.
 *
 * <p>This isn't a toolbar in the android framework sense, it's merely a custom view that can be
 * added to a layout. (You can't call
 * {@link android.app.Activity#setActionBar(android.widget.Toolbar)} with it)
 *
 * <p>The toolbar supports a navigation button, title, tabs, search, and {@link MenuItem MenuItems}
 */
public class Toolbar extends FrameLayout {

    /** Callback that will be issued whenever the height of toolbar is changed. */
    public interface OnHeightChangedListener {
        /**
         * Will be called when the height of the toolbar is changed.
         * @param height new height of the toolbar
         */
        void onHeightChanged(int height);
    }

    /** Back button listener */
    public interface OnBackListener {
        /**
         * Invoked when the user clicks on the back button. By default, the toolbar will call
         * the Activity's {@link android.app.Activity#onBackPressed()}. Returning true from
         * this method will absorb the back press and prevent that behavior.
         */
        boolean onBack();
    }

    /** Tab selection listener */
    public interface OnTabSelectedListener {
        /** Called when a {@link TabLayout.Tab} is selected */
        void onTabSelected(TabLayout.Tab tab);
    }

    /** Search listener */
    public interface OnSearchListener {
        /**
         * Invoked when the user submits a search query.
         *
         * <p>This is called for every letter the user types, and also empty strings if the user
         * erases everything.
         */
        void onSearch(String query);
    }

    private static final String TAG = "CarUiToolbar";

    /** Enum of states the toolbar can be in. Controls what elements of the toolbar are displayed */
    public enum State {
        /**
         * In the HOME state, the logo will be displayed if there is one, and no navigation icon
         * will be displayed. The tab bar will be visible. The title will be displayed if there
         * is space. MenuItems will be displayed.
         */
        HOME,
        /**
         * In the SUBPAGE state, the logo will be replaced with a back button, the tab bar won't
         * be visible. The title and MenuItems will be displayed.
         */
        SUBPAGE,
        /**
         * In the SUBPAGE_CUSTOM state, everything is the same as SUBPAGE except the title will
         * be hidden and the custom view will be shown.
         */
        SUBPAGE_CUSTOM,
        /**
         * In the SEARCH state, only the back button and the search bar will be visible.
         */
        SEARCH,
    }

    private ImageView mNavIcon;
    private ImageView mLogo;
    private ViewGroup mNavIconContainer;
    private TextView mTitle;
    private TabLayout mTabLayout;
    private LinearLayout mMenuItemsContainer;
    private FrameLayout mCustomViewContainer;
    private View mOverflowButton;
    private final Set<OnBackListener> mOnBackListeners = new HashSet<>();
    private final Set<OnTabSelectedListener> mOnTabSelectedListeners = new HashSet<>();
    private final Set<OnHeightChangedListener> mOnHeightChangedListeners = new HashSet<>();
    private SearchView mSearchView;
    private boolean mHasLogo = false;
    private boolean mShowMenuItemsWhileSearching;
    private View mSearchButton;
    private State mState = State.HOME;
    private NavButtonMode mNavButtonMode = NavButtonMode.BACK;
    @NonNull
    private List<MenuItem> mMenuItems = Collections.emptyList();
    private List<MenuItem> mOverflowItems = new ArrayList<>();
    private MenuItem.Listener mMenuItemListener = (item, title) -> {
        if (item.getDisplayBehavior() == MenuItem.DisplayBehavior.NEVER) {
            createOverflowDialog();
        }
    };
    private AlertDialog mOverflowDialog;



    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.CarUiToolbarStyle);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.car_ui_toolbar, this, true);

        mTabLayout = requireViewById(R.id.tabs);
        mNavIcon = requireViewById(R.id.nav_icon);
        mLogo = requireViewById(R.id.logo);
        mNavIconContainer = requireViewById(R.id.nav_icon_container);
        mMenuItemsContainer = requireViewById(R.id.menu_items_container);
        mTitle = requireViewById(R.id.title);
        mSearchView = requireViewById(R.id.search_view);
        mCustomViewContainer = requireViewById(R.id.custom_view_container);
        mOverflowButton = requireViewById(R.id.car_ui_toolbar_overflow_button);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CarUiToolbar, defStyleAttr, defStyleRes);

        try {
            mTitle.setText(a.getString(R.styleable.CarUiToolbar_title));
            setLogo(a.getResourceId(R.styleable.CarUiToolbar_logo, 0));
            setBackgroundShown(a.getBoolean(R.styleable.CarUiToolbar_showBackground, true));
            mShowMenuItemsWhileSearching = a.getBoolean(
                    R.styleable.CarUiToolbar_showMenuItemsWhileSearching, false);
            String searchHint = a.getString(R.styleable.CarUiToolbar_searchHint);
            if (searchHint != null) {
                setSearchHint(searchHint);
            }

            switch (a.getInt(R.styleable.CarUiToolbar_state, 0)) {
                case 0:
                    setState(State.HOME);
                    break;
                case 1:
                    setState(State.SUBPAGE);
                    break;
                case 2:
                    setState(State.SUBPAGE_CUSTOM);
                    break;
                case 3:
                    setState(State.SEARCH);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unknown initial state");
                    }
                    break;
            }

            switch (a.getInt(R.styleable.CarUiToolbar_navButtonMode, 0)) {
                case 0:
                    setNavButtonMode(NavButtonMode.BACK);
                    break;
                case 1:
                    setNavButtonMode(NavButtonMode.CLOSE);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unknown navigation button style");
                    }
                    break;
            }
        } finally {
            a.recycle();
        }

        mTabLayout.addListener(new TabLayout.Listener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mOnTabSelectedListeners.forEach(listener -> listener.onTabSelected(tab));
            }
        });

        mOverflowButton.setOnClickListener(v -> {
            if (mOverflowDialog == null) {
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "Overflow dialog was null when trying to show it!");
                }
            } else {
                mOverflowDialog.show();
            }
        });
    }

    /**
     * Sets the title of the toolbar to a string resource.
     *
     * <p>The title may not always be shown, for example in landscape with tabs.
     */
    public void setTitle(@StringRes int title) {
        mTitle.setText(title);
    }

    /**
     * Sets the title of the toolbar to a CharSequence.
     *
     * <p>The title may not always be shown, for example in landscape with tabs.
     */
    public void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    /**
     * Gets the {@link TabLayout} for this toolbar.
     */
    public TabLayout getTabLayout() {
        return mTabLayout;
    }

    /**
     * Adds a tab to this toolbar. You can listen for when it is selected via
     * {@link #registerOnTabSelectedListener(OnTabSelectedListener)}.
     */
    public void addTab(TabLayout.Tab tab) {
        mTabLayout.addTab(tab);
    }

    /**
     * Gets a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    public TabLayout.Tab getTab(int position) {
        return mTabLayout.get(position);
    }

    /**
     * Selects a tab added to this toolbar. See
     * {@link #addTab(TabLayout.Tab)}.
     */
    public void selectTab(int position) {
        mTabLayout.selectTab(position);
    }

    /**
     * Sets the logo to display in this toolbar.
     * Will not be displayed if a navigation icon is currently being displayed.
     */
    public void setLogo(int resId) {
        if (resId != 0) {
            mLogo.setImageResource(resId);
            mHasLogo = true;
        } else {
            mHasLogo = false;
        }
        setState(mState);
    }

    /**
     * Sets the hint for the search bar.
     */
    public void setSearchHint(int resId) {
        mSearchView.setHint(resId);
    }

    /**
     * Sets the hint for the search bar.
     */
    public void setSearchHint(CharSequence hint) {
        mSearchView.setHint(hint);
    }

    /**
     * An enum of possible styles the nav button could be in. All styles will still call
     * {@link OnBackListener#onBack()}.
     */
    public enum NavButtonMode {
        /** Display the nav button as a back button */
        BACK,
        /** Display the nav button as a close button */
        CLOSE
    }

    /** Sets the {@link NavButtonMode} */
    public void setNavButtonMode(NavButtonMode style) {
        if (style != mNavButtonMode) {
            mNavButtonMode = style;
            setState(mState);
        }
    }

    /** Gets the {@link NavButtonMode} */
    public NavButtonMode getNavButtonMode() {
        return mNavButtonMode;
    }

    /**
     * setBackground is disallowed, to prevent apps from deviating from the intended style too much.
     */
    @Override
    public void setBackground(Drawable d) {
        throw new UnsupportedOperationException(
                "You can not change the background of a CarUi toolbar, use "
                        + "setBackgroundShown(boolean) or an RRO instead.");
    }

    /**
     * Invokes all OnToolbarHeightChangeListener handlers registered in {@link
     * OnHeightChangedListener}s array.
     */
    private void handleToolbarHeightChangeListeners(int height) {
        for (OnHeightChangedListener listener : mOnHeightChangedListeners) {
            listener.onHeightChanged(height);
        }
    }

    /**
     * Show/hide the background. When hidden, the toolbar is completely transparent.
     */
    public void setBackgroundShown(boolean shown) {
        if (shown) {
            super.setBackground(getContext().getDrawable(R.color.car_ui_toolbar_background_color));
        } else {
            super.setBackground(null);
        }
    }

    /**
     * Sets the {@link MenuItem Menuitems} to display.
     */
    public void setMenuItems(@Nullable List<MenuItem> items) {
        if (items == null) {
            items = Collections.emptyList();
        }

        if (items.equals(mMenuItems)) {
            return;
        }

        mMenuItems = items;

        mOverflowItems.clear();
        mMenuItemsContainer.removeAllViews();

        for (MenuItem item : items) {
            item.setListener(mMenuItemListener);
            if (item.getDisplayBehavior() == MenuItem.DisplayBehavior.NEVER) {
                mOverflowItems.add(item);
            } else {
                View menuItemView = item.createView(mMenuItemsContainer);

                // Add views with index 0 so that they are added right-to-left
                mMenuItemsContainer.addView(menuItemView, 0);
            }
        }

        createOverflowDialog();

        mSearchButton = mMenuItemsContainer.findViewById(R.id.search);

        setState(mState);
    }

    /** Gets the {@link MenuItem MenuItems} currently displayed */
    @NonNull
    public List<MenuItem> getMenuItems() {
        return mMenuItems;
    }

    private void createOverflowDialog() {
        // TODO(b/140564530) Use a carui alert with a (paged)recyclerview here
        // TODO(b/140563930) Support enabled/disabled overflow items

        CharSequence[] itemTitles = new CharSequence[mOverflowItems.size()];
        for (int i = 0; i < mOverflowItems.size(); i++) {
            itemTitles[i] = mOverflowItems.get(i).getTitle();
        }

        mOverflowDialog = new AlertDialog.Builder(getContext())
                .setItems(itemTitles, (dialog, which) -> {
                    MenuItem item = mOverflowItems.get(which);
                    MenuItem.OnClickListener listener = item.getOnClickListener();
                    if (listener != null) {
                        listener.onClick(item);
                    }
                })
                .create();
    }

    /**
     * Set whether or not to show the {@link MenuItem MenuItems} while searching. Default false.
     * Even if this is set to true, the {@link MenuItem} created by
     * {@link MenuItem.Builder#createSearch(Context, MenuItem.OnClickListener)} will still be
     * hidden.
     */
    public void setShowMenuItemsWhileSearching(boolean showMenuItems) {
        mShowMenuItemsWhileSearching = showMenuItems;
        setState(mState);
    }

    /**
     * Sets the search query.
     */
    public void setSearchQuery(String query) {
        mSearchView.setSearchQuery(query);
    }

    /**
     * Sets a custom view to display, and sets the current state to {@link State#SUBPAGE_CUSTOM}.
     *
     * @param resId A layout id of the view to display.
     * @return The inflated custom view.
     */
    public View setCustomView(int resId) {
        mCustomViewContainer.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(resId, mCustomViewContainer, false);
        mCustomViewContainer.addView(v);
        setState(State.SUBPAGE_CUSTOM);
        return v;
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Sets the state of the toolbar. This will show/hide the appropriate elements of the toolbar
     * for the desired state.
     */
    public void setState(State state) {
        mState = state;

        View.OnClickListener backClickListener = (v) -> {
            boolean absorbed = false;
            List<OnBackListener> listenersCopy = new ArrayList<>(mOnBackListeners);
            for (OnBackListener listener : listenersCopy) {
                absorbed = absorbed || listener.onBack();
            }

            if (!absorbed) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        };
        mNavIcon.setVisibility(state != State.HOME ? VISIBLE : INVISIBLE);
        mNavIcon.setImageResource(mNavButtonMode == NavButtonMode.BACK
                ? R.drawable.car_ui_icon_arrow_back
                : R.drawable.car_ui_icon_close);
        mLogo.setVisibility(state == State.HOME && mHasLogo ? VISIBLE : INVISIBLE);
        mNavIconContainer.setVisibility(state != State.HOME || mHasLogo ? VISIBLE : GONE);
        mNavIconContainer.setOnClickListener(state != State.HOME ? backClickListener : null);
        mNavIconContainer.setClickable(state != State.HOME);
        mTitle.setVisibility(state == State.HOME || state == State.SUBPAGE ? VISIBLE : GONE);
        mTabLayout.setVisibility(state == State.HOME ? VISIBLE : GONE);
        mSearchView.setVisibility(state == State.SEARCH ? VISIBLE : GONE);
        boolean showButtons = state != State.SEARCH || mShowMenuItemsWhileSearching;
        mMenuItemsContainer.setVisibility(showButtons ? VISIBLE : GONE);
        mOverflowButton.setVisibility(showButtons && mOverflowItems.size() > 0 ? VISIBLE : GONE);
        if (mSearchButton != null) {
            mSearchButton.setVisibility(state != State.SEARCH ? VISIBLE : GONE);
        }
        mCustomViewContainer.setVisibility(state == State.SUBPAGE_CUSTOM ? VISIBLE : GONE);
        if (state != State.SUBPAGE_CUSTOM) {
            mCustomViewContainer.removeAllViews();
        }
    }

    /** Gets the current {@link State} of the toolbar. */
    public State getState() {
        return mState;
    }

    /**
     * Registers a new {@link OnHeightChangedListener} to the list of listeners. Register a
     * {@link com.android.car.ui.pagedrecyclerview.PagedRecyclerView} only if there is a toolbar at
     * the top and a {@link com.android.car.ui.pagedrecyclerview.PagedRecyclerView} in the view and
     * nothing else. {@link com.android.car.ui.pagedrecyclerview.PagedRecyclerView} will
     * automatically adjust its height according to the height of the Toolbar.
     */
    public void registerToolbarHeightChangeListener(
            OnHeightChangedListener listener) {
        mOnHeightChangedListeners.add(listener);
    }

    /** Unregisters a {@link OnHeightChangedListener} from the list of listeners. */
    public boolean unregisterToolbarHeightChangeListener(
            OnHeightChangedListener listener) {
        return mOnHeightChangedListeners.remove(listener);
    }

    /** Registers a new {@link OnTabSelectedListener} to the list of listeners. */
    public void registerOnTabSelectedListener(OnTabSelectedListener listener) {
        mOnTabSelectedListeners.add(listener);
    }

    /** Unregisters a new {@link OnTabSelectedListener} from the list of listeners. */
    public boolean unregisterOnTabSelectedListener(OnTabSelectedListener listener) {
        return mOnTabSelectedListeners.remove(listener);
    }

    /** Registers a new {@link OnSearchListener} to the list of listeners. */
    public void registerOnSearchListener(OnSearchListener listener) {
        mSearchView.registerOnSearchListener(listener);
    }

    /** Unregisters a new {@link OnSearchListener} from the list of listeners. */
    public boolean unregisterOnSearchListener(OnSearchListener listener) {
        return mSearchView.unregisterOnSearchListener(listener);
    }

    /** Registers a new {@link OnBackListener} to the list of listeners. */
    public void registerOnBackListener(OnBackListener listener) {
        mOnBackListeners.add(listener);
    }

    /** Unregisters a new {@link OnTabSelectedListener} from the list of listeners. */
    public boolean unregisterOnBackListener(OnBackListener listener) {
        return mOnBackListeners.remove(listener);
    }
}
