/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.car.chassis.paintbooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.car.chassis.Toolbar;

/**
 * Activity that shows different dialogs from the device default theme.
 */
public class DialogSamples extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_samples);

        Button showDialogButton = findViewById(R.id.show_dialog_bt);
        Button showDialogOnlyPositiveButton = findViewById(R.id.show_dialog_only_positive_bt);
        Button showDialogWithoutTitleButton = findViewById(R.id.show_dialog_without_title);
        Button showDialogWithCheckboxButton = findViewById(R.id.show_dialog_with_checkbox_bt);
        showDialogButton.setOnClickListener(v -> openDialog(false));
        showDialogOnlyPositiveButton.setOnClickListener(v -> openDialogWithOnlyPositiveButton());
        showDialogWithoutTitleButton.setOnClickListener(v -> openDialogWithoutTitle());
        showDialogWithCheckboxButton.setOnClickListener(v -> openDialog(true));
        Button showToast = findViewById(R.id.show_toast);
        showToast.setOnClickListener(v -> showToast());
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setState(Toolbar.State.SUBPAGE);
        toolbar.addListener(new Toolbar.Listener() {
            @Override
            public void onBack() {
                finish();
            }
        });
    }

    private void openDialog(boolean showCheckbox) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (showCheckbox) {
            // Set Custom Title
            TextView title = new TextView(this);
            // Title Properties
            title.setText("Custom Dialog Box");
            builder.setCustomTitle(title);
            builder.setMultiChoiceItems(
                    new CharSequence[]{"I am a checkbox"},
                    new boolean[]{false},
                    (dialog, which, isChecked) -> {
                    });
        } else {
            builder.setTitle("Standard Alert Dialog").setMessage("With a message to show.");
        }

        builder
                .setPositiveButton("OK", (dialoginterface, i) -> {
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                });
        builder.show();
    }

    private void openDialogWithOnlyPositiveButton() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Standard Alert Dialog").setMessage("With a message to show.");
        builder.setPositiveButton("OK", (dialoginterface, i) -> {
        });
        builder.show();
    }

    private void openDialogWithoutTitle() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("I dont have a titile.");
        builder
                .setPositiveButton("OK", (dialoginterface, i) -> {
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                });
        builder.show();
    }

    private void showToast() {
        Toast.makeText(this, "Toast message looks like this", Toast.LENGTH_LONG).show();
    }
}
