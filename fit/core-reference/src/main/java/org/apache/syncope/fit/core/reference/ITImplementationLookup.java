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
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.logic.job.MacroJobDelegate;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.AlwaysTrueValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BasicValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BinaryValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.EmailAddressValidator;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PullCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;
import org.apache.syncope.core.provisioning.java.job.ExpiredAccessTokenCleanup;
import org.apache.syncope.core.provisioning.java.job.ExpiredBatchCleanup;
import org.apache.syncope.core.provisioning.java.propagation.AzurePropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.GoogleAppsPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.DefaultProvisionSorter;
import org.apache.syncope.core.provisioning.java.pushpull.DefaultPullCorrelationRule;
import org.apache.syncope.core.provisioning.java.pushpull.DefaultPushCorrelationRule;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.policy.DefaultAccountRule;
import org.apache.syncope.core.spring.policy.DefaultPasswordRule;
import org.apache.syncope.core.spring.policy.HaveIBeenPwnedPasswordRule;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.aop.support.AopUtils;

/**
 * Static implementation providing information about the integration test environment.
 */
public class ITImplementationLookup implements ImplementationLookup {

    private static final Map<Class<? extends ReportConf>, Class<? extends ReportJobDelegate>> REPORT_CLASSES =
            Map.of(SampleReportConf.class, SampleReportJobDelegate.class);

    private static final Map<Class<? extends AccountRuleConf>, Class<? extends AccountRule>> ACCOUNT_RULE_CLASSES =
            Map.of(
                    TestAccountRuleConf.class, TestAccountRule.class,
                    DefaultAccountRuleConf.class, DefaultAccountRule.class);

    private static final Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> PASSWORD_RULE_CLASSES =
            Map.of(
                    TestPasswordRuleConf.class, TestPasswordRule.class,
                    DefaultPasswordRuleConf.class, DefaultPasswordRule.class,
                    HaveIBeenPwnedPasswordRuleConf.class, HaveIBeenPwnedPasswordRule.class);

    private static final Map<
            Class<? extends PullCorrelationRuleConf>, Class<? extends PullCorrelationRule>> PULL_CR_CLASSES =
            Map.of(
                    DummyPullCorrelationRuleConf.class, DummyPullCorrelationRule.class,
                    DefaultPullCorrelationRuleConf.class, DefaultPullCorrelationRule.class,
                    LinkedAccountSamplePullCorrelationRuleConf.class, LinkedAccountSamplePullCorrelationRule.class);

    private static final Map<
            Class<? extends PushCorrelationRuleConf>, Class<? extends PushCorrelationRule>> PUSH_CR_CLASSES =
            Map.of(
                    DummyPushCorrelationRuleConf.class, DummyPushCorrelationRule.class,
                    DefaultPushCorrelationRuleConf.class, DefaultPushCorrelationRule.class);

    private static final Set<Class<?>> PROVISION_SORTER_CLASSES =
            Set.of(DefaultProvisionSorter.class);

    private static final Set<Class<?>> COMMAND_CLASSES =
            Set.of(TestCommand.class);

    private static final Map<String, Set<String>> CLASS_NAMES = new HashMap<>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            Set<String> classNames = new HashSet<>();
            classNames.add(SampleReportJobDelegate.class.getName());
            put(IdRepoImplementationType.REPORT_DELEGATE, classNames);

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
            classNames.add(MacroJobDelegate.class.getName());
            classNames.add(PullJobDelegate.class.getName());
            classNames.add(PushJobDelegate.class.getName());
            classNames.add(ExpiredAccessTokenCleanup.class.getName());
            classNames.add(ExpiredBatchCleanup.class.getName());
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
            put(IdRepoImplementationType.ATTR_VALUE_VALIDATOR, classNames);

            classNames = new HashSet<>();
            classNames.add(RealmFullPathDropdownValueProvider.class.getName());
            put(IdRepoImplementationType.DROPDOWN_VALUE_PROVIDER, classNames);

            classNames = new HashSet<>();
            classNames.add(TestNotificationRecipientsProvider.class.getName());
            put(IdRepoImplementationType.RECIPIENTS_PROVIDER, classNames);

            classNames = ITImplementationLookup.PROVISION_SORTER_CLASSES.stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdMImplementationType.PROVISION_SORTER, classNames);

            classNames = ITImplementationLookup.COMMAND_CLASSES.stream().
                    map(Class::getName).collect(Collectors.toSet());
            put(IdRepoImplementationType.COMMAND, classNames);
        }
    };

    private final UserWorkflowAdapter uwf;

    private final AnySearchDAO anySearchDAO;

    private final EnableFlowableForTestUsers enableFlowableForTestUsers;

    private final ElasticsearchInit elasticsearchInit;

    private final OpenSearchInit openSearchInit;

    public ITImplementationLookup(
            final UserWorkflowAdapter uwf,
            final AnySearchDAO anySearchDAO,
            final EnableFlowableForTestUsers enableFlowableForTestUsers,
            final ElasticsearchInit elasticsearchInit,
            final OpenSearchInit openSearchInit) {

        this.uwf = uwf;
        this.anySearchDAO = anySearchDAO;
        this.enableFlowableForTestUsers = enableFlowableForTestUsers;
        this.elasticsearchInit = elasticsearchInit;
        this.openSearchInit = openSearchInit;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
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

        // in case the OpenSearch extension is enabled, reinit a clean index for all available domains
        if (openSearchInit != null && AopUtils.getTargetClass(anySearchDAO).getName().contains("OpenSearch")) {
            AuthContextUtils.callAsAdmin(domain, () -> {
                openSearchInit.init();
                return null;
            });
        }
    }

    @Override
    public Set<String> getClassNames(final String type) {
        return CLASS_NAMES.get(type);
    }

    @Override
    public Class<? extends ReportJobDelegate> getReportClass(final Class<? extends ReportConf> reportConfClass) {
        return REPORT_CLASSES.get(reportConfClass);
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
}
