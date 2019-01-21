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
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.provisioning.api.RequestedAuthnContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.apache.syncope.core.provisioning.api.SAML2IdPActions;

@Component
public class SAML2SPClassPathScanImplementationLookup implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope.core";

    private Set<String> actionsClasses;

    private Set<String> requestedAuthnContextProvidersClasses;

    @Override
    public int getOrder() {
        return 999;
    }

    @Override
    public void load() {
        actionsClasses = new HashSet<>();
        requestedAuthnContextProvidersClasses = new HashSet<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(SAML2IdPActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(RequestedAuthnContextProvider.class));

        scanner.findCandidateComponents(DEFAULT_BASE_PACKAGE).forEach(bd -> {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbstractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (SAML2IdPActions.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    actionsClasses.add(clazz.getName());
                } else if (RequestedAuthnContextProvider.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    requestedAuthnContextProvidersClasses.add(clazz.getName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });

        actionsClasses = Collections.unmodifiableSet(actionsClasses);
        requestedAuthnContextProvidersClasses = Collections.unmodifiableSet(requestedAuthnContextProvidersClasses);
    }

    public Set<String> getActionsClasses() {
        return actionsClasses;
    }

    public Set<String> getRequestedAuthnContextProvidersClasses() {
        return requestedAuthnContextProvidersClasses;
    }
}
