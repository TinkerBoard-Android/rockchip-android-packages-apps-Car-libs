/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.apps.common;

import android.annotation.Nullable;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.car.drawer.CarDrawerAdapter;
import androidx.car.drawer.CarDrawerController;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

/**
 * Common base Activity for car apps that need to present a Drawer.
 *
 * <p>This Activity manages the overall layout. To use it, sub-classes need to:
 *
 * <ul>
 *   <li>Provide the root-items for the drawer by calling {@link #getDrawerController()}.
 *       {@link CarDrawerController#setRootAdapter(CarDrawerAdapter)}.
 *   <li>Add their main content using {@link #setMainContent(int)} or {@link #setMainContent(View)}.
 *       They can also add fragments to the main-content container by obtaining its id using
 *       {@link #getContentContainerId()}
 * </ul>
 *
 * <p>The rootAdapter can implement nested-navigation, in its click-handling, by passing the
 * CarDrawerAdapter for the next level to
 * {@link CarDrawerController#pushAdapter(CarDrawerAdapter)}.
 *
 * <p>Any Activity's based on this class need to set their theme to
 * {@code Theme.CarDefault.NoActionBar.Drawer} or a derivative.
 */
public class DrawerActivity extends FragmentActivity {
    private CarDrawerController mDrawerController;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.drawer_activity);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.car_drawer_open, R.string.car_drawer_close);

        mToolbar = findViewById(R.id.car_toolbar);
        setActionBar(mToolbar);

        mDrawerController = new CarDrawerController(drawerLayout, mActionBarDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Returns the {@link CarDrawerController} that is responsible for handling events relating
     * to the drawer in this Activity.
     *
     * @return The {@link CarDrawerController} linked to this Activity. This value will be
     * {@code null} if this method is called before {@code onCreate()} has been called.
     */
    @Nullable
    protected CarDrawerController getDrawerController() {
        return mDrawerController;
    }

    /**
     * Returns the {@link ActionBarDrawerToggle} that can switch between back button and drawer
     * toggle in the case of back navigation. See
     * {@link ActionBarDrawerToggle#setDrawerIndicatorEnabled(boolean)}.
     */
    protected ActionBarDrawerToggle getActionBarDrawerToggle() {
        return mActionBarDrawerToggle;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerController.syncState();
    }

    @Override
    public void setContentView(View view) {
        ViewGroup parent = findViewById(getContentContainerId());
        parent.addView(view);
    }

    @Override
    public void setContentView(@LayoutRes int resourceId) {
        ViewGroup parent = findViewById(getContentContainerId());
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(resourceId, parent, true);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ViewGroup parent = findViewById(getContentContainerId());
        parent.addView(view, params);
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

    @Override
    protected void onStop() {
        super.onStop();
        mDrawerController.closeDrawer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerController.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }
}
