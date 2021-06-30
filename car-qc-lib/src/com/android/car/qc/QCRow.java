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

package com.android.car.qc;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quick Control Row Element
 * ------------------------------------
 * |            | Title    |          |
 * | StartItems | Subtitle | EndItems |
 * |            | Sliders  |          |
 * ------------------------------------
 */
public class QCRow extends QCItem {
    private final String mTitle;
    private final String mSubtitle;
    private final Icon mStartIcon;
    private final List<QCActionItem> mStartItems;
    private final List<QCActionItem> mEndItems;
    private final List<QCSlider> mSliders;
    private final PendingIntent mPrimaryAction;

    public QCRow(String title, String subtitle, PendingIntent primaryAction, Icon startIcon,
            List<QCActionItem> startItems, List<QCActionItem> endItems,
            List<QCSlider> sliders) {
        super(QC_TYPE_ROW);
        mTitle = title;
        mSubtitle = subtitle;
        mPrimaryAction = primaryAction;
        mStartIcon = startIcon;
        mStartItems = Collections.unmodifiableList(startItems);
        mEndItems = Collections.unmodifiableList(endItems);
        mSliders = Collections.unmodifiableList(sliders);
    }

    public QCRow(Parcel in) {
        super(in);
        mTitle = in.readString();
        mSubtitle = in.readString();
        boolean hasIcon = in.readBoolean();
        if (hasIcon) {
            mStartIcon = Icon.CREATOR.createFromParcel(in);
        } else {
            mStartIcon = null;
        }
        List<QCActionItem> startItems = new ArrayList<>();
        int startItemCount = in.readInt();
        for (int i = 0; i < startItemCount; i++) {
            startItems.add(QCActionItem.CREATOR.createFromParcel(in));
        }
        mStartItems = Collections.unmodifiableList(startItems);
        List<QCActionItem> endItems = new ArrayList<>();
        int endItemCount = in.readInt();
        for (int i = 0; i < endItemCount; i++) {
            endItems.add(QCActionItem.CREATOR.createFromParcel(in));
        }
        mEndItems = Collections.unmodifiableList(endItems);
        List<QCSlider> sliders = new ArrayList<>();
        int sliderCount = in.readInt();
        for (int i = 0; i < sliderCount; i++) {
            sliders.add(QCSlider.CREATOR.createFromParcel(in));
        }
        mSliders = Collections.unmodifiableList(sliders);
        boolean hasPrimaryAction = in.readBoolean();
        if (hasPrimaryAction) {
            mPrimaryAction = PendingIntent.CREATOR.createFromParcel(in);
        } else {
            mPrimaryAction = null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mTitle);
        dest.writeString(mSubtitle);
        boolean hasStartIcon = mStartIcon != null;
        dest.writeBoolean(hasStartIcon);
        if (hasStartIcon) {
            mStartIcon.writeToParcel(dest, flags);
        }
        dest.writeInt(mStartItems.size());
        for (QCActionItem startItem : mStartItems) {
            startItem.writeToParcel(dest, flags);
        }
        dest.writeInt(mEndItems.size());
        for (QCActionItem endItem : mEndItems) {
            endItem.writeToParcel(dest, flags);
        }
        dest.writeInt(mSliders.size());
        for (QCSlider slider : mSliders) {
            slider.writeToParcel(dest, flags);
        }
        dest.writeBoolean(mPrimaryAction != null);
        boolean hasPrimaryAction = mPrimaryAction != null;
        if (hasPrimaryAction) {
            mPrimaryAction.writeToParcel(dest, flags);
        }
    }

    @Override
    public PendingIntent getPrimaryAction() {
        return mPrimaryAction;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getSubtitle() {
        return mSubtitle;
    }

    @Nullable
    public Icon getStartIcon() {
        return mStartIcon;
    }

    @NonNull
    public List<QCActionItem> getStartItems() {
        return mStartItems;
    }

    @NonNull
    public List<QCActionItem> getEndItems() {
        return mEndItems;
    }

    @NonNull
    public List<QCSlider> getSliders() {
        return mSliders;
    }

    public static Creator<QCRow> CREATOR = new Creator<QCRow>() {
        @Override
        public QCRow createFromParcel(Parcel source) {
            return new QCRow(source);
        }

        @Override
        public QCRow[] newArray(int size) {
            return new QCRow[size];
        }
    };

    /**
     * Builder for {@link QCRow}.
     */
    public static class Builder {
        private final List<QCActionItem> mStartItems = new ArrayList<>();
        private final List<QCActionItem> mEndItems = new ArrayList<>();
        private final List<QCSlider> mSliders = new ArrayList<>();
        private Icon mStartIcon;
        private String mTitle;
        private String mSubtitle;
        private PendingIntent mPrimaryAction;

        /**
         * Sets the row title.
         */
        public Builder setTitle(@Nullable String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the row subtitle.
         */
        public Builder setSubtitle(@Nullable String subtitle) {
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Sets the row icon.
         */
        public Builder setIcon(@Nullable Icon icon) {
            mStartIcon = icon;
            return this;
        }

        /**
         * Sets the PendingIntent to be sent when the row is clicked.
         */
        public Builder setPrimaryAction(@Nullable PendingIntent action) {
            mPrimaryAction = action;
            return this;
        }

        /**
         * Adds a {@link QCActionItem} to the start items area.
         */
        public Builder addStartItem(@NonNull QCActionItem item) {
            mStartItems.add(item);
            return this;
        }

        /**
         * Adds a {@link QCActionItem} to the end items area.
         */
        public Builder addEndItem(@NonNull QCActionItem item) {
            mEndItems.add(item);
            return this;
        }

        /**
         * Adds a {@link QCSlider} to the slider area.
         */
        public Builder addSlider(@NonNull QCSlider slider) {
            mSliders.add(slider);
            return this;
        }

        /**
         * Builds the final {@link QCRow}.
         */
        public QCRow build() {
            return new QCRow(mTitle, mSubtitle, mPrimaryAction, mStartIcon, mStartItems, mEndItems,
                    mSliders);
        }
    }
}
