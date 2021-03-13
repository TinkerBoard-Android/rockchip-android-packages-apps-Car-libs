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
package com.android.car.ui;

import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Text that includes rendering information.
 */
public class CarUiText {
    private final int mMaxLines;
    private final List<CharSequence> mText;

    /**
     * Convenience method that returns a single {@link CharSequence} that is a combination of the
     * preferred text of a list of {@link CarUiText}, separated by line breaks.
     */
    public static CharSequence combineMultiLine(@NonNull List<CarUiText> lines) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        CharSequence delimiter = "";
        for (CarUiText line : lines) {
            builder.append(delimiter).append(line.getPreferredText());
            delimiter = "\n";
        }
        return builder;
    }

    /**
     * Create a new {@link CarUiText}.
     *
     * @param text text to display
     */
    public CarUiText(@NonNull CharSequence text) {
        this(text, Integer.MAX_VALUE);
    }

    /**
     * Create a new {@link CarUiText}.
     *
     * @param text     text to display
     * @param maxLines the maximum number of lines the text should be displayed on when width
     *                 constraints force the text to be wrapped. Text that exceeds the maximum
     *                 number of lines is ellipsized
     */
    public CarUiText(@NonNull CharSequence text, int maxLines) {
        mText = Collections.singletonList(text);
        mMaxLines = maxLines;
    }

    /**
     * Returns the maximum number of lines the text should be displayed on when width constraints
     * force the text to be wrapped
     */
    public int getMaxLines() {
        return mMaxLines;
    }


    public List<CharSequence> getText() {
        return mText;
    }

    /**
     * Returns the preferred text to render for this {@link CarUiText}.
     */
    public CharSequence getPreferredText() {
        return mText.get(0);
    }
}
