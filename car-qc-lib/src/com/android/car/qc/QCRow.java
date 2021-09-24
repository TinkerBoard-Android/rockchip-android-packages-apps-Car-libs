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
    private final QCSlider mSlider;
    private final List<QCActionItem> mStartItems;
    private final List<QCActionItem> mEndItems;
    private final PendingIntent mPrimaryAction;

    public QCRow(@Nullable String title, @Nullable String subtitle,
            @Nullable PendingIntent primaryAction, @Nullable Icon startIcon,
            @Nullable QCSlider slider, @NonNull List<QCActionItem> startItems,
            @NonNull List<QCActionItem> endItems) {
        super(QC_TYPE_ROW);
        mTitle = title;
        mSubtitle = subtitle;
        mPrimaryAction = primaryAction;
        mStartIcon = startIcon;
        mSlider = slider;
        mStartItems = Collections.unmodifiableList(startItems);
        mEndItems = Collections.unmodifiableList(endItems);
    }

    public QCRow(@NonNull Parcel in) {
        super(in);
        mTitle = in.readString();
        mSubtitle = in.readString();
        boolean hasIcon = in.readBoolean();
        if (hasIcon) {
            mStartIcon = Icon.CREATOR.createFromParcel(in);
        } else {
            mStartIcon = null;
        }
        boolean hasSlider = in.readBoolean();
        if (hasSlider) {
            mSlider = QCSlider.CREATOR.createFromParcel(in);
        } else {
            mSlider = null;
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
        boolean hasSlider = mSlider != null;
        dest.writeBoolean(hasSlider);
        if (hasSlider) {
            mSlider.writeToParcel(dest, flags);
        }
        dest.writeInt(mStartItems.size());
        for (QCActionItem startItem : mStartItems) {
            startItem.writeToParcel(dest, flags);
        }
        dest.writeInt(mEndItems.size());
        for (QCActionItem endItem : mEndItems) {
            endItem.writeToParcel(dest, flags);
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

    @Nullable
    public QCSlider getSlider() {
        return mSlider;
    }

    @NonNull
    public List<QCActionItem> getStartItems() {
        return mStartItems;
    }

    @NonNull
    public List<QCActionItem> getEndItems() {
        return mEndItems;
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
        private Icon mStartIcon;
        private String mTitle;
        private String mSubtitle;
        private QCSlider mSlider;
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
         * Adds a {@link QCSlider} to the slider area.
         */
        public Builder addSlider(@Nullable QCSlider slider) {
            mSlider = slider;
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
         * Builds the final {@link QCRow}.
         */
        public QCRow build() {
            return new QCRow(mTitle, mSubtitle, mPrimaryAction, mStartIcon, mSlider, mStartItems,
                    mEndItems);
        }
    }
}
