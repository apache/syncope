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
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.audit.JdbcAuditAppender;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.RecipientsProvider;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRuleConfClass;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.java.data.JEXLItemTransformerImpl;
import org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.security.JWTSSOProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Cache class names for all implementations of Syncope interfaces found in classpath, for later usage.
 */
public class ClassPathScanImplementationLookup implements ImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope.core";

    private Map<String, Set<String>> classNames;

    private Set<Class<?>> jwtSSOProviderClasses;

    private Map<Class<? extends ReportletConf>, Class<? extends Reportlet>> reportletClasses;

    private Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> accountRuleClasses;

    private Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> passwordRuleClasses;

    private Map<Class<? extends PullCorrelationRuleConf>, Class<? extends PullCorrelationRule>> pullCRClasses;

    private Map<Class<? extends PushCorrelationRuleConf>, Class<? extends PushCorrelationRule>> pushCRClasses;

    private Set<Class<?>> auditAppenderClasses;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
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

        jwtSSOProviderClasses = new HashSet<>();
        reportletClasses = new HashMap<>();
        accountRuleClasses = new HashMap<>();
        passwordRuleClasses = new HashMap<>();
        pullCRClasses = new HashMap<>();
        pushCRClasses = new HashMap<>();
        auditAppenderClasses = new HashSet<>();

        scanner.findCandidateComponents(getBasePackage()).forEach(bd -> {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        Objects.requireNonNull(bd.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                boolean isAbstractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (JWTSSOProvider.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdRepoImplementationType.JWT_SSO_PROVIDER).add(clazz.getName());
                    jwtSSOProviderClasses.add(clazz);
                }

                if (Reportlet.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    ReportletConfClass annotation = clazz.getAnnotation(ReportletConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found Reportlet {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.REPORTLET).add(clazz.getName());
                        reportletClasses.put(annotation.value(), (Class<? extends Reportlet>) clazz);
                    }
                }

                if (AccountRule.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    AccountRuleConfClass annotation = clazz.getAnnotation(AccountRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found account policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.ACCOUNT_RULE).add(clazz.getName());
                        accountRuleClasses.put(annotation.value(), (Class<? extends AccountRule>) clazz);
                    }
                }

                if (PasswordRule.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    PasswordRuleConfClass annotation = clazz.getAnnotation(PasswordRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found password policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.PASSWORD_RULE).add(clazz.getName());
                        passwordRuleClasses.put(annotation.value(), (Class<? extends PasswordRule>) clazz);
                    }
                }

                if (PullCorrelationRule.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    PullCorrelationRuleConfClass annotation = clazz.getAnnotation(PullCorrelationRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found pull correlation rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdMImplementationType.PULL_CORRELATION_RULE).add(clazz.getName());
                        pullCRClasses.put(annotation.value(), (Class<? extends PullCorrelationRule>) clazz);
                    }
                }

                if (PushCorrelationRule.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    PushCorrelationRuleConfClass annotation = clazz.getAnnotation(PushCorrelationRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found push correlation rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdMImplementationType.PUSH_CORRELATION_RULE).add(clazz.getName());
                        pushCRClasses.put(annotation.value(), (Class<? extends PushCorrelationRule>) clazz);
                    }
                }

                if (ItemTransformer.class.isAssignableFrom(clazz) && !isAbstractClazz
                        && !clazz.equals(JEXLItemTransformerImpl.class)) {

                    classNames.get(IdRepoImplementationType.ITEM_TRANSFORMER).add(clazz.getName());
                }

                if (SchedTaskJobDelegate.class.isAssignableFrom(clazz) && !isAbstractClazz
                        && !PullJobDelegate.class.isAssignableFrom(clazz)
                        && !PushJobDelegate.class.isAssignableFrom(clazz)
                        && !GroupMemberProvisionTaskJobDelegate.class.isAssignableFrom(clazz)) {

                    classNames.get(IdRepoImplementationType.TASKJOB_DELEGATE).add(bd.getBeanClassName());
                }

                if (ReconFilterBuilder.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdMImplementationType.RECON_FILTER_BUILDER).add(bd.getBeanClassName());
                }

                if (LogicActions.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdRepoImplementationType.LOGIC_ACTIONS).add(bd.getBeanClassName());
                }

                if (PropagationActions.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdMImplementationType.PROPAGATION_ACTIONS).add(bd.getBeanClassName());
                }

                if (PullActions.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdMImplementationType.PULL_ACTIONS).add(bd.getBeanClassName());
                }

                if (PushActions.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdMImplementationType.PUSH_ACTIONS).add(bd.getBeanClassName());
                }

                if (Validator.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdRepoImplementationType.VALIDATOR).add(bd.getBeanClassName());
                }

                if (RecipientsProvider.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdRepoImplementationType.RECIPIENTS_PROVIDER).add(bd.getBeanClassName());
                }

                if (AuditAppender.class.isAssignableFrom(clazz)
                        && !JdbcAuditAppender.class.equals(clazz) && !isAbstractClazz) {

                    classNames.get(IdRepoImplementationType.AUDIT_APPENDER).add(clazz.getName());
                    auditAppenderClasses.add(clazz);
                }

                if (ProvisionSorter.class.isAssignableFrom(clazz) && !isAbstractClazz) {
                    classNames.get(IdMImplementationType.PROVISION_SORTER).add(bd.getBeanClassName());
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });

        classNames = Collections.unmodifiableMap(classNames);
        LOG.debug("Implementation classes found: {}", classNames);

        jwtSSOProviderClasses = Collections.unmodifiableSet(jwtSSOProviderClasses);
        reportletClasses = Collections.unmodifiableMap(reportletClasses);
        accountRuleClasses = Collections.unmodifiableMap(accountRuleClasses);
        passwordRuleClasses = Collections.unmodifiableMap(passwordRuleClasses);
        pullCRClasses = Collections.unmodifiableMap(pullCRClasses);
        pushCRClasses = Collections.unmodifiableMap(pushCRClasses);
        auditAppenderClasses = Collections.unmodifiableSet(auditAppenderClasses);
    }

    @Override
    public Set<String> getClassNames(final String type) {
        return classNames.get(type);
    }

    @Override
    public Set<Class<?>> getJWTSSOProviderClasses() {
        return jwtSSOProviderClasses;
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

    @Override
    public Class<? extends PullCorrelationRule> getPullCorrelationRuleClass(
            final Class<? extends PullCorrelationRuleConf> correlationRuleConfClass) {

        return pullCRClasses.get(correlationRuleConfClass);
    }

    @Override
    public Class<? extends PushCorrelationRule> getPushCorrelationRuleClass(
            final Class<? extends PushCorrelationRuleConf> correlationRuleConfClass) {

        return pushCRClasses.get(correlationRuleConfClass);
    }

    @Override
    public Set<Class<?>> getAuditAppenderClasses() {
        return auditAppenderClasses;
    }
}
