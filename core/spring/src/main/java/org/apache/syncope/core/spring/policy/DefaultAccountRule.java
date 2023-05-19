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
package org.apache.syncope.core.spring.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.AccountRuleConfClass;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@AccountRuleConfClass(DefaultAccountRuleConf.class)
public class DefaultAccountRule implements AccountRule {

    private DefaultAccountRuleConf conf;

    @Override
    public void setConf(final AccountRuleConf conf) {
        if (conf instanceof DefaultAccountRuleConf) {
            this.conf = DefaultAccountRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    DefaultAccountRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    protected void enforce(final String username, final Set<String> wordsNotPermitted) {
        // check min length
        if (conf.getMinLength() > 0 && conf.getMinLength() > username.length()) {
            throw new AccountPolicyException("Username too short");
        }

        // check max length
        if (conf.getMaxLength() > 0 && conf.getMaxLength() < username.length()) {
            throw new AccountPolicyException("Username too long");
        }

        // check words not permitted
        wordsNotPermitted.stream().
                filter(word -> StringUtils.containsIgnoreCase(username, word)).
                forEach(item -> {
                    throw new AccountPolicyException("Used word(s) not permitted");
                });

        // check case
        if (conf.isAllUpperCase() && !username.equals(username.toUpperCase())) {
            throw new AccountPolicyException("No lowercase characters permitted");
        }
        if (conf.isAllLowerCase() && !username.equals(username.toLowerCase())) {
            throw new AccountPolicyException("No uppercase characters permitted");
        }

        // check pattern
        Pattern pattern = conf.getPattern() == null ? Entity.ID_PATTERN : Pattern.compile(conf.getPattern());
        if (!pattern.matcher(username).matches()) {
            throw new AccountPolicyException("Username does not match pattern");
        }

        // check prefix
        conf.getPrefixesNotPermitted().stream().
                filter(username::startsWith).findAny().
                ifPresent(item -> {
                    throw new AccountPolicyException("Prefix not permitted");
                });

        // check suffix
        conf.getSuffixesNotPermitted().stream().
                filter(username::endsWith).findAny().
                ifPresent(item -> {
                    throw new AccountPolicyException("Suffix not permitted");
                });
    }

    @Override
    public void enforce(final String username) {
        Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
        enforce(username, wordsNotPermitted);
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user) {
        Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
        wordsNotPermitted.addAll(
                conf.getSchemasNotPermitted().stream().
                        map(schema -> user.getPlainAttr(schema).
                        map(PlainAttr::getValuesAsStrings).orElse(null)).
                        filter(Objects::nonNull).
                        filter(values -> !CollectionUtils.isEmpty(values)).
                        flatMap(Collection::stream).
                        collect(Collectors.toSet()));

        enforce(user.getUsername(), wordsNotPermitted);
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        if (StringUtils.isBlank(account.getUsername())) {
            return;
        }

        Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
        wordsNotPermitted.addAll(
                conf.getSchemasNotPermitted().stream().
                        map(schema -> account.getPlainAttr(schema).
                        map(PlainAttr::getValuesAsStrings).orElse(null)).
                        filter(Objects::nonNull).
                        filter(values -> !CollectionUtils.isEmpty(values)).
                        flatMap(Collection::stream).
                        collect(Collectors.toSet()));

        enforce(account.getUsername(), wordsNotPermitted);
    }
}
