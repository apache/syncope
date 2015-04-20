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
package org.apache.syncope.core.misc.policy;

import java.util.regex.Pattern;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserSuspender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicyEnforcer implements PolicyEnforcer<AccountPolicySpec, User> {

    private static final Pattern DEFAULT_PATTERN = Pattern.compile("[a-zA-Z0-9-_@. ]+");

    @Autowired(required = false)
    private UserSuspender userSuspender;

    @Override
    public void enforce(final AccountPolicySpec policy, final PolicyType type, final User user) {
        if (user.getUsername() == null) {
            throw new PolicyEnforceException("Invalid account");
        }

        if (policy == null) {
            throw new PolicyEnforceException("Invalid policy");
        }

        // check min length
        if (policy.getMinLength() > 0 && policy.getMinLength() > user.getUsername().length()) {
            throw new AccountPolicyException("Username too short");
        }

        // check max length
        if (policy.getMaxLength() > 0 && policy.getMaxLength() < user.getUsername().length()) {
            throw new AccountPolicyException("Username too long");
        }

        // check words not permitted
        for (String word : policy.getWordsNotPermitted()) {
            if (user.getUsername().contains(word)) {
                throw new AccountPolicyException("Used word(s) not permitted");
            }
        }

        // check case
        if (policy.isAllUpperCase() && !user.getUsername().equals(user.getUsername().toUpperCase())) {
            throw new AccountPolicyException("No lowercase characters permitted");
        }
        if (policy.isAllLowerCase() && !user.getUsername().equals(user.getUsername().toLowerCase())) {
            throw new AccountPolicyException("No uppercase characters permitted");
        }

        // check pattern
        Pattern pattern = (policy.getPattern() == null) ? DEFAULT_PATTERN : Pattern.compile(policy.getPattern());
        if (!pattern.matcher(user.getUsername()).matches()) {
            throw new AccountPolicyException("Username does not match pattern");
        }

        // check prefix
        for (String prefix : policy.getPrefixesNotPermitted()) {
            if (user.getUsername().startsWith(prefix)) {
                throw new AccountPolicyException("Prefix not permitted");
            }
        }

        // check suffix
        for (String suffix : policy.getSuffixesNotPermitted()) {
            if (user.getUsername().endsWith(suffix)) {
                throw new AccountPolicyException("Suffix not permitted");
            }
        }

        // check for subsequent failed logins
        if (userSuspender != null
                && user.getFailedLogins() != null && policy.getMaxAuthenticationAttempts() > 0
                && user.getFailedLogins() > policy.getMaxAuthenticationAttempts() && !user.isSuspended()) {

            userSuspender.suspend(user, policy.isPropagateSuspension());
        }
    }
}
