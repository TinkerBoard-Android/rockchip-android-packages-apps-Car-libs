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
package com.android.car.ui.paintbooth.toolbar;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.car.ui.AlertDialogBuilder;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.TabLayout;
import com.android.car.ui.toolbar.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class ToolbarActivity extends Activity {

    private List<MenuItem> mMenuItems = new ArrayList<>();
    private List<Pair<CharSequence, View.OnClickListener>> mButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_ui_recycler_view_activity);

        Toolbar toolbar = requireViewById(R.id.toolbar);
        toolbar.registerOnBackListener(() -> {
            if (toolbar.getState() == Toolbar.State.SEARCH) {
                toolbar.setState(Toolbar.State.SUBPAGE);
                return true;
            }
            return false;
        });

        toolbar.getRootView().setBackgroundColor(0xFFFFFF00);

        mMenuItems.add(MenuItem.Builder.createSearch(this, i ->
                toolbar.setState(Toolbar.State.SEARCH)));

        toolbar.setMenuItems(mMenuItems);

        mButtons.add(Pair.create("Change title", v ->
                toolbar.setTitle(toolbar.getTitle() + " X")));

        mButtons.add(Pair.create("MenuItem: Add Icon", v -> {
            mMenuItems.add(MenuItem.Builder.createSettings(this, i ->
                    Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()));
            toolbar.setMenuItems(mMenuItems);
        }));

        Mutable<Integer> overflowCounter = new Mutable<>(1);
        mButtons.add(Pair.create("MenuItem: Add Overflow", v -> {
            mMenuItems.add(new MenuItem.Builder(this)
                    .setTitle("Foo " + overflowCounter.value)
                    .setOnClickListener(i ->
                            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show())
                    .setDisplayBehavior(MenuItem.DisplayBehavior.NEVER)
                    .build());
            toolbar.setMenuItems(mMenuItems);
            overflowCounter.value++;
        }));

        mButtons.add(Pair.create("MenuItem: Add Switch", v -> {
            mMenuItems.add(new MenuItem.Builder(this)
                    .setCheckable()
                    .setOnClickListener(i ->
                            Toast.makeText(this, "Checked? " + i.isChecked(),
                                    Toast.LENGTH_SHORT).show())
                    .build());
            toolbar.setMenuItems(mMenuItems);
        }));

        mButtons.add(Pair.create("MenuItem: Add text", v -> {
            mMenuItems.add(new MenuItem.Builder(this)
                    .setTitle("Baz")
                    .setOnClickListener(i ->
                            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show())
                    .build());
            toolbar.setMenuItems(mMenuItems);
        }));

        mButtons.add(Pair.create("MenuItem: Add activatable", v -> {
            mMenuItems.add(new MenuItem.Builder(this)
                    .setIcon(R.drawable.ic_tracklist)
                    .setActivatable()
                    .setOnClickListener(i ->
                            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show())
                    .build());
            toolbar.setMenuItems(mMenuItems);
        }));

        mButtons.add(Pair.create("MenuItem: Toggle Visibility", v -> {
            SimpleTextWatcher textWatcher = new SimpleTextWatcher();
            new AlertDialogBuilder(this)
                    .setEditBox("", textWatcher, null, InputType.TYPE_CLASS_NUMBER)
                    .setTitle("Enter the index of the MenuItem to toggle")
                    .setPositiveButton("Ok", (dialog, which) -> {
                        try {
                            MenuItem item = mMenuItems.get(Integer.parseInt(textWatcher.getText()));
                            item.setVisible(!item.isVisible());
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            Toast.makeText(this, "Invalid index \""
                                            + textWatcher.getText()
                                            + "\", valid range is 0 to " + (mMenuItems.size() - 1),
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .show();
        }));

        mButtons.add(Pair.create("MenuItem: Toggle show while searching", v ->
                toolbar.setShowMenuItemsWhileSearching(!toolbar.getShowMenuItemsWhileSearching())));

        mButtons.add(Pair.create("Cycle nav button mode", v -> {
            Toolbar.NavButtonMode mode = toolbar.getNavButtonMode();
            if (mode == Toolbar.NavButtonMode.BACK) {
                toolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);
            } else if (mode == Toolbar.NavButtonMode.CLOSE) {
                toolbar.setNavButtonMode(Toolbar.NavButtonMode.DOWN);
            } else {
                toolbar.setNavButtonMode(Toolbar.NavButtonMode.BACK);
            }
        }));

        Mutable<Boolean> hasLogo = new Mutable<>(true);
        mButtons.add(Pair.create("Toggle logo", v -> {
            toolbar.setLogo(hasLogo.value ? 0 : R.drawable.ic_launcher);
            hasLogo.value = !hasLogo.value;
        }));

        mButtons.add(Pair.create("Toggle state", v -> {
            if (toolbar.getState() == Toolbar.State.SUBPAGE) {
                toolbar.setState(Toolbar.State.HOME);
            } else {
                toolbar.setState(Toolbar.State.SUBPAGE);
            }
        }));

        mButtons.add(Pair.create("Toggle search hint", v -> {
            if (toolbar.getSearchHint().toString().contentEquals("Foo")) {
                toolbar.setSearchHint("Bar");
            } else {
                toolbar.setSearchHint("Foo");
            }
        }));

        mButtons.add(Pair.create("Toggle background", v ->
                toolbar.setBackgroundShown(!toolbar.getBackgroundShown())));

        mButtons.add(Pair.create("Add tab", v ->
                toolbar.addTab(new TabLayout.Tab(getDrawable(R.drawable.ic_launcher), "Foo"))));

        mButtons.add(Pair.create("Add tab with custom text", v -> {
            SimpleTextWatcher textWatcher = new SimpleTextWatcher();
            new AlertDialogBuilder(this)
                    .setEditBox(null, textWatcher, null)
                    .setTitle("Enter the text for the title")
                    .setPositiveButton("Ok", (dialog, which) ->
                        toolbar.addTab(new TabLayout.Tab(getDrawable(R.drawable.ic_launcher),
                                textWatcher.getText())))
                    .show();
        }));

        CarUiRecyclerView prv = requireViewById(R.id.list);
        prv.setAdapter(mAdapter);
    }

    private static class ViewHolder extends CarUiRecyclerView.ViewHolder {
        private final Button mButton;

        ViewHolder(View itemView) {
            super(itemView);
            mButton = itemView.requireViewById(R.id.button);
        }

        public void bind(CharSequence title, View.OnClickListener listener) {
            mButton.setText(title);
            mButton.setOnClickListener(listener);
        }
    }

    private CarUiRecyclerView.Adapter<ViewHolder> mAdapter =
            new CarUiRecyclerView.Adapter<ViewHolder>() {
        @Override
        public int getItemCount() {
            return mButtons.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
            View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent,
                    false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pair<CharSequence, View.OnClickListener> pair = mButtons.get(position);
            holder.bind(pair.first, pair.second);
        }
    };

    /** For changing values from lambdas */
    private static final class Mutable<E> {
        public E value;

        Mutable() {
            value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

    /** Used for getting text from a dialog. */
    private static final class SimpleTextWatcher implements TextWatcher {

        private String mValue;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mValue = s.toString();
        }

        public String getText() {
            return mValue;
        }
    }
}
