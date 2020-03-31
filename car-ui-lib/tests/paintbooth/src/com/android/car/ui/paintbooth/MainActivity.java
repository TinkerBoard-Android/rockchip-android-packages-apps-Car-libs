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

package com.android.car.ui.paintbooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.paintbooth.caruirecyclerview.CarUiListItemActivity;
import com.android.car.ui.paintbooth.caruirecyclerview.CarUiRecyclerViewActivity;
import com.android.car.ui.paintbooth.caruirecyclerview.GridCarUiRecyclerViewActivity;
import com.android.car.ui.paintbooth.currentactivity.CurrentActivityService;
import com.android.car.ui.paintbooth.dialogs.DialogsActivity;
import com.android.car.ui.paintbooth.overlays.OverlayActivity;
import com.android.car.ui.paintbooth.preferences.PreferenceActivity;
import com.android.car.ui.paintbooth.toolbar.NoCarUiToolbarActivity;
import com.android.car.ui.paintbooth.toolbar.ToolbarActivity;
import com.android.car.ui.paintbooth.widgets.WidgetActivity;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.ToolbarController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Paint booth app
 */
public class MainActivity extends Activity implements InsetsChangedListener {

    /**
     * List of all sample activities.
     */
    private final List<Element> mActivities = Arrays.asList(
            new Element("Show foreground activities", CurrentActivityService.class, true),
            new Element("Simulate Screen Bounds", VisibleBoundsSimulator.class, true),
            new Element("Dialogs sample", DialogsActivity.class, false),
            new Element("List sample", CarUiRecyclerViewActivity.class, false),
            new Element("Grid sample", GridCarUiRecyclerViewActivity.class, false),
            new Element("Preferences sample", PreferenceActivity.class, false),
            new Element("Overlays", OverlayActivity.class, false),
            new Element("Toolbar sample", ToolbarActivity.class, false),
            new Element("No CarUiToolbar sample", NoCarUiToolbarActivity.class, false),
            new Element("Widget sample", WidgetActivity.class, false),
            new Element("ListItem sample", CarUiListItemActivity.class, false)
    );

    private class ViewHolder extends RecyclerView.ViewHolder {
        private Button mButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mButton = itemView.findViewById(R.id.button);
        }

