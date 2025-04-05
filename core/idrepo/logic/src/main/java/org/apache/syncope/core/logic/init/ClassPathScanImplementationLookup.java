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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.logic.api.LogicActions;
import org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValueValidator;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.job.report.ReportConfClass;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.macro.Command;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.provisioning.api.notification.RecipientsProvider;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.LiveSyncDeltaMapper;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.AccountRuleConfClass;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRuleConfClass;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRuleConfClass;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRuleConfClass;
import org.apache.syncope.core.provisioning.java.data.JEXLItemTransformerImpl;
import org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.MacroJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.LiveSyncJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
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

    private Map<Class<? extends ReportConf>, Class<? extends ReportJobDelegate>> reportJobDelegateClasses;

    private Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> accountRuleClasses;

    private Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> passwordRuleClasses;

    private Map<Class<? extends InboundCorrelationRuleConf>, Class<? extends InboundCorrelationRule>> inboundCRClasses;

    private Map<Class<? extends PushCorrelationRuleConf>, Class<? extends PushCorrelationRule>> pushCRClasses;

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

        Map<String, String> extImplTypes = ImplementationTypesHolder.getInstance().getValues().entrySet().stream().
                filter(e -> !IdRepoImplementationType.values().containsKey(e.getKey())
                && !IdMImplementationType.values().containsKey(e.getKey())).
                collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        reportJobDelegateClasses = new HashMap<>();
        accountRuleClasses = new HashMap<>();
        passwordRuleClasses = new HashMap<>();
        inboundCRClasses = new HashMap<>();
        pushCRClasses = new HashMap<>();

        for (BeanDefinition bd : scanner.findCandidateComponents(getBasePackage())) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        Objects.requireNonNull(bd.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                if (AccountRule.class.isAssignableFrom(clazz)) {
                    AccountRuleConfClass annotation = clazz.getAnnotation(AccountRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found account policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.ACCOUNT_RULE).add(clazz.getName());
                        accountRuleClasses.put(annotation.value(), (Class<? extends AccountRule>) clazz);
                    }
                } else if (PasswordRule.class.isAssignableFrom(clazz)) {
                    PasswordRuleConfClass annotation = clazz.getAnnotation(PasswordRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found password policy rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.PASSWORD_RULE).add(clazz.getName());
                        passwordRuleClasses.put(annotation.value(), (Class<? extends PasswordRule>) clazz);
                    }
                } else if (SchedTaskJobDelegate.class.isAssignableFrom(clazz)
                        && !PullJobDelegate.class.isAssignableFrom(clazz)
                        && !PushJobDelegate.class.isAssignableFrom(clazz)
                        && !GroupMemberProvisionTaskJobDelegate.class.isAssignableFrom(clazz)
                        && !MacroJobDelegate.class.isAssignableFrom(clazz)
                        && !LiveSyncJobDelegate.class.isAssignableFrom(clazz)) {

                    classNames.get(IdRepoImplementationType.TASKJOB_DELEGATE).add(bd.getBeanClassName());
                } else if (ReportJobDelegate.class.isAssignableFrom(clazz)) {
                    ReportConfClass annotation = clazz.getAnnotation(ReportConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found Report {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdRepoImplementationType.REPORT_DELEGATE).add(clazz.getName());
                        reportJobDelegateClasses.put(annotation.value(), (Class<? extends ReportJobDelegate>) clazz);
                    }
                } else if (LogicActions.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.LOGIC_ACTIONS).add(bd.getBeanClassName());
                } else if (MacroActions.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.MACRO_ACTIONS).add(bd.getBeanClassName());
                } else if (PlainAttrValueValidator.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.ATTR_VALUE_VALIDATOR).add(bd.getBeanClassName());
                } else if (DropdownValueProvider.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.DROPDOWN_VALUE_PROVIDER).add(bd.getBeanClassName());
                } else if (Command.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.COMMAND).add(bd.getBeanClassName());
                } else if (RecipientsProvider.class.isAssignableFrom(clazz)) {
                    classNames.get(IdRepoImplementationType.RECIPIENTS_PROVIDER).add(bd.getBeanClassName());
                } else if (ItemTransformer.class.isAssignableFrom(clazz)
                        && !clazz.equals(JEXLItemTransformerImpl.class)) {

                    classNames.get(IdRepoImplementationType.ITEM_TRANSFORMER).add(clazz.getName());
                } else if (ReconFilterBuilder.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.RECON_FILTER_BUILDER).add(bd.getBeanClassName());
                } else if (PropagationActions.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.PROPAGATION_ACTIONS).add(bd.getBeanClassName());
                } else if (InboundActions.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.INBOUND_ACTIONS).add(bd.getBeanClassName());
                } else if (PushActions.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.PUSH_ACTIONS).add(bd.getBeanClassName());
                } else if (InboundCorrelationRule.class.isAssignableFrom(clazz)) {
                    InboundCorrelationRuleConfClass annotation = clazz.getAnnotation(
                            InboundCorrelationRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found pull correlation rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdMImplementationType.INBOUND_CORRELATION_RULE).add(clazz.getName());
                        inboundCRClasses.put(annotation.value(), (Class<? extends InboundCorrelationRule>) clazz);
                    }
                } else if (PushCorrelationRule.class.isAssignableFrom(clazz)) {
                    PushCorrelationRuleConfClass annotation = clazz.getAnnotation(PushCorrelationRuleConfClass.class);
                    if (annotation == null) {
                        LOG.warn("Found push correlation rule {} without declared configuration", clazz.getName());
                    } else {
                        classNames.get(IdMImplementationType.PUSH_CORRELATION_RULE).add(clazz.getName());
                        pushCRClasses.put(annotation.value(), (Class<? extends PushCorrelationRule>) clazz);
                    }
                } else if (ProvisionSorter.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.PROVISION_SORTER).add(bd.getBeanClassName());
                } else if (LiveSyncDeltaMapper.class.isAssignableFrom(clazz)) {
                    classNames.get(IdMImplementationType.LIVE_SYNC_DELTA_MAPPER).add(bd.getBeanClassName());
                } else {
                    extImplTypes.forEach((typeName, typeInterface) -> {
                        Class<?> tic = ClassUtils.resolveClassName(typeInterface, ClassUtils.getDefaultClassLoader());
                        if (tic.isAssignableFrom(clazz)) {
                            classNames.get(typeName).add(bd.getBeanClassName());
                        }
                    });
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }

        classNames = Collections.unmodifiableMap(classNames);
        LOG.debug("Implementation classes found: {}", classNames);

        reportJobDelegateClasses = Collections.unmodifiableMap(reportJobDelegateClasses);
        accountRuleClasses = Collections.unmodifiableMap(accountRuleClasses);
        passwordRuleClasses = Collections.unmodifiableMap(passwordRuleClasses);
        inboundCRClasses = Collections.unmodifiableMap(inboundCRClasses);
        pushCRClasses = Collections.unmodifiableMap(pushCRClasses);
    }

    @Override
    public Set<String> getClassNames(final String type) {
        return classNames.get(type);
    }

    @Override
    public Class<? extends ReportJobDelegate> getReportClass(final Class<? extends ReportConf> reportConfClass) {
        return reportJobDelegateClasses.get(reportConfClass);
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
    public Class<? extends InboundCorrelationRule> getInboundCorrelationRuleClass(
            final Class<? extends InboundCorrelationRuleConf> inboundCorrelationRuleConfClass) {

        return inboundCRClasses.get(inboundCorrelationRuleConfClass);
    }

    @Override
    public Class<? extends PushCorrelationRule> getPushCorrelationRuleClass(
            final Class<? extends PushCorrelationRuleConf> correlationRuleConfClass) {

        return pushCRClasses.get(correlationRuleConfClass);
    }
}
