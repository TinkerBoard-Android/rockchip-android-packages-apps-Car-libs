/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.ui;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class OverlayTestService extends Service {
    public static final String OVERLAY_WINDOW_ADDED = "com.android.car.ui.OVERLAY_WINDOW_ADDED";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private View mView;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = getSystemService(WindowManager.class);
        mParams = new WindowManager.LayoutParams();
        mParams.setTitle("Overlay!");
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mParams.flags = FLAG_LAYOUT_IN_SCREEN | FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE;
        mParams.width = 100;
        mParams.height = 100;
        mParams.gravity = Gravity.RIGHT | Gravity.TOP;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && mView == null) {
            // Have to be a foreground service since this app is in the background
            startForeground();
            addWindow();
        }
        return START_NOT_STICKY;
    }

    private void addWindow() {
        mView = new View(this);
        mView.setBackgroundColor(Color.RED);
        mWindowManager.addView(mView, mParams);
        sendBroadcast(new Intent(OVERLAY_WINDOW_ADDED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mView != null) {
            mWindowManager.removeViewImmediate(mView);
            mView = null;
        }
    }

    private void startForeground() {
        String channel = "test";
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(
                new NotificationChannel(channel, channel, NotificationManager.IMPORTANCE_DEFAULT));
        Notification notification =
                new Notification.Builder(this, channel)
                        .setContentTitle("CTS")
                        .setContentText(getClass().getCanonicalName())
                        .setSmallIcon(android.R.drawable.btn_default)
                        .build();
        startForeground(1, notification);
    }
}

