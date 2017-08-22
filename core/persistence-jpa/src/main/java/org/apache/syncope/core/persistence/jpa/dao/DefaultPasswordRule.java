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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.PasswordPolicyException;
import org.apache.syncope.core.provisioning.api.utils.policy.PolicyPattern;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@PasswordRuleConfClass(DefaultPasswordRuleConf.class)
public class DefaultPasswordRule implements PasswordRule {

    private DefaultPasswordRuleConf conf;

    @Transactional(readOnly = true)
    @Override
    public void enforce(final PasswordRuleConf conf, final User user) {
        if (conf instanceof DefaultPasswordRuleConf) {
            this.conf = (DefaultPasswordRuleConf) conf;
        } else {
            throw new IllegalArgumentException(
                    PasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }

        this.conf.getSchemasNotPermitted().stream().
                map(schema -> user.getPlainAttr(schema)).
                filter(attr -> attr.isPresent()).
                map(attr -> attr.get().getValuesAsStrings()).
                filter(values -> (values != null && !values.isEmpty())).
                forEachOrdered(values -> this.conf.getWordsNotPermitted().add(values.get(0)));

        String clearPassword = user.getClearPassword();
        String password = user.getPassword();

        if (password != null && clearPassword != null) {
            // check length
            if (this.conf.getMinLength() > 0 && this.conf.getMinLength() > clearPassword.length()) {
                throw new PasswordPolicyException("Password too short");
            }

            if (this.conf.getMaxLength() > 0 && this.conf.getMaxLength() < clearPassword.length()) {
                throw new PasswordPolicyException("Password too long");
            }

            // check words not permitted
            this.conf.getWordsNotPermitted().stream().
                    filter(word -> StringUtils.containsIgnoreCase(clearPassword, word)).
                    forEachOrdered(item -> {
                        throw new PasswordPolicyException("Used word(s) not permitted");
                    });

            // check digits occurrence
            if (this.conf.isDigitRequired() && !checkDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must contain digit(s)");
            }

            // check lowercase alphabetic characters occurrence
            if (this.conf.isLowercaseRequired() && !checkLowercase(clearPassword)) {
                throw new PasswordPolicyException("Password must contain lowercase alphabetic character(s)");
            }

            // check uppercase alphabetic characters occurrence
            if (this.conf.isUppercaseRequired() && !checkUppercase(clearPassword)) {
                throw new PasswordPolicyException("Password must contain uppercase alphabetic character(s)");
            }

            // check prefix
            this.conf.getPrefixesNotPermitted().stream().
                    filter(prefix -> clearPassword.startsWith(prefix)).
                    forEachOrdered(item -> {
                        throw new PasswordPolicyException("Prefix not permitted");
                    });

            // check suffix
            this.conf.getSuffixesNotPermitted().stream().
                    filter(suffix -> clearPassword.endsWith(suffix)).
                    forEachOrdered(item -> {
                        throw new PasswordPolicyException("Suffix not permitted");
                    });

            // check digit first occurrence
            if (this.conf.isMustStartWithDigit() && !checkFirstDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must start with a digit");
            }

            if (this.conf.isMustntStartWithDigit() && checkFirstDigit(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with a digit");
            }

            // check digit last occurrence
            if (this.conf.isMustEndWithDigit() && !checkLastDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must end with a digit");
            }

            if (this.conf.isMustntEndWithDigit() && checkLastDigit(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with a digit");
            }

            // check alphanumeric characters occurence
            if (this.conf.isAlphanumericRequired() && !checkAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must contain alphanumeric character(s)");
            }

            // check non alphanumeric characters occurence
            if (this.conf.isNonAlphanumericRequired() && !checkNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must contain non-alphanumeric character(s)");
            }

            // check alphanumeric character first occurrence
            if (this.conf.isMustStartWithAlpha() && !checkFirstAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must start with an alphanumeric character");
            }

            if (this.conf.isMustntStartWithAlpha() && checkFirstAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with an alphanumeric character");
            }

            // check alphanumeric character last occurrence
            if (this.conf.isMustEndWithAlpha() && !checkLastAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must end with an alphanumeric character");
            }

            if (this.conf.isMustntEndWithAlpha() && checkLastAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with an alphanumeric character");
            }

            // check non alphanumeric character first occurrence
            if (this.conf.isMustStartWithNonAlpha() && !checkFirstNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must start with a non-alphanumeric character");
            }

            if (this.conf.isMustntStartWithNonAlpha() && checkFirstNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with a non-alphanumeric character");
            }

            // check non alphanumeric character last occurrence
            if (this.conf.isMustEndWithNonAlpha() && !checkLastNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must end with a non-alphanumeric character");
            }

            if (this.conf.isMustntEndWithNonAlpha() && checkLastNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with a non-alphanumeric character");
            }

            if (!this.conf.isUsernameAllowed()
                    && user.getUsername() != null && user.getUsername().equals(clearPassword)) {

                throw new PasswordPolicyException("Password mustn't be equal to username");
            }
        }
    }

    private boolean checkDigit(final String str) {
        return PolicyPattern.DIGIT.matcher(str).matches();
    }

    private boolean checkLowercase(final String str) {
        return PolicyPattern.ALPHA_LOWERCASE.matcher(str).matches();
    }

    private boolean checkUppercase(final String str) {
        return PolicyPattern.ALPHA_UPPERCASE.matcher(str).matches();
    }

    private boolean checkFirstDigit(final String str) {
        return PolicyPattern.FIRST_DIGIT.matcher(str).matches();
    }

    private boolean checkLastDigit(final String str) {
        return PolicyPattern.LAST_DIGIT.matcher(str).matches();
    }

    private boolean checkAlphanumeric(final String str) {
        return PolicyPattern.ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkFirstAlphanumeric(final String str) {
        return PolicyPattern.FIRST_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkLastAlphanumeric(final String str) {
        return PolicyPattern.LAST_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkNonAlphanumeric(final String str) {
        return PolicyPattern.NON_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkFirstNonAlphanumeric(final String str) {
        return PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkLastNonAlphanumeric(final String str) {
        return PolicyPattern.LAST_NON_ALPHANUMERIC.matcher(str).matches();
    }
}
