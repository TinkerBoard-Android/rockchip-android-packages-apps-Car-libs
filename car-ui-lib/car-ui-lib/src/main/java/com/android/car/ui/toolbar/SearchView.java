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

import static android.view.WindowInsets.Type.ime;

import static com.android.car.ui.core.SearchResultsProvider.CONTENT;
import static com.android.car.ui.core.SearchResultsProvider.SEARCH_RESULTS_PROVIDER;
import static com.android.car.ui.core.SearchResultsProvider.SEARCH_RESULTS_TABLE_NAME;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.CONTENT_AREA_SURFACE_PACKAGE;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_ACTION;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_EXTRACTED_TEXT_ICON;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_SEARCH_RESULTS;
import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.car.ui.CarUiText;
import com.android.car.ui.R;
import com.android.car.ui.core.SearchResultsProvider;
import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A search view used by {@link Toolbar}.
 */
public class SearchView extends ConstraintLayout {

    private final InputMethodManager mInputMethodManager;
    private final ImageView mIcon;
    private final EditText mSearchText;
    private final View mCloseIcon;
    private final int mStartPaddingWithoutIcon;
    private final int mStartPadding;
    private final int mEndPadding;
    private View mContentView;
    private View mOriginalView;
    private View mParent;
    private ViewGroup.LayoutParams mLayoutParams;
    private boolean mIsImeWidescreenViewSet;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private SurfaceControlViewHost mSurfaceControlViewHost;
    private FrameLayout mWideScreenImeContentAreaViewContainer;
    private Uri mContentUri;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private List<? extends CarUiImeSearchListItem> mWideScreenSearchItemList;
    private final Map<String, CarUiImeSearchListItem> mIdToListItem = new HashMap<>();

