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
import org.apache.syncope.core.provisioning.api.utils.policy.PasswordPolicyException;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@PasswordRuleConfClass(TestPasswordRuleConf.class)
public class TestPasswordRule implements PasswordRule {

    private TestPasswordRuleConf conf;

    @Transactional(readOnly = true)
    @Override
    public void enforce(final PasswordRuleConf conf, final User user) {
        if (conf instanceof TestPasswordRuleConf) {
            this.conf = TestPasswordRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    PasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }

        if (!user.getClearPassword().endsWith(this.conf.getMustEndWith())) {
            throw new PasswordPolicyException("Password not ending with " + this.conf.getMustEndWith());
        }
    }

}
