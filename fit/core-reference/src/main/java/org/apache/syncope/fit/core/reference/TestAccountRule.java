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

import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.AccountPolicyException;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@AccountRuleConfClass(TestAccountRuleConf.class)
public class TestAccountRule implements AccountRule {

    private TestAccountRuleConf conf;

    @Transactional(readOnly = true)
    @Override
    public void enforce(final AccountRuleConf conf, final User user) {
        if (conf instanceof TestAccountRuleConf) {
            this.conf = TestAccountRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    AccountRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }

        if (!user.getUsername().contains(this.conf.getMustContainSubstring())) {
            throw new AccountPolicyException("Username not containing " + this.conf.getMustContainSubstring());
        }
    }

}
