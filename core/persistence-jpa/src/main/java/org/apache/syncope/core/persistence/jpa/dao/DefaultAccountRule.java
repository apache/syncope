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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.core.misc.policy.AccountPolicyException;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@AccountRuleConfClass(DefaultAccountRuleConf.class)
public class DefaultAccountRule implements AccountRule {

    private static final Pattern DEFAULT_PATTERN = Pattern.compile("[a-zA-Z0-9-_@. ]+");

    private DefaultAccountRuleConf conf;

    @Override
    public void setConf(final AccountRuleConf conf) {
        if (conf instanceof DefaultAccountRuleConf) {
            this.conf = (DefaultAccountRuleConf) conf;
        } else {
            throw new IllegalArgumentException(
                    AccountRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void isCompliant(final User user) {
        for (String schema : conf.getSchemasNotPermitted()) {
            PlainAttr<?> attr = user.getPlainAttr(schema);
            if (attr != null) {
                List<String> values = attr.getValuesAsStrings();
                if (values != null && !values.isEmpty()) {
                    conf.getWordsNotPermitted().add(values.get(0));
                }
            }
        }

        if (user.getUsername() == null) {
            throw new AccountPolicyException("Invalid account");
        }

        // check min length
        if (conf.getMinLength() > 0 && conf.getMinLength() > user.getUsername().length()) {
            throw new AccountPolicyException("Username too short");
        }

        // check max length
        if (conf.getMaxLength() > 0 && conf.getMaxLength() < user.getUsername().length()) {
            throw new AccountPolicyException("Username too long");
        }

        // check words not permitted
        for (String word : conf.getWordsNotPermitted()) {
            if (user.getUsername().contains(word)) {
                throw new AccountPolicyException("Used word(s) not permitted");
            }
        }

        // check case
        if (conf.isAllUpperCase() && !user.getUsername().equals(user.getUsername().toUpperCase())) {
            throw new AccountPolicyException("No lowercase characters permitted");
        }
        if (conf.isAllLowerCase() && !user.getUsername().equals(user.getUsername().toLowerCase())) {
            throw new AccountPolicyException("No uppercase characters permitted");
        }

        // check pattern
        Pattern pattern = (conf.getPattern() == null) ? DEFAULT_PATTERN : Pattern.compile(conf.getPattern());
        if (!pattern.matcher(user.getUsername()).matches()) {
            throw new AccountPolicyException("Username does not match pattern");
        }

        // check prefix
        for (String prefix : conf.getPrefixesNotPermitted()) {
            if (user.getUsername().startsWith(prefix)) {
                throw new AccountPolicyException("Prefix not permitted");
            }
        }

        // check suffix
        for (String suffix : conf.getSuffixesNotPermitted()) {
            if (user.getUsername().endsWith(suffix)) {
                throw new AccountPolicyException("Suffix not permitted");
            }
        }
    }

}
