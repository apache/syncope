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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.EncryptorFactory;
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

    public static String encrypt(final String clear) {
        byte[] encryptedBytes = EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(clear.getBytes());
        return Base64.encode(encryptedBytes);
    }

    public static String decrypt(final String crypted) {
        byte[] decryptedBytes = EncryptorFactory.getInstance().getDefaultEncryptor().decrypt(Base64.decode(crypted));
        return new String(decryptedBytes);
    }

    @Override
    public String getPrefix() {
        return null;
    }

    protected abstract WorkflowResult<Long> doActivate(User user, String token);

    @Override
    public WorkflowResult<Long> activate(final Long key, final String token) {
        return doActivate(userDAO.authFind(key), token);
    }

    protected abstract WorkflowResult<Pair<UserPatch, Boolean>> doUpdate(User user, UserPatch userPatch);

    @Override
    public WorkflowResult<Pair<UserPatch, Boolean>> update(final UserPatch userPatch) {
        return doUpdate(userDAO.authFind(userPatch.getKey()), userPatch);
    }

    protected abstract WorkflowResult<Long> doSuspend(User user);

    @Override
    public WorkflowResult<Long> suspend(final Long key) {
        User user = userDAO.authFind(key);

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    @Override
    public Pair<WorkflowResult<Long>, Boolean> internalSuspend(final Long key) {
        User user = userDAO.authFind(key);

        Pair<WorkflowResult<Long>, Boolean> result = null;

        Pair<Boolean, Boolean> enforce = userDAO.enforcePolicies(user);
        if (enforce.getKey()) {
            LOG.debug("User {} {} is over the max failed logins", user.getKey(), user.getUsername());

            // reduce failed logins number to avoid multiple request       
            user.setFailedLogins(user.getFailedLogins() - 1);

            // set suspended flag
            user.setSuspended(Boolean.TRUE);

            result = ImmutablePair.of(doSuspend(user), enforce.getValue());
        }

        return result;
    }

    protected abstract WorkflowResult<Long> doReactivate(User user);

    @Override
    public WorkflowResult<Long> reactivate(final Long key) {
        User user = userDAO.authFind(key);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doRequestPasswordReset(User user);

    @Override
    public void requestPasswordReset(final Long key) {
        doRequestPasswordReset(userDAO.authFind(key));
    }

    protected abstract WorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            User user, String token, String password);

    @Override
    public WorkflowResult<Pair<UserPatch, Boolean>> confirmPasswordReset(
            final Long key, final String token, final String password) {

        return doConfirmPasswordReset(userDAO.authFind(key), token, password);
    }

    protected abstract void doDelete(User user);

    @Override
    public void delete(final Long key) {
        doDelete(userDAO.authFind(key));
    }
}
