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
package com.android.car.ui.sharedlibrarysupport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.android.car.ui.CarUiAppComponentFactory;

import dalvik.system.PathClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a {@link PathClassLoader} that you can also call
 * {@link #addAdditionalClassLoader(ClassLoader)} on. This will add another classloader
 * that will be searched after the classes in this classloader have been searched, but before
 * the parent classloader is searched.
 *
 * Much of the code is copied from {@link dalvik.system.DelegateLastClassLoader}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ModifiableClassLoader extends PathClassLoader {

    private final boolean mDelegateResourceLoading;
    private final Set<ClassLoader> mAdditionalClassLoaders = new HashSet<>();

    /**
     * Equivalent to calling {@link #ModifiableClassLoader(String, String, ClassLoader, boolean)}
     * with {@code librarySearchPath = null, delegateResourceLoading = true}.
     */
    public ModifiableClassLoader(String dexPath, ClassLoader parent) {
        this(dexPath, null, parent, true);
    }

    /**
     * Equivalent to calling {@link #ModifiableClassLoader(String, String, ClassLoader, boolean)}
     * with {@code delegateResourceLoading = true}.
     */
    public ModifiableClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        this(dexPath, librarySearchPath, parent, true);
    }

    /**
     * Adds another classloader that will be searched after the classes in this classloader
     * are searched, but before the parent classloader is searched.
     *
     * Make sure to add your classloader before trying to load any classes from it, or
     * else they will be cached as not found.
     */
    public void addAdditionalClassLoader(ClassLoader classLoader) {
        mAdditionalClassLoaders.add(classLoader);
    }

    /**
     * See {@link dalvik.system.DelegateLastClassLoader#DelegateLastClassLoader
     * (String, String, ClassLoader, boolean)}.
     */
    public ModifiableClassLoader(@NonNull String dexPath, @Nullable String librarySearchPath,
            @Nullable ClassLoader parent, boolean delegateResourceLoading) {
        super(dexPath, librarySearchPath, parent);
        this.mDelegateResourceLoading = delegateResourceLoading;
    }

    /**
     * A copy from {@link dalvik.system.DelegateLastClassLoader}, but with changes
     * to support loading classloaders added via {@link #addAdditionalClassLoader(ClassLoader)}.
     *
     * If ModifiableClassLoader or {@link CarUiAppComponentFactory} are loaded, loading them
     * from this classloader will be skipped and instead they'll be loaded from the parent
     * classloader, so that they are not duplicated.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check whether the class has already been loaded. Return it if that's the case.
        Class<?> cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }

        // Next, check whether the class in question is present in the boot classpath.
        try {
            return Object.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }

        ClassNotFoundException fromSuper = null;

        // Load CarUiAppComponentFactory and ModifiableClassLoader from the parent classloader,
        // because we need to get the copy of it that was actually used as a factory.
        if (!CarUiAppComponentFactory.class.getName().equals(name)
                && !ModifiableClassLoader.class.getName().equals(name)) {
            // Next, check whether the class in question is present in the dexPath that this
            // classloader operates on, or its shared libraries.
            try {
                return findClass(name);
            } catch (ClassNotFoundException ex) {
                fromSuper = ex;
            }

            // Next, check any additional classloaders that were registered later.
            for (ClassLoader classLoader : mAdditionalClassLoaders) {
                try {
                    return classLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        // Finally, check whether the class in question is present in the parent classloader.
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            // The exception we're catching here is the CNFE thrown by the parent of this
            // classloader. However, we would like to throw a CNFE that provides details about
            // the class path / list of dex files associated with *this* classloader, so we choose
            // to throw the exception thrown from that lookup.
            if (fromSuper == null) {
                throw cnfe;
            }
            throw fromSuper;
        }
    }

    /**
     * A direct copy from {@link dalvik.system.DelegateLastClassLoader}.
     */
    @Override
    public URL getResource(String name) {
        // The lookup order we use here is the same as for classes.

        URL resource = Object.class.getClassLoader().getResource(name);
        if (resource != null) {
            return resource;
        }

        resource = findResource(name);
        if (resource != null) {
            return resource;
        }

        if (mDelegateResourceLoading) {
            final ClassLoader cl = getParent();
            return (cl == null) ? null : cl.getResource(name);
        }
        return null;
    }

    /**
     * A direct copy from {@link dalvik.system.DelegateLastClassLoader}.
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        @SuppressWarnings("unchecked")
        final Enumeration<URL>[] resources = (Enumeration<URL>[]) new Enumeration<?>[] {
                Object.class.getClassLoader().getResources(name),
                findResources(name),
                (getParent() == null || !mDelegateResourceLoading)
                        ? null : getParent().getResources(name) };

        return new CompoundEnumeration<>(resources);
    }
}
