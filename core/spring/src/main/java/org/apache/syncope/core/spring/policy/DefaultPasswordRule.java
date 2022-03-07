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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.Encryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@PasswordRuleConfClass(DefaultPasswordRuleConf.class)
public class DefaultPasswordRule implements PasswordRule {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPasswordRule.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    private DefaultPasswordRuleConf conf;

    @Override
    public PasswordRuleConf getConf() {
        return conf;
    }

    @Override
    public void setConf(final PasswordRuleConf conf) {
        if (conf instanceof DefaultPasswordRuleConf) {
            this.conf = (DefaultPasswordRuleConf) conf;
        } else {
            throw new IllegalArgumentException(
                    DefaultPasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    protected void enforce(final String clear, final String username, final Set<String> wordsNotPermitted) {
        // check length
        if (conf.getMinLength() > 0 && conf.getMinLength() > clear.length()) {
            throw new PasswordPolicyException("Password too short");
        }

        if (conf.getMaxLength() > 0 && conf.getMaxLength() < clear.length()) {
            throw new PasswordPolicyException("Password too long");
        }

        // check words not permitted
        if (!conf.isUsernameAllowed() && username != null && username.equals(clear)) {
            throw new PasswordPolicyException("Password mustn't be equal to username");
        }

        wordsNotPermitted.stream().
                filter(word -> StringUtils.containsIgnoreCase(clear, word)).
                forEach(item -> {
                    throw new PasswordPolicyException("Used word(s) not permitted");
                });

        // check digits occurrence
        if (conf.isDigitRequired() && !PolicyPattern.DIGIT.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must contain digit(s)");
        }

        // check lowercase alphabetic characters occurrence
        if (conf.isLowercaseRequired() && !PolicyPattern.ALPHA_LOWERCASE.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must contain lowercase alphabetic character(s)");
        }

        // check uppercase alphabetic characters occurrence
        if (conf.isUppercaseRequired() && !PolicyPattern.ALPHA_UPPERCASE.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must contain uppercase alphabetic character(s)");
        }

        // check prefix
        conf.getPrefixesNotPermitted().stream().
                filter(clear::startsWith).
                forEach(item -> {
                    throw new PasswordPolicyException("Prefix not permitted");
                });

        // check suffix
        conf.getSuffixesNotPermitted().stream().
                filter(clear::endsWith).
                forEach(item -> {
                    throw new PasswordPolicyException("Suffix not permitted");
                });

        // check digit first occurrence
        if (conf.isMustStartWithDigit() && !PolicyPattern.FIRST_DIGIT.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must start with a digit");
        }

        if (conf.isMustntStartWithDigit() && PolicyPattern.FIRST_DIGIT.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't start with a digit");
        }

        // check digit last occurrence
        if (conf.isMustEndWithDigit() && !PolicyPattern.LAST_DIGIT.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must end with a digit");
        }

        if (conf.isMustntEndWithDigit() && PolicyPattern.LAST_DIGIT.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't end with a digit");
        }

        // check alphanumeric characters occurence
        if (conf.isAlphanumericRequired() && !PolicyPattern.ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must contain alphanumeric character(s)");
        }

        // check non alphanumeric characters occurence
        if (conf.isNonAlphanumericRequired() && !PolicyPattern.NON_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must contain non-alphanumeric character(s)");
        }

        // check alphanumeric character first occurrence
        if (conf.isMustStartWithAlpha() && !PolicyPattern.FIRST_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must start with an alphanumeric character");
        }

        if (conf.isMustntStartWithAlpha() && PolicyPattern.FIRST_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't start with an alphanumeric character");
        }

        // check alphanumeric character last occurrence
        if (conf.isMustEndWithAlpha() && !PolicyPattern.LAST_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must end with an alphanumeric character");
        }

        if (conf.isMustntEndWithAlpha() && PolicyPattern.LAST_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't end with an alphanumeric character");
        }

        // check non alphanumeric character first occurrence
        if (conf.isMustStartWithNonAlpha() && !PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must start with a non-alphanumeric character");
        }

        if (conf.isMustntStartWithNonAlpha() && PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't start with a non-alphanumeric character");
        }

        // check non alphanumeric character last occurrence
        if (conf.isMustEndWithNonAlpha() && !PolicyPattern.LAST_NON_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password must end with a non-alphanumeric character");
        }

        if (conf.isMustntEndWithNonAlpha() && PolicyPattern.LAST_NON_ALPHANUMERIC.matcher(clear).matches()) {
            throw new PasswordPolicyException("Password mustn't end with a non-alphanumeric character");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user) {
        if (user.getPassword() != null && user.getClearPassword() != null) {
            Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
            wordsNotPermitted.addAll(
                    conf.getSchemasNotPermitted().stream().
                            map(user::getPlainAttr).
                            filter(Optional::isPresent).
                            map(attr -> attr.get().getValuesAsStrings()).
                            filter(values -> !CollectionUtils.isEmpty(values)).
                            flatMap(Collection::stream).
                            collect(Collectors.toSet()));

            enforce(user.getClearPassword(), user.getUsername(), wordsNotPermitted);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        conf.getWordsNotPermitted().addAll(
                conf.getSchemasNotPermitted().stream().
                        map(account::getPlainAttr).
                        filter(Optional::isPresent).
                        map(attr -> attr.get().getValuesAsStrings()).
                        filter(values -> !CollectionUtils.isEmpty(values)).
                        flatMap(Collection::stream).
                        collect(Collectors.toList()));

        if (account.getPassword() != null) {
            String clear = null;
            if (account.canDecodeSecrets()) {
                try {
                    clear = ENCRYPTOR.decode(account.getPassword(), account.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.error("Could not decode password for {}", account, e);
                }
            }

            if (clear != null) {
                Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
                wordsNotPermitted.addAll(
                        conf.getSchemasNotPermitted().stream().
                                map(account::getPlainAttr).
                                filter(Optional::isPresent).
                                map(attr -> attr.get().getValuesAsStrings()).
                                filter(values -> !CollectionUtils.isEmpty(values)).
                                flatMap(Collection::stream).
                                collect(Collectors.toSet()));

                enforce(clear, account.getUsername(), wordsNotPermitted);
            }
        }
    }
}
