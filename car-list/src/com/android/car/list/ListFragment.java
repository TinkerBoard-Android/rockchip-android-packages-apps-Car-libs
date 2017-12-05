/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.list;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.android.car.list.R;

import java.util.ArrayList;

import androidx.car.widget.PagedListView;

/**
 *  This class represents an extensible Fragment that holds a TypedPagedListAdapter.
 */
public abstract class ListFragment extends Fragment {
    public static final String EXTRA_LAYOUT = "extra_layout";

    protected PagedListView mListView;
    protected TypedPagedListAdapter mPagedListAdapter;

    protected static Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_LAYOUT, R.layout.list);
        return bundle;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListView = (PagedListView) getView().findViewById(R.id.list);
        mPagedListAdapter = new TypedPagedListAdapter(getContext(), getLineItems());
        mListView.setAdapter(mPagedListAdapter);
    }

    /**
     * Gets the list of the LineItems to show up in the list
     */
    public abstract ArrayList<TypedPagedListAdapter.LineItem> getLineItems();
}
