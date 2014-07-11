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
package org.apache.syncope.core.policy;

import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyEnforcer extends PolicyEnforcer<PasswordPolicySpec, SyncopeUser> {

    /* (non-Javadoc)
     * @see
     * org.apache.syncope.core.policy.PasswordPolicyEnforcer#enforce(org.apache.syncope.common.types.PasswordPolicySpec,
     * org.apache.syncope.common.types.PolicyType, java.lang.String)
     */
    @Override
    public void enforce(final PasswordPolicySpec policy, final PolicyType type, final SyncopeUser user)
            throws PasswordPolicyException, PolicyEnforceException {

        final String clearPassword = user.getClearPassword();
        final String password = user.getPassword();

        if (policy == null) {
            throw new PolicyEnforceException("Invalid policy");
        }

        if (password == null && !policy.isAllowNullPassword()) {
            throw new PolicyEnforceException("Password must not be null and must be stored internally");
        } else if (password != null && clearPassword != null) {
            // check length
            if (policy.getMinLength() > 0 && policy.getMinLength() > clearPassword.length()) {
                throw new PasswordPolicyException("Password too short");
            }

            if (policy.getMaxLength() > 0 && policy.getMaxLength() < clearPassword.length()) {
                throw new PasswordPolicyException("Password too long");
            }

            // check words not permitted
            for (String word : policy.getWordsNotPermitted()) {
                if (clearPassword.contains(word)) {
                    throw new PasswordPolicyException("Used word(s) not permitted");
                }
            }

            // check digits occurrence
            if (policy.isDigitRequired() && !checkForDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must contain digit(s)");
            }

            // check lowercase alphabetic characters occurrence
            if (policy.isLowercaseRequired() && !checkForLowercase(clearPassword)) {
                throw new PasswordPolicyException("Password must contain lowercase alphabetic character(s)");
            }

            // check uppercase alphabetic characters occurrence
            if (policy.isUppercaseRequired() && !checkForUppercase(clearPassword)) {
                throw new PasswordPolicyException("Password must contain uppercase alphabetic character(s)");
            }

            // check prefix
            for (String prefix : policy.getPrefixesNotPermitted()) {
                if (clearPassword.startsWith(prefix)) {
                    throw new PasswordPolicyException("Prefix not permitted");
                }
            }

            // check suffix
            for (String suffix : policy.getSuffixesNotPermitted()) {
                if (clearPassword.endsWith(suffix)) {
                    throw new PasswordPolicyException("Suffix not permitted");
                }
            }

            // check digit first occurrence
            if (policy.isMustStartWithDigit() && !checkForFirstDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must start with a digit");
            }

            if (policy.isMustntStartWithDigit() && checkForFirstDigit(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with a digit");
            }

            // check digit last occurrence
            if (policy.isMustEndWithDigit() && !checkForLastDigit(clearPassword)) {
                throw new PasswordPolicyException("Password must end with a digit");
            }

            if (policy.isMustntEndWithDigit() && checkForLastDigit(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with a digit");
            }

            // check alphanumeric characters occurence
            if (policy.isAlphanumericRequired() && !checkForAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must contain alphanumeric character(s)");
            }

            // check non alphanumeric characters occurence
            if (policy.isNonAlphanumericRequired() && !checkForNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must contain non-alphanumeric character(s)");
            }

            // check alphanumeric character first occurrence
            if (policy.isMustStartWithAlpha() && !checkForFirstAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must start with an alphanumeric character");
            }

            if (policy.isMustntStartWithAlpha() && checkForFirstAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with an alphanumeric character");
            }

            // check alphanumeric character last occurrence
            if (policy.isMustEndWithAlpha() && !checkForLastAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must end with an alphanumeric character");
            }

            if (policy.isMustntEndWithAlpha() && checkForLastAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with an alphanumeric character");
            }

            // check non alphanumeric character first occurrence
            if (policy.isMustStartWithNonAlpha() && !checkForFirstNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must start with a non-alphanumeric character");
            }

            if (policy.isMustntStartWithNonAlpha() && checkForFirstNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't start with a non-alphanumeric character");
            }

            // check non alphanumeric character last occurrence
            if (policy.isMustEndWithNonAlpha() && !checkForLastNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password must end with a non-alphanumeric character");
            }

            if (policy.isMustntEndWithNonAlpha() && checkForLastNonAlphanumeric(clearPassword)) {
                throw new PasswordPolicyException("Password mustn't end with a non-alphanumeric character");
            }
        }
    }

    private boolean checkForDigit(final String str) {
        return PolicyPattern.DIGIT.matcher(str).matches();
    }

    private boolean checkForLowercase(final String str) {
        return PolicyPattern.ALPHA_LOWERCASE.matcher(str).matches();
    }

    private boolean checkForUppercase(final String str) {
        return PolicyPattern.ALPHA_UPPERCASE.matcher(str).matches();
    }

    private boolean checkForFirstDigit(final String str) {
        return PolicyPattern.FIRST_DIGIT.matcher(str).matches();
    }

    private boolean checkForLastDigit(final String str) {
        return PolicyPattern.LAST_DIGIT.matcher(str).matches();
    }

    private boolean checkForAlphanumeric(final String str) {
        return PolicyPattern.ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForFirstAlphanumeric(final String str) {
        return PolicyPattern.FIRST_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForLastAlphanumeric(final String str) {
        return PolicyPattern.LAST_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForNonAlphanumeric(final String str) {
        return PolicyPattern.NON_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForFirstNonAlphanumeric(final String str) {
        return PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForLastNonAlphanumeric(final String str) {
        return PolicyPattern.LAST_NON_ALPHANUMERIC.matcher(str).matches();
    }
}
