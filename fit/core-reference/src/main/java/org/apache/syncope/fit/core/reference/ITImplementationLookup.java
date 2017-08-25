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
import javax.sql.DataSource;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.AuditReportletConf;
import org.apache.syncope.common.lib.report.GroupReportletConf;
import org.apache.syncope.common.lib.report.ReconciliationReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.report.StaticReportletConf;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.core.logic.TaskLogic;
import org.apache.syncope.core.provisioning.java.job.report.AuditReportlet;
import org.apache.syncope.core.provisioning.java.job.report.GroupReportlet;
import org.apache.syncope.core.provisioning.java.job.report.ReconciliationReportlet;
import org.apache.syncope.core.provisioning.java.job.report.StaticReportlet;
import org.apache.syncope.core.provisioning.java.job.report.UserReportlet;
import org.apache.syncope.core.migration.MigrationPullActions;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.AlwaysTrueValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BasicValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.EmailAddressValidator;
import org.apache.syncope.core.persistence.jpa.dao.DefaultAccountRule;
import org.apache.syncope.core.persistence.jpa.dao.DefaultPasswordRule;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeJWTSSOProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Static implementation providing information about the integration test environment.
 */
public class ITImplementationLookup implements ImplementationLookup {

    private static final Map<Type, Set<String>> CLASS_NAMES = new HashMap<Type, Set<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            Set<String> classNames = new HashSet<>();
            classNames.add(SyncopeJWTSSOProvider.class.getName());
            classNames.add(CustomJWTSSOProvider.class.getName());
            put(Type.JWT_SSO_PROVIDER, classNames);

            classNames = new HashSet<>();
            classNames.add(ReconciliationReportletConf.class.getName());
            classNames.add(UserReportletConf.class.getName());
            classNames.add(GroupReportletConf.class.getName());
            classNames.add(AuditReportletConf.class.getName());
            classNames.add(StaticReportletConf.class.getName());
            put(Type.REPORTLET_CONF, classNames);

            classNames = new HashSet<>();
            classNames.add(TestAccountRuleConf.class.getName());
            classNames.add(DefaultAccountRuleConf.class.getName());
            put(Type.ACCOUNT_RULE_CONF, classNames);

            classNames = new HashSet<>();
            classNames.add(TestPasswordRuleConf.class.getName());
            classNames.add(DefaultPasswordRuleConf.class.getName());
            put(Type.PASSWORD_RULE_CONF, classNames);

            classNames = new HashSet<>();
            classNames.add(PrefixItemTransformer.class.getName());
            put(Type.ITEM_TRANSFORMER, classNames);

            classNames = new HashSet<>();
            classNames.add(TestSampleJobDelegate.class.getName());
            put(Type.TASKJOBDELEGATE, classNames);

            classNames = new HashSet<>();
            classNames.add(TestReconciliationFilterBuilder.class.getName());
            put(Type.RECONCILIATION_FILTER_BUILDER, classNames);

            classNames = new HashSet<>();
            classNames.add(DoubleValueLogicActions.class.getName());
            put(Type.LOGIC_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(LDAPMembershipPropagationActions.class.getName());
            classNames.add(LDAPPasswordPropagationActions.class.getName());
            classNames.add(DBPasswordPropagationActions.class.getName());
            put(Type.PROPAGATION_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(LDAPPasswordPullActions.class.getName());
            classNames.add(TestPullActions.class.getName());
            classNames.add(MigrationPullActions.class.getName());
            classNames.add(LDAPMembershipPullActions.class.getName());
            classNames.add(DBPasswordPullActions.class.getName());
            put(Type.PULL_ACTIONS, classNames);

            classNames = new HashSet<>();
            put(Type.PUSH_ACTIONS, classNames);

            classNames = new HashSet<>();
            classNames.add(TestPullRule.class.getName());
            put(Type.PULL_CORRELATION_RULE, classNames);

            classNames = new HashSet<>();
            classNames.add(BasicValidator.class.getName());
            classNames.add(EmailAddressValidator.class.getName());
            classNames.add(AlwaysTrueValidator.class.getName());
            put(Type.VALIDATOR, classNames);

            classNames = new HashSet<>();
            classNames.add(TestNotificationRecipientsProvider.class.getName());
            put(Type.NOTIFICATION_RECIPIENTS_PROVIDER, classNames);

            classNames = new HashSet<>();
            classNames.add(TestFileRewriteAuditAppender.class.getName());
            classNames.add(TestFileAuditAppender.class.getName());
            put(Type.AUDIT_APPENDER, classNames);
        }
    };

    private static final Map<Class<? extends ReportletConf>, Class<? extends Reportlet>> REPORTLET_CLASSES =
            new HashMap<Class<? extends ReportletConf>, Class<? extends Reportlet>>() {

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
            new HashMap<Class<? extends AccountRuleConf>, Class<? extends AccountRule>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(TestAccountRuleConf.class, TestAccountRule.class);
            put(DefaultAccountRuleConf.class, DefaultAccountRule.class);
        }
    };

    private static final Map<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>> PASSWORD_RULE_CLASSES =
            new HashMap<Class<? extends PasswordRuleConf>, Class<? extends PasswordRule>>() {

        private static final long serialVersionUID = -6624291041977583649L;

        {
            put(TestPasswordRuleConf.class, TestPasswordRule.class);
            put(DefaultPasswordRuleConf.class, DefaultPasswordRule.class);
        }
    };

    @Autowired
    private AnySearchDAO anySearchDAO;

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private TaskLogic taskLogic;

    @Override
    public Integer getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void load() {
        // in case the Elasticsearch extension is enabled, reinit a clean index for all available domains
        if (AopUtils.getTargetClass(anySearchDAO).getName().contains("Elasticsearch")) {
            for (Map.Entry<String, DataSource> entry : domainsHolder.getDomains().entrySet()) {
                AuthContextUtils.execWithAuthContext(entry.getKey(), () -> {
                    SchedTaskTO task = new SchedTaskTO();
                    task.setJobDelegateClassName(
                            "org.apache.syncope.core.provisioning.java.job.ElasticsearchReindex");
                    task.setName("Elasticsearch Reindex");
                    task = taskLogic.createSchedTask(task);

                    taskLogic.execute(task.getKey(), null, false);

                    return null;
                });
            }
        }
    }

    @Override
    public Set<String> getClassNames(final Type type) {
        return CLASS_NAMES.get(type);
    }

    @Override
    public Set<Class<?>> getJWTSSOProviderClasses() {
        Set<Class<?>> classNames = new HashSet<>();
        classNames.add(SyncopeJWTSSOProvider.class);
        classNames.add(CustomJWTSSOProvider.class);
        return classNames;
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
    public Set<Class<?>> getAuditAppenderClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(TestFileRewriteAuditAppender.class);
        classes.add(TestFileAuditAppender.class);
        return classes;
    }
}
