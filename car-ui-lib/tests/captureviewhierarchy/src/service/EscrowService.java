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

package service;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;

/**
 * To start the service:
 * adb shell am startservice <package-name>/service.EscrowService
 *
 * To stop the service:
 * adb shell am stopservice <package-name>/service.EscrowService
 *
 * To test the components
 * adb shell am broadcast -a com.android.car.ui.intent.DUMP_VIEW_HIERARCHY
 *
 * Start the service, navigate to the screen to be tested. Fire the intent.
 */
public class EscrowService extends Service {

    private static final String TAG = EscrowService.class.getSimpleName();
    private static final String INTENT_FILTER = "com.android.car.ui.intent.DUMP_VIEW_HIERARCHY";

    private View mRootView;
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

    public static final boolean IS_DEBUG_DEVICE =
            Build.TYPE.toLowerCase(Locale.ROOT).contains("debug")
                    || Build.TYPE.toLowerCase(Locale.ROOT).equals("eng");

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CarUiComponents carUiComponents = new CarUiComponents();
            checkForCarUiComponents(mRootView, carUiComponents);
            if (carUiComponents.mIsUsingCarUiRecyclerView
                    && !carUiComponents.mIsCarUiRecyclerViewUsingListItem) {
                Log.e(TAG, "CarUiListItem are not used within CarUiRecyclerView: ");
                Toast.makeText(getApplicationContext(),
                        "CarUiListItem are not used within CarUiRecyclerView",
                        Toast.LENGTH_LONG).show();
            }
            if (!carUiComponents.mIsUsingCarUiToolbar) {
                Log.e(TAG, "CarUiToolbar is not used: ");
                Toast.makeText(getApplicationContext(), "CarUiToolbar is not used",
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                mRootView = activity.getWindow().getDecorView().getRootView();
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
            }
        };
        ((Application) getApplicationContext()).registerActivityLifecycleCallbacks(
                mActivityLifecycleCallbacks);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Escrow service started", Toast.LENGTH_LONG).show();

        // Register the filter only if we are in debug mode.
        if (IS_DEBUG_DEVICE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(INTENT_FILTER);
            registerReceiver(mReceiver, filter);
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        ((Application) getApplicationContext()).unregisterActivityLifecycleCallbacks(
                mActivityLifecycleCallbacks);
        Toast.makeText(this, "Escrow service destroyed", Toast.LENGTH_LONG).show();
    }

    private void checkForCarUiComponents(View view, CarUiComponents carUiComponents) {

        if (view == null) {
            return;
        }

        if (isCarUiRecyclerView(view)) {
            carUiComponents.mIsUsingCarUiRecyclerView = true;
            if (isCarUiListItemFound(view)) {
                carUiComponents.mIsCarUiRecyclerViewUsingListItem = true;
            }
            return;
        }

        if (isCarUiToolbar(view)) {
            carUiComponents.mIsUsingCarUiToolbar = true;
        }

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                checkForCarUiComponents(((ViewGroup) view).getChildAt(i), carUiComponents);
            }
        }
    }

    private boolean isCarUiListItemFound(View view) {

        if (view == null) {
            return false;
        }

        if (isCarUiListItem(view)) {
            return true;
        }

        if (!(view instanceof ViewGroup)) {
            return false;
        }

        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            if (isCarUiListItemFound(((ViewGroup) view).getChildAt(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean isCarUiRecyclerView(View view) {
        return view.getTag() != null && view.getTag().toString().equals("carUiRecyclerView");
    }

    private boolean isCarUiListItem(View view) {
        return view.getTag() != null && view.getTag().toString().equals("carUiListItem");
    }

    private boolean isCarUiToolbar(View view) {
        return view.getTag() != null && (view.getTag().toString().equals("carUiToolbar")
                || view.getTag().toString().equals(
                "CarUiBaseLayoutToolbar"));
    }

    private static class CarUiComponents {
        boolean mIsUsingCarUiRecyclerView;
        boolean mIsCarUiRecyclerViewUsingListItem;
        boolean mIsUsingCarUiToolbar;
    }

    /**
     * Dump's the view hierarchy.
     */
    private void printViewHierarchy(String indent, View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n ");
        sb.append(indent);
        sb.append('{');

        if (view == null) {
            sb.append("viewNode= NULL, ");
            sb.append('}');
            return;
        }

        sb.append("viewNode= " + view.toString() + ", ");
        sb.append("id= " + view.getId() + ", ");
        sb.append("name= " + view.getAccessibilityClassName() + ", ");

        sb.append('}');
        System.out.println(sb.toString());

        indent += "  ";
        if (!(view instanceof ViewGroup)) {
            return;
        }
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            printViewHierarchy(indent, ((ViewGroup) view).getChildAt(i));
        }
    }
}
