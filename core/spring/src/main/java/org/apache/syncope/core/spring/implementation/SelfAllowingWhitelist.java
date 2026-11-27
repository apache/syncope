/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.spring.implementation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

/**
 * Extends a configured {@link Whitelist} with the members of the classes generated from the Groovy code being
 * executed: such classes cannot be listed in advance in a static whitelist file.
 */
public class SelfAllowingWhitelist extends Whitelist {

    private final Whitelist delegate;

    private final ClassLoader sandboxClassLoader;

    public SelfAllowingWhitelist(final Whitelist delegate, final ClassLoader sandboxClassLoader) {
        this.delegate = delegate;
        this.sandboxClassLoader = sandboxClassLoader;
    }

    protected boolean isSandboxClass(final Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        for (ClassLoader loader = clazz.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader == sandboxClassLoader) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean permitsMethod(final Method method, final Object receiver, final Object[] args) {
        return isSandboxClass(method.getDeclaringClass())
                || delegate.permitsMethod(method, receiver, args);
    }

    @Override
    public boolean permitsConstructor(final Constructor<?> constructor, final Object[] args) {
        return isSandboxClass(constructor.getDeclaringClass())
                || delegate.permitsConstructor(constructor, args);
    }

    @Override
    public boolean permitsStaticMethod(final Method method, final Object[] args) {
        return isSandboxClass(method.getDeclaringClass())
                || delegate.permitsStaticMethod(method, args);
    }

    @Override
    public boolean permitsFieldGet(final Field field, final Object receiver) {
        return isSandboxClass(field.getDeclaringClass())
                || delegate.permitsFieldGet(field, receiver);
    }

    @Override
    public boolean permitsFieldSet(final Field field, final Object receiver, final Object value) {
        return isSandboxClass(field.getDeclaringClass())
                || delegate.permitsFieldSet(field, receiver, value);
    }

    @Override
    public boolean permitsStaticFieldGet(final Field field) {
        return isSandboxClass(field.getDeclaringClass())
                || delegate.permitsStaticFieldGet(field);
    }

    @Override
    public boolean permitsStaticFieldSet(final Field field, final Object value) {
        return isSandboxClass(field.getDeclaringClass())
                || delegate.permitsStaticFieldSet(field, value);
    }
}
