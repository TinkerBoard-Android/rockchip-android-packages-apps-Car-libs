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
import android.widget.RadioButton;
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
public final class CarUiListItemAdapter extends
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
        private final ImageView mContentIcon;
        private final ImageView mAvatarIcon;
        private final ViewGroup mIconContainer;
        private final ViewGroup mActionContainer;
        private final View mActionDivider;
        private final Switch mSwitch;
        private final CheckBox mCheckBox;
        private final RadioButton mRadioButton;
        private final ImageView mSupplementalIcon;
        private final View mTouchInterceptor;
        private final View mReducedTouchInterceptor;
        private final View mActionContainerTouchInterceptor;


        ListItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.requireViewById(R.id.title);
            mBody = itemView.requireViewById(R.id.body);
            mIcon = itemView.requireViewById(R.id.icon);
            mContentIcon = itemView.requireViewById(R.id.content_icon);
            mAvatarIcon = itemView.requireViewById(R.id.avatar_icon);
            mIconContainer = itemView.requireViewById(R.id.icon_container);
            mActionContainer = itemView.requireViewById(R.id.action_container);
            mActionDivider = itemView.requireViewById(R.id.action_divider);
            mSwitch = itemView.requireViewById(R.id.switch_widget);
            mCheckBox = itemView.requireViewById(R.id.checkbox_widget);
            mRadioButton = itemView.requireViewById(R.id.radio_button_widget);
            mSupplementalIcon = itemView.requireViewById(R.id.supplemental_icon);
            mReducedTouchInterceptor = itemView.requireViewById(R.id.reduced_touch_interceptor);
            mTouchInterceptor = itemView.requireViewById(R.id.touch_interceptor);
            mActionContainerTouchInterceptor = itemView.requireViewById(
                    R.id.action_container_touch_interceptor);
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
                mBody.setVisibility(View.VISIBLE);
            } else {
                mBody.setVisibility(View.GONE);
            }

            mIcon.setVisibility(View.GONE);
            mContentIcon.setVisibility(View.GONE);
            mAvatarIcon.setVisibility(View.GONE);

            if (icon != null) {
                mIconContainer.setVisibility(View.VISIBLE);

                switch (item.getPrimaryIconType()) {
                    case CONTENT:
                        mContentIcon.setVisibility(View.VISIBLE);
                        mContentIcon.setImageDrawable(icon);
                        break;
                    case STANDARD:
                        mIcon.setVisibility(View.VISIBLE);
                        mIcon.setImageDrawable(icon);
                        break;
                    case AVATAR:
                        mAvatarIcon.setVisibility(View.VISIBLE);
                        mAvatarIcon.setImageDrawable(icon);
                        mAvatarIcon.setClipToOutline(true);
                        break;
                }
            } else {
                mIconContainer.setVisibility(View.GONE);
            }

            mActionDivider.setVisibility(
                    item.isActionDividerVisible() ? View.VISIBLE : View.GONE);
            mSwitch.setVisibility(View.GONE);
            mCheckBox.setVisibility(View.GONE);
            mRadioButton.setVisibility(View.GONE);
            mSupplementalIcon.setVisibility(View.GONE);

            CarUiContentListItem.OnClickListener itemOnClickListener = item.getOnClickListener();

            switch (item.getAction()) {
                case NONE:
                    mActionContainer.setVisibility(View.GONE);

                    // Display ripple effects across entire item when clicked by using full-sized
                    // touch interceptor.
                    mTouchInterceptor.setVisibility(View.VISIBLE);
                    mTouchInterceptor.setOnClickListener(v -> {
                        if (itemOnClickListener != null) {
                            itemOnClickListener.onClick(item);
                        }
                    });
                    mReducedTouchInterceptor.setVisibility(View.GONE);
                    mActionContainerTouchInterceptor.setVisibility(View.GONE);
                    break;
                case SWITCH:
                    mSwitch.setVisibility(View.VISIBLE);
                    mSwitch.setOnCheckedChangeListener(null);
                    mSwitch.setChecked(item.isChecked());
                    mSwitch.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {
                                item.setChecked(isChecked);
                                CarUiContentListItem.OnCheckedChangedListener itemListener =
                                        item.getOnCheckedChangedListener();
                                if (itemListener != null) {
                                    itemListener.onCheckedChanged(item, isChecked);
                                }
                            });

                    // Clicks anywhere on the item should toggle the switch state. Use full touch
                    // interceptor.
                    mTouchInterceptor.setVisibility(View.VISIBLE);
                    mTouchInterceptor.setOnClickListener(v -> {
                        mSwitch.toggle();
                        if (itemOnClickListener != null) {
                            itemOnClickListener.onClick(item);
                        }
                    });
                    mReducedTouchInterceptor.setVisibility(View.GONE);
                    mActionContainerTouchInterceptor.setVisibility(View.GONE);

                    mActionContainer.setVisibility(View.VISIBLE);
                    mActionContainer.setClickable(false);
                    break;
                case CHECK_BOX:
                    mCheckBox.setVisibility(View.VISIBLE);
                    mCheckBox.setOnCheckedChangeListener(null);
                    mCheckBox.setChecked(item.isChecked());
                    mCheckBox.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {
                                item.setChecked(isChecked);
                                CarUiContentListItem.OnCheckedChangedListener itemListener =
                                        item.getOnCheckedChangedListener();
                                if (itemListener != null) {
                                    itemListener.onCheckedChanged(item, isChecked);
                                }
                            });

                    // Clicks anywhere on the item should toggle the checkbox state. Use full touch
                    // interceptor.
                    mTouchInterceptor.setVisibility(View.VISIBLE);
                    mTouchInterceptor.setOnClickListener(v -> {
                        mCheckBox.toggle();
                        if (itemOnClickListener != null) {
                            itemOnClickListener.onClick(item);
                        }
                    });
                    mReducedTouchInterceptor.setVisibility(View.GONE);
                    mActionContainerTouchInterceptor.setVisibility(View.GONE);

                    mActionContainer.setVisibility(View.VISIBLE);
                    mActionContainer.setClickable(false);
                    break;
                case RADIO_BUTTON:
                    mRadioButton.setVisibility(View.VISIBLE);
                    mRadioButton.setOnCheckedChangeListener(null);
                    mRadioButton.setChecked(item.isChecked());
                    mRadioButton.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {
                                item.setChecked(isChecked);
                                CarUiContentListItem.OnCheckedChangedListener itemListener =
                                        item.getOnCheckedChangedListener();
                                if (itemListener != null) {
                                    itemListener.onCheckedChanged(item, isChecked);
                                }
                            });

                    // Clicks anywhere on the item should toggle the switch state. Use full touch
                    // interceptor.
                    mTouchInterceptor.setVisibility(View.VISIBLE);
                    mTouchInterceptor.setOnClickListener(v -> {
                        mRadioButton.toggle();
                        if (itemOnClickListener != null) {
                            itemOnClickListener.onClick(item);
                        }
                    });
                    mReducedTouchInterceptor.setVisibility(View.GONE);
                    mActionContainerTouchInterceptor.setVisibility(View.GONE);

                    mActionContainer.setVisibility(View.VISIBLE);
                    mActionContainer.setClickable(false);
                    break;
                case ICON:
                    mSupplementalIcon.setVisibility(View.VISIBLE);
                    mSupplementalIcon.setImageDrawable(item.getSupplementalIcon());
                    mActionContainer.setVisibility(View.VISIBLE);
                    mActionContainerTouchInterceptor.setOnClickListener(
                            (container) -> {
                                if (item.getSupplementalIconOnClickListener() != null) {
                                    item.getSupplementalIconOnClickListener().onClick(mIcon);
                                }
                                if (itemOnClickListener != null) {
                                    itemOnClickListener.onClick(item);
                                }
                            });

                    // If the icon has a click listener, use a reduced touch interceptor to create
                    // two distinct touch area; the action container and the remainder of the list
                    // item. Each touch area will have its own ripple effect. If the icon has no
                    // click listener, it shouldn't be clickable.
                    if (item.getSupplementalIconOnClickListener() == null) {
                        mTouchInterceptor.setVisibility(View.VISIBLE);
                        mTouchInterceptor.setOnClickListener(v -> {
                            if (itemOnClickListener != null) {
                                itemOnClickListener.onClick(item);
                            }
                        });
                        mReducedTouchInterceptor.setVisibility(View.GONE);
                        mActionContainerTouchInterceptor.setVisibility(View.GONE);
                    } else {
                        mReducedTouchInterceptor.setVisibility(View.VISIBLE);
                        mReducedTouchInterceptor.setOnClickListener(v -> {
                            if (itemOnClickListener != null) {
                                itemOnClickListener.onClick(item);
                            }
                        });
                        mActionContainerTouchInterceptor.setVisibility(View.VISIBLE);
                        mTouchInterceptor.setVisibility(View.GONE);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown secondary action type.");
            }

            itemView.setActivated(item.isActivated());
            setEnabled(itemView, item.isEnabled());
        }

        void setEnabled(View view, boolean enabled) {
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;

                for (int i = 0; i < group.getChildCount(); i++) {
                    setEnabled(group.getChildAt(i), enabled);
                }
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
