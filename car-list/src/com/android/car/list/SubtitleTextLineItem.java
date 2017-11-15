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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Contains logic for a line item represents text only view of a subtitle.
 */
public class SubtitleTextLineItem
        extends TypedPagedListAdapter.LineItem<SubtitleTextLineItem.ViewHolder> {
    private final CharSequence mTitle;

    /**
     * Construct SubtitleTextLineItem with title.
     */
    public SubtitleTextLineItem(CharSequence title) {
        mTitle = title;
    }

    @Override
    public int getType() {
        return SUBTITLE_TEXT_TYPE;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(mTitle);
    }

    /**
     * ViewHolder that contains the title of the SubtitleTextLineItem
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.subtitle);
        }
    }

    /**
     * Creates ViewHolder with the elements of SubtitleTextLineItem
     */
    public static RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.subtitle_text_line_item, parent, false));
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

    @Override
    public boolean isClickable() {
        return true;
    }
}
