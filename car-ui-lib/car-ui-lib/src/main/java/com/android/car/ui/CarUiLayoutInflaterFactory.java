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
package com.android.car.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.ui.button.CarUiButton;
import com.android.car.ui.button.CarUiButtonAttributes;
import com.android.car.ui.sharedlibrarysupport.SharedLibraryFactorySingleton;

/**
 * A custom {@link LayoutInflater.Factory2} that will create CarUi components such as
 * {@link CarUiButton}.
 */
public final class CarUiLayoutInflaterFactory implements LayoutInflater.Factory2 {

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Deprecated, do nothing.
        return null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context,
            AttributeSet attributeSet) {
        if (CarUiButton.class.getSimpleName().equals(name)) {
            CarUiButton controller = SharedLibraryFactorySingleton.get(context).createButton(
                    context, CarUiButtonAttributes.fromAttributeSet(context, attributeSet));
            View view = controller.getView();
            if (view != null) {
                if (parent instanceof ViewGroup) {
                    view.setLayoutParams(((ViewGroup) parent).generateLayoutParams(attributeSet));
                }
                view.setTag(R.id.car_ui_component_reference, controller);
                return view;
            }
        }

        return null;
    }
}
