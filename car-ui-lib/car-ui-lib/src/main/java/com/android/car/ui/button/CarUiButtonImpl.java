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
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.R;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Static implementation of {@link CarUiButton}. Will be swapped out for a shared library
 * implementation when present.
 * <p>
 * Do not use this from client apps, for car-ui-lib internal use only.
 */
//TODO(b/179092760) Find a way to prevent apps from using this
@SuppressWarnings("AndroidJdkLibsChecker")
public class CarUiButtonImpl implements CarUiButton {
    private final Context mContext;
    private final FrameLayout mContainer;
    private CarUiButtonAttributes mAttributes;
    @Nullable
    private Consumer<CarUiButton> mOnClickListener;

    public CarUiButtonImpl(@NonNull Context context, @Nullable CarUiButtonAttributes attributes) {
        mContext = Objects.requireNonNull(context);
        if (attributes == null) {
            attributes = CarUiButtonAttributes.builder().build();
        }
        mContainer = new FrameLayout(context);
        init(attributes);
    }

    private void init(@NonNull CarUiButtonAttributes attributes) {
        CarUiButtonStyle style = attributes.getStyle();

        mContainer.removeAllViews();
        if (style == CarUiButtonStyle.FLOATING) {
            if (!TextUtils.isEmpty(attributes.getText())) {
                LayoutInflater.from(mContext)
                        .inflate(R.layout.car_ui_button_floating_text, mContainer, true);
            } else {
                LayoutInflater.from(mContext)
                        .inflate(R.layout.car_ui_button_floating_icon, mContainer, true);
            }
        } else {
            if (!TextUtils.isEmpty(attributes.getText())) {
                LayoutInflater.from(mContext)
                        .inflate(R.layout.car_ui_button_text, mContainer, true);
            } else {
                LayoutInflater.from(mContext)
                        .inflate(R.layout.car_ui_button_icon, mContainer, true);
            }
        }
        TextView textView = mContainer.findViewById(R.id.car_ui_button_text_view);
        ImageView imageView = mContainer.findViewById(R.id.car_ui_button_image_view);

        if (style != CarUiButtonStyle.FLOATING) {
            View background = mContainer.findViewWithTag("car_ui_button_background_view");
            if (background != null) {
                background.setBackground(createBackground(attributes));
            }

            if (textView != null) {
                int padding = dpToPx(attributes.getIcon() != null ? 32 : 48);
                textView.setPaddingRelative(padding, 0, padding, 0);
            }
        }

        if (textView != null) {
            textView.setText(attributes.getText());
            if (attributes.getIcon() != null) {
                Drawable icon = attributes.getIcon().getConstantState().newDrawable().mutate();
                icon.setBounds(0, 0, dpToPx(32), dpToPx(32));
                textView.setCompoundDrawablesRelative(icon, null, null, null);
            }

            if (style == CarUiButtonStyle.PRIMARY) {
                textView.setTextColor(getPrimaryButtonTextColor());
                textView.setCompoundDrawableTintList(getPrimaryButtonTextColor());
            } else if (style == CarUiButtonStyle.SECONDARY) {
                textView.setTextColor(getBackgroundColor(attributes));
                textView.setCompoundDrawableTintList(getBackgroundColor(attributes));
            }
        }
        if (imageView != null) {
            imageView.setImageDrawable(attributes.getIcon());

            if (style == CarUiButtonStyle.PRIMARY) {
                imageView.setImageTintList(getPrimaryButtonTextColor());
            } else if (style == CarUiButtonStyle.SECONDARY) {
                imageView.setImageTintList(getBackgroundColor(attributes));
            }
        }

        View clickTarget = mContainer.findViewWithTag("car_ui_button_background_view");

        if (mOnClickListener == null) {
            clickTarget.setOnClickListener(null);
        } else {
            clickTarget.setOnClickListener(v -> mOnClickListener.accept(this));
        }

        mContainer.setId(attributes.getId());
        recursiveSetEnabled(mContainer, attributes.getEnabled());

        mAttributes = attributes;
    }

    private Drawable createBackground(
            @NonNull CarUiButtonAttributes attributes) {
        if (attributes.getStyle() == CarUiButtonStyle.FLOATING) {
            throw new IllegalArgumentException("Cannot create background for floating button");
        }

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);

