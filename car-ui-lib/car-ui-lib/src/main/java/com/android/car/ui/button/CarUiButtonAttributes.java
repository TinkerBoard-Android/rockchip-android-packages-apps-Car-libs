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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.R;

import java.util.Objects;

/**
 * A class representing the properties a button can be created with.
 *
 * This replaces the {@link android.util.AttributeSet} of regular custom views.
 */
public class CarUiButtonAttributes {
    private final int mId;
    private final boolean mEnabled;
    @Nullable
    private final String mText;
    @Nullable
    private final Drawable mIcon;
    @NonNull
    private final CarUiButtonColorScheme mColorScheme;
    @NonNull
    private final CarUiButtonStyle mStyle;

    /** Creates a new {@link Builder} */
    public static Builder builder() {
        return new Builder();
    }

    private CarUiButtonAttributes(Builder builder) {
        mId = builder.mId;
        mEnabled = builder.mEnabled;
        mText = builder.mText;
        mIcon = builder.mIcon;
        mColorScheme = builder.mColorScheme;
        mStyle = builder.mStyle;
    }

    public int getId() {
        return mId;
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    @Nullable
    public String getText() {
        return mText;
    }

    @Nullable
    public Drawable getIcon() {
        return mIcon;
    }

    @NonNull
    public CarUiButtonColorScheme getColorScheme() {
        return mColorScheme;
    }

    @NonNull
    public CarUiButtonStyle getStyle() {
        return mStyle;
    }

    /**
     * Creates a new {@link Builder} that starts off as a copy of these attributes.
     *
     * This is to allow getting a new modified copy of the immutable CarUiButtonAttributes class,
     * by doing something like {@code attributes.copy().setText("Foo").build()}.
     */
    public Builder copy() {
        return new Builder(this);
    }

    /** A builder class for CarUiButtonAttributes. Create it with {@link #builder()} */
    public static class Builder {
        private int mId = View.NO_ID;
        private boolean mEnabled = true;
        @Nullable
        private String mText;
        @Nullable
        private Drawable mIcon;
        @NonNull
        private CarUiButtonColorScheme mColorScheme = CarUiButtonColorScheme.BASIC;
        @NonNull
        private CarUiButtonStyle mStyle = CarUiButtonStyle.PRIMARY;

        private Builder() {
        }

        private Builder(CarUiButtonAttributes toCopy) {
            mId = toCopy.getId();
            mEnabled = toCopy.getEnabled();
            mText = toCopy.getText();
            mIcon = toCopy.getIcon();
            mColorScheme = toCopy.getColorScheme();
            mStyle = toCopy.getStyle();
        }

        /**
         * Sets the id of the button, to be used with
         * {@link com.android.car.ui.core.CarUi#requireCarUiComponentById(android.view.View, int)}
         */
        public Builder setId(int id) {
            mId = id;
            return this;
        }

        /** Sets whether the button should be enabled or not */
        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        /** Sets the text of the button */
        public Builder setText(@Nullable String text) {
            mText = text;
            return this;
        }

        /** Sets the icon of the button */
        public Builder setIcon(@Nullable Drawable icon) {
            mIcon = icon;
            return this;
        }

        /** Sets the color of the button. See {@link CarUiButtonColorScheme} */
        public Builder setColorScheme(@NonNull CarUiButtonColorScheme colorScheme) {
            mColorScheme = Objects.requireNonNull(colorScheme);
            return this;
        }

        /** Sets the style of the button. See {@link CarUiButtonStyle} */
        public Builder setStyle(@NonNull CarUiButtonStyle style) {
            mStyle = Objects.requireNonNull(style);
            return this;
        }

        /** Builds the {@link CarUiButtonAttributes} */
        public CarUiButtonAttributes build() {
            return new CarUiButtonAttributes(this);
        }
    }

    /**
     * Creates a CarUiButtonAttributes from an {@link AttributeSet}.
     *
     * This uses the attributes in the {@code CarUiButton} declare-styleable.
     */
    public static CarUiButtonAttributes fromAttributeSet(Context context, AttributeSet set) {
        Builder builder = builder();

        TypedArray array = context.obtainStyledAttributes(set, R.styleable.CarUiButton);
        try {
            builder.setId(array.getResourceId(R.styleable.CarUiButton_android_id, View.NO_ID));
            builder.setEnabled(array.getBoolean(R.styleable.CarUiButton_android_enabled, true));
            builder.setText(array.getString(R.styleable.CarUiButton_carUiButtonTitle));
            builder.setIcon(array.getDrawable(R.styleable.CarUiButton_carUiButtonIcon));
            builder.setColorScheme(unpackEnum(
                    array.getInteger(R.styleable.CarUiButton_carUiButtonColorScheme, 0),
                    CarUiButtonColorScheme.BASIC,
                    CarUiButtonColorScheme.RED,
                    CarUiButtonColorScheme.BLUE,
                    CarUiButtonColorScheme.GREEN,
                    CarUiButtonColorScheme.YELLOW));
            builder.setStyle(unpackEnum(
                    array.getInteger(R.styleable.CarUiButton_carUiButtonStyle, 0),
                    CarUiButtonStyle.PRIMARY,
                    CarUiButtonStyle.SECONDARY,
                    CarUiButtonStyle.FLOATING));
        } finally {
            array.recycle();
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T unpackEnum(int intVal, T... enumVals) {
        return enumVals[intVal];
    }
}
