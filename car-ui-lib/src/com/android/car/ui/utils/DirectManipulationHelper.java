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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.android.car.ui.R;

import java.lang.reflect.Method;

/** Helper class to toggle direct manipulation mode. */
public final class DirectManipulationHelper {

    /**
     * StateDescription for a {@link View} to support direct manipulation mode. It's also used as
     * class name of {@link AccessibilityEvent} to indicate that the AccessibilityEvent represents
     * a request to toggle direct manipulation mode.
     */
    private final String mDirectManipulation;

    public DirectManipulationHelper(@NonNull Context context) {
        mDirectManipulation =
                context.getResources().getString(R.string.car_ui_direct_manipulation);
    }

    /**
     * Enables or disables direct manipulation mode. This method sends an {@link AccessibilityEvent}
     * to tell {@link com.android.car.rotary.RotaryService} to enter or exit direct manipulation
     * mode. Typically pressing the center button of the rotary controller with a direct
     * manipulation view focused will enter direct manipulation mode, while pressing the Back button
     * will exit direct manipulation mode.
     *
     * @param view   the direct manipulation view
     * @param enable true to enter direct manipulation mode, false to exit direct manipulation mode
     * @return whether the AccessibilityEvent was sent
     */
    public boolean enableDirectManipulationMode(@NonNull View view, boolean enable) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return false;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain();
        event.setClassName(mDirectManipulation);
        event.setSource(view);
        event.setEventType(enable
                ? AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                : AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
        accessibilityManager.sendAccessibilityEvent(event);
        return true;
    }

    /** Returns whether the given {@code event} is for direct manipulation. */
    public boolean isDirectManipulation(@NonNull AccessibilityEvent event) {
        return TextUtils.equals(mDirectManipulation, event.getClassName());
    }

    /** Returns whether the given {@code node} supports direct manipulation. */
    @TargetApi(Build.VERSION_CODES.R)
    public boolean supportDirectManipulation(@NonNull AccessibilityNodeInfo node) {
        try {
            // TODO(b/156115044): remove the reflection once Android R sdk is publicly released.
            Method getStateDescription =
                    AccessibilityNodeInfo.class.getMethod("getStateDescription");
            CharSequence stateDescription = (CharSequence) getStateDescription.invoke(node);
            return TextUtils.equals(mDirectManipulation, stateDescription);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Sets whether the given {@code view} supports direct manipulation. */
    @TargetApi(Build.VERSION_CODES.R)
    public void setSupportsDirectManipulation(@NonNull View view, boolean enable) {
        try {
            // TODO(b/156115044): remove the reflection once Android R sdk is publicly released.
            Method setStateDescription =
                    View.class.getMethod("setStateDescription", CharSequence.class);
            CharSequence stateDescription = enable ? mDirectManipulation : null;
            setStateDescription.invoke(view, stateDescription);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
