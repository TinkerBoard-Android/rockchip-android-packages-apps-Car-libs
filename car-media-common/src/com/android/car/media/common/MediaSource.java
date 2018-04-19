/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.car.media.common;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Handler;
import android.service.media.MediaBrowserService;
import android.util.Log;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This represents a source of media content. It provides convenient methods to access media source
 * metadata, such as primary color and application name.
 *
 * <p>It also allows consumers to subscribe to its {@link android.service.media.MediaBrowserService}
 * if such service is implemented by the source.
 */
public class MediaSource {
    private static final String TAG = "MediaSource";

    /** Third-party defined application theme to use **/
    private static final String THEME_META_DATA_NAME =
            "com.google.android.gms.car.application.theme";
    /** Mark used to indicate that we couldn't find a color and the default one should be used */
    private static final int DEFAULT_COLOR = 0;

    private final String mPackageName;
    @Nullable
    private final String mBrowseServiceClassName;
    @Nullable
    private final MediaBrowser mBrowser;
    private final Context mContext;
    private final Handler mHandler = new Handler();
    private List<Observer> mObservers = new ArrayList<>();
    private CharSequence mName;
    private String mRootNode;
    private @ColorInt int mPrimaryColor;
    private @ColorInt int mAccentColor;
    private @ColorInt int mPrimaryColorDark;

    /**
     * An observer of this media source.
     */
    public abstract static class Observer {
        /**
         * This method is called if a successful connection to the {@link MediaBrowserService} is
         * made for this source. A connection is initiated as soon as there is at least one
         * {@link Observer} subscribed by using {@link MediaSource#subscribe(Observer)}.
         *
         * @param success true if the connection was successful or false otherwise.
         */
        protected void onBrowseConnected(boolean success) {};

        /**
         * This method is called if the connection to the {@link MediaBrowserService} is lost.
         */
        protected void onBrowseDisconnected() {};
    }

    /**
     * A subscription to a collection of items
     */
    public interface ItemsSubscription {
        /**
         * This method is called whenever media items are loaded or updated.
         *
         * @param parentId identifier of the items parent.
         * @param items items loaded, or null if there was an error trying to load them.
         */
        void onChildrenLoaded(String parentId, @Nullable List<MediaItemMetadata> items);
    }

