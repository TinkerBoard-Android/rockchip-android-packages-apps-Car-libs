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

package com.android.car.ui.paintbooth.widescreenime;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.ADD_DESC_TITLE_TO_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.ADD_DESC_TO_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.ADD_ERROR_DESC_TO_INPUT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.REQUEST_RENDER_CONTENT_AREA;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_ICON_RES_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_ITEM_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SUB_TITLE_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SUPPLEMENTAL_ICON_RES_ID_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_TITLE_LIST;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_ACTION;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_EXTRACTED_TEXT_ICON_RES_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows different scenarios for wide screen ime.
 */
public class WideScreenImeActivity extends AppCompatActivity implements InsetsChangedListener {

    private static final String TAG = "WideScreenImeActivity";

    private final List<MenuItem> mMenuItems = new ArrayList<>();
    private final List<Pair<CharSequence, View.OnFocusChangeListener>> mEditText =
            new ArrayList<>();

    private final ArrayList<String> mItemIdList = new ArrayList<>();
    private final ArrayList<String> mTitleList = new ArrayList<>();
    private final ArrayList<String> mSubTitleList = new ArrayList<>();
    private final ArrayList<Integer> mPrimaryImageResId = new ArrayList<>();
    private final ArrayList<String> mSecondaryItemId = new ArrayList<>();
    private final ArrayList<Integer> mSecondaryImageResId = new ArrayList<>();

    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_ui_recycler_view_activity);

        mInputMethodManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        ToolbarController toolbarNonFinal = CarUi.getToolbar(this);
        if (toolbarNonFinal == null) {
            toolbarNonFinal = requireViewById(R.id.toolbar);
        }
        ToolbarController toolbar = toolbarNonFinal;
        toolbar.setTitle(getTitle());
        toolbar.setState(Toolbar.State.SUBPAGE);
        toolbar.setLogo(R.drawable.ic_launcher);
        toolbar.registerOnBackListener(
                () -> {
                    if (toolbar.getState() == Toolbar.State.SEARCH
                            || toolbar.getState() == Toolbar.State.EDIT) {
                        toolbar.setState(Toolbar.State.SUBPAGE);
                        return true;
                    }
                    return false;
                });

        CarUiContentListItem.OnClickListener mainClickListener = i ->
                Toast.makeText(this, "Item clicked!", Toast.LENGTH_SHORT).show();

        CarUiContentListItem.OnClickListener secondaryClickListener = i ->
                Toast.makeText(this, "Item's secondary action clicked!", Toast.LENGTH_SHORT).show();

        final int[] count = {1};
        CarUiImeSearchListItem item = new CarUiImeSearchListItem(CarUiContentListItem.Action.ICON);
        item.setTitle("Title " + count[0]);
        item.setBody("Sub title " + count[0]);
        item.setIconResId(R.drawable.ic_launcher);
        item.setSupplementalIconResId(R.drawable.ic_launcher);
        item.setSupplementalIcon(getDrawable(R.drawable.ic_launcher), secondaryClickListener);
        item.setOnItemClickedListener(mainClickListener);

        List<CarUiImeSearchListItem> searchItems = new ArrayList<>();

        searchItems.add(item);

        // initial list to display in search view.
        if (toolbar.canShowSearchResultItems()) {
            toolbar.setSearchResultItems(searchItems);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View contentArea = inflater.inflate(R.layout.ime_wide_screen_dummy_view, null, true);

        if (toolbar.canShowSearchResultsView()) {
            toolbar.setSearchResultsView(contentArea);
        }

        contentArea.findViewById(R.id.button_1).setOnClickListener(v ->
                Toast.makeText(this, "Button 1 clicked", Toast.LENGTH_SHORT).show()
        );

        contentArea.findViewById(R.id.button_2).setOnClickListener(v -> {
                    Toast.makeText(this, "Clearing the view...", Toast.LENGTH_SHORT).show();
                    toolbar.setSearchResultsView(null);
                }
        );

        toolbar.registerOnSearchListener((query) -> {
            count[0]++;
            CarUiImeSearchListItem item1 = new CarUiImeSearchListItem(
                    CarUiContentListItem.Action.ICON);
            item1.setTitle("Title " + count[0]);
            item1.setBody("Sub title " + count[0]);
            item1.setIconResId(R.drawable.ic_launcher);
            item1.setSupplementalIconResId(R.drawable.ic_launcher);
            item1.setSupplementalIcon(getDrawable(R.drawable.ic_launcher), secondaryClickListener);
            item1.setOnItemClickedListener(mainClickListener);
            searchItems.add(item1);

            if (toolbar.canShowSearchResultItems()) {
                toolbar.setSearchResultItems(searchItems);
            }
        });

        mMenuItems.add(MenuItem.builder(this)
                .setToSearch()
                .setOnClickListener(i -> {
                    toolbar.setState(Toolbar.State.SEARCH);
                })
                .build());

        toolbar.setMenuItems(mMenuItems);

        mEditText.add(Pair.create("Default Input Edit Text field", null));

        mEditText.add(Pair.create("Add Desc to content area",
                this::addDescToContentArea));

        mEditText.add(Pair.create("Hide the content area",
                this::hideContentArea));

        mEditText.add(Pair.create("Hide extraction view",
                this::hideExtractionView));

        for (int i = 0; i < 7; i++) {
            mItemIdList.add("itemId" + i);
            mTitleList.add("Title " + i);
            mSubTitleList.add("subtitle " + i);
            mPrimaryImageResId.add(R.drawable.ic_launcher);
            mSecondaryItemId.add("imageId" + i);
            mSecondaryImageResId.add(R.drawable.ic_launcher);
        }

        mEditText.add(Pair.create("Show IME list view", this::showImeListView));

        mEditText.add(Pair.create("Add icon to extracted view", this::addIconToExtractedView));

        mEditText.add(
                Pair.create("Add error message to content area", this::addErrorDescToContentArea));

        CarUiRecyclerView recyclerView = requireViewById(R.id.list);
        recyclerView.setAdapter(mAdapter);
    }

    private void addIconToExtractedView(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(WIDE_SCREEN_EXTRACTED_TEXT_ICON_RES_ID, R.drawable.car_ui_icon_edit);
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }

    private void addErrorDescToContentArea(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(ADD_ERROR_DESC_TO_INPUT_AREA, "Some error message");
        bundle.putString(ADD_DESC_TITLE_TO_CONTENT_AREA, "Title");
        bundle.putString(ADD_DESC_TO_CONTENT_AREA, "Description provided by the application");
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }

    private void showImeListView(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        Bundle bundle = new Bundle();

        EditText editText = (EditText) view;
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mItemIdList.add("itemId " + s.toString());
                mTitleList.add("Title " + s.toString());
                mSubTitleList.add("subtitle ");
                mPrimaryImageResId.add(R.drawable.ic_launcher);
                mSecondaryItemId.add("imageId" + s.toString());
                mSecondaryImageResId.add(R.drawable.ic_launcher);

                bundle.putStringArrayList(SEARCH_RESULT_TITLE_LIST, mTitleList);
                bundle.putStringArrayList(SEARCH_RESULT_SUB_TITLE_LIST, mSubTitleList);
                bundle.putIntegerArrayList(SEARCH_RESULT_ICON_RES_ID_LIST,
                        mPrimaryImageResId);
                mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
            }
        });

        bundle.putStringArrayList(SEARCH_RESULT_ITEM_ID_LIST, mItemIdList);
        bundle.putStringArrayList(SEARCH_RESULT_TITLE_LIST, mTitleList);
        bundle.putStringArrayList(SEARCH_RESULT_SUB_TITLE_LIST, mSubTitleList);
        bundle.putStringArrayList(SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST, mSecondaryItemId);
        bundle.putIntegerArrayList(SEARCH_RESULT_ICON_RES_ID_LIST, mPrimaryImageResId);
        bundle.putIntegerArrayList(SEARCH_RESULT_SUPPLEMENTAL_ICON_RES_ID_LIST,
                mSecondaryImageResId);
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }

    private void hideExtractionView(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        EditText editText = (EditText) view;
        editText.setImeOptions(IME_FLAG_NO_EXTRACT_UI);

        Bundle bundle = new Bundle();
        bundle.putBoolean(REQUEST_RENDER_CONTENT_AREA, false);
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }

    private void addDescToContentArea(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(ADD_DESC_TITLE_TO_CONTENT_AREA, "Title");
        bundle.putString(ADD_DESC_TO_CONTENT_AREA, "Description provided by the application");
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }

    private void hideContentArea(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean(REQUEST_RENDER_CONTENT_AREA, false);
        mInputMethodManager.sendAppPrivateCommand(view, WIDE_SCREEN_ACTION, bundle);
    }


    private static class ViewHolder extends RecyclerView.ViewHolder {

        private final EditText mEditText;

        ViewHolder(View itemView) {
            super(itemView);
            mEditText = itemView.requireViewById(R.id.edit_text);
        }

        public void bind(CharSequence title, View.OnFocusChangeListener listener) {
            mEditText.setText(title);
            mEditText.setOnFocusChangeListener(listener);
        }
    }

    private final RecyclerView.Adapter<ViewHolder> mAdapter =
            new RecyclerView.Adapter<ViewHolder>() {
                @Override
                public int getItemCount() {
                    return mEditText.size();
                }

                @Override
                public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
                    View item =
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.edit_text_list_item,
                                            parent, false);

                    return new ViewHolder(item);
                }

                @Override
                public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                    Pair<CharSequence, View.OnFocusChangeListener> pair = mEditText.get(position);
                    holder.bind(pair.first, pair.second);
                }
            };

    @Override
    public void onCarUiInsetsChanged(@NonNull Insets insets) {
        requireViewById(R.id.list)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        requireViewById(android.R.id.content)
                .setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }
}
