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

package com.android.car.ui.toolbar;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.ProgressBarControllerOEMV1;

class ProgressBarControllerAdapterV1 implements ProgressBarController {

    private ProgressBarControllerOEMV1 mOemProgressbar;

    ProgressBarControllerAdapterV1(ProgressBarControllerOEMV1 oemProgressbar) {
        mOemProgressbar = oemProgressbar;
    }

    @Override
    public void setVisible(boolean visible) {
        mOemProgressbar.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return mOemProgressbar.isVisible();
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        mOemProgressbar.setIndeterminate(indeterminate);
    }

    @Override
    public boolean isIndeterminate() {
        return mOemProgressbar.isIndeterminate();
    }

    @Override
    public void setMax(int max) {
        mOemProgressbar.setMax(max);
    }

    @Override
    public int getMax() {
        return mOemProgressbar.getMax();
    }

    @Override
    public void setMin(int min) {
        mOemProgressbar.setMin(min);
    }

    @Override
    public int getMin() {
        return mOemProgressbar.getMin();
    }

    @Override
    public void setProgress(int progress) {
        mOemProgressbar.setProgress(progress);
    }

    @Override
    public int getProgress() {
        return mOemProgressbar.getProgress();
    }
}