        ColorStateList color = getBackgroundColor(attributes);
        if (attributes.getStyle() == CarUiButtonStyle.PRIMARY) {
            gradientDrawable.setColor(color);
        } else {
            gradientDrawable.setStroke(dpToPx(2), color);
            gradientDrawable.setColor(0x00FFFFFF);
        }

        gradientDrawable.setCornerRadius(dpToPx(TextUtils.isEmpty(attributes.getText()) ? 8 : 4));

        GradientDrawable rippleMask = new GradientDrawable();
        rippleMask.setCornerRadius(dpToPx(TextUtils.isEmpty(attributes.getText()) ? 8 : 4));
        rippleMask.setColor(0xFFFFFFFF); // required for the ripple to work

        return new RippleDrawable(
                getThemeColor(android.R.attr.colorControlHighlight),
                gradientDrawable,
                rippleMask);
    }

    private ColorStateList getBackgroundColor(@NonNull CarUiButtonAttributes attributes) {
        CarUiButtonColorScheme colorScheme = attributes.getColorScheme();
        ColorStateList color;
        if (colorScheme == CarUiButtonColorScheme.BASIC) {
            color = withDisabledColor(getThemeColor(android.R.attr.colorAccent).getDefaultColor());
        } else if (colorScheme == CarUiButtonColorScheme.RED) {
            color = withDisabledColor(0xfff51414);
        } else if (colorScheme == CarUiButtonColorScheme.BLUE) {
            color = withDisabledColor(0xff1e66eb);
        } else if (colorScheme == CarUiButtonColorScheme.GREEN) {
            color = withDisabledColor(0xff19bf2c);
        } else if (colorScheme == CarUiButtonColorScheme.YELLOW) {
            color = withDisabledColor(0xfff2f542);
        } else {
            if (colorScheme.getType() != CarUiButtonColorScheme.TYPE_CUSTOM) {
                throw new IllegalArgumentException("Unrecognized color scheme");
            }
            color = withDisabledColor(colorScheme.getCustomColor());
        }

        return color;
    }

    private ColorStateList withDisabledColor(int color) {
        return new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{R.attr.state_ux_restricted},
                new int[]{},
        }, new int[]{
                0xffdadce0,
                0xffdadce0,
                color
        });
    }

    private ColorStateList getPrimaryButtonTextColor() {
        return new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{R.attr.state_ux_restricted},
                new int[]{},
        }, new int[]{
                0x7f282a2d,
                0x7f282a2d,
                0xff282a2d,
        });
    }

    private ColorStateList getThemeColor(int themeAttribute) {
        final TypedValue value = new TypedValue();
        mContext.getTheme().resolveAttribute(themeAttribute, value, false);
        return mContext.getResources().getColorStateList(value.data, mContext.getTheme());
    }

    @Override
    public void setEnabled(boolean enabled) {
        mAttributes = mAttributes.copy().setEnabled(enabled).build();
        // We could just call init(newAttributes), but that would be much slower
        recursiveSetEnabled(mContainer, enabled);
    }

    @Override
    public void setOnClickListener(Consumer<CarUiButton> onClickListener) {
        mOnClickListener = onClickListener;
        View clickTarget = mContainer.findViewWithTag("car_ui_button_background_view");

        if (onClickListener == null) {
            clickTarget.setOnClickListener(null);
        } else {
            clickTarget.setOnClickListener(v -> onClickListener.accept(this));
        }
    }

    @Override
    public void setText(String text) {
        CarUiButtonAttributes newAttributes = mAttributes.copy().setText(text).build();
        init(newAttributes);
    }

    @Override
    public void setIcon(Drawable icon) {
        CarUiButtonAttributes newAttributes = mAttributes.copy().setIcon(icon).build();
        init(newAttributes);
    }

    @Override
    public void setColorScheme(CarUiButtonColorScheme scheme) {
        CarUiButtonAttributes newAttributes = mAttributes.copy().setColorScheme(scheme).build();
        init(newAttributes);
    }

    @Override
    public View getView() {
        return mContainer;
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density);
    }

    private static void recursiveSetEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }

        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                recursiveSetEnabled(vg.getChildAt(i), enabled);
            }
        }
    }
}
