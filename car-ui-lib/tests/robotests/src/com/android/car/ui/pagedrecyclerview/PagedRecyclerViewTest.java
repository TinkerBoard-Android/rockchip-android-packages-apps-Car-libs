/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.ui.pagedrecyclerview;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.CarUiRobolectricTestRunner;
import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarUiRobolectricTestRunner.class)
public class PagedRecyclerViewTest {

    private Context mContext;
    private View mView;
    private PagedRecyclerView mPagedRecyclerView;

    @Mock
    private RecyclerView.Adapter mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void onHeightChanged_shouldAddTheValueToInitialTopValue() {
        mView = LayoutInflater.from(mContext)
                .inflate(R.layout.test_linear_paged_recycler_view, null);

        mPagedRecyclerView = mView.findViewById(R.id.test_prv);

        assertThat(mPagedRecyclerView.getPaddingBottom()).isEqualTo(0);
        assertThat(mPagedRecyclerView.getPaddingTop()).isEqualTo(0);
        assertThat(mPagedRecyclerView.getPaddingStart()).isEqualTo(0);
        assertThat(mPagedRecyclerView.getPaddingEnd()).isEqualTo(0);

        mPagedRecyclerView.onHeightChanged(10);

        assertThat(mPagedRecyclerView.getPaddingTop()).isEqualTo(10);
        assertThat(mPagedRecyclerView.getPaddingBottom()).isEqualTo(0);
        assertThat(mPagedRecyclerView.getPaddingStart()).isEqualTo(0);
        assertThat(mPagedRecyclerView.getPaddingEnd()).isEqualTo(0);
    }

    @Test
    public void setAdapter_shouldInitializeLinearLayoutManager() {
        mView = LayoutInflater.from(mContext)
                .inflate(R.layout.test_linear_paged_recycler_view, null);

        mPagedRecyclerView = mView.findViewById(R.id.test_prv);
        mPagedRecyclerView.setAdapter(mAdapter);

        assertThat(mPagedRecyclerView.getLayoutManager()).isInstanceOf(LinearLayoutManager.class);
    }

    @Test
    public void setAdapter_shouldInitializeGridLayoutManager() {
        mView = LayoutInflater.from(mContext)
                .inflate(R.layout.test_grid_paged_recycler_view, null);

        mPagedRecyclerView = mView.findViewById(R.id.test_prv);
        mPagedRecyclerView.setAdapter(mAdapter);

        assertThat(mPagedRecyclerView.getLayoutManager()).isInstanceOf(GridLayoutManager.class);
    }

    @Test
    public void init_shouldContainNestedRecyclerView() {
        mView = LayoutInflater.from(mContext)
                .inflate(R.layout.test_grid_paged_recycler_view, null);

        mPagedRecyclerView = mView.findViewById(R.id.test_prv);

        assertThat(mPagedRecyclerView.mNestedRecyclerView).isNotNull();
    }
}
