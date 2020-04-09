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
package org.apache.syncope.core.logic.init;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.apache.syncope.common.lib.policy.AuthPolicyConf;

/**
 * Cache class names for all implementations of Syncope interfaces found in classpath, for later usage.
 */
public class ClassPathScanImplementationLookup implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope.core";

    private Map<String, Set<String>> classNames;

    private Map<Class<? extends AuthPolicyConf>, Class<? extends AuthPolicyConf>> authPolicyClasses;

    private Map<Class<? extends AccessPolicyConf>, Class<? extends AccessPolicyConf>> accessPolicyClasses;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * This method can be overridden by subclasses to customize classpath scan.
     *
     * @return basePackage for classpath scanning
     */
    protected static String getBasePackage() {
        return DEFAULT_BASE_PACKAGE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load() {
        classNames = new HashMap<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        ImplementationTypesHolder.getInstance().getValues().forEach((typeName, typeInterface) -> {
            classNames.put(typeName, new HashSet<>());
            try {
                scanner.addIncludeFilter(new AssignableTypeFilter(
                        ClassUtils.resolveClassName(typeInterface, ClassUtils.getDefaultClassLoader())));
            } catch (IllegalArgumentException e) {
                LOG.error("Could not find class, ignoring...", e);
            }
        });

        authPolicyClasses = new HashMap<>();
        accessPolicyClasses = new HashMap<>();

        scanner.findCandidateComponents(getBasePackage()).forEach(bd -> {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        Objects.requireNonNull(bd.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                boolean isAbstractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (AuthPolicyConf.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(AMImplementationType.AUTH_POLICY_CONFIGURATIONS).add(bd.getBeanClassName());
                }
                if (AccessPolicyConf.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(AMImplementationType.ACCESS_POLICY_CONFIGURATIONS).add(bd.getBeanClassName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });

        classNames = Collections.unmodifiableMap(classNames);
        LOG.debug("Implementation classes found: {}", classNames);

        authPolicyClasses = Collections.unmodifiableMap(authPolicyClasses);
        accessPolicyClasses = Collections.unmodifiableMap(accessPolicyClasses);
    }

    public Set<String> getClassNames(final String type) {
        return classNames.get(type);
    }

    public Class<? extends AuthPolicyConf> getAuthPolicyConfClass(
            final Class<? extends AuthPolicyConf> authPolicyConfClass) {

        return authPolicyClasses.get(authPolicyConfClass);
    }

    public Class<? extends AccessPolicyConf> getAccessPolicyConfClass(
            final Class<? extends AccessPolicyConf> accessPolicyConfClass) {

        return accessPolicyClasses.get(accessPolicyConfClass);
    }
}
