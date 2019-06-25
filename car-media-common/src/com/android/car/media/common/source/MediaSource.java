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

package com.android.car.media.common.source;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.service.media.MediaBrowserService;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * This represents a source of media content. It provides convenient methods to access media source
 * metadata, such as primary color and application name.
 */
public class MediaSource {
    private static final String TAG = "MediaSource";

    /**
     * Custom media sources which should not be templatized.
     */
    private static final Set<String> CUSTOM_MEDIA_SOURCES = new HashSet<>();

    static {
        CUSTOM_MEDIA_SOURCES.add("com.android.car.radio");
    }

    @NonNull
    private final String mPackageName;
    @Nullable
    private String mBrowseServiceClassName;
    @NonNull
    private final Context mContext;
    @Nullable
    private final ServiceInfo mServiceInfo;
    private CharSequence mName;

    /**
     * Creates a {@link MediaSource} for the given {@link ComponentName}
     */
    @Nullable
    public static MediaSource create(@NonNull Context context,
            @NonNull ComponentName componentName) {
        MediaSource mediaSource = new MediaSource(context, componentName);
        if (mediaSource.mBrowseServiceClassName == null) {
            return null;
        }
        return mediaSource;
    }

    // TODO(b/136274456): Clean up the internals of MediaSource.
    private MediaSource(@NonNull Context context, @NonNull ComponentName componentName) {
        mContext = context;
        mPackageName = componentName.getPackageName();
        mServiceInfo = getBrowseServiceInfo(componentName);
        extractComponentInfo();
    }

    /**
     * @return the {@link ServiceInfo} corresponding to a {@link MediaBrowserService} in the media
     * source, or null if the media source doesn't implement {@link MediaBrowserService}. A non-null
     * result doesn't imply that this service is accessible. The consumer code should attempt to
     * connect and handle rejections gracefully.
     */
    @Nullable
    private ServiceInfo getBrowseServiceInfo(@NonNull ComponentName componentName) {
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(MediaBrowserService.SERVICE_INTERFACE);
        intent.setPackage(componentName.getPackageName());
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent,
                PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }
        String className = componentName.getClassName();
        if (TextUtils.isEmpty(className)) {
            return resolveInfos.get(0).serviceInfo;
        }
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo result = resolveInfo.serviceInfo;
            if (result.name.equals(className)) {
                return result;
            }
        }
        return null;
    }

    private void extractComponentInfo() {
        mBrowseServiceClassName = mServiceInfo != null ? mServiceInfo.name : null;
        try {
            // Gets a proper app name. Checks service label first. If failed, uses application
            // label as fallback.
            if (mServiceInfo != null && mServiceInfo.labelRes != 0) {
                mName = mServiceInfo.loadLabel(mContext.getPackageManager());
            } else {
                ApplicationInfo applicationInfo =
                        mContext.getPackageManager().getApplicationInfo(mPackageName,
                                PackageManager.GET_META_DATA);
                mName = applicationInfo.loadLabel(mContext.getPackageManager());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to update media client package attributes.", e);
        }
    }

    /**
     * @return media source human readable name for display.
     */
    public CharSequence getDisplayName() {
        return mName;
    }

    /**
     * @return the package name of this media source.
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
     * @return a {@link Drawable} as the media source's icon.
     */
    public Drawable getIcon() {
        // Checks service icon first. If failed, uses application icon as fallback.
        try {
            if (mServiceInfo != null) {
                return mServiceInfo.loadIcon(mContext.getPackageManager());
            }
            return mContext.getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns this media source's icon cropped to a circle.
     */
    public Bitmap getRoundPackageIcon() {
        Drawable icon = getIcon();
        if (icon != null) {
            return getRoundCroppedBitmap(drawableToBitmap(icon));
        }
        return null;
    }

    /**
     * Returns {@code true} iff this media source should not be templatized.
     */
    public boolean isCustom() {
        return isCustom(mPackageName);
    }

    /**
     * Returns {@code true} iff the provided media package should not be templatized.
     */
    public static boolean isCustom(String packageName) {
        return CUSTOM_MEDIA_SOURCES.contains(packageName);
    }

    /**
     * Returns {@code true} iff this media source has a browse service to connect to.
     */
    public boolean isBrowsable() {
        return mBrowseServiceClassName != null;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap getRoundCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaSource that = (MediaSource) o;
        return Objects.equals(mPackageName, that.mPackageName)
                && Objects.equals(mBrowseServiceClassName, that.mBrowseServiceClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageName, mBrowseServiceClassName);
    }

    @Override
    @NonNull
    public String toString() {
        return mPackageName + (mBrowseServiceClassName == null ? "" : mBrowseServiceClassName);
    }

    /** Returns the package name of the given source, or null. */
    @Nullable
    public static String getPackageName(@Nullable MediaSource source) {
        return (source != null) ? source.getPackageName() : null;
    }
}
