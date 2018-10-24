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
 * Provides a loading field switchMaps which may take a long time to finish.
 *
 * @param <T> the output type for the switchMap
 * @see androidx.lifecycle.Transformations#switchMap(LiveData, Function) switchMap
 */
// TODO(b/117950502): Remove single method interface, just return a LiveData<FutureData<T>>
public interface LoadingSwitchMap<T> {

    /**
     * Returns a FutureData with the loading status and output data of the function.
     */
    LiveData<FutureData<T>> getLoadingOutput();

    /**
     * Custom MediatorLiveData that emits a FutureData containing loading status and function output
     *
     * This MediatorLiveData emits values only when the loading status of the output changes.
     * If the output is loading, the emitted FutureData will have a null value for the data.
     */
    static <X, Y> LoadingSwitchMap<Y> loadingSwitchMap(LiveData<X> trigger,
            Function<X, LiveData<Y>> function) {
        LiveData<Y> output = switchMap(trigger, function);
        LiveData<FutureData<Y>> loadingOutput =
                new MediatorLiveData<FutureData<Y>>() {
                    {
                        addSource(trigger, data ->
                                setValue(new FutureData<>(true, null)));
                        addSource(output, data ->
                                setValue(new FutureData<>(false, output.getValue())));
                    }
                };
        return () -> loadingOutput;
    }
}
