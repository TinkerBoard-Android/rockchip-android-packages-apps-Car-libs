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

package com.android.car.ui.paintbooth.widescreenime;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.paintbooth.caruirecyclerview.RecyclerViewAdapter;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.Toolbar.State;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that the custom view inflated in IME window.
 */
public class WideScreenTestView extends AppCompatActivity implements InsetsChangedListener {

    private final ArrayList<String> mData = new ArrayList<>();
    private final List<MenuItem> mMenuItems = new ArrayList<>();
    private final int mDataToGenerate = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_ui_recycler_view_activity);

        ToolbarController toolbar = CarUi.requireToolbar(this);
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

        mMenuItems.add(MenuItem.builder(this)
                .setToSearch()
                .setOnClickListener(i -> {
                    toolbar.setState(State.SEARCH);
                    if (toolbar.canShowSearchResultsView()) {
                        toolbar.setSearchResultsView(findViewById(R.id.list));
                    }
                }).build());

        toolbar.setMenuItems(mMenuItems);

        CarUiRecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(generateSampleData());
        recyclerView.setAdapter(adapter);
    }

    private ArrayList<String> generateSampleData() {
        for (int i = 0; i <= mDataToGenerate; i++) {
            mData.add(getString(R.string.test_data) + i);
        }
        return mData;
    }

    @Override
    public void onCarUiInsetsChanged(@NonNull Insets insets) {
        requireViewById(R.id.list)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        requireViewById(android.R.id.content)
                .setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }
}
