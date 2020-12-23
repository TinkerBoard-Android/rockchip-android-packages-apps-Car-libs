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
package com.android.car.ui.sharedlibrary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryVersionProviderOEMV1;

import dalvik.system.PathClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is an implementation of {@link SharedLibraryVersionProviderOEMV1} that proxies it's methods
 * to a different {@link SharedLibraryVersionProviderOEMV1} that is loaded from a different apk.
 *
 * This proxy is so that the real {@link SharedLibraryVersionProviderOEMV1} and all the classes
 * it depends on are not on the classpath. Since shared libraries come before the app on the
 * classpath, this could produce version conflicts between different versions of static libraries
 * compiled into both the app and the shared library.
 *
 * The real SharedLibraryVersionProviderOEMV1's classloader's parent classloader is the classloader
 * used to load this class. That way, the real shared library can still access the classes defined
 * in this "proxy" shared library. Since shared library classloaders are isolated, and cannot
 * access the app's classes, this does not produce the same version conflict mentioned before.
 */
public final class SharedLibraryVersionProviderImpl implements SharedLibraryVersionProviderOEMV1 {

    private static final String TAG = "carui";
    // This should match with the package name in the <queries/> in the manifest.
    private static final String OEM_SHAREDLIB_PACKAGENAME =
            "com.google.car.ui.sharedlibrary";

    private static ClassLoader sCachedClassLoader;
    private static boolean sHasInjectedResources = false;

    private Object mOEMVersionProvider;

    @Override
    public synchronized Object getSharedLibraryFactory(int maxVersion, Context context) {
        ApplicationInfo appInfo = getApplicationInfo(context, OEM_SHAREDLIB_PACKAGENAME);
        ClassLoader cl = getClassLoader(appInfo);

        if (!sHasInjectedResources) {
            injectResources(cl, context, appInfo);
            sHasInjectedResources = true;
        }

        try {
            if (mOEMVersionProvider == null) {
                mOEMVersionProvider = cl
                        .loadClass(OEM_SHAREDLIB_PACKAGENAME + ".SharedLibraryVersionProviderImpl")
                        .getDeclaredConstructor()
                        .newInstance();
            }

            return mOEMVersionProvider.getClass()
                    .getDeclaredMethod("getSharedLibraryFactory", int.class, Context.class)
                    .invoke(mOEMVersionProvider, maxVersion, context);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Could not call SharedLibraryVersionProviderImpl.getSharedLibraryFactory()", e);
        }
    }

    /**
     * Injects the resources belonging to the {@link ApplicationInfo} into the provided
     * {@link Context}.
     */
    private static void injectResources(ClassLoader cl, Context context, ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // TODO(b/175649937): Add a solution for P/Q.
            throw new UnsupportedOperationException(
                    "Shared library cannot run pre R at the moment");
        }

        try {
            ResourcesLoader rl = new ResourcesLoader();
            File apk = new File(appInfo.sourceDir);
            rl.addProvider(
                    ResourcesProvider.loadFromApk(
                            ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY)));
            context.getResources().addLoaders(rl);

            Class<?> assetManagerClazz = AssetManager.class;
            Method getAssignedPackageIdentifiers =
                    assetManagerClazz.getDeclaredMethod("getAssignedPackageIdentifiers");
            getAssignedPackageIdentifiers.setAccessible(true);
            SparseArray<String> packageIdentifiers =
                    (SparseArray<String>) getAssignedPackageIdentifiers
                            .invoke(context.getResources().getAssets());
            for (int i = 0; i < packageIdentifiers.size(); i++) {
                if (OEM_SHAREDLIB_PACKAGENAME.equals(packageIdentifiers.valueAt(i))) {
                    rewriteRValues(cl, packageIdentifiers.valueAt(i), packageIdentifiers.keyAt(i));
                    break;
                }
            }
        } catch (IOException | ReflectiveOperationException | SecurityException e) {
            throw new RuntimeException("Unable to load shared library resources", e);
        }
    }

    /**
     * Exact copy from {@link LoadedApk}
     */
    private static void rewriteRValues(ClassLoader cl, String packageName, int id) {
        final Class<?> rClazz;
        try {
            rClazz = cl.loadClass(packageName + ".R");
        } catch (ClassNotFoundException e) {
            // This is not necessarily an error, as some packages do not ship with resources
            // (or they do not need rewriting).
            Log.i(TAG, "No resource references to update in package " + packageName);
            return;
        }

        final Method callback;
        try {
            callback = rClazz.getMethod("onResourcesLoaded", int.class);
        } catch (NoSuchMethodException e) {
            // No rewriting to be done.
            return;
        }

        Throwable cause;
        try {
            callback.invoke(null, id);
            return;
        } catch (IllegalAccessException e) {
            cause = e;
        } catch (InvocationTargetException e) {
            cause = e.getCause();
        }

        throw new RuntimeException("Failed to rewrite resource references for " + packageName,
                cause);
    }

    private static ApplicationInfo getApplicationInfo(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Package not found: " + packageName, e);
        }
    }

    /**
     * Returns a classloader that loads classes from the apk specificed by the
     * {@link ApplicationInfo}.
     */
    private static ClassLoader getClassLoader(ApplicationInfo appInfo) {
        if (sCachedClassLoader != null) {
            return sCachedClassLoader;
        }

        List<String> libraryPaths = new ArrayList<>(3);
        if (appInfo.nativeLibraryDir != null) {
            libraryPaths.add(appInfo.nativeLibraryDir);
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) == 0) {
            for (String abi : getSupportedAbisForCurrentRuntime()) {
                libraryPaths.add(appInfo.sourceDir + "!/lib/" + abi);
            }
        }

        String flatLibraryPaths = (libraryPaths.size() == 0
                ? null : TextUtils.join(File.pathSeparator, libraryPaths));

        String apkPaths = appInfo.sourceDir;
        if (appInfo.sharedLibraryFiles != null && appInfo.sharedLibraryFiles.length > 0) {
            // Unless you pass PackageManager.GET_SHARED_LIBRARY_FILES this will always be null
            // HOWEVER, if you running on a device with F5 active, the module's dex files are
            // always listed in ApplicationInfo.sharedLibraryFiles and should be included in
            // the classpath.
            apkPaths +=
                    File.pathSeparator + TextUtils.join(File.pathSeparator,
                            appInfo.sharedLibraryFiles);
        }

        sCachedClassLoader = new PathClassLoader(apkPaths, flatLibraryPaths,
                SharedLibraryVersionProviderImpl.class.getClassLoader());
        return sCachedClassLoader;
    }

    private static List<String> getSupportedAbisForCurrentRuntime() {
        List<String> abis = new ArrayList<>();
        if (Process.is64Bit()) {
            Collections.addAll(abis, Build.SUPPORTED_64_BIT_ABIS);
        } else {
            Collections.addAll(abis, Build.SUPPORTED_32_BIT_ABIS);
        }
        return abis;
    }
}
