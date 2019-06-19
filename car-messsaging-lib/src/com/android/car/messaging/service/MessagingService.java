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

package com.android.car.messaging.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.MainThread;

import com.android.car.messaging.entity.BaseEntity;
import com.android.car.messaging.entity.Conversation;
import com.android.car.messaging.entity.ConversationContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for messaging apps which enables messaging apps to provide their data to authorized
 * clients.
 */
public abstract class MessagingService extends Service {
    private static final String TAG = "MessagingService";

    /**
     * The key for a client to specify the class name of an entity which the client is
     * subscribed to.
     */
    public static final String EXTRA_SUBSCRIBED_CLASS_NAME = "subscribed_class_name";

    /**
     * The root id for {@link ConversationContainer}.
     */
    public static final String CONVERSATION_CONTAINER_ROOT_ID = "conversation_container_id";

    private MessagingServiceProviderImpl mMessagingServiceProviderImpl;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    // TODO: Merging these two Maps into one when there are more attributes to store for an ID.
    private Map<String, List<IBinder>> mIdToSubscriptionMap = new ArrayMap<>();
    private Map<String, String> mIdToClassNameMap = new ArrayMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mMessagingServiceProviderImpl = new MessagingServiceProviderImpl();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessagingServiceProviderImpl;
    }

    private class MessagingServiceProviderImpl extends IMessagingServiceProvider.Stub {
        private MessagingServiceImpl mMessagingService = new MessagingServiceImpl();

        @Override
        public SupportedVersionInfo getSupportedApiVersionInfo() {
            return new SupportedVersionInfo(/* minVersion = */ 1, /* maxVersion = */ 1);
        }

        @Override
        public void connect(ConnectionConfig connectionConfig,
                IConnectionCallback callback) throws RemoteException {
            int uid = Binder.getCallingUid();
            if (isValidPackage(connectionConfig.getPackageName(), uid)) {
                mMainThreadHandler.post(() -> {
                    try {
                        if (onAuthenticate(connectionConfig.getPackageName())) {
                            callback.onConnect(mMessagingService);
                        } else {
                            callback.onConnectFailed();
                        }
                    } catch (RemoteException e) {
                        Log.w(TAG, "Calling IConnectionCallback failed. Ignoring. "
                                + "pkg=" + connectionConfig.getPackageName());
                    }
                });
            } else {
                callback.onConnectFailed();
            }
        }

        /**
         * Returns true if the provided package name belongs to the given uid.
         */
        private boolean isValidPackage(String pkg, int uid) {
            if (pkg == null) {
                return false;
            }

            final PackageManager pm = getPackageManager();
            final String[] packages = pm.getPackagesForUid(uid);
            if (packages == null) {
                return false;
            }

            for (String aPackage : packages) {
                if (aPackage.equals(pkg)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class MessagingServiceImpl extends IMessagingService.Stub {

        @Override
        public void subscribeContent(
                String id, ILoadEntityContentCallback callback, Bundle options) {
            mMainThreadHandler.post(() -> {
                try {
                    MessagingService.this.subscribeContent(id, callback, options);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to subscribe to content. callback = " + callback);
                    MessagingService.this.unsubscribeContent(id, callback);
                }
            });
        }

        @Override
        public void unsubscribeContent(String id, ILoadEntityContentCallback callback) {
            mMainThreadHandler.post(() -> MessagingService.this.unsubscribeContent(id, callback));
        }
    }

    @MainThread
    private void subscribeContent(String id, ILoadEntityContentCallback callback, Bundle options)
            throws RemoteException {
        List<IBinder> allSubscriptions = mIdToSubscriptionMap.computeIfAbsent(
                id, (key) -> new ArrayList<>());

        if (allSubscriptions.contains(callback.asBinder())) {
            return;
        }

        allSubscriptions.add(callback.asBinder());
        String entityClassName = options.getString(EXTRA_SUBSCRIBED_CLASS_NAME);
        mIdToClassNameMap.put(id, entityClassName);
        List<? extends BaseEntity> entities = loadEntityContentList(id, entityClassName);
        callback.onEntitiesLoaded(entities);
    }

    @MainThread
    private void unsubscribeContent(String id, ILoadEntityContentCallback callback) {
        List<IBinder> allSubscriptions = mIdToSubscriptionMap.computeIfAbsent(
                id, (key) -> new ArrayList<>());
        allSubscriptions.remove(callback.asBinder());
        if (allSubscriptions.isEmpty()) {
            mIdToClassNameMap.remove(id);
        }
    }

    private List<? extends BaseEntity> loadEntityContentList(String id, String className) {
        if (CONVERSATION_CONTAINER_ROOT_ID.equals(id)) {
            return onLoadAllConversationContainers();
        } else if (className.equals(ConversationContainer.class.getName())) {
            return onLoadConversations(id);
        }

        return new ArrayList<>();
    }

    /**
     * Called when a client tries to connect to this {@link MessagingService}. Returns true if
     * the client is authorized to access
     *
     * @param packageName The calling client's package name.
     */
    protected abstract boolean onAuthenticate(String packageName);

    /**
     * Called to load all {@link ConversationContainer}s.
     */
    protected abstract List<ConversationContainer> onLoadAllConversationContainers();

    /**
     * Called to load all conversations in a {@link ConversationContainer}.
     */
    protected abstract List<Conversation> onLoadConversations(String containerId);

    /**
     * Notifies the client that content {@link BaseEntity entities} of a {@link BaseEntity} has
     * changed.
     */
    @MainThread
    public void notifyContentListChanged(String id) {
        List<IBinder> allSubscriptions = mIdToSubscriptionMap.get(id);
        if (allSubscriptions == null) {
            return;
        }
        try {
            for (IBinder callback : allSubscriptions) {
                List<? extends BaseEntity> entities =
                        loadEntityContentList(id, mIdToClassNameMap.get(id));
                ILoadEntityContentCallback.Stub.asInterface(callback).onEntitiesLoaded(entities);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to call ILoadEntityContentCallback.");
        }
    }
}
