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
package com.android.car.ui.button;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.function.Consumer;

/**
 * Static implementation of {@link CarUiButton}. Will be swapped out for a shared library
 * implementation when present.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CarUiButtonImpl extends FrameLayout implements CarUiButton {
    private TextView mTextView;

    public CarUiButtonImpl(Context context) {
        super(context);
        init();
    }

    public CarUiButtonImpl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CarUiButtonImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CarUiButtonImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CarUiButtonImpl(Context context, @Nullable CarUiButtonAttributes attributes) {
        super(context);
        init();
        if (attributes != null) {
            setId(attributes.getId());
            setEnabled(attributes.getEnabled());
            setSize(attributes.getSize());
            setTitle(attributes.getTitle());
            setIcon(attributes.getIcon());
            setColorScheme(attributes.getColorScheme());
        }
    }

    private void init() {
        mTextView = new TextView(getContext());
        addView(mTextView, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }


    @Override
    public void setColorScheme(@NonNull CarUiButtonColorScheme scheme) {
        //TODO(b/172345817)
    }

    @Override
    public void setIcon(Drawable icon) {
        //TODO(b/172345817) Make sure the icon isn't too big
        mTextView.setCompoundDrawablesRelative(icon, null, null, null);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTextView.setText(title);
    }

    @Override
    public void setSize(@NonNull Size size) {
        //TODO(b/172345817)
    }

    @Override
    public void setOnClickListener(Consumer<CarUiButton> onClickListener) {
        if (onClickListener == null) {
            setOnClickListener((OnClickListener) null);
        } else {
            setOnClickListener((OnClickListener) v -> onClickListener.accept(this));
        }
    }

    @Override
    public View getView() {
        return this;
    }
}
