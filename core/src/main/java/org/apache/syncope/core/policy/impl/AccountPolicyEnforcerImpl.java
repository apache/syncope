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
package org.apache.syncope.core.policy.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.policy.AccountPolicyEnforcer;
import org.apache.syncope.core.policy.AccountPolicyException;
import org.apache.syncope.core.policy.PolicyEnforceException;
import org.apache.syncope.core.policy.PolicyEnforcer;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.types.AccountPolicySpec;
import org.apache.syncope.types.PolicyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicyEnforcerImpl extends PolicyEnforcer<AccountPolicySpec, SyncopeUser> implements AccountPolicyEnforcer {

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private UserDataBinder userDataBinder;

    private static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9-_@. ]+");

    private static final Pattern LCPATTERN = Pattern.compile("[a-z0-9-_@. ]+");

    private static final Pattern UCPATTERN = Pattern.compile("[A-Z0-9-_@. ]+");

    /* (non-Javadoc)
	 * @see org.apache.syncope.core.policy.AccountPolicyEnforcer#enforce(org.apache.syncope.types.AccountPolicySpec, org.apache.syncope.types.PolicyType, org.apache.syncope.core.persistence.beans.user.SyncopeUser)
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
        if (user.getFailedLogins() != null && policy.getPermittedLoginRetries() > 0
                && user.getFailedLogins() > policy.getPermittedLoginRetries() && !user.getSuspended()) {
            try {
                LOG.debug("User {}:{} is over to max failed logins", user.getId(), user.getUsername());

                // reduce failed logins number to avoid multiple request
                user.setFailedLogins(user.getFailedLogins() - 1);

                // disable user
                final WorkflowResult<Long> updated = uwfAdapter.suspend(user);

                // propagate suspension if and only if it is required by policy
                if (policy.isPropagateSuspension()) {
                    final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                            new WorkflowResult<Map.Entry<Long, Boolean>>(
                            new DefaultMapEntry(updated.getResult(), Boolean.FALSE),
                            updated.getPropByRes(), updated.getPerformedTasks()));

                    taskExecutor.execute(tasks);
                }

                if (LOG.isDebugEnabled()) {
                    final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());
                    LOG.debug("About to return suspended user\n{}", savedTO);
                }
            } catch (Exception e) {
                LOG.error("Error during user suspension", e);
            }
        }
    }
}
