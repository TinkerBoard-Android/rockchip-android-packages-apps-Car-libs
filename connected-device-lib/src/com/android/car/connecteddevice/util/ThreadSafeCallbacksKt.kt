/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.connecteddevice.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/** Class for issuing thread-safe callbacks. */
internal class ThreadSafeCallbacksKt<T> {

    private val callbacks = ConcurrentHashMap<T, Executor>()

    /** Number of callbacks in collection. */
    val size = callbacks.size

    /** Add a [callback] to be notified on its [executor]. */
    fun add(callback: T, executor: Executor) {
        callbacks[callback] = executor
    }

    /** Remove a callback from the collection. */
    fun remove(callback: T) {
        callbacks.remove(callback)
    }

    /** Clear all callbacks from the collection. */
    fun clear() {
        callbacks.clear()
    }

    /** Invoke [notification] on all callbacks with their supplied [Executor]. */
    fun invoke(notification: (T) -> Unit) {
        callbacks.forEach { (callback, executor) ->
            executor.execute { notification(callback) }
        }
    }
}