    private final MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaSource.this.notify(observer -> observer.onBrowseConnected(true));
                }

                @Override
                public void onConnectionSuspended() {
                    MediaSource.this.notify(Observer::onBrowseDisconnected);
                }

                @Override
                public void onConnectionFailed() {
                    MediaSource.this.notify(observer -> observer.onBrowseConnected(false));
                }
            };

    /**
     * Creates a {@link MediaSource} for the given application package name
     */
    public MediaSource(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
        mBrowseServiceClassName = getBrowseServiceClassName(packageName);
        if (mBrowseServiceClassName != null) {
            mBrowser = new MediaBrowser(mContext,
                    new ComponentName(mPackageName, mBrowseServiceClassName),
                    mConnectionCallback,
                    null);
        } else {
            // This media source doesn't provide browsing.
            mBrowser = null;
        }
        extractComponentInfo(mPackageName, mBrowseServiceClassName);
    }

    /**
     * @return the classname corresponding to a {@link MediaBrowserService} in the
     * media source, or null if the media source doesn't implement {@link MediaBrowserService}.
     * A non-null result doesn't imply that this service is accessible. The consumer code should
     * attempt to connect and handle rejections gracefully.
     */
    @Nullable
    private String getBrowseServiceClassName(String packageName) {
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(MediaBrowserService.SERVICE_INTERFACE);
        intent.setPackage(packageName);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent,
                PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }
        return resolveInfos.get(0).serviceInfo.name;
    }

    /**
     * Subscribes to this media source. This allows consuming browse information.

     * @return true if a subscription could be added, or false otherwise (for example, if the
     * {@link MediaBrowserService} is not available for this source.
     */
    public boolean subscribe(Observer observer) {
        if (mBrowser == null) {
            return false;
        }
        mObservers.add(observer);
        if (!mBrowser.isConnected()) {
            try {
                mBrowser.connect();
            } catch (IllegalStateException ex) {
                // Ignore: MediaBrowse could be in an intermediate state (not connected, but not
                // disconnected either.). In this situation, trying to connect again can throw
                // this exception, but there is no way to know without trying.
            }
        } else {
            observer.onBrowseConnected(true);
        }
        return true;
    }

    /**
     * Unsubscribe from this media source
     */
    public void unsubscribe(Observer observer) {
        mObservers.remove(observer);
        if (mObservers.isEmpty()) {
            // TODO(b/77640010): Review MediaBrowse disconnection.
            // Some media sources are not responding correctly to MediaBrowser#disconnect(). We
            // are keeping the connection going.
            //   mBrowser.disconnect();
        }
    }

    /**
     * Subscribes to changes on the list of media item children of the given parent.
     *
     * @param parentId parent of the children to load, or null to indicate children of the root
     *                 node.
     * @param callback callback used to provide updates on the subscribed node.
     * @throws IllegalStateException if browsing is not available or it is not connected.
     */
    public void subscribeChildren(@Nullable String parentId,
            ItemsSubscription callback) {
        if (mBrowser == null) {
            throw new IllegalStateException("Browsing is not available for this source: "
                    + getName());
        }
        if (mRootNode == null && !mBrowser.isConnected()) {
            throw new IllegalStateException("Subscribing to the root node can only be done while "
                    + "connected: " + getName());
        }
        mRootNode = mBrowser.getRoot();
        mBrowser.subscribe(parentId != null ? parentId : mRootNode,
                wrapCallback(callback));
    }

    /**
     * Unsubscribes to changes on the list of media items children of the given parent
     *
     * @param parentId parent to unsubscribe, or null to unsubscribe from the root node.
     * @throws IllegalStateException if browsing is not available or it is not connected.
     */
    public void unsubscribeChildren(@Nullable String parentId) {
        // If we are not connected
        if (mBrowser == null) {
            throw new IllegalStateException("Browsing is not available for this source: "
                    + getName());
        }
        if (parentId == null && mRootNode == null) {
            // If we are trying to unsubscribe from root, but we haven't determine it's Id, then
            // there is nothing we can do.
            return;
        }
        mBrowser.unsubscribe(parentId != null ? parentId : mRootNode);
    }

    private MediaBrowser.SubscriptionCallback wrapCallback(ItemsSubscription subscription) {
        return new MediaBrowser.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId,
                    List<MediaBrowser.MediaItem> children) {
                List<MediaItemMetadata> items = children.stream()
                        .map(child -> new MediaItemMetadata(child))
                        .collect(Collectors.toList());
                subscription.onChildrenLoaded(parentId, items);
            }

            @Override
            public void onError(String parentId) {
                subscription.onChildrenLoaded(parentId, null);
            }
        };
    }

    private void extractComponentInfo(@NonNull String packageName,
            @Nullable String browseServiceClassName) {
        TypedArray ta = null;
        try {
            ApplicationInfo applicationInfo =
                    mContext.getPackageManager().getApplicationInfo(packageName,
                            PackageManager.GET_META_DATA);
            ServiceInfo serviceInfo = browseServiceClassName != null
                    ? mContext.getPackageManager().getServiceInfo(
                        new ComponentName(packageName, browseServiceClassName),
                        PackageManager.GET_META_DATA)
                    : null;

            // Get the proper app name, check service label, then application label.
            if (serviceInfo != null && serviceInfo.labelRes != 0) {
                mName = serviceInfo.loadLabel(mContext.getPackageManager());
            } else if (applicationInfo.labelRes != 0) {
                mName = applicationInfo.loadLabel(mContext.getPackageManager());
            } else {
                mName = null;
            }

            // Get the proper theme, check theme for service, then application.
            Context packageContext = mContext.createPackageContext(packageName, 0);
            int appTheme = applicationInfo.metaData != null
                    ? applicationInfo.metaData.getInt(THEME_META_DATA_NAME)
                    : 0;
            appTheme = appTheme == 0
                    ? applicationInfo.theme
                    : appTheme;
            packageContext.setTheme(appTheme);
            Resources.Theme theme = packageContext.getTheme();
            ta = theme.obtainStyledAttributes(new int[] {
                    android.R.attr.colorPrimary,
                    android.R.attr.colorAccent,
                    android.R.attr.colorPrimaryDark
            });
            mPrimaryColor = ta.getColor(0, DEFAULT_COLOR);
            mAccentColor = ta.getColor(1, DEFAULT_COLOR);
            mPrimaryColorDark = ta.getColor(2, DEFAULT_COLOR);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to update media client package attributes.", e);
            mPrimaryColor = DEFAULT_COLOR;
            mAccentColor = DEFAULT_COLOR;
            mPrimaryColorDark = DEFAULT_COLOR;
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
    }

    /**
     * @return media source primary color, or the given default color if the source metadata
     * is not available.
     */
    public @ColorInt int getPrimaryColor(@ColorInt int defaultColor) {
        return mPrimaryColor != DEFAULT_COLOR ? mPrimaryColor : defaultColor;
    }

    /**
     * @return media source accent color, or the given default color if the source metadata
     * is not available.
     */
    public @ColorInt int getAccentColor(@ColorInt int defaultColor) {
        return mAccentColor != DEFAULT_COLOR ? mAccentColor : defaultColor;
    }

    /**
     * @return media source primary dark color, or the given default color if the source metadata
     * is not available.
     */
    public @ColorInt int getPrimaryColorDark(@ColorInt int defaultColor) {
        return mPrimaryColorDark != DEFAULT_COLOR ? mPrimaryColorDark : defaultColor;
    }

    private void notify(Consumer<Observer> notification) {
        mHandler.post(() -> {
            List<Observer> observers = new ArrayList<>(mObservers);
            for (Observer observer : observers) {
                notification.accept(observer);
            }
        });
    }

    /**
     * @return media source human readable name.
     */
    public CharSequence getName() {
        return mName;
    }

    /**
     * @return the package name that identifies this media source.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * @return a {@link ComponentName} referencing this media source's {@link MediaBrowserService},
     * or NULL if this media source doesn't implement such service.
     */
    @Nullable
    public ComponentName getBrowseServiceComponentName() {
        if (mBrowseServiceClassName != null) {
            return new ComponentName(mPackageName, mBrowseServiceClassName);
        } else {
            return null;
        }
    }

    /**
     * @return a {@link PlaybackModel} that allows controlling this media source. This method
     * should only be used if this {@link MediaSource} is connected.
     * @see #subscribe(Observer)
     */
    @Nullable
    public PlaybackModel getPlaybackModel() {
        if (mBrowser == null) {
            return null;
        }

        MediaSession.Token token = mBrowser.getSessionToken();
        MediaController controller = new MediaController(mContext, token);
        PlaybackModel playbackModel = new PlaybackModel(mContext);
        playbackModel.setMediaController(controller);
        return playbackModel;
    }

    /**
     * @return this media source's icon as a {@link Drawable}
     */
    public Drawable getPackageIcon() {
        try {
            return mContext.getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaSource that = (MediaSource) o;
        return Objects.equals(mPackageName, that.mPackageName)
                && Objects.equals(mBrowseServiceClassName, that.mBrowseServiceClassName);
    }

    /** @return the current media browser. This media browser might not be connected yet. */
    public MediaBrowser getMediaBrowser() {
        return mBrowser;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageName, mBrowseServiceClassName);
    }
}
