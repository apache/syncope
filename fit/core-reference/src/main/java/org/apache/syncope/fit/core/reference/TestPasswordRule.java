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

import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.policy.PasswordPolicyException;
import org.apache.syncope.core.spring.security.Encryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@PasswordRuleConfClass(TestPasswordRuleConf.class)
public class TestPasswordRule implements PasswordRule {

    protected static final Logger LOG = LoggerFactory.getLogger(TestPasswordRule.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    private TestPasswordRuleConf conf;

    @Override
    public TestPasswordRuleConf getConf() {
        return conf;
    }

    @Override
    public void setConf(final PasswordRuleConf conf) {
        if (conf instanceof TestPasswordRuleConf) {
            this.conf = TestPasswordRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    PasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user) {
        if (user.getClearPassword() != null && !user.getClearPassword().endsWith(conf.getMustEndWith())) {
            throw new PasswordPolicyException("Password not ending with " + conf.getMustEndWith());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        if (account.getPassword() != null) {
            String clear = null;
            if (account.canDecodeSecrets()) {
                try {
                    clear = ENCRYPTOR.decode(account.getPassword(), account.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.error("Could not decode password for {}", account, e);
                }
            }

            if (clear != null && !clear.endsWith(conf.getMustEndWith())) {
                throw new PasswordPolicyException("Password not ending with " + conf.getMustEndWith());
            }
        }
    }
}
