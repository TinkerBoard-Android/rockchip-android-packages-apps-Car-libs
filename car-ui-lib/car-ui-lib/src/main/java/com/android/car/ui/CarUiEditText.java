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

package com.android.car.ui;

import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_ITEM_ID;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.SEARCH_RESULT_SECONDARY_IMAGE_ID;
import static com.android.car.ui.imewidescreen.CarUiImeWideScreenController.WIDE_SCREEN_CLEAR_DATA_ACTION;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.HashSet;
import java.util.Set;

/**
 * Edit text supporting the callbacks from the IMS.This will be useful in widescreen IME mode to
 * notify apps of specific action through Interface.
 */
public class CarUiEditText extends EditText {
    /**
     * Interface for {@link CarUiEditText} to support different actions and callbacks from IME
     * when running in wide screen mode.
     */
    public interface PrivateImeCommandCallback {
        /**
         * Called when user clicks on an item in the search results.
         *
         * @param itemId the id of the item clicked. This will be the same id that was passed by the
         *               application to the IMS template to display search results.
         */
        void onItemClicked(String itemId);

        /**
         * Called when user clicks on a secondary image within item in the search results.
         *
         * @param secondaryImageId the id of the secondary image clicked. This will be the same id
         *                         that was passed by the application to the IMS template to display
         *                         search results.
         */
        void onSecondaryImageClicked(String secondaryImageId);
    }

    private final Set<PrivateImeCommandCallback> mPrivateImeCommandCallback = new HashSet<>();

    public CarUiEditText(Context context) {
        super(context);
    }

    public CarUiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiEditText(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onPrivateIMECommand(String action, Bundle data) {

        if (WIDE_SCREEN_CLEAR_DATA_ACTION.equals(action)) {
            // clear the text.
            setText("");
        }

        if (data == null || mPrivateImeCommandCallback == null) {
            return false;
        }

        if (data.getString(SEARCH_RESULT_ITEM_ID) != null) {
            for (PrivateImeCommandCallback listener : mPrivateImeCommandCallback) {
                listener.onItemClicked(data.getString(SEARCH_RESULT_ITEM_ID));
            }
        }

        if (data.getString(SEARCH_RESULT_SECONDARY_IMAGE_ID) != null) {
            for (PrivateImeCommandCallback listener : mPrivateImeCommandCallback) {
                listener.onSecondaryImageClicked(
                        data.getString(SEARCH_RESULT_SECONDARY_IMAGE_ID));
            }
        }

        return false;
    }

    /**
     * Registers a new {@link PrivateImeCommandCallback} to the list of
     * listeners.
     */
    public void registerOnPrivateImeCommandListener(PrivateImeCommandCallback listener) {
        mPrivateImeCommandCallback.add(listener);
    }

    /**
     * Unregisters an existing {@link PrivateImeCommandCallback} from the list
     * of listeners.
     */
    public boolean unregisterOnPrivateImeCommandListener(PrivateImeCommandCallback listener) {
        return mPrivateImeCommandCallback.remove(listener);
    }
}
