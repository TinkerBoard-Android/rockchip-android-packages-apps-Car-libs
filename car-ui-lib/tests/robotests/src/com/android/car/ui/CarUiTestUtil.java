/*
 * Copyright 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;

import org.robolectric.RuntimeEnvironment;

/**
 * Collection of test utility methods
 */
public class CarUiTestUtil {

    /**
     * Returns a mocked {@link Context} to be used in Robolectric tests.
     */
    public static Context getMockContext() {
        Context context = spy(RuntimeEnvironment.application);
        Resources mResources = spy(context.getResources());

        when(context.getResources()).thenReturn(mResources);

        // Temporarily create a layout inflater that will be used to clone a new one.
        LayoutInflater tempInflater = LayoutInflater.from(context);
        // Force layout inflater to use spied context
        doAnswer(invocation -> tempInflater.cloneInContext(context))
                .when(context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Older versions of Robolectric do not correctly handle the Resources#getValue() method.
        // This breaks CarUtils.findViewByRefId() functionality in tests. To workaround this issue,
        // use a spy to rely on findViewById() functionality instead.
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((TypedValue) args[1]).resourceId = (int) args[0];
            return null; // void method, so return null
        }).when(mResources).getValue(anyInt(), isA(TypedValue.class), isA(Boolean.class));
        return context;
    }
}