    private Set<Consumer<String>> mSearchListeners = Collections.emptySet();
    private Set<Runnable> mSearchCompletedListeners =
            Collections.emptySet();
    private Set<Toolbar.OnSearchListener> mDeprecatedSearchListeners = Collections.emptySet();
    private Set<Toolbar.OnSearchCompletedListener> mDeprecatedSearchCompletedListeners =
            Collections.emptySet();
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            onSearch(editable.toString());
        }
    };

    private boolean mIsPlainText = false;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mInputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.car_ui_toolbar_search_view, this, true);

        mSearchText = requireViewByRefId(this, R.id.car_ui_toolbar_search_bar);
        mIcon = requireViewByRefId(this, R.id.car_ui_toolbar_search_icon);
        mCloseIcon = requireViewByRefId(this, R.id.car_ui_toolbar_search_close);

        mCloseIcon.setOnClickListener(view -> {
            if (view.isFocused()) {
                mSearchText.requestFocus();
                mInputMethodManager.showSoftInput(mSearchText, 0);
            }
            mSearchText.getText().clear();
        });
        mCloseIcon.setVisibility(View.GONE);

        mStartPaddingWithoutIcon = mSearchText.getPaddingStart();
        mStartPadding = context.getResources().getDimensionPixelSize(
                R.dimen.car_ui_toolbar_search_search_icon_container_width);
        mEndPadding = context.getResources().getDimensionPixelSize(
                R.dimen.car_ui_toolbar_search_close_icon_container_width);

        mSearchText.setSaveEnabled(false);
        mSearchText.setPaddingRelative(mStartPadding, 0, mEndPadding, 0);

        mSearchText.setOnClickListener((view) -> mInputMethodManager.showSoftInput(view, 0));

        mSearchText.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                });

        mSearchText.addTextChangedListener(mTextWatcher);

        mSearchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_SEARCH) {
                notifyQuerySubmit();
            } else if (isEnter(event)) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    // Note that we want to trigger search only on ACTION_UP, but want to return
                    // true for all actions for the relevant key event.
                    notifyQuerySubmit();
                }
                return true;
            }
            return false;
        });

        if (mSearchText instanceof CarUiEditText) {
            ((CarUiEditText) mSearchText).registerOnPrivateImeCommandListener(
                    new SearchViewImeCallback());
        }
    }

    void setSearchConfig(SearchConfig searchConfig, SearchCapabilities searchCapabilities) {
        if (searchConfig != null && searchConfig.getSearchResultsView() != null
                && searchCapabilities.canShowSearchResultsView()) {
            setViewToImeWideScreenSurface(
                    searchConfig.getSearchResultsView());
        }

        if (searchConfig != null && searchConfig.getSearchResultItems() != null) {
            setSearchItemsForWideScreen(
                    searchConfig.getSearchResultItems());
        }

        if (searchConfig != null
                && searchConfig.getSearchResultsInputViewIcon() != null) {
            setSearchResultsInputViewIcon(
                    searchConfig.getSearchResultsInputViewIcon());
        }
    }

    /**
     * Apply window inset listener to the search container.
     */
    void installWindowInsetsListener(View searchContainer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // WindowInsets.isVisible() is only available on R or above
            return;
        }

        searchContainer.getRootView().setOnApplyWindowInsetsListener((v, insets) -> {

            if (insets.isVisible(ime())) {
                if (!mIsImeWidescreenViewSet && mContentView != null) {
                    // When the IME first time opens after the setViewToImeWideScreenSurface(View)
                    // call, setup a container and attach the view to the IME surface.
                    // When the IME is closed and opened again by clicking on the EditText, we
                    // also need to reset the container as the previous instance would be
                    // attached to the surface and we cannot detach it. It will be removed
                    // automatically by GC.
                    mWideScreenImeContentAreaViewContainer = new FrameLayout(getContext());
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    ViewGroup parent = (ViewGroup) mContentView.getParent();
                    if (parent != null) {
                        parent.removeView(mContentView);
                    }

                    if (mOriginalView instanceof CarUiRecyclerView) {
                        // We need to use a negative layout margin to have the list take the full
                        // content area.
                        params.topMargin = -mOriginalView.getPaddingTop();
                        params.bottomMargin = -mOriginalView.getPaddingBottom();
                        params.leftMargin = -mOriginalView.getPaddingLeft();
                        params.rightMargin = -mOriginalView.getPaddingRight();
                    }
                    mWideScreenImeContentAreaViewContainer.addView(mContentView, params);
                }

                displaySearchWideScreen();
                mHandler.post(() -> {
                    if (mSurfaceControlViewHost != null
                            && mWideScreenImeContentAreaViewContainer != null
                            && mSurfaceControlViewHost.getView() == null) {
                        // set the container with app's view into the Surface view.
                        mIsImeWidescreenViewSet = true;
                        mSurfaceControlViewHost.setView(
                                mWideScreenImeContentAreaViewContainer, mSurfaceWidth,
                                mSurfaceHeight);
                    }
                });
            } else {
                removeView();
                mIsImeWidescreenViewSet = false;
            }
            return v.onApplyWindowInsets(insets);
        });
    }

    /**
     * Remove the app's view from the container and attach it back to its original parent.
     */
    private void removeView() {
        if (mWideScreenImeContentAreaViewContainer != null && mParent != null) {
            mHandler.post(() -> {
                ViewGroup parent = (ViewGroup) mContentView.getParent();
                if (parent != null) {
                    parent.removeView(mContentView);
                }
                ((ViewGroup) mParent).addView(mContentView, mLayoutParams);
                mParent.requestLayout();
            });
        }
    }

    private void setSearchResultsInputViewIcon(Drawable drawable) {
        Bitmap bitmap = CarUiUtils.drawableToBitmap(drawable);
        byte[] byteArray = bitmapToByteArray(bitmap);

        Bundle bundle = new Bundle();
        bundle.putByteArray(WIDE_SCREEN_EXTRACTED_TEXT_ICON, byteArray);
        mInputMethodManager.sendAppPrivateCommand(mSearchText, WIDE_SCREEN_ACTION, bundle);
    }

    private void setViewToImeWideScreenSurface(View view) {
        if (view == null) {
            mWideScreenImeContentAreaViewContainer = null;
            return;
        }

        mOriginalView = view;

        if (view instanceof CarUiRecyclerView) {
            mContentView = ((CarUiRecyclerView) view).getContainer();
        } else {
            mContentView = view;
        }

        ViewGroup parentView = (ViewGroup) mContentView.getParent();
        mParent = parentView;
        mLayoutParams = mContentView.getLayoutParams();
    }

    private boolean isEnter(KeyEvent event) {
        boolean result = false;
        if (event != null) {
            int keyCode = event.getKeyCode();
            result = keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                    || keyCode == KeyEvent.KEYCODE_SEARCH;
        }
        return result;
    }

    private void notifyQuerySubmit() {
        mSearchText.clearFocus();
        for (Runnable listener : mSearchCompletedListeners) {
            listener.run();
        }
        for (Toolbar.OnSearchCompletedListener listener : mDeprecatedSearchCompletedListeners) {
            listener.onSearchCompleted();
        }
    }

    private boolean mWasShown = false;

    @Override
    public void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        boolean isShown = isShown();
        if (isShown && !mWasShown) {
            boolean hasQuery = mSearchText.getText().length() > 0;
            mCloseIcon.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
            mSearchText.requestFocus();
            mInputMethodManager.showSoftInput(mSearchText, 0);
        }
        mWasShown = isShown;
    }

    /**
     * Sets a listener for the search text changing.
     */
    public void setSearchListeners(
            Set<Toolbar.OnSearchListener> deprecatedListeners,
            Set<Consumer<String>> listeners) {
        mSearchListeners = listeners;
        mDeprecatedSearchListeners = deprecatedListeners;
    }

    /**
     * Sets a listener for the user completing their search, for example by clicking the
     * enter/search button on the keyboard.
     */
    public void setSearchCompletedListeners(
            Set<Toolbar.OnSearchCompletedListener> deprecatedListeners,
            Set<Runnable> listeners) {
        mSearchCompletedListeners = listeners;
        mDeprecatedSearchCompletedListeners = deprecatedListeners;
    }

    /**
     * Sets list of search item {@link CarUiListItem} to be displayed in the IMS template.
     */
    private void setSearchItemsForWideScreen(List<? extends CarUiImeSearchListItem> searchItems) {
        mWideScreenSearchItemList = searchItems != null ? new ArrayList<>(searchItems) : null;
        displaySearchWideScreen();
    }

    private void displaySearchWideScreen() {
        String url = CONTENT + getContext().getPackageName() + SEARCH_RESULTS_PROVIDER + "/"
                + SEARCH_RESULTS_TABLE_NAME;
        mContentUri = Uri.parse(url);
        mIdToListItem.clear();
        // clear the table.
        getContext().getContentResolver().delete(mContentUri, null, null);

        // mWideScreenImeContentAreaView will only be set when running in widescreen mode and
        // apps allowed by OEMs are trying to set their own view. In that case we did not want to
        // send the information to IME for templatized solution.
        if (mWideScreenImeContentAreaViewContainer != null) {
            return;
        }

        if (mWideScreenSearchItemList == null) {
            mInputMethodManager.sendAppPrivateCommand(mSearchText, WIDE_SCREEN_ACTION, null);
            return;
        }

        int id = 0;

        for (CarUiImeSearchListItem item : mWideScreenSearchItemList) {
            ContentValues values = new ContentValues();
            String idString = String.valueOf(id);
            values.put(SearchResultsProvider.ITEM_ID, id);
            values.put(SearchResultsProvider.SECONDARY_IMAGE_ID, id);
            BitmapDrawable icon = (BitmapDrawable) item.getIcon();
            values.put(SearchResultsProvider.PRIMARY_IMAGE_BLOB,
                    icon != null ? bitmapToByteArray(icon.getBitmap()) : null);
            BitmapDrawable supplementalIcon = (BitmapDrawable) item.getSupplementalIcon();
            values.put(SearchResultsProvider.SECONDARY_IMAGE_BLOB,
                    supplementalIcon != null ? bitmapToByteArray(supplementalIcon.getBitmap())
                            : null);
            values.put(SearchResultsProvider.TITLE,
                    item.getTitle() != null ? item.getTitle().getPreferredText().toString() : null);
            values.put(SearchResultsProvider.SUBTITLE,
                    item.getBody() != null ? CarUiText.combineMultiLine(item.getBody()).toString()
                            : null);
            getContext().getContentResolver().insert(mContentUri, values);
            mIdToListItem.put(idString, item);
            id++;
        }
        mInputMethodManager.sendAppPrivateCommand(mSearchText, WIDE_SCREEN_SEARCH_RESULTS,
                new Bundle());
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        Parcel parcel = Parcel.obtain();
        bitmap.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    /**
     * Sets the search hint
     *
     * @param hint A CharSequence of the search hint.
     */
    public void setHint(CharSequence hint) {
        mSearchText.setHint(hint);
    }

    /**
     * Sets a custom icon to display in the search box.
     */
    public void setIcon(Drawable d) {
        if (d == null) {
            mIcon.setImageResource(R.drawable.car_ui_icon_search);
        } else {
            mIcon.setImageDrawable(d);
        }
    }

    /**
     * Sets a custom icon to display in the search box.
     */
    public void setIcon(int resId) {
        if (resId == 0) {
            mIcon.setImageResource(R.drawable.car_ui_icon_search);
        } else {
            mIcon.setImageResource(resId);
        }
    }

    /**
     * Sets whether or not the search bar should look like a regular text box instead of a search
     * box.
     */
    public void setPlainText(boolean plainText) {
        if (plainText != mIsPlainText) {
            if (plainText) {
                mSearchText.setPaddingRelative(mStartPaddingWithoutIcon, 0, mEndPadding, 0);
                mSearchText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                mIcon.setVisibility(View.GONE);
            } else {
                mSearchText.setPaddingRelative(mStartPadding, 0, mEndPadding, 0);
                mSearchText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                mIcon.setVisibility(View.VISIBLE);
            }
            mIsPlainText = plainText;

            // Needed to detect changes to imeOptions
            mInputMethodManager.restartInput(mSearchText);
        }
    }

    private void onSearch(String query) {
        mCloseIcon.setVisibility(TextUtils.isEmpty(query) ? View.GONE : View.VISIBLE);

        for (Consumer<String> listener : mSearchListeners) {
            listener.accept(query);
        }
        for (Toolbar.OnSearchListener listener : mDeprecatedSearchListeners) {
            listener.onSearch(query);
        }
    }

    /**
     * Sets the text being searched.
     */
    public void setSearchQuery(String query) {
        mSearchText.setText(query);
        mSearchText.setSelection(mSearchText.getText().length());
    }

    private class SearchViewImeCallback implements PrivateImeCommandCallback {

        @Override
        public void onItemClicked(String itemId) {
            CarUiImeSearchListItem item = mIdToListItem.get(itemId);
            if (item != null) {
                CarUiContentListItem.OnClickListener listener =
                        item.getOnClickListener();
                if (listener != null) {
                    listener.onClick(item);
                }
            }
        }

        @Override
        public void onSecondaryImageClicked(String secondaryImageId) {
            CarUiImeSearchListItem item = mIdToListItem.get(secondaryImageId);
            if (item != null) {
                CarUiContentListItem.OnClickListener listener =
                        item.getSupplementalIconOnClickListener();
                if (listener != null) {
                    listener.onClick(item);
                }
            }
        }

        @Override
        public void onSurfaceInfo(int displayId, IBinder binder, int height,
                int width) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.R) {
                // SurfaceControlViewHost is only available on R and above
                return;
            }

            DisplayManager dm = (DisplayManager) getContext().getSystemService(
                    Context.DISPLAY_SERVICE);

            Display display = dm.getDisplay(displayId);

            mSurfaceControlViewHost = new SurfaceControlViewHost(getContext(),
                    display, binder);

            mSurfaceHeight = height;
            mSurfaceWidth = width;

            Bundle bundle = new Bundle();
            bundle.putParcelable(CONTENT_AREA_SURFACE_PACKAGE,
                    mSurfaceControlViewHost.getSurfacePackage());
            mInputMethodManager.sendAppPrivateCommand(mSearchText,
                    WIDE_SCREEN_ACTION, bundle);
        }

        @Override
        public void reLayout(int height, int width) {
            mSurfaceHeight = height;
            mSurfaceWidth = width;

            if (mSurfaceControlViewHost != null) {
                mSurfaceControlViewHost.relayout(width, height);
            }
        }

        @Override
        public void onPostLoadSearchResults() {
            if (mContentUri != null) {
                getContext().getContentResolver().delete(mContentUri, null, null);
            }
        }
    }
}
