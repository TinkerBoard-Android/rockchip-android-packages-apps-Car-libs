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
package com.android.car.ui.paintbooth.button;

import static com.android.car.ui.core.CarUi.requireCarUiComponentById;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.car.ui.button.CarUiButton;
import com.android.car.ui.button.CarUiButtonAttributes;
import com.android.car.ui.button.CarUiButtonColorScheme;
import com.android.car.ui.button.CarUiButtonStyle;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Activity to demo {@link com.android.car.ui.button.CarUiButton CarUiButtons} */
public class CarUiButtonActivity extends AppCompatActivity {

    private ValueAnimator mColorAnimation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ToolbarController toolbar = CarUi.requireToolbar(this);
        toolbar.setTitle("CarUiButtons");
        toolbar.setState(Toolbar.State.SUBPAGE);
        setContentView(R.layout.car_ui_button_activity);
        List<LinearLayout> columns = Arrays.asList(
                findViewById(R.id.column_1),
                findViewById(R.id.column_2),
                findViewById(R.id.column_3));
        List<CarUiButton> allButtons = new ArrayList<>();
        CarUiButton disableButton = requireCarUiComponentById(
                findViewById(android.R.id.content), R.id.disable_button);
        boolean[] enabled = new boolean[] { true };
        disableButton.setOnClickListener(b -> {
            enabled[0] = !enabled[0];
            for (CarUiButton button : allButtons) {
                button.setEnabled(enabled[0]);
            }
        });

        for (CarUiButtonStyle style : CarUiButtonStyle.values()) {
            for (CarUiButtonColorScheme colorScheme : new CarUiButtonColorScheme[] {
                    CarUiButtonColorScheme.BASIC,
                    CarUiButtonColorScheme.RED,
                    CarUiButtonColorScheme.BLUE,
                    CarUiButtonColorScheme.GREEN,
                    CarUiButtonColorScheme.YELLOW,
                    CarUiButtonColorScheme.fromColor(0xFF00FF)}) {
                for (String text : new String[] {"Test", null}) {
                    for (Drawable icon : new Drawable[] { getDrawable(R.drawable.ic_cut), null}) {
                        CarUiButton button = CarUiButton.create(this, CarUiButtonAttributes
                                .builder()
                                .setStyle(style)
                                .setColorScheme(colorScheme)
                                .setText(text)
                                .setIcon(icon)
                                .build());

                        button.setOnClickListener(b ->
                                Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show());

                        allButtons.add(button);

                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        switch (style) {
                            case PRIMARY:
                                columns.get(0).addView(button.getView(), layoutParams);
                                break;
                            case SECONDARY:
                                columns.get(1).addView(button.getView(), layoutParams);
                                break;
                            default:
                                columns.get(2).addView(button.getView(), layoutParams);
                        }
                    }
                }
            }
        }

        mColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFFFF0000,
                0xFFFFFF00,
                0xFF00FF00,
                0xFF00FFFF,
                0xFF0000FF,
                0xFFFF00FF,
                0xFFFF0000);
        mColorAnimation.setDuration(10000);
        View floatingButtonsBackground = requireViewById(R.id.floating_buttons_background);
        mColorAnimation.addUpdateListener(animator ->
                floatingButtonsBackground.setBackgroundColor((int) animator.getAnimatedValue()));
        mColorAnimation.setRepeatCount(ValueAnimator.INFINITE);
        mColorAnimation.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mColorAnimation.cancel();
    }
}
