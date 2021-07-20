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
package com.chassis.car.ui.plugin.toolbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.car.ui.plugin.oemapis.toolbar.MenuItemOEMV1;

import com.chassis.car.ui.plugin.R;

import java.util.Collections;
import java.util.List;

class OverflowMenuItem {

    @NonNull
    private final Context mSourceContext;

    @NonNull
    private List<MenuItemOEMV1> mOverflowMenuItems = Collections.emptyList();

    @Nullable
    private Dialog mDialog;

    private MenuItemOEMV1 mMenuItem;

    OverflowMenuItem(
            @NonNull Context pluginContext,
            @NonNull Context sourceContext) {
        mSourceContext = sourceContext;

        mMenuItem = MenuItemOEMV1.builder()
                .setTitle(pluginContext.getString(R.string.toolbar_menu_item_overflow_title))
                .setIcon(ContextCompat.getDrawable(
                        pluginContext, R.drawable.toolbar_menu_item_overflow))
                .setVisible(false)
                .setOnClickListener(() -> {
                    String[] titles = mOverflowMenuItems.stream()
                            .map(MenuItemOEMV1::getTitle)
                            .toArray(String[]::new);

                    // TODO(b/194233067) Do not create dialogs using the source context, it is
                    //                   not always an activity context and will crash in cases
                    //                   where it isn't. Replace this with a layer in the base
                    //                   layout that looks like a dialog.
                    mDialog = new AlertDialog.Builder(sourceContext)
                            .setItems(titles, (dialog, which) -> {
                                Runnable onClickListener = mOverflowMenuItems.get(which)
                                        .getOnClickListener();
                                if (onClickListener != null) {
                                    onClickListener.run();
                                }
                                dialog.dismiss();
                            }).create();
                    mDialog.show();
                })
                .build();
    }

    public MenuItemOEMV1 getMenuItem() {
        return mMenuItem;
    }

    public void setOverflowMenuItems(List<MenuItemOEMV1> menuItems) {
        mOverflowMenuItems = menuItems;
        mMenuItem = mMenuItem.copy().setVisible(!menuItems.isEmpty()).build();

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
