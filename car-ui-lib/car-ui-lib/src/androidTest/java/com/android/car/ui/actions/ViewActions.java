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

package com.android.car.ui.actions;

import android.view.View;

import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

public class ViewActions {

    public static ViewAction waitForView(Matcher<View> matcher, long waitTimeMillis) {
        return new WaitForViewAction(matcher, waitTimeMillis);
    }

    public static ViewAction waitForView(Matcher<View> matcher) {
        return new WaitForViewAction(matcher, 500);
    }

    public static ViewAction waitForNoMatchingView(Matcher<View> matcher, long waitTimeMillis) {
        return new WaitForNoMatchingViewAction(matcher, waitTimeMillis);
    }

    public static ViewAction waitForNoMatchingView(Matcher<View> matcher) {
        return new WaitForNoMatchingViewAction(matcher, 500);
    }

    public static ViewAction setProgress(int progress) {
        return new SetProgressViewAction(progress);
    }
}
