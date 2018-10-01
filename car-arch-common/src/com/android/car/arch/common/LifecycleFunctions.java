/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.car.arch.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Utility methods for combining or mutating {@link Lifecycle}s
 */
public class LifecycleFunctions {
    private LifecycleFunctions() {
        // no instances
    }

    /**
     * Returns a LifecycleOwner whose Lifecycle's state is the lower of the two. E.g. if one is
     * {@link State#CREATED} and the other is {@link State#RESUMED}, the returned LifecycleOwner
     * will be {@link State#CREATED}.
     */
    public static LifecycleOwner lesserOf(Lifecycle left, Lifecycle right) {
        return new LifecycleOwner() {
            private DefaultLifecycleObserver mUpdateObserver = new DefaultLifecycleObserver() {
                @Override
                public void onCreate(@NonNull LifecycleOwner owner) {
                    update();
                }

                @Override
                public void onStart(@NonNull LifecycleOwner owner) {
                    update();
                }

                @Override
                public void onResume(@NonNull LifecycleOwner owner) {
                    update();
                }

                @Override
                public void onPause(@NonNull LifecycleOwner owner) {
                    update();
                }

                @Override
                public void onStop(@NonNull LifecycleOwner owner) {
                    update();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    update();
                }
            };

            private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

            {
                left.addObserver(mUpdateObserver);
                right.addObserver(mUpdateObserver);
            }

            @NonNull
            @Override
            public Lifecycle getLifecycle() {
                return mLifecycleRegistry;
            }

            private void update() {
                Lifecycle.State leftState = left.getCurrentState();
                Lifecycle.State rightState = right.getCurrentState();
                // choose the lower state
                if (leftState.isAtLeast(rightState)) {
                    mLifecycleRegistry.markState(rightState);
                } else {
                    mLifecycleRegistry.markState(leftState);
                }
            }
        };

    }

    /**
     * Returns a LifecycleOwner whose Lifecycle's state is the lower of the two. E.g. if one is
     * {@link State#CREATED} and the other is {@link State#RESUMED}, the returned LifecycleOwner
     * will be {@link State#CREATED}.
     */
    public static LifecycleOwner lesserOf(LifecycleOwner left, LifecycleOwner right) {
        return lesserOf(left.getLifecycle(), right.getLifecycle());
    }
}
