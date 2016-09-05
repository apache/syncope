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

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class DefaultUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    @Autowired
    private ConfDAO confDAO;

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, false, true);
    }

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final boolean storePassword) {

        return create(userTO, disablePwdPolicyCheck, null, storePassword);
    }

    @Override
    public WorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final Boolean enabled, final boolean storePassword) {

        User user = entityFactory.newEntity(User.class);
        dataBinder.create(user, userTO, storePassword);

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

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(
                ResourceOperation.CREATE,
                CollectionUtils.collect(userDAO.findAllResources(user), EntityUtils.keyTransformer()));

        return new WorkflowResult<Pair<String, Boolean>>(
                new ImmutablePair<>(user.getKey(), propagateEnable), propByRes, "create");
    }

    @Override
    protected WorkflowResult<String> doActivate(final User user, final String token) {
        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setStatus("active");
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, "activate");
    }

    @Override
    protected WorkflowResult<Pair<UserPatch, Boolean>> doUpdate(final User user, final UserPatch userPatch) {
        PropagationByResource propByRes = dataBinder.update(user, userPatch);

        userDAO.save(user);

        return new WorkflowResult<Pair<UserPatch, Boolean>>(
                new ImmutablePair<>(userPatch, !user.isSuspended()), propByRes, "update");
    }

    @Override
    protected WorkflowResult<String> doSuspend(final User user) {
        user.setStatus("suspended");
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, "suspend");
    }

    @Override
    protected WorkflowResult<String> doReactivate(final User user) {
        user.setStatus("active");
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, "reactivate");
    }

    @Override
    protected void doRequestPasswordReset(final User user) {
        user.generateToken(
                confDAO.find("token.length", "256").getValues().get(0).getLongValue().intValue(),
                confDAO.find("token.expireTime", "60").getValues().get(0).getLongValue().intValue());
        userDAO.save(user);
    }

    @Override
    protected WorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            final User user, final String token, final String password) {

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(true).
                resources(CollectionUtils.collect(userDAO.findAllResources(user), EntityUtils.keyTransformer())).
                value(password).build());

        return doUpdate(user, userPatch);
    }

    @Override
    protected void doDelete(final User user) {
        userDAO.delete(user);
    }

    @Override
    public WorkflowResult<String> execute(final UserTO userTO, final String taskId) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDefinition(final WorkflowDefinitionFormat format, final OutputStream os) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDiagram(final OutputStream os) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void importDefinition(final WorkflowDefinitionFormat format, final String definition) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.emptyList();
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId) {
        return null;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<UserPatch> submitForm(final WorkflowFormTO form) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<String> requestCertify(final User user) {
        throw new UnsupportedOperationException("Not supported.");
    }

}
