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
package org.apache.syncope.fit.core.reference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.HaveIBeenPwnedPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.AuditReportletConf;
import org.apache.syncope.common.lib.report.GroupReportletConf;
import org.apache.syncope.common.lib.report.ReconciliationReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.report.StaticReportletConf;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.provisioning.java.job.report.AuditReportlet;
import org.apache.syncope.core.provisioning.java.job.report.GroupReportlet;
import org.apache.syncope.core.provisioning.java.job.report.ReconciliationReportlet;
import org.apache.syncope.core.provisioning.java.job.report.StaticReportlet;
import org.apache.syncope.core.provisioning.java.job.report.UserReportlet;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.AlwaysTrueValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BasicValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BinaryValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.EmailAddressValidator;
import org.apache.syncope.core.persistence.jpa.dao.DefaultPullCorrelationRule;
import org.apache.syncope.core.persistence.jpa.dao.DefaultPushCorrelationRule;
import org.apache.syncope.core.provisioning.java.pushpull.DefaultProvisionSorter;
import org.apache.syncope.core.provisioning.java.propagation.AzurePropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.GoogleAppsPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.core.spring.policy.DefaultAccountRule;
import org.apache.syncope.core.spring.policy.DefaultPasswordRule;
import org.apache.syncope.core.spring.policy.HaveIBeenPwnedPasswordRule;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeJWTSSOProvider;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

/**
 * Static implementation providing information about the integration test environment.
 */
