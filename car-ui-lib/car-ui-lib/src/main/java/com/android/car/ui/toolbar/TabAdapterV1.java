/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.ui.toolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.TabOEMV1;
import com.android.car.ui.toolbar.TabLayout.Tab;

import java.util.function.Consumer;

class TabAdapterV1 implements TabOEMV1 {

    private final Runnable mOnClickListener;
    private final Tab mClientTab;
    private Drawable mIcon;
    private CharSequence mText;
    private Consumer<TabOEMV1> mUpdateListener;

    TabAdapterV1(Context context, Tab clientTab, Runnable onClickListener) {
        ImageViewListener imageView = new ImageViewListener(context);
        imageView.setImageDrawableListener(drawable -> {
            mIcon = drawable;
            if (mUpdateListener != null) {
                mUpdateListener.accept(this);
            }
        });
        clientTab.bindIcon(imageView);

        TextViewListener textView = new TextViewListener(context);
        textView.setTextListener(text -> {
            mText = text;
            if (mUpdateListener != null) {
                mUpdateListener.accept(this);
            }
        });
        clientTab.bindText(textView);

        mOnClickListener = onClickListener;
        mClientTab = clientTab;
    }

    public Tab getClientTab() {
        return mClientTab;
    }

    @Override
    public void setUpdateListener(Consumer<TabOEMV1> listener) {
        mUpdateListener = listener;
    }

    @Override
    public CharSequence getTitle() {
        return mText;
    }

    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public Runnable getOnClickListener() {
        return mOnClickListener;
    }

    @Override
    public boolean shouldTint() {
        return true;
    }
}
