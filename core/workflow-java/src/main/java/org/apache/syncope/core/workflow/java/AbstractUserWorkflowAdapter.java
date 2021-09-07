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

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractUserWorkflowAdapter extends AbstractWorkflowAdapter implements UserWorkflowAdapter {

    protected static final Logger LOG = LoggerFactory.getLogger(UserWorkflowAdapter.class);

    protected final UserDataBinder dataBinder;

    protected final UserDAO userDAO;

    protected final EntityFactory entityFactory;

    public AbstractUserWorkflowAdapter(
            final UserDataBinder dataBinder,
            final UserDAO userDAO,
            final EntityFactory entityFactory) {

        this.dataBinder = dataBinder;
        this.userDAO = userDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(
            final UserCR userCR, final String creator, final String context) {

        return create(userCR, false, null, creator, context);
    }

    protected abstract UserWorkflowResult<Pair<String, Boolean>> doCreate(
            UserCR userCR, boolean disablePwdPolicyCheck, Boolean enabled, String creator, String context);

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(
            final UserCR userCR,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final String creator,
            final String context) {

        return doCreate(userCR, disablePwdPolicyCheck, enabled, creator, context);
    }

    protected abstract UserWorkflowResult<String> doActivate(User user, String token, String updater, String context);

    @Override
    public UserWorkflowResult<String> activate(
            final String key, final String token, final String updater, final String context) {

        return doActivate(userDAO.authFind(key), token, updater, context);
    }

    protected abstract UserWorkflowResult<Pair<UserUR, Boolean>> doUpdate(
            User user, UserUR userUR, String updater, String context);

    @Override
    public UserWorkflowResult<Pair<UserUR, Boolean>> update(
            final UserUR userUR, final String updater, final String context) {

        UserWorkflowResult<Pair<UserUR, Boolean>> result = doUpdate(
                userDAO.authFind(userUR.getKey()), userUR, updater, context);

        User user = userDAO.find(userUR.getKey());
        if (!AuthContextUtils.getUsername().equals(user.getUsername())) {
            // ensure that requester's administration rights are still valid
            Set<String> authRealms = new HashSet<>();
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(IdRepoEntitlement.USER_READ, Set.of()));
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(IdRepoEntitlement.USER_UPDATE, Set.of()));
            userDAO.securityChecks(
                    authRealms,
                    user.getKey(),
                    user.getRealm().getFullPath(),
                    userDAO.findAllGroupKeys(user));
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doSuspend(User user, String updater, String context);

    @Override
    public UserWorkflowResult<String> suspend(final String key, final String updater, final String context) {
        User user = userDAO.authFind(key);

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user, updater, context);
    }

    @Override
    public Pair<UserWorkflowResult<String>, Boolean> internalSuspend(
            final String key, final String updater, final String context) {

        User user = userDAO.authFind(key);

        Pair<UserWorkflowResult<String>, Boolean> result = null;

        Pair<Boolean, Boolean> enforce = userDAO.enforcePolicies(user);
        if (enforce.getKey()) {
            LOG.debug("User {} {} is over the max failed logins", user.getKey(), user.getUsername());

            // reduce failed logins number to avoid multiple request       
            user.setFailedLogins(user.getFailedLogins() - 1);

            // set suspended flag
            user.setSuspended(Boolean.TRUE);

            result = Pair.of(doSuspend(user, updater, context), enforce.getValue());
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doReactivate(User user, String updater, String context);

    @Override
    public UserWorkflowResult<String> reactivate(final String key, final String updater, final String context) {
        User user = userDAO.authFind(key);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user, updater, context);
    }

    protected abstract void doRequestPasswordReset(User user, String updater, String context);

    @Override
    public void requestPasswordReset(final String key, final String updater, final String context) {
        doRequestPasswordReset(userDAO.authFind(key), updater, context);
    }

    protected abstract UserWorkflowResult<Pair<UserUR, Boolean>> doConfirmPasswordReset(
            User user, String token, String password, String updater, String context);

    @Override
    public UserWorkflowResult<Pair<UserUR, Boolean>> confirmPasswordReset(
            final String key, final String token, final String password, final String updater, final String context) {

        return doConfirmPasswordReset(userDAO.authFind(key), token, password, updater, context);
    }

    protected abstract void doDelete(User user);

    @Override
    public void delete(final String userKey) {
        doDelete(userDAO.authFind(userKey));
    }
}
