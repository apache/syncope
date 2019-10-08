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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class DefaultUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    @Autowired
    private ConfParamOps confParamOps;

    @Override
    protected UserWorkflowResult<Pair<String, Boolean>> doCreate(
            final UserCR userCR,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled) {

        User user = entityFactory.newEntity(User.class);
        dataBinder.create(user, userCR);

        // this will make UserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        String status;
        boolean propagateEnable;
        if (enabled == null) {
            status = "created";
            propagateEnable = true;
        } else {
            status = enabled
                    ? "active"
                    : "suspended";
            propagateEnable = enabled;
            user.setSuspended(!enabled);
        }

        user.setStatus(status);
        user = userDAO.save(user);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.CREATE, userDAO.findAllResourceKeys(user.getKey()));

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        user.getLinkedAccounts().forEach(account -> propByLinkedAccount.add(
                ResourceOperation.CREATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        return new UserWorkflowResult<>(
                Pair.of(user.getKey(), propagateEnable),
                propByRes,
                propByLinkedAccount,
                "create");
    }

    @Override
    protected UserWorkflowResult<String> doActivate(final User user, final String token) {
        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setStatus("active");
        User updated = userDAO.save(user);

        return new UserWorkflowResult<>(updated.getKey(), null, null, "activate");
    }

    @Override
    protected UserWorkflowResult<Pair<UserUR, Boolean>> doUpdate(final User user, final UserUR userUR) {
        Pair<PropagationByResource<String>, PropagationByResource<Pair<String, String>>> propInfo =
                dataBinder.update(user, userUR);
        return new UserWorkflowResult<>(
                Pair.of(userUR, !user.isSuspended()),
                propInfo.getLeft(),
                propInfo.getRight(),
                "update");
    }

    @Override
    protected UserWorkflowResult<String> doSuspend(final User user) {
        user.setStatus("suspended");
        User updated = userDAO.save(user);

        return new UserWorkflowResult<>(updated.getKey(), null, null, "suspend");
    }

    @Override
    protected UserWorkflowResult<String> doReactivate(final User user) {
        user.setStatus("active");
        User updated = userDAO.save(user);

        return new UserWorkflowResult<>(updated.getKey(), null, null, "reactivate");
    }

    @Override
    protected void doRequestPasswordReset(final User user) {
        user.generateToken(
                confParamOps.get(AuthContextUtils.getDomain(), "token.length", 256, Integer.class),
                confParamOps.get(AuthContextUtils.getDomain(), "token.expireTime", 60, Integer.class));
        userDAO.save(user);
    }

    @Override
    protected UserWorkflowResult<Pair<UserUR, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password) {

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();

        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.setPassword(new PasswordPatch.Builder().
                onSyncope(true).
                resources(userDAO.findAllResourceKeys(user.getKey())).
                value(password).build());

        return doUpdate(user, userUR);
    }

    @Override
    protected void doDelete(final User user) {
        userDAO.delete(user);
    }
}
