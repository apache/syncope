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
package org.apache.syncope.core.provisioning.api;

import java.util.Set;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;

public interface ImplementationLookup extends SyncopeCoreLoader {

    Set<String> getClassNames(String type);

    Class<? extends ReportJobDelegate> getReportClass(
            Class<? extends ReportConf> reportConfClass);

    Class<? extends AccountRule> getAccountRuleClass(
            Class<? extends AccountRuleConf> accountRuleConfClass);

    Class<? extends PasswordRule> getPasswordRuleClass(
            Class<? extends PasswordRuleConf> passwordRuleConfClass);

    Class<? extends InboundCorrelationRule> getInboundCorrelationRuleClass(
            Class<? extends InboundCorrelationRuleConf> inboundCorrelationRuleConfClass);

    Class<? extends PushCorrelationRule> getPushCorrelationRuleClass(
            Class<? extends PushCorrelationRuleConf> pushCorrelationRuleConfClass);
}
