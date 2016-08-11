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
import org.apache.syncope.core.logic.report.AuditReportlet;
import org.apache.syncope.core.logic.report.GroupReportlet;
import org.apache.syncope.core.logic.report.ReconciliationReportlet;
import org.apache.syncope.core.logic.report.StaticReportlet;
import org.apache.syncope.core.logic.report.UserReportlet;
import org.apache.syncope.core.migration.MigrationPullActions;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.AlwaysTrueValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BasicValidator;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.EmailAddressValidator;
import org.apache.syncope.core.persistence.jpa.dao.DefaultAccountRule;
import org.apache.syncope.core.persistence.jpa.dao.DefaultPasswordRule;
import org.apache.syncope.core.provisioning.java.DefaultLogicActions;
import org.apache.syncope.core.provisioning.java.data.DefaultMappingItemTransformer;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;

/**
 * Static implementation providing information about the integration test environment.
 */
public class ITImplementationLookup implements ImplementationLookup {

    private static final Map<Type, Set<String>> CLASS_NAMES = new HashMap<Type, Set<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            Set<String> classNames = new HashSet<>();
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
            classNames.add(PrefixMappingItemTransformer.class.getName());
            classNames.add(DefaultMappingItemTransformer.class.getName());
            put(Type.MAPPING_ITEM_TRANSFORMER, classNames);

            classNames = new HashSet<>();
            classNames.add(TestSampleJobDelegate.class.getName());
            put(Type.TASKJOBDELEGATE, classNames);

            classNames = new HashSet<>();
            classNames.add(TestReconciliationFilterBuilder.class.getName());
            put(Type.RECONCILIATION_FILTER_BUILDER, classNames);

            classNames = new HashSet<>();
            classNames.add(DoubleValueLogicActions.class.getName());
            classNames.add(DefaultLogicActions.class.getName());
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

    @Override
    public Integer getPriority() {
        return 400;
    }

    @Override
    public void load() {
        // nothing to do
    }

    @Override
    public Set<String> getClassNames(final Type type) {
        return CLASS_NAMES.get(type);
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
}
