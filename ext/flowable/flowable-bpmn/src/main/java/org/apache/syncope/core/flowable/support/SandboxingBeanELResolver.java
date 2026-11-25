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
package org.apache.syncope.core.flowable.support;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.MethodNotFoundException;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;

/**
 * {@link BeanELResolver} which only allows method invocation on a small, explicitly whitelisted set of Syncope types,
 * and denies access to properties and types that can be abused to reach reflection and arbitrary code execution
 * (e.g. {@code getClass}, {@code class}, {@code Class}, {@code Runtime}, {@code ProcessBuilder}).
 */
public class SandboxingBeanELResolver extends BeanELResolver {

    protected static final Set<String> FORBIDDEN_METHODS = Set.of(
            "getClass", "wait", "notify", "notifyAll", "clone", "finalize",
            "forName", "getRuntime", "exec", "invoke", "newInstance",
            "getMethod", "getDeclaredMethod", "getConstructor", "getDeclaredConstructor",
            "getMethods", "getDeclaredMethods", "getConstructors", "getDeclaredConstructors",
            "getFields", "getDeclaredFields", "loadClass", "defineClass", "findClass",
            "getClassLoader", "getSystemClassLoader", "getContextClassLoader",
            "setAccessible", "getProtectionDomain", "getResource", "getResourceAsStream",
            "getSuperclass", "getComponentType", "getEnumConstants", "getDeclaringClass");

    protected static final Set<String> FORBIDDEN_PROPERTIES = Set.of(
            "class", "classLoader", "protectionDomain", "declaringClass", "superclass",
            "componentType", "enumConstants");

    protected static final Map<Class<?>, Set<String>> ALLOWED_METHODS = Map.of(
            User.class, methodNames(User.class),
            UserCR.class, methodNames(UserCR.class),
            UserUR.class, methodNames(UserUR.class),
            UserTO.class, methodNames(UserTO.class),
            FlowableWorkflowUtils.class, methodNames(FlowableWorkflowUtils.class),
            String.class, methodNames(String.class),
            Collection.class, methodNames(Collection.class),
            List.class, methodNames(List.class),
            Map.class, methodNames(Map.class));

    protected static final Set<Class<?>> FORBIDDEN_BASE_TYPES = Set.of(
            Class.class,
            ClassLoader.class,
            Runtime.class,
            ProcessBuilder.class,
            Process.class,
            AccessibleObject.class);

    protected static Set<String> methodNames(final Class<?> type) {
        return Stream.of(type.getMethods()).
                map(Method::getName).
                filter(name -> !FORBIDDEN_METHODS.contains(name)).
                collect(Collectors.toUnmodifiableSet());
    }

    protected static boolean isForbiddenBase(final Object base) {
        return FORBIDDEN_BASE_TYPES.stream().anyMatch(type -> type.isInstance(base));
    }

    protected static boolean isAllowedMethod(final Object base, final String method) {
        if (base == null || method == null || FORBIDDEN_METHODS.contains(method)) {
            return false;
        }

        return ALLOWED_METHODS.entrySet().stream().
                anyMatch(entry -> entry.getKey().isInstance(base) && entry.getValue().contains(method));
    }

    protected static void checkProperty(final Object base, final Object property) {
        if (base == null) {
            return;
        }
        if (isForbiddenBase(base)) {
            throw new PropertyNotFoundException("Property access is not allowed on " + base.getClass().getName());
        }

        String name = property == null ? null : property.toString();
        if (name != null && FORBIDDEN_PROPERTIES.contains(name)) {
            throw new PropertyNotFoundException("Property '" + name + "' is not allowed on "
                    + base.getClass().getName());
        }
    }

    @Override
    public Object invoke(
            final ELContext context,
            final Object base,
            final Object method,
            final Class<?>[] paramTypes,
            final Object[] params) {

        if (base != null) {
            String name = method == null ? null : method.toString();
            if (!isAllowedMethod(base, name)) {
                throw new MethodNotFoundException("Method '" + name + "' is not allowed on "
                        + base.getClass().getName());
            }
        }

        return super.invoke(context, base, method, paramTypes, params);
    }

    @Override
    public Object getValue(final ELContext context, final Object base, final Object property) {
        checkProperty(base, property);
        return super.getValue(context, base, property);
    }

    @Override
    public Class<?> getType(final ELContext context, final Object base, final Object property) {
        checkProperty(base, property);
        return super.getType(context, base, property);
    }

    @Override
    public boolean isReadOnly(final ELContext context, final Object base, final Object property) {
        checkProperty(base, property);
        return super.isReadOnly(context, base, property);
    }

    @Override
    public void setValue(final ELContext context, final Object base, final Object property, final Object value) {
        checkProperty(base, property);
        super.setValue(context, base, property, value);
    }
}
