/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.car.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import com.android.car.stream.ui.R;

/**
 * Common base Activity for car apps that need to present a Drawer.
 * <p>
 * This Activity manages the overall layout. To use it sub-classes need to:
 * <ul>
 *     <li>Provide the layout to use for the drawer contents via
 *     {@link #getDrawerContentLayoutId()} ()}</li>
 *     <li>Add their main content to the container FrameLayout
 *     (with id = {@link #getContentContainerId()}_</li>
 * </ul>
 * This class will take care of drawer toggling and display.
 * <p>
 * Any Activity's based on this class need to set their theme to CarDrawerActivityTheme or a
 * derivative.
 * <p>
 * NOTE: This version is based on a regular Activity unlike car-support-lib's CarDrawerActivity
 * which is based on CarActivity.
 */
public abstract class CarDrawerActivity extends AppCompatActivity {
    private static final float COLOR_SWITCH_SLIDE_OFFSET = 0.25f;

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mDrawerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.car_drawer_activity);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        ViewStub drawerStub = (ViewStub)findViewById(R.id.left_drawer_stub);
        drawerStub.setLayoutResource(getDrawerContentLayoutId());
        mDrawerView = drawerStub.inflate();

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        setupDrawerToggling();
    }

    /**
     * Get the DrawerLayout that this activity owns. Callers can perform operations like opening/
     * closing or adding listeners.
     *
     * @return DrawerLayout managed by this activity.
     */
    protected DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    /**
     * Sub-classes should return the id of the layout to use as the drawer content.
     *
     * @return Id of layout to display in Drawer when opened.
     */
    protected abstract int getDrawerContentLayoutId();

    /**
     * View obtained by inflating drawer layout (obtained using
     * {@link #getDrawerContentLayoutId()}). Subclasses can perform additional initialization of the
     * inflated view.
     *
     * @return View inflated from drawer layout.
     */
    protected View getDrawerView() {
        return mDrawerView;
    }

    /**
     * Close the drawer if open.
     */
    protected void closeDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    /**
     * Get the id of the main content Container which is a FrameLayout. Subclasses can add their own
     * content/fragments inside here.
     *
     * @return Id of FrameLayout where main content of the subclass Activity can be added.
     */
    protected int getContentContainerId() {
        return R.id.content_frame;
    }

    private void setupDrawerToggling() {
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                // The string id's below are for accessibility. However
                // since they won't be used in cars, we just pass car_drawer_unused.
                R.string.car_drawer_unused,
                R.string.car_drawer_unused
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                setTitleAndArrowColor(slideOffset >= COLOR_SWITCH_SLIDE_OFFSET);
            }
            @Override
            public void onDrawerOpened(View drawerView) {}
            @Override
            public void onDrawerClosed(View drawerView) {}
            @Override
            public void onDrawerStateChanged(int newState) {}
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setTitleAndArrowColor(boolean drawerOpen) {
        // When drawer open, use car_title, which resolves to appropriate color depending on
        // day-night mode. When drawer is closed, we always use light color.
        int titleColorResId =  drawerOpen ?
                R.color.car_title : R.color.car_title_light;
        int titleColor = getColor(titleColorResId);
        mToolbar.setTitleTextColor(titleColor);
        mDrawerToggle.getDrawerArrowDrawable().setColor(titleColor);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();

        // In case we're restarting after a config change (e.g. day, night switch), set colors
        // again. Doing it here so that Drawer state is fully synced and we know if its open or not.
        // NOTE: isDrawerOpen must be passed the second child of the DrawerLayout.
        setTitleAndArrowColor(mDrawerLayout.isDrawerOpen(mDrawerView));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Delegate touches; It toobar handled them (e.g. menu icon tap), we return.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
