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
package org.apache.syncope.server.workflow.java;

import java.util.Map;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.entity.EntityFactory;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.provisioning.api.WorkflowResult;
import org.apache.syncope.server.provisioning.api.data.UserDataBinder;
import org.apache.syncope.server.workflow.api.UserWorkflowAdapter;
import org.apache.syncope.server.workflow.api.WorkflowException;
import org.apache.syncope.server.workflow.api.WorkflowInstanceLoader;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.EncryptorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractUserWorkflowAdapter implements UserWorkflowAdapter {

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
    public Class<? extends WorkflowInstanceLoader> getLoaderClass() {
        return null;
    }

    protected abstract WorkflowResult<Long> doActivate(User user, String token) throws WorkflowException;

    @Override
    public WorkflowResult<Long> activate(final Long userKey, final String token)
            throws WorkflowException {

        return doActivate(userDAO.authFetch(userKey), token);
    }

    protected abstract WorkflowResult<Map.Entry<UserMod, Boolean>> doUpdate(User user, UserMod userMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Map.Entry<UserMod, Boolean>> update(final UserMod userMod)
            throws WorkflowException {

        return doUpdate(userDAO.authFetch(userMod.getKey()), userMod);
    }

    protected abstract WorkflowResult<Long> doSuspend(User user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> suspend(final Long userKey)
            throws WorkflowException {

        return suspend(userDAO.authFetch(userKey));
    }

    @Override
    public WorkflowResult<Long> suspend(final User user) throws WorkflowException {
        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    protected abstract WorkflowResult<Long> doReactivate(User user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> reactivate(final Long userKey) throws WorkflowException {
        final User user = userDAO.authFetch(userKey);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doRequestPasswordReset(User user) throws WorkflowException;

    @Override
    public void requestPasswordReset(final Long userKey) throws WorkflowException {
        doRequestPasswordReset(userDAO.authFetch(userKey));
    }

    protected abstract void doConfirmPasswordReset(User user, String token, String password)
            throws WorkflowException;

    @Override
    public void confirmPasswordReset(final Long userKey, final String token, final String password)
            throws WorkflowException {

        doConfirmPasswordReset(userDAO.authFetch(userKey), token, password);
    }

    protected abstract void doDelete(User user) throws WorkflowException;

    @Override
    public void delete(final Long userKey) throws WorkflowException {
        doDelete(userDAO.authFetch(userKey));
    }
}
