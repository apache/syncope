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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationRecipientsProvider;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.apache.syncope.core.provisioning.api.pushpull.ReconciliationFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.PullCorrelationRule;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.java.data.JEXLMappingItemTransformerImpl;
import org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PlainAttrsPullCorrelationRule;

/**
 * Cache class names for all implementations of Syncope interfaces found in classpath, for later usage.
 */
@Component
public class ClassPathScanImplementationLookup implements ImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationLookup.class);

    private Map<Type, Set<String>> classNames;

    private Map<Class<? extends ReportletConf>, Class<? extends Reportlet>> reportletClasses;

    private Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> accountRuleClasses;

    private Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> passwordRuleClasses;
    
    private List<String> basePackages = Arrays.asList("org.apache.syncope.fit.core", "org.apache.syncope.core");

    @Override
    public Integer getPriority() {
        return 400;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        classNames = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            classNames.put(type, new HashSet<String>());
        }

        reportletClasses = new HashMap<>();
        accountRuleClasses = new HashMap<>();
        passwordRuleClasses = new HashMap<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Reportlet.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(MappingItemTransformer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SchedTaskJobDelegate.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ReconciliationFilterBuilder.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(LogicActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PropagationActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PullActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PushActions.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PullCorrelationRule.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(Validator.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(NotificationRecipientsProvider.class));

        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        for (String basePackage: getBasePackages()) {
            beanDefinitions.addAll(scanner.findCandidateComponents(basePackage));
        }
        for (BeanDefinition bd : beanDefinitions) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (Reportlet.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    ReportletConfClass annotation = clazz.getAnnotation(ReportletConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found Reportlet {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(Type.REPORTLET_CONF).add(annotation.value().getName());
                        reportletClasses.put(annotation.value(), (Class<? extends Reportlet>) clazz);
                    }
                }

                if (AccountRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    AccountRuleConfClass annotation = clazz.getAnnotation(AccountRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found account policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(Type.ACCOUNT_RULE_CONF).add(annotation.value().getName());
                        accountRuleClasses.put(annotation.value(), (Class<? extends AccountRule>) clazz);
                    }
                }

                if (PasswordRule.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    PasswordRuleConfClass annotation = clazz.getAnnotation(PasswordRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found password policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(Type.PASSWORD_RULE_CONF).add(annotation.value().getName());
                        passwordRuleClasses.put(annotation.value(), (Class<? extends PasswordRule>) clazz);
                    }
                }

                if (MappingItemTransformer.class.isAssignableFrom(clazz) && !isAbsractClazz
                        && !clazz.equals(JEXLMappingItemTransformerImpl.class)) {

                    classNames.get(Type.MAPPING_ITEM_TRANSFORMER).add(clazz.getName());
                }

                if (SchedTaskJobDelegate.class.isAssignableFrom(clazz) && !isAbsractClazz
                        && !PullJobDelegate.class.isAssignableFrom(clazz)
                        && !PushJobDelegate.class.isAssignableFrom(clazz)
                        && !GroupMemberProvisionTaskJobDelegate.class.isAssignableFrom(clazz)) {

                    classNames.get(Type.TASKJOBDELEGATE).add(bd.getBeanClassName());
                }

                if (ReconciliationFilterBuilder.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.RECONCILIATION_FILTER_BUILDER).add(bd.getBeanClassName());
                }

                if (LogicActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.LOGIC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PropagationActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PROPAGATION_ACTIONS).add(bd.getBeanClassName());
                }

                if (PullActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.SYNC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PushActions.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.PUSH_ACTIONS).add(bd.getBeanClassName());
                }

                if (PullCorrelationRule.class.isAssignableFrom(clazz) && !isAbsractClazz
                        && !PlainAttrsPullCorrelationRule.class.isAssignableFrom(clazz)) {
                    classNames.get(Type.PULL_CORRELATION_RULE).add(bd.getBeanClassName());
                }

                if (Validator.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.VALIDATOR).add(bd.getBeanClassName());
                }

                if (NotificationRecipientsProvider.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    classNames.get(Type.NOTIFICATION_RECIPIENTS_PROVIDER).add(bd.getBeanClassName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        classNames = Collections.unmodifiableMap(classNames);
        reportletClasses = Collections.unmodifiableMap(reportletClasses);
        accountRuleClasses = Collections.unmodifiableMap(accountRuleClasses);
        passwordRuleClasses = Collections.unmodifiableMap(passwordRuleClasses);

        LOG.debug("Implementation classes found: {}", classNames);
    }

    @Override
    public Set<String> getClassNames(final Type type) {
        return classNames.get(type);
    }

    @Override
    public Class<? extends Reportlet> getReportletClass(
            final Class<? extends ReportletConf> reportletConfClass) {

        return reportletClasses.get(reportletConfClass);
    }

    @Override
    public Class<? extends AccountRule> getAccountRuleClass(
            final Class<? extends AccountRuleConf> accountRuleConfClass) {

        return accountRuleClasses.get(accountRuleConfClass);
    }

    @Override
    public Class<? extends PasswordRule> getPasswordRuleClass(
            final Class<? extends PasswordRuleConf> passwordRuleConfClass) {

        return passwordRuleClasses.get(passwordRuleConfClass);
    }
    
    /**
     * @return basePackages for syncope class classpath scanning
     */
    protected List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(final List<String> basePackages) {
        this.basePackages = basePackages;
    }
}
