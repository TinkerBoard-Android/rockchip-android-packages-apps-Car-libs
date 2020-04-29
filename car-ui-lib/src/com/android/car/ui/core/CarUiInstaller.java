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
package com.android.car.ui.core;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * {@link ContentProvider ContentProvider's} onCreate() methods are "called for all registered
 * content providers on the application main thread at application launch time." This means we
 * can use a content provider to register for Activity lifecycle callbacks before any activities
 * have started, for installing the CarUi base layout into all activities.
 */
public class CarUiInstaller extends ContentProvider {

    private static final boolean IS_DEBUG_DEVICE =
            Build.TYPE.toLowerCase(Locale.ROOT).contains("debug")
                    || Build.TYPE.toLowerCase(Locale.ROOT).equals("eng");

    private static boolean sIsInstalled = false;

    /**
     * If for some reason the ContentProvider cannot be used, this method can be
     * used instead. Be sure to call it before the first activity is created, so ideally
     * in your {@link Application} class. This should only be used as a last resort,
     * prefer relying on the ContentProvider to call this for you.
     */
    public static void install(Context context) {
        if (sIsInstalled) {
            return;
        }

        Application application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        BaseLayoutController.build(activity);
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        BaseLayoutController.destroy(activity);
                    }
                });

        // Check only if we are in debug mode.
        if (IS_DEBUG_DEVICE) {
            CheckCarUiComponents checkCarUiComponents = new CheckCarUiComponents(application);
            application.registerActivityLifecycleCallbacks(checkCarUiComponents);
        }

        sIsInstalled = true;
    }

    @Override
    public boolean onCreate() {
        install(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }
}
