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

package com.android.car.ui.preference;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.List;

/**
 * Adapter for {@link RecyclerView} to display items use checkboxes to allow for multi-select
 * functionality.
 */
public class CarUiMultiSelectListItemAdapter extends
        RecyclerView.Adapter<CarUiMultiSelectListItemAdapter.ViewHolder> {

    /**
     * Callback that will be issued when a checkbox state is toggled.
     */
    public interface OnCheckedChangeListener {

        /**
         * Will be called when checkbox checked state changes.
         *
         * @param position of the checkbox.
         */
        void onCheckChanged(int position, boolean isChecked);
    }

    private OnCheckedChangeListener mListener;

    private final List<String> mList;
    private boolean[] mCheckedItems;

    CarUiMultiSelectListItemAdapter(List<String> list, boolean[] checkedItems) {
        if (list.size() != checkedItems.length) {
            throw new IllegalStateException("Item list must be same size and checked items list");
        }

        mList = list;
        mCheckedItems = checkedItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_ui_check_box_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String entry = mList.get(position);
        holder.mCheckBox.setChecked(mCheckedItems[position]);
        holder.mTextView.setText(entry);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    void setOnCheckedChangedListener(@Nullable OnCheckedChangeListener listener) {
        mListener = listener;
    }

    /** Holds views of list items that have checkboxes. */
    class ViewHolder extends RecyclerView.ViewHolder {

        CheckBox mCheckBox;
        TextView mTextView;

        ViewHolder(View view) {
            super(view);
            mCheckBox = view.findViewById(R.id.checkbox);
            mTextView = view.findViewById(R.id.text);

            view.setOnClickListener(v -> {
                boolean isChecked = !mCheckBox.isChecked();
                mCheckBox.setChecked(isChecked);
                mCheckedItems[getAdapterPosition()] = isChecked;
                if (mListener != null) {
                    mListener.onCheckChanged(getAdapterPosition(), isChecked);
                }
            });
        }
    }
}
