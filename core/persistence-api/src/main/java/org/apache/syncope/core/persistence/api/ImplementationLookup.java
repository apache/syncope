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
package org.apache.syncope.core.persistence.api;

import java.util.Set;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;

public interface ImplementationLookup extends SyncopeLoader {

    enum Type {

        JWT_SSO_PROVIDER,
        REPORTLET_CONF,
        ACCOUNT_RULE_CONF,
        PASSWORD_RULE_CONF,
        MAPPING_ITEM_TRANSFORMER,
        TASKJOBDELEGATE,
        RECONCILIATION_FILTER_BUILDER,
        LOGIC_ACTIONS,
        PROPAGATION_ACTIONS,
        PULL_ACTIONS,
        PUSH_ACTIONS,
        PULL_CORRELATION_RULE,
        VALIDATOR,
        NOTIFICATION_RECIPIENTS_PROVIDER,
        AUDIT_APPENDER;

    }

    Set<String> getClassNames(Type type);

    Set<Class<?>> getJWTSSOProviderClasses();

    Class<? extends Reportlet> getReportletClass(Class<? extends ReportletConf> reportletConfClass);

    Class<? extends AccountRule> getAccountRuleClass(Class<? extends AccountRuleConf> accountRuleConfClass);

    Class<? extends PasswordRule> getPasswordRuleClass(Class<? extends PasswordRuleConf> passwordRuleConfClass);

    Set<Class<?>> getAuditAppenderClasses();
}
