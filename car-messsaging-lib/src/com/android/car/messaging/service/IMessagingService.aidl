/**
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

package com.android.car.messaging.service;

import android.os.Bundle;

import  com.android.car.messaging.service.ILoadEntityContentCallback;

/**
 * An interface through which clients access to a messaging service.
 */
interface IMessagingService {
    /**
     * Loads the sub content of an entity and subscribes to any changes of the sub content.
     * Specify the class name of the entity with
     * {@link MessagingService#EXTRA_SUBSCRIBED_CLASS_NAME} key in the option Bundle.
     */
    void subscribeContent(String id, ILoadEntityContentCallback callback, in Bundle options) = 1;

    /**
     * Unregisters a callback which subscribed to sub content of an entity through
     * {@link #subscribeContent}.
     */
    void unsubscribeContent(String id, ILoadEntityContentCallback callback) = 2;
}