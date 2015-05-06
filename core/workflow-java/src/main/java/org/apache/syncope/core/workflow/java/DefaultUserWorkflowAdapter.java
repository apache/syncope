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
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple implementation basically not involving any workflow engine.
 */
@Transactional(rollbackFor = { Throwable.class })
public class DefaultUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    @Autowired
    private ConfDAO confDAO;

    @Override
    public WorkflowResult<Pair<Long, Boolean>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, false, true);
    }

    @Override
    public WorkflowResult<Pair<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final boolean storePassword) {

        return create(userTO, disablePwdPolicyCheck, null, storePassword);
    }

    @Override
    public WorkflowResult<Pair<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
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

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, user.getResourceNames());

        return new WorkflowResult<Pair<Long, Boolean>>(
                new ImmutablePair<>(user.getKey(), propagateEnable), propByRes, "create");
    }

    @Override
    protected WorkflowResult<Long> doActivate(final User user, final String token) {
        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setStatus("active");
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, "activate");
    }

    @Override
    protected WorkflowResult<Pair<UserMod, Boolean>> doUpdate(final User user, final UserMod userMod) {
        // update password internally only if required
        UserMod actualMod = SerializationUtils.clone(userMod);
        if (actualMod.getPwdPropRequest() != null && !actualMod.getPwdPropRequest().isOnSyncope()) {
            actualMod.setPassword(null);
        }
        // update User
        PropagationByResource propByRes = dataBinder.update(user, actualMod);

        User updated = userDAO.save(user);

        userMod.setKey(updated.getKey());
        return new WorkflowResult<Pair<UserMod, Boolean>>(
                new ImmutablePair<>(userMod, !user.isSuspended()), propByRes, "update");
    }

    @Override
    protected WorkflowResult<Long> doSuspend(final User user) {
        user.setStatus("suspended");
        User updated = userDAO.save(user);

        return new WorkflowResult<>(updated.getKey(), null, "suspend");
    }

    @Override
    protected WorkflowResult<Long> doReactivate(final User user) {
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
    protected void doConfirmPasswordReset(final User user, final String token, final String password) {
        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setPassword(password, user.getCipherAlgorithm());
        userDAO.save(user);
    }

    @Override
    protected void doDelete(final User user) {
        userDAO.delete(user);
    }

    @Override
    public WorkflowResult<Long> execute(final UserTO userTO, final String taskId) {
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
    public List<WorkflowFormTO> getForms(final String workflowId, final String name) {
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
    public WorkflowResult<UserMod> submitForm(final WorkflowFormTO form) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

}
