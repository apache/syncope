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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.provisioning.api.sync.SyncCorrelationRule;
import org.apache.syncope.core.logic.report.Reportlet;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.sync.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.sync.SyncJobDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Cache class names for all implementations of Syncope interfaces found in classpath, for later usage.
 */
@Component
public class ImplementationClassNamesLoader implements SyncopeLoader {

    public enum Type {

        REPORTLET,
        ACCOUNT_RULE,
        PASSWORD_RULE,
        TASKJOBDELEGATE,
        PROPAGATION_ACTIONS,
        SYNC_ACTIONS,
        PUSH_ACTIONS,
        SYNC_CORRELATION_RULE,
        PUSH_CORRELATION_RULE,
        VALIDATOR

    }

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationClassNamesLoader.class);

    private Map<Type, Set<String>> classNames;

    @Override
    public Integer getPriority() {
        return 400;
    }

    @Override
    public void load() {
        classNames = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            classNames.put(type, new HashSet<String>());
        }

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Reportlet.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SchedTaskJobDelegate.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SyncActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PushActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SyncCorrelationRule.class));
        // Remove once SYNCOPE-470 is done
        //scanner.addIncludeFilter(new AssignableTypeFilter(PushCorrelationRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PropagationActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(Validator.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.EMPTY)) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (Reportlet.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.REPORTLET).add(clazz.getName());
                }

                if (AccountRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.ACCOUNT_RULE).add(clazz.getName());
                }
                if (PasswordRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PASSWORD_RULE).add(clazz.getName());
                }

                if (SchedTaskJobDelegate.class.isAssignableFrom(clazz) && !isAbsractClazz
                        && !SyncJobDelegate.class.isAssignableFrom(clazz)
                        && !PushJobDelegate.class.isAssignableFrom(clazz)) {

                    classNames.get(Type.TASKJOBDELEGATE).add(bd.getBeanClassName());
                }

                if (SyncActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.SYNC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PushActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PUSH_ACTIONS).add(bd.getBeanClassName());
                }

                if (SyncCorrelationRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.SYNC_CORRELATION_RULE).add(bd.getBeanClassName());
                }

                // Uncomment when SYNCOPE-470 is done
                /* if (PushCorrelationRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                 * classNames.get(Type.PUSH_CORRELATION_RULES).add(metadata.getClassName());
                 * } */
                if (PropagationActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PROPAGATION_ACTIONS).add(bd.getBeanClassName());
                }

                if (Validator.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.VALIDATOR).add(bd.getBeanClassName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        classNames = Collections.unmodifiableMap(classNames);

        LOG.debug("Implementation classes found: {}", classNames);
    }

    public Set<String> getClassNames(final Type type) {
        return classNames.get(type);
    }
}
