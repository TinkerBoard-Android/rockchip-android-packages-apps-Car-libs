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

package com.android.car.ui.recyclerview;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.List;

/**
 * Adapter for {@link CarUiRecyclerView} to display {@link CarUiListItem}.
 *
 * <ul>
 * <li> Implements {@link CarUiRecyclerView.ItemCap} - defaults to unlimited item count.
 * </ul>
 */
public class CarUiListItemAdapter extends
        RecyclerView.Adapter<CarUiListItemAdapter.ViewHolder> implements
        CarUiRecyclerView.ItemCap {

    private List<CarUiListItem> mItems;
    private int mMaxItems = CarUiRecyclerView.ItemCap.UNLIMITED;

    public CarUiListItemAdapter(List<CarUiListItem> items) {
        this.mItems = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.car_ui_list_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Returns the data set held by the adapter.
     *
     * <p>Any changes performed to this mutable list must be followed with an invocation of the
     * appropriate notify method for the adapter.
     */
    @NonNull
    public List<CarUiListItem> getItems() {
        return mItems;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarUiListItem item = mItems.get(position);
        CharSequence title = item.getTitle();
        CharSequence body = item.getBody();
        Drawable icon = item.getIcon();

        if (!TextUtils.isEmpty(title)) {
            holder.getTitle().setText(title);
            holder.getTitle().setVisibility(View.VISIBLE);
        } else {
            holder.getTitle().setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(body)) {
            holder.getBody().setText(body);
        } else {
            holder.getBody().setVisibility(View.GONE);
        }

        if (icon != null) {
            holder.getIcon().setImageDrawable(icon);
            holder.getIconContainer().setVisibility(View.VISIBLE);
        } else {
            holder.getIconContainer().setVisibility(View.GONE);
        }

        Switch switchWidget = holder.getSwitch();
        CheckBox checkBox = holder.getCheckBox();
        ViewGroup actionContainer = holder.getActionContainer();

        switch (item.getAction()) {
            case NONE:
                holder.getActionContainer().setVisibility(View.GONE);
                break;
            case SWITCH:
                switchWidget.setVisibility(View.VISIBLE);
                switchWidget.setChecked(item.isChecked());
                switchWidget.setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {
                            item.setChecked(isChecked);
                            CarUiListItem.OnCheckedChangedListener itemListener =
                                    item.getOnCheckedChangedListener();
                            if (itemListener != null) {
                                itemListener.onCheckedChanged(isChecked);
                            }
                        });
                checkBox.setVisibility(View.GONE);
                actionContainer.setVisibility(View.VISIBLE);
                break;
            case CHECK_BOX:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(item.isChecked());
                checkBox.setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {
                            item.setChecked(isChecked);
                            CarUiListItem.OnCheckedChangedListener itemListener =
                                    item.getOnCheckedChangedListener();
                            if (itemListener != null) {
                                itemListener.onCheckedChanged(isChecked);
                            }
                        });
                switchWidget.setVisibility(View.GONE);
                actionContainer.setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalStateException("Unknown secondary action type.");
        }
    }

    @Override
    public int getItemCount() {
        return mMaxItems == CarUiRecyclerView.ItemCap.UNLIMITED
                ? mItems.size()
                : Math.min(mItems.size(), mMaxItems);
    }

    @Override
    public void setMaxItems(int maxItems) {
        mMaxItems = maxItems;
    }

    /**
     * Holds views of {@link CarUiListItem}.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mBody;
        private ImageView mIcon;
        private ViewGroup mIconContainer;
        private ViewGroup mActionContainer;
        private Switch mSwitch;
        private CheckBox mCheckBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.requireViewById(R.id.title);
            mBody = itemView.requireViewById(R.id.body);
            mIcon = itemView.requireViewById(R.id.icon);
            mIconContainer = itemView.requireViewById(R.id.icon_container);
            mActionContainer = itemView.requireViewById(R.id.action_container);
            mSwitch = itemView.requireViewById(R.id.switch_widget);
            mCheckBox = itemView.requireViewById(R.id.checkbox_widget);
        }

        @NonNull
        TextView getTitle() {
            return mTitle;
        }

        @NonNull
        TextView getBody() {
            return mBody;
        }

        @NonNull
        ImageView getIcon() {
            return mIcon;
        }

        @NonNull
        ViewGroup getIconContainer() {
            return mIconContainer;
        }

        @NonNull
        ViewGroup getActionContainer() {
            return mActionContainer;
        }

        @NonNull
        Switch getSwitch() {
            return mSwitch;
        }

        @NonNull
        CheckBox getCheckBox() {
            return mCheckBox;
        }
    }
}
