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

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.AccountPolicyException;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.AccountRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@AccountRuleConfClass(DefaultAccountRuleConf.class)
public class DefaultAccountRule implements AccountRule {

    private static final Pattern DEFAULT_PATTERN = Pattern.compile("[a-zA-Z0-9-_@. ]+");

    private DefaultAccountRuleConf conf;

    @Transactional(readOnly = true)
    @Override
    public void enforce(final AccountRuleConf conf, final User user) {
        if (conf instanceof DefaultAccountRuleConf) {
            this.conf = DefaultAccountRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    AccountRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }

        this.conf.getSchemasNotPermitted().stream().
                map(schema -> user.getPlainAttr(schema)).
                filter(attr -> attr.isPresent()).
                map(attr -> attr.get().getValuesAsStrings()).
                filter(values -> (values != null && !values.isEmpty())).
                forEachOrdered(values -> this.conf.getWordsNotPermitted().add(values.get(0)));

        if (user.getUsername() == null) {
            throw new AccountPolicyException("Invalid account");
        }

        // check min length
        if (this.conf.getMinLength() > 0 && this.conf.getMinLength() > user.getUsername().length()) {
            throw new AccountPolicyException("Username too short");
        }

        // check max length
        if (this.conf.getMaxLength() > 0 && this.conf.getMaxLength() < user.getUsername().length()) {
            throw new AccountPolicyException("Username too long");
        }

        // check words not permitted
        this.conf.getWordsNotPermitted().stream().
                filter(word -> StringUtils.containsIgnoreCase(user.getUsername(), word)).
                forEachOrdered(item -> {
                    throw new AccountPolicyException("Used word(s) not permitted");
                });

        // check case
        if (this.conf.isAllUpperCase() && !user.getUsername().equals(user.getUsername().toUpperCase())) {
            throw new AccountPolicyException("No lowercase characters permitted");
        }
        if (this.conf.isAllLowerCase() && !user.getUsername().equals(user.getUsername().toLowerCase())) {
            throw new AccountPolicyException("No uppercase characters permitted");
        }

        // check pattern
        Pattern pattern = (this.conf.getPattern() == null) ? DEFAULT_PATTERN : Pattern.compile(this.conf.getPattern());
        if (!pattern.matcher(user.getUsername()).matches()) {
            throw new AccountPolicyException("Username does not match pattern");
        }

        // check prefix
        this.conf.getPrefixesNotPermitted().stream().
                filter(prefix -> user.getUsername().startsWith(prefix)).
                forEachOrdered(item -> {
                    throw new AccountPolicyException("Prefix not permitted");
                });

        // check suffix
        this.conf.getSuffixesNotPermitted().stream().
                filter(suffix -> user.getUsername().endsWith(suffix)).
                forEachOrdered(item -> {
                    throw new AccountPolicyException("Suffix not permitted");
                });
    }

}
