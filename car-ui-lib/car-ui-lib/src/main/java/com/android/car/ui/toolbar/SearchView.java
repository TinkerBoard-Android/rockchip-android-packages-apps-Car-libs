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

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_ITEM_ID;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_PRIMARY_IMAGE_RES_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SECONDARY_IMAGE_ID;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SECONDARY_IMAGE_RES_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SUB_TITLE_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_TITLE_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_ACTION;
import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.car.ui.R;
import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private List<? extends CarUiImeSearchListItem> mWideScreenSearchItemList = new ArrayList<>();
    private final Map<String, CarUiImeSearchListItem> mIdToListItem = new HashMap<>();

    private Set<Toolbar.OnSearchListener> mSearchListeners = Collections.emptySet();
    private Set<Toolbar.OnSearchCompletedListener> mSearchCompletedListeners =
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

        mCloseIcon.setOnClickListener(view -> mSearchText.getText().clear());
        mCloseIcon.setVisibility(View.GONE);

        mStartPaddingWithoutIcon = mSearchText.getPaddingStart();
        mStartPadding = context.getResources().getDimensionPixelSize(
                R.dimen.car_ui_toolbar_search_search_icon_container_width);
        mEndPadding = context.getResources().getDimensionPixelSize(
                R.dimen.car_ui_toolbar_search_close_icon_container_width);

        mSearchText.setSaveEnabled(false);
        mSearchText.setPaddingRelative(mStartPadding, 0, mEndPadding, 0);

        mSearchText.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (hasFocus) {
                        mInputMethodManager.showSoftInput(view, 0);
                    } else {
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
                    new CarUiEditText.PrivateImeCommandCallback() {
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
                    });
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
                displaySearchWideScreen();
            }
            return v.onApplyWindowInsets(insets);
        });
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
        for (Toolbar.OnSearchCompletedListener listener : mSearchCompletedListeners) {
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
        }
        mWasShown = isShown;
    }

    /**
     * Sets a listener for the search text changing.
     */
    public void setSearchListeners(Set<Toolbar.OnSearchListener> listeners) {
        mSearchListeners = listeners;
    }

    /**
     * Sets a listener for the user completing their search, for example by clicking the
     * enter/search button on the keyboard.
     */
    public void setSearchCompletedListeners(Set<Toolbar.OnSearchCompletedListener> listeners) {
        mSearchCompletedListeners = listeners;
    }

    /**
     * Sets list of search item {@link CarUiListItem} to be displayed in the IMS
     * template.
     */
    public void setSearchItemsForWideScreen(List<? extends CarUiImeSearchListItem> searchItems) {
        mWideScreenSearchItemList = new ArrayList<>(searchItems);
        displaySearchWideScreen();
    }

    private void displaySearchWideScreen() {
        mIdToListItem.clear();
        if (mWideScreenSearchItemList.isEmpty()) {
            return;
        }
        ArrayList<String> itemIdList = new ArrayList<>();
        ArrayList<String> titleList = new ArrayList<>();
        ArrayList<String> subTitleList = new ArrayList<>();
        ArrayList<Integer> primaryImageResId = new ArrayList<>();
        ArrayList<String> secondaryItemId = new ArrayList<>();
        ArrayList<Integer> secondaryImageResId = new ArrayList<>();
        int id = 0;
        for (CarUiImeSearchListItem item : mWideScreenSearchItemList) {
            String idString = String.valueOf(id);
            itemIdList.add(idString);
            titleList.add(item.getTitle() != null ? item.getTitle().toString() : null);
            subTitleList.add(item.getBody() != null ? item.getBody().toString() : null);
            primaryImageResId.add(item.getIconResId());
            secondaryItemId.add(idString);
            secondaryImageResId.add(item.getSupplementalIconResId());

            mIdToListItem.put(idString, item);
            id++;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SEARCH_RESULT_ITEM_ID, itemIdList);
        bundle.putStringArrayList(SEARCH_RESULT_TITLE_LIST, titleList);
        bundle.putStringArrayList(SEARCH_RESULT_SUB_TITLE_LIST, subTitleList);
        bundle.putIntegerArrayList(SEARCH_RESULT_PRIMARY_IMAGE_RES_ID_LIST, primaryImageResId);
        bundle.putStringArrayList(SEARCH_RESULT_SECONDARY_IMAGE_ID, secondaryItemId);
        bundle.putIntegerArrayList(SEARCH_RESULT_SECONDARY_IMAGE_RES_ID_LIST, secondaryImageResId);
        mInputMethodManager.sendAppPrivateCommand(mSearchText, WIDE_SCREEN_ACTION, bundle);
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
     * Sets whether or not the search bar should look like a regular text box
     * instead of a search box.
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

        for (Toolbar.OnSearchListener listener : mSearchListeners) {
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
}