        void bind(Element element) {
            mButton.setText(element.mDisplayText);
            mButton.setOnClickListener(element.mOnClickListener);

            if (element.mIsService && mButton instanceof Switch) {
                ((Switch) mButton).setChecked(isServiceRunning(element.mClass));
            }
        }
    }

    private final RecyclerView.Adapter<ViewHolder> mAdapter =
            new RecyclerView.Adapter<ViewHolder>() {

                private static final int TYPE_SWITCH = 0;
                private static final int TYPE_ACTIVITY = 1;

                @NonNull
                @Override
                public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View item = LayoutInflater.from(parent.getContext()).inflate(
                            viewType == TYPE_SWITCH ? R.layout.list_item_switch
                                    : R.layout.list_item,
                            parent, false);
                    return new ViewHolder(item);
                }

                @Override
                public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                    if (getItemViewType(position) == TYPE_SWITCH) {
                        Element item = mActivities.get(position);
                        item.mOnClickListener = v -> {
                            Intent intent = new Intent(holder.itemView.getContext(), item.mClass);
                            if (isServiceRunning(item.mClass)) {
                                // If you are about to add a new service you should extract an
                                // interface and generalize this instead.
                                if (item.mClass.getName().equals(
                                        CurrentActivityService.class.getName())) {
                                    intent.setAction(CurrentActivityService.STOP_SERVICE);
                                } else if (item.mClass.getName().equals(
                                        VisibleBoundsSimulator.class.getName())) {
                                    intent.setAction(VisibleBoundsSimulator.STOP_SERVICE);
                                }
                            }
                            startForegroundService(intent);
                        };
                        holder.bind(item);
                    } else {
                        Element item = mActivities.get(position);
                        item.mOnClickListener = v -> {
                            Intent intent = new Intent(holder.itemView.getContext(), item.mClass);
                            startActivity(intent);
                        };
                        holder.bind(item);
                    }
                }

                @Override
                public int getItemCount() {
                    return mActivities.size();
                }

                @Override
                public int getItemViewType(int position) {
                    return mActivities.get(position).mIsService ? TYPE_SWITCH : TYPE_ACTIVITY;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_ui_recycler_view_activity);

        ToolbarController toolbar = CarUi.requireToolbar(this);
        toolbar.setLogo(R.drawable.ic_launcher);
        toolbar.setTitle(getTitle());

        CarUiRecyclerView prv = findViewById(R.id.list);
        prv.setAdapter(mAdapter);

        initLeakCanary();
    }

    private void initLeakCanary() {
        // This sets LeakCanary to report errors after a single leak instead of 5, and to ask for
        // permission to use storage, which it needs to work.
        //
        // Equivalent to this non-reflection code:
        //
        // Config config = LeakCanary.INSTANCE.getConfig();
        // LeakCanary.INSTANCE.setConfig(config.copy(config.getDumpHeap(),
        //     config.getDumpHeapWhenDebugging(),
        //     1,
        //     config.getReferenceMatchers(),
        //     config.getObjectInspectors(),
        //     config.getOnHeapAnalyzedListener(),
        //     config.getMetatadaExtractor(),
        //     config.getComputeRetainedHeapSize(),
        //     config.getMaxStoredHeapDumps(),
        //     true,
        //     config.getUseExperimentalLeakFinders()));
        try {
            Class<?> canaryClass = Class.forName("leakcanary.LeakCanary");
            try {
                Class<?> onHeapAnalyzedListenerClass =
                        Class.forName("leakcanary.OnHeapAnalyzedListener");
                Class<?> metadataExtractorClass = Class.forName("shark.MetadataExtractor");
                Method getConfig = canaryClass.getMethod("getConfig");
                Class<?> configClass = getConfig.getReturnType();
                Method setConfig = canaryClass.getMethod("setConfig", configClass);
                Method copy = configClass.getMethod("copy", boolean.class, boolean.class,
                        int.class, List.class, List.class, onHeapAnalyzedListenerClass,
                        metadataExtractorClass, boolean.class, int.class, boolean.class,
                        boolean.class);

                Object canary = canaryClass.getField("INSTANCE").get(null);
                Object currentConfig = getConfig.invoke(canary);

                Boolean dumpHeap = (Boolean) configClass
                        .getMethod("getDumpHeap").invoke(currentConfig);
                Boolean dumpHeapWhenDebugging = (Boolean) configClass
                        .getMethod("getDumpHeapWhenDebugging").invoke(currentConfig);
                List<?> referenceMatchers = (List<?>) configClass
                        .getMethod("getReferenceMatchers").invoke(currentConfig);
                List<?> objectInspectors = (List<?>) configClass
                        .getMethod("getObjectInspectors").invoke(currentConfig);
                Object onHeapAnalyzedListener = configClass
                        .getMethod("getOnHeapAnalyzedListener").invoke(currentConfig);
                // Yes, LeakCanary misspelled metadata
                Object metadataExtractor = configClass
                        .getMethod("getMetatadaExtractor").invoke(currentConfig);
                Boolean computeRetainedHeapSize = (Boolean) configClass
                        .getMethod("getComputeRetainedHeapSize").invoke(currentConfig);
                Integer maxStoredHeapDumps = (Integer) configClass
                        .getMethod("getMaxStoredHeapDumps").invoke(currentConfig);
                Boolean useExperimentalLeakFinders = (Boolean) configClass
                        .getMethod("getUseExperimentalLeakFinders").invoke(currentConfig);

                setConfig.invoke(canary, copy.invoke(currentConfig,
                        dumpHeap,
                        dumpHeapWhenDebugging,
                        1,
                        referenceMatchers,
                        objectInspectors,
                        onHeapAnalyzedListener,
                        metadataExtractor,
                        computeRetainedHeapSize,
                        maxStoredHeapDumps,
                        true,
                        useExperimentalLeakFinders));

            } catch (ReflectiveOperationException e) {
                Log.e("paintbooth", "Error initializing LeakCanary", e);
                Toast.makeText(this, "Error initializing LeakCanary", Toast.LENGTH_LONG).show();
            }
        } catch (ClassNotFoundException e) {
            // LeakCanary is not used in this build, do nothing.
        }
    }

    private boolean isServiceRunning(Class serviceClazz) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceClazz.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        requireViewById(R.id.list)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        requireViewById(android.R.id.content)
                .setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }

    private class Element {
        String mDisplayText;
        Class mClass;
        boolean mIsService;
        View.OnClickListener mOnClickListener;

        Element(String displayText, Class clazz, boolean isService) {
            this.mDisplayText = displayText;
            this.mClass = clazz;
            this.mIsService = isService;
        }
    }
}
