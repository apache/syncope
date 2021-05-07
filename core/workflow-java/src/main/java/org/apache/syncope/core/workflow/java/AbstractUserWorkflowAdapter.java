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
package org.apache.syncope.core.workflow.java;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractUserWorkflowAdapter implements UserWorkflowAdapter {

    protected static final Logger LOG = LoggerFactory.getLogger(UserWorkflowAdapter.class);

    @Autowired
    protected UserDataBinder dataBinder;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, false, null, storePassword);
    }

    protected abstract UserWorkflowResult<Pair<String, Boolean>> doCreate(
            UserTO userTO, boolean disablePwdPolicyCheck, Boolean enabled, boolean storePassword);

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(
            final UserTO userTO,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final boolean storePassword) {

        return doCreate(userTO, disablePwdPolicyCheck, enabled, storePassword);
    }

    protected abstract UserWorkflowResult<String> doActivate(User user, String token);

    @Override
    public UserWorkflowResult<String> activate(final String key, final String token) {
        return doActivate(userDAO.authFind(key), token);
    }

    protected abstract UserWorkflowResult<Pair<UserPatch, Boolean>> doUpdate(User user, UserPatch userPatch);

    @Override
    public UserWorkflowResult<Pair<UserPatch, Boolean>> update(final UserPatch userPatch) {
        UserWorkflowResult<Pair<UserPatch, Boolean>> result = doUpdate(userDAO.authFind(userPatch.getKey()), userPatch);

        User user = userDAO.find(userPatch.getKey());
        if (!AuthContextUtils.getUsername().equals(user.getUsername())) {
            // ensure that requester's administration rights are still valid
            Set<String> authRealms = new HashSet<>();
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(StandardEntitlement.USER_READ, Collections.emptySet()));
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(StandardEntitlement.USER_UPDATE, Collections.emptySet()));
            userDAO.securityChecks(
                    authRealms,
                    user.getKey(),
                    user.getRealm().getFullPath(),
                    userDAO.findAllGroupKeys(user));
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doSuspend(User user);

    @Override
    public UserWorkflowResult<String> suspend(final String key) {
        User user = userDAO.authFind(key);

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    @Override
    public Pair<UserWorkflowResult<String>, Boolean> internalSuspend(final String key) {
        User user = userDAO.authFind(key);

        Pair<UserWorkflowResult<String>, Boolean> result = null;

        Pair<Boolean, Boolean> enforce = userDAO.enforcePolicies(user);
        if (enforce.getKey()) {
            LOG.debug("User {} {} is over the max failed logins", user.getKey(), user.getUsername());

            // reduce failed logins number to avoid multiple request       
            user.setFailedLogins(user.getFailedLogins() - 1);

            // set suspended flag
            user.setSuspended(Boolean.TRUE);

            result = Pair.of(doSuspend(user), enforce.getValue());
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doReactivate(User user);

    @Override
    public UserWorkflowResult<String> reactivate(final String key) {
        User user = userDAO.authFind(key);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doRequestPasswordReset(User user);

    @Override
    public void requestPasswordReset(final String key) {
        doRequestPasswordReset(userDAO.authFind(key));
    }

    protected abstract UserWorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            User user, String token, String password);

    @Override
    public UserWorkflowResult<Pair<UserPatch, Boolean>> confirmPasswordReset(
            final String key, final String token, final String password) {

        return doConfirmPasswordReset(userDAO.authFind(key), token, password);
    }

    protected abstract void doDelete(User user);

    @Override
    public void delete(final String userKey) {
        doDelete(userDAO.authFind(userKey));
    }
}
