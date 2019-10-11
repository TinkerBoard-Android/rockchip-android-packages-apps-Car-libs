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
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.List;

/**
 * The adapter for the recyclerview containing radio buttons as the items.
 */
public class CarUiRecyclerViewRadioButtonAdapter extends
        RecyclerView.Adapter<CarUiRecyclerViewRadioButtonAdapter.ViewHolder> {

    /**
     * Callback that will be issued after any radio button is clicked.
     */
    public interface OnRadioButtonClickedListener {
        /**
         * Will be called when radio button is clicked.
         *
         * @param position of the radio button.
         */
        void onClick(int position);
    }

    private OnRadioButtonClickedListener mOnRadioButtonClickedListener;

    private List<String> mList;
    private int mSelectedPosition = -1;

    public CarUiRecyclerViewRadioButtonAdapter(List<String> list, int position) {
        mList = list;
        mSelectedPosition = position;
    }

    @Override
    public CarUiRecyclerViewRadioButtonAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_ui_radio_button_item, parent, false);
        return new CarUiRecyclerViewRadioButtonAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CarUiRecyclerViewRadioButtonAdapter.ViewHolder holder,
            int position) {
        String entry = mList.get(position);
        //since only one radio button is allowed to be selected,
        // this condition un-checks previous selections
        holder.mRadioButton.setChecked(mSelectedPosition == position);
        holder.mRadioButton.setText(entry);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    /** Registers a new {@link OnRadioButtonClickedListener} listener. */
    public void registerListener(OnRadioButtonClickedListener listener) {
        mOnRadioButtonClickedListener = listener;
    }

    /** Unregisters a {@link OnRadioButtonClickedListener} listener\. */
    public void unregisterOnBackListener(OnRadioButtonClickedListener listener) {
        mOnRadioButtonClickedListener = null;
    }

    /** The viewholder class for recyclerview containing radio buttons. */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public RadioButton mRadioButton;

        public ViewHolder(View view) {
            super(view);
            mRadioButton = (RadioButton) view.findViewById(R.id.radio_button);

            mRadioButton.setOnClickListener(v -> {
                mSelectedPosition = getAdapterPosition();
                notifyDataSetChanged();
                if (mOnRadioButtonClickedListener != null) {
                    mOnRadioButtonClickedListener.onClick(mSelectedPosition);
                }
            });
        }
    }
}