public class ITImplementationLookup implements ImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ITImplementationLookup.class);

    private static final Set<Class<?>> JWTSSOPROVIDER_CLASSES = new HashSet<>(
            List.of(SyncopeJWTSSOProvider.class, CustomJWTSSOProvider.class));

    private static final Map<Class<? extends ReportletConf>, Class<? extends Reportlet>> REPORTLET_CLASSES =
            new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(AuditReportletConf.class, AuditReportlet.class);
            put(ReconciliationReportletConf.class, ReconciliationReportlet.class);
            put(GroupReportletConf.class, GroupReportlet.class);
            put(UserReportletConf.class, UserReportlet.class);
            put(StaticReportletConf.class, StaticReportlet.class);
        }
    };

    private static final Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> ACCOUNT_RULE_CLASSES =
            new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(TestAccountRuleConf.class, TestAccountRule.class);
            put(DefaultAccountRuleConf.class, DefaultAccountRule.class);
        }
    };

    private static final Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> PASSWORD_RULE_CLASSES =
            new HashMap<>() {

        private static final long serialVersionUID = -6624291041977583649L;

        {
            put(TestPasswordRuleConf.class, TestPasswordRule.class);
            put(DefaultPasswordRuleConf.class, DefaultPasswordRule.class);
            put(HaveIBeenPwnedPasswordRuleConf.class, HaveIBeenPwnedPasswordRule.class);
        }
    };

    private static final Map<
            Class<? extends PullCorrelationRuleConf>, Class<? extends PullCorrelationRule>> PULL_CR_CLASSES =
            new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(DummyPullCorrelationRuleConf.class, DummyPullCorrelationRule.class);
            put(DefaultPullCorrelationRuleConf.class, DefaultPullCorrelationRule.class);
            put(LinkedAccountSamplePullCorrelationRuleConf.class, LinkedAccountSamplePullCorrelationRule.class);
        }
    };

    private static final Map<
            Class<? extends PushCorrelationRuleConf>, Class<? extends PushCorrelationRule>> PUSH_CR_CLASSES =
            new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(DummyPushCorrelationRuleConf.class, DummyPushCorrelationRule.class);
            put(DefaultPushCorrelationRuleConf.class, DefaultPushCorrelationRule.class);
        }
    };

    private static final Set<Class<?>> AUDITAPPENDER_CLASSES = new HashSet<>(
            List.of(TestFileAuditAppender.class, TestFileRewriteAuditAppender.class));

    private static final Set<Class<?>> PROVISION_SORTER_CLASSES = new HashSet<>(
            List.of(DefaultProvisionSorter.class));

    private static final Map<String, Set<String>> CLASS_NAMES = new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            Set<String> classNames = ITImplementationLookup.JWTSSOPROVIDER_CLASSES.stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdRepoImplementationType.JWT_SSO_PROVIDER, classNames);

            classNames = new HashSet<>();
            classNames.add(ReconciliationReportletConf.class.getName());
            classNames.add(UserReportletConf.class.getName());
            classNames.add(GroupReportletConf.class.getName());
            classNames.add(AuditReportletConf.class.getName());
            classNames.add(StaticReportletConf.class.getName());
            put(IdRepoImplementationType.REPORTLET, classNames);

            classNames = ITImplementationLookup.ACCOUNT_RULE_CLASSES.values().stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdRepoImplementationType.ACCOUNT_RULE, classNames);

            classNames = ITImplementationLookup.PASSWORD_RULE_CLASSES.values().stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdRepoImplementationType.PASSWORD_RULE, classNames);

            classNames = new HashSet<>();
            classNames.add(DateToDateItemTransformer.class.getName());
            classNames.add(DateToLongItemTransformer.class.getName());
            put(IdRepoImplementationType.ITEM_TRANSFORMER, classNames);

            classNames = new HashSet<>();
            classNames.add(TestSampleJobDelegate.class.getName());
            put(IdRepoImplementationType.TASKJOB_DELEGATE, classNames);

            classNames = new HashSet<>();
            put(IdMImplementationType.RECON_FILTER_BUILDER, classNames);

            classNames = new HashSet<>();
            put(IdRepoImplementationType.LOGIC_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(LDAPMembershipPropagationActions.class.getName());
            classNames.add(LDAPPasswordPropagationActions.class.getName());
            classNames.add(DBPasswordPropagationActions.class.getName());
            classNames.add(AzurePropagationActions.class.getName());
            classNames.add(GoogleAppsPropagationActions.class.getName());
            put(IdMImplementationType.PROPAGATION_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(LDAPPasswordPullActions.class.getName());
            classNames.add(TestPullActions.class.getName());
            classNames.add(LDAPMembershipPullActions.class.getName());
            classNames.add(DBPasswordPullActions.class.getName());
            put(IdMImplementationType.PULL_ACTIONS, classNames);

            classNames = new HashSet<>();
            put(IdMImplementationType.PUSH_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(DummyPullCorrelationRule.class.getName());
            put(IdMImplementationType.PULL_CORRELATION_RULE, classNames);

            classNames = new HashSet<>();
            classNames.add(DummyPushCorrelationRule.class.getName());
            put(IdMImplementationType.PUSH_CORRELATION_RULE, classNames);

            classNames = new HashSet<>();
            classNames.add(BasicValidator.class.getName());
            classNames.add(EmailAddressValidator.class.getName());
            classNames.add(AlwaysTrueValidator.class.getName());
            classNames.add(BinaryValidator.class.getName());
            put(IdRepoImplementationType.VALIDATOR, classNames);

            classNames = new HashSet<>();
            classNames.add(TestNotificationRecipientsProvider.class.getName());
            put(IdRepoImplementationType.RECIPIENTS_PROVIDER, classNames);

            classNames = ITImplementationLookup.AUDITAPPENDER_CLASSES.stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdRepoImplementationType.AUDIT_APPENDER, classNames);

            classNames = ITImplementationLookup.PROVISION_SORTER_CLASSES.stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdMImplementationType.PROVISION_SORTER, classNames);
        }
    };

    private final UserWorkflowAdapter uwf;

    private final AnySearchDAO anySearchDAO;

    private final EnableFlowableForTestUsers enableFlowableForTestUsers;

    private final ElasticsearchInit elasticsearchInit;

    private boolean loaded;

    public ITImplementationLookup(
            final UserWorkflowAdapter uwf,
            final AnySearchDAO anySearchDAO,
            final EnableFlowableForTestUsers enableFlowableForTestUsers,
            final ElasticsearchInit elasticsearchInit) {

        this.uwf = uwf;
        this.anySearchDAO = anySearchDAO;
        this.enableFlowableForTestUsers = enableFlowableForTestUsers;
        this.elasticsearchInit = elasticsearchInit;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        if (loaded) {
            LOG.debug("Already loaded, nothing to do");
            return;
        }

        // in case the Flowable extension is enabled, enable modifications for test users
        if (enableFlowableForTestUsers != null && AopUtils.getTargetClass(uwf).getName().contains("Flowable")) {
            AuthContextUtils.callAsAdmin(domain, () -> {
                enableFlowableForTestUsers.init(datasource);
                return null;
            });
        }

        // in case the Elasticsearch extension is enabled, reinit a clean index for all available domains
        if (elasticsearchInit != null && AopUtils.getTargetClass(anySearchDAO).getName().contains("Elasticsearch")) {
            AuthContextUtils.callAsAdmin(domain, () -> {
                elasticsearchInit.init();
                return null;
            });
        }

        loaded = true;
    }

    @Override
    public Set<String> getClassNames(final String type) {
        return CLASS_NAMES.get(type);
    }

    @Override
    public Set<Class<?>> getJWTSSOProviderClasses() {
        return JWTSSOPROVIDER_CLASSES;
    }

    @Override
    public Class<? extends Reportlet> getReportletClass(
            final Class<? extends ReportletConf> reportletConfClass) {

        return REPORTLET_CLASSES.get(reportletConfClass);
    }

    @Override
    public Class<? extends AccountRule> getAccountRuleClass(
            final Class<? extends AccountRuleConf> accountRuleConfClass) {

        return ACCOUNT_RULE_CLASSES.get(accountRuleConfClass);
    }

    @Override
    public Class<? extends PasswordRule> getPasswordRuleClass(
            final Class<? extends PasswordRuleConf> passwordRuleConfClass) {

        return PASSWORD_RULE_CLASSES.get(passwordRuleConfClass);
    }

    @Override
    public Class<? extends PullCorrelationRule> getPullCorrelationRuleClass(
            final Class<? extends PullCorrelationRuleConf> pullCorrelationRuleConfClass) {

        return PULL_CR_CLASSES.get(pullCorrelationRuleConfClass);
    }

    @Override
    public Class<? extends PushCorrelationRule> getPushCorrelationRuleClass(
            final Class<? extends PushCorrelationRuleConf> pushCorrelationRuleConfClass) {

        return PUSH_CR_CLASSES.get(pushCorrelationRuleConfClass);
    }

    @Override
    public Set<Class<?>> getAuditAppenderClasses() {
        return AUDITAPPENDER_CLASSES;
    }
}
