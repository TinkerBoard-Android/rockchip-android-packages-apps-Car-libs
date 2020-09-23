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

package com.android.car.ui.utils;

import static android.view.View.VISIBLE;

import static com.android.car.ui.utils.RotaryConstants.ROTARY_HORIZONTALLY_SCROLLABLE;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_VERTICALLY_SCROLLABLE;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class used by {@link com.android.car.ui.FocusArea} and {@link
 * com.android.car.ui.FocusParkingView}.
 *
 * @hide
 */
public final class ViewUtils {

    /** This is a utility class */
    private ViewUtils() {
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * view that is focused by default, can take focus, but has no invisible ancestors. Returns null
     * if not found.
     */
    @Nullable
    public static View findFocusedByDefaultView(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> v.isFocusedByDefault() && canTakeFocus(v),
                /* skipPredicate= */ v -> v.getVisibility() != VISIBLE);
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * primary focus view, i.e., the first focusable item in a scrollable container. Returns null
     * if not found.
     */
    public static View findPrimaryFocusView(@NonNull View view) {
        View scrollableContainer = findScrollableContainer(view);
        return scrollableContainer == null ? null : findFocusableDescendant(scrollableContainer);
    }

    /**
     * Searches the {@code view}'s descendants in depth first order, and returns the first view
     * that can take focus but has no invisible ancestors, or null if not found.
     */
    @Nullable
    public static View findFocusableDescendant(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> v != view && canTakeFocus(v),
                /* skipPredicate= */ v -> v.getVisibility() != VISIBLE);
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * view that meets the given condition. Returns null if not found.
     */
    @Nullable
    public static View depthFirstSearch(@NonNull View view, @NonNull Predicate<View> predicate) {
        return depthFirstSearch(view, predicate, /* skipPredicate= */ null);
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, skips the views that
     * match {@code skipPredicate} and their descendants, and returns the first view that matches
     * {@code targetPredicate}. Returns null if not found.
     */
    @Nullable
    private static View depthFirstSearch(@NonNull View view,
            @NonNull Predicate<View> targetPredicate,
            @NonNull Predicate<View> skipPredicate) {
        if (skipPredicate != null && skipPredicate.test(view)) {
            return null;
        }
        if (targetPredicate.test(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                View target = depthFirstSearch(child, targetPredicate, skipPredicate);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * This is a functional interface and can therefore be used as the assignment target for a
     * lambda expression or method reference.
     *
     * @param <T> the type of the input to the predicate
     */
    public interface Predicate<T> {
        /** Evaluates this predicate on the given argument. */
        boolean test(@NonNull T t);
    }

    /**
     * Searches the {@code view} and its descendants in depth first order, and returns the first
     * scrollable container that has no invisible ancestors. Returns null if not found.
     */
    @Nullable
    private static View findScrollableContainer(@NonNull View view) {
        return depthFirstSearch(view,
                /* targetPredicate= */ v -> {
                    CharSequence contentDescription = v.getContentDescription();
                    return TextUtils.equals(contentDescription, ROTARY_VERTICALLY_SCROLLABLE)
                            || TextUtils.equals(contentDescription, ROTARY_HORIZONTALLY_SCROLLABLE);
                },
                /* skipPredicate= */ v -> v.getVisibility() != VISIBLE);
    }

    /** Returns whether {@code view} can be focused. */
    private static boolean canTakeFocus(@NonNull View view) {
        return view.isFocusable() && view.isEnabled() && view.getVisibility() == VISIBLE
                && view.getWidth() > 0 && view.getHeight() > 0;
    }
}
