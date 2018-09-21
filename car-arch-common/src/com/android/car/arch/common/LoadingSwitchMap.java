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

import static androidx.lifecycle.Transformations.switchMap;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

/**
 * Provides an isLoading() method for switchMaps which may take a long time to finish.
 *
 * @param <T> the output type for the switchMap
 * @see androidx.lifecycle.Transformations#switchMap(LiveData, Function) switchMap
 */
public interface LoadingSwitchMap<T> {

    /**
     * Returns the output data of the function.
     */
    LiveData<T> getOutput();

    /**
     * Returns a LiveData that emits true iff the trigger has notified but the output has not yet.
     */
    LiveData<Boolean> isLoading();

    /**
     * Returns a new LoadingSwitchMap
     *
     * @see androidx.lifecycle.Transformations#switchMap(LiveData, Function)
     */
    static <X, Y> LoadingSwitchMap<Y> loadingSwitchMap(LiveData<X> trigger,
            Function<X, LiveData<Y>> function) {
        LiveData<Y> output = switchMap(trigger, function);
        LiveData<Boolean> isLoading = new MediatorLiveData<Boolean>() {
            {
                addSource(trigger, data -> setValue(true));
                addSource(output, data -> setValue(false));
            }
        };
        return new LoadingSwitchMap<Y>() {
            @Override
            public LiveData<Y> getOutput() {
                return output;
            }

            @Override
            public LiveData<Boolean> isLoading() {
                return isLoading;
            }
        };
    }
}
