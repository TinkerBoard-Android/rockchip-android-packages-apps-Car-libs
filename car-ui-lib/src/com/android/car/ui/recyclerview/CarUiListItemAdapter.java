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
 * Adapter for {@link CarUiRecyclerView} to display {@link CarUiContentListItem} and {@link
 * CarUiHeaderListItem}.
 *
 * <ul>
 * <li> Implements {@link CarUiRecyclerView.ItemCap} - defaults to unlimited item count.
 * </ul>
 */
public class CarUiListItemAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        CarUiRecyclerView.ItemCap {

    private static final int VIEW_TYPE_LIST_ITEM = 1;
    private static final int VIEW_TYPE_LIST_HEADER = 2;

    private List<CarUiListItem> mItems;
    private int mMaxItems = CarUiRecyclerView.ItemCap.UNLIMITED;

    public CarUiListItemAdapter(List<CarUiListItem> items) {
        this.mItems = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_LIST_ITEM:
                return new ListItemViewHolder(
                        inflater.inflate(R.layout.car_ui_list_item, parent, false));
            case VIEW_TYPE_LIST_HEADER:
                return new HeaderViewHolder(
                        inflater.inflate(R.layout.car_ui_header_list_item, parent, false));
            default:
                throw new IllegalStateException("Unknown item type.");
        }
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
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof CarUiContentListItem) {
            return VIEW_TYPE_LIST_ITEM;
        } else if (mItems.get(position) instanceof CarUiHeaderListItem) {
            return VIEW_TYPE_LIST_HEADER;
        }

        throw new IllegalStateException("Unknown view type.");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_LIST_ITEM:
                if (!(holder instanceof ListItemViewHolder)) {
                    throw new IllegalStateException("Incorrect view holder type for list item.");
                }

                CarUiListItem item = mItems.get(position);
                if (!(item instanceof CarUiContentListItem)) {
                    throw new IllegalStateException(
                            "Expected item to be bound to viewholder to be instance of "
                                    + "CarUiContentListItem.");
                }

                ((ListItemViewHolder) holder).bind((CarUiContentListItem) item);
                break;
            case VIEW_TYPE_LIST_HEADER:
                if (!(holder instanceof HeaderViewHolder)) {
                    throw new IllegalStateException("Incorrect view holder type for list item.");
                }

                CarUiListItem header = mItems.get(position);
                if (!(header instanceof CarUiHeaderListItem)) {
                    throw new IllegalStateException(
                            "Expected item to be bound to viewholder to be instance of "
                                    + "CarUiHeaderListItem.");
                }

                ((HeaderViewHolder) holder).bind((CarUiHeaderListItem) header);
                break;
            default:
                throw new IllegalStateException("Unknown item view type.");
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
     * Holds views of {@link CarUiContentListItem}.
     */
    static class ListItemViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTitle;
        private final TextView mBody;
        private final ImageView mIcon;
        private final ViewGroup mIconContainer;
        private final ViewGroup mActionContainer;
        private final View mActionDivider;
        private final Switch mSwitch;
        private final CheckBox mCheckBox;
        private final ImageView mSupplementalIcon;

        ListItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.requireViewById(R.id.title);
            mBody = itemView.requireViewById(R.id.body);
            mIcon = itemView.requireViewById(R.id.icon);
            mIconContainer = itemView.requireViewById(R.id.icon_container);
            mActionContainer = itemView.requireViewById(R.id.action_container);
            mActionDivider = itemView.requireViewById(R.id.action_divider);
            mSwitch = itemView.requireViewById(R.id.switch_widget);
            mCheckBox = itemView.requireViewById(R.id.checkbox_widget);
            mSupplementalIcon = itemView.requireViewById(R.id.supplemental_icon);
        }

        private void bind(@NonNull CarUiContentListItem item) {
            CharSequence title = item.getTitle();
            CharSequence body = item.getBody();
            Drawable icon = item.getIcon();

            if (!TextUtils.isEmpty(title)) {
                mTitle.setText(title);
                mTitle.setVisibility(View.VISIBLE);
            } else {
                mTitle.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(body)) {
                mBody.setText(body);
            } else {
                mBody.setVisibility(View.GONE);
            }

            if (icon != null) {
                mIcon.setImageDrawable(icon);
                mIconContainer.setVisibility(View.VISIBLE);
            } else {
                mIconContainer.setVisibility(View.GONE);
            }

            mActionDivider.setVisibility(
                    item.isActionDividerVisible() ? View.VISIBLE : View.GONE);

            switch (item.getAction()) {
                case NONE:
                    mActionContainer.setVisibility(View.GONE);
                    break;
                case SWITCH:
                    mSwitch.setVisibility(View.VISIBLE);
                    mSwitch.setChecked(item.isChecked());
                    mSwitch.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {
                                item.setChecked(isChecked);
                                CarUiContentListItem.OnCheckedChangedListener itemListener =
                                        item.getOnCheckedChangedListener();
                                if (itemListener != null) {
                                    itemListener.onCheckedChanged(isChecked);
                                }
                            });
                    mCheckBox.setVisibility(View.GONE);
                    mSupplementalIcon.setVisibility(View.GONE);
                    mActionContainer.setVisibility(View.VISIBLE);
                    break;
                case CHECK_BOX:
                    mCheckBox.setVisibility(View.VISIBLE);
                    mCheckBox.setChecked(item.isChecked());
                    mCheckBox.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {
                                item.setChecked(isChecked);
                                CarUiContentListItem.OnCheckedChangedListener itemListener =
                                        item.getOnCheckedChangedListener();
                                if (itemListener != null) {
                                    itemListener.onCheckedChanged(isChecked);
                                }
                            });
                    mSwitch.setVisibility(View.GONE);
                    mSupplementalIcon.setVisibility(View.GONE);
                    mActionContainer.setVisibility(View.VISIBLE);
                    break;
                case ICON:
                    mSupplementalIcon.setVisibility(View.VISIBLE);
                    mSupplementalIcon.setImageDrawable(item.getSupplementalIcon());
                    mSupplementalIcon.setOnClickListener(
                            (iconView) -> {
                                if (item.getSupplementalIconOnClickListener() != null) {
                                    item.getSupplementalIconOnClickListener().onClick(iconView);
                                }
                            });
                    mSwitch.setVisibility(View.GONE);
                    mCheckBox.setVisibility(View.GONE);
                    mActionContainer.setVisibility(View.VISIBLE);
                    break;
                default:
                    throw new IllegalStateException("Unknown secondary action type.");
            }
        }
    }

    /**
     * Holds views of {@link CarUiHeaderListItem}.
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTitle;
        private final TextView mBody;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.requireViewById(R.id.title);
            mBody = itemView.requireViewById(R.id.body);
        }

        private void bind(@NonNull CarUiHeaderListItem item) {
            mTitle.setText(item.getTitle());

            CharSequence body = item.getBody();
            if (!TextUtils.isEmpty(body)) {
                mBody.setText(body);
            } else {
                mBody.setVisibility(View.GONE);
            }
        }
    }
}
