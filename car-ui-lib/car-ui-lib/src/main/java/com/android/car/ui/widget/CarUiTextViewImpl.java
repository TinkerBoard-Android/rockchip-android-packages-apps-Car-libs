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

package com.android.car.ui.widget;

import android.content.Context;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;

import com.android.car.ui.CarUiText;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Extension of {@link TextView} that supports {@link CarUiText}.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public final class CarUiTextViewImpl extends CarUiTextView {

    @NonNull
    private List<CarUiText> mText = Collections.emptyList();
    private OneShotPreDrawListener mOneShotPreDrawListener;

    public CarUiTextViewImpl(Context context) {
        super(context);
    }

    public CarUiTextViewImpl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiTextViewImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiTextViewImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Set text to display.
     *
     * @param textList list of text to display. Each {@link CarUiText} in the list will be rendered
     *                 on a new line, separated by a line break
     */
    @Override
    public void setText(@NonNull List<CarUiText> textList) {
        mText = Objects.requireNonNull(textList);
        if (mOneShotPreDrawListener == null) {
            mOneShotPreDrawListener = OneShotPreDrawListener.add(this, this::updateText);
        }
        setText(CarUiText.combineMultiLine(textList));
    }

    /**
     * Set text to display.
     */
    @Override
    public void setText(@NonNull CarUiText text) {
        mText = Collections.singletonList(Objects.requireNonNull(text));
        if (mOneShotPreDrawListener == null) {
            mOneShotPreDrawListener = OneShotPreDrawListener.add(this, this::updateText);
        }
        setText(text.getPreferredText());
    }

    private void updateText() {
        Objects.requireNonNull(mText);
        mOneShotPreDrawListener = null;

        // If all lines of text have no maxLines limit, the preferred text set at invocation of
        // setText(List<CarUiText>)/ setText(CarUiText) does not need updating
        if (mText.stream().allMatch(line -> line.getMaxLines() == Integer.MAX_VALUE)) {
            return;
        }

        // Update rendered text if preferred text is truncated
        SpannableStringBuilder builder = new SpannableStringBuilder();
        CharSequence delimiter = "";
        for (int i = 0; i < mText.size(); i++) {
            CarUiText line = mText.get(i);
            builder.append(delimiter).append(getBestVariant(line));
            delimiter = "\n";
        }

        setText(builder);
    }

    private CharSequence getBestVariant(CarUiText text) {
        if (text.getTextVariants().size() > 1) {
            for (CharSequence variant : text.getTextVariants()) {
                StaticLayout updatedLayout = getUpdatedLayout(variant, Integer.MAX_VALUE);
                if (updatedLayout.getLineCount() <= text.getMaxLines()) {
                    return variant;
                }
            }
        }

        // If no text variant can be rendered without truncation, use the preferred text
        return getUpdatedLayout(text.getPreferredText(), text.getMaxLines()).getText();
    }

    private StaticLayout getUpdatedLayout(CharSequence text, int maxLines) {
        Layout layout = Objects.requireNonNull(getLayout());
        int width = layout.getWidth();
        Layout.Alignment alignment = layout.getAlignment();
        float spacingAdd = layout.getSpacingAdd();
        float spacingMult = layout.getSpacingMultiplier();

        return StaticLayout.Builder.obtain(text, 0, text.length(), getPaint(), width)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMult)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
    }
}
