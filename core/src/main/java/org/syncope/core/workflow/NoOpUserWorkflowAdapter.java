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
package org.syncope.core.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.propagation.PropagationByResource;
import org.syncope.core.rest.controller.UnauthorizedRoleException;
import org.syncope.types.PropagationOperation;

/**
 * Simple implementation basically not involving any workflow engine.
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class NoOpUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    private static final List<String> TASKS =
            Arrays.asList(
            new String[]{
                "create", "activate", "update",
                "suspend", "reactivate", "delete"});

    public static final String ENABLED = "enabled";

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(
            final UserTO userTO,
            final boolean disablePwdPolicyCheck)
            throws WorkflowException {
        return create(userTO, disablePwdPolicyCheck, null);
    }

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(
            final UserTO userTO,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled)
            throws WorkflowException {

        SyncopeUser user = new SyncopeUser();
        dataBinder.create(user, userTO);

        // this will make SyncopeUserValidator not to consider
        // password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        String status;
        boolean propagate_enable;

        if (enabled == null) {
            status = "created";
            propagate_enable = true;
        } else {
            status = enabled ? "active" : "suspended";
            propagate_enable = enabled;
        }

        user.setStatus(status);
        user = userDAO.save(user);

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.CREATE, user.getResourceNames());

        return new WorkflowResult<Map.Entry<Long, Boolean>>(
                new DefaultMapEntry(user.getId(), propagate_enable),
                propByRes, "create");
    }

    @Override
    protected WorkflowResult<Long> doActivate(final SyncopeUser user,
            final String token)
            throws WorkflowException {

        if (!user.checkToken(token)) {
            throw new WorkflowException(
                    new RuntimeException("Wrong token: " + token));
        }

        user.removeToken();
        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "activate");
    }

    @Override
    protected WorkflowResult<Long> doUpdate(
            final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        PropagationByResource propByRes = dataBinder.update(user, userMod);

        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(
                updated.getId(), propByRes, "update");
    }

    @Override
    protected WorkflowResult<Long> doSuspend(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("suspended");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "suspend");
    }

    @Override
    protected WorkflowResult<Long> doReactivate(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "reactivate");
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        userDAO.delete(user);
    }

    @Override
    public WorkflowResult<Long> execute(final UserTO userTO,
            final String taskId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        return new WorkflowDefinitionTO();
    }

    @Override
    public void updateDefinition(final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<String> getDefinedTasks()
            throws WorkflowException {

        return TASKS;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId)
            throws NotFoundException, WorkflowException {

        return null;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<Map.Entry<Long, String>> submitForm(
            final WorkflowFormTO form, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }
}
