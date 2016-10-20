/**
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.car.stream;

import com.google.android.car.stream.IStreamListener;
import com.google.android.car.stream.StreamCard;

/**
 * An API for clients to communicate with StreamManager.
 */
interface IStreamManagerService {
    /**
     * Registers the given listener to listen to changes to cards within the StreamManager. The
     * listener will not notified of StreamCards that do not support the {@code StreamCardCategory}
     * passed to this method.
     *
     * @param listener The listener for changes.
     * @param streamCardCategories The size of {@link StreamCard}s that the listener is interested
     *     in. Must be one of the stream card sized defined in {@link StreamCardCategory}. To
     *     indicate more than one cateogry, separate each size with a "|" operator.
     */
    void registerListener(in IStreamListener listener, int streamCardCategories);

    /**
     * Removes the given listener from the list of listeners within the StreamManager.
     */
    void unregisterListener(in IStreamListener listener);

    /**
     * Returns all {@link StreamCard}s that the StreamManager currently has.
     */
    List<StreamCard> fetchAllStreamCards();
}
