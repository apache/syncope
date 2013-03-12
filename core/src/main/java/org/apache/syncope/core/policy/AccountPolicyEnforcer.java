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

import java.util.regex.Pattern;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicyEnforcer extends PolicyEnforcer<AccountPolicySpec, SyncopeUser> {

    private static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9-_@. ]+");

    private static final Pattern LCPATTERN = Pattern.compile("[a-z0-9-_@. ]+");

    private static final Pattern UCPATTERN = Pattern.compile("[A-Z0-9-_@. ]+");

    @Autowired(required = false)
    private UserSuspender userSuspender;

    /* (non-Javadoc)
     * @see AccountPolicyEnforcer#enforce(AccountPolicySpec, PolicyType, SyncopeUser)
     */
    @Override
    public void enforce(final AccountPolicySpec policy, final PolicyType type, final SyncopeUser user)
            throws AccountPolicyException, PolicyEnforceException {

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

        // check syntax
        if ((policy.isAllLowerCase() && !LCPATTERN.matcher(user.getUsername()).matches())
                || (policy.isAllUpperCase() && !UCPATTERN.matcher(user.getUsername()).matches())
                || !PATTERN.matcher(user.getUsername()).matches()) {

            throw new AccountPolicyException("Invalid username syntax");
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
                && user.getFailedLogins() != null && policy.getPermittedLoginRetries() > 0
                && user.getFailedLogins() > policy.getPermittedLoginRetries() && !user.isSuspended()) {

            userSuspender.suspend(user, policy.isPropagateSuspension());
        }
    }
}
