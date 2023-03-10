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

package com.android.car.ui.recyclerview;

import static com.android.car.ui.core.CarUi.MIN_TARGET_API;

import android.annotation.TargetApi;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.test.R;

@TargetApi(MIN_TARGET_API)
public class TestViewHolder extends RecyclerView.ViewHolder {

    private CharSequence mText;

    TestViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    void bind(CharSequence text) {
        mText = text;
        TextView textView = itemView.requireViewById(R.id.textTitle);
        textView.setText(text);
    }

    CharSequence getText() {
        return mText;
    }
}
