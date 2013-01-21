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
package org.apache.syncope.core.workflow.role;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple implementation basically not involving any workflow engine.
 */
@Transactional(rollbackFor = {Throwable.class})
public class NoOpRoleWorkflowAdapter extends AbstractRoleWorkflowAdapter {

    private static final List<String> TASKS = Arrays.asList(new String[]{"create", "update", "delete"});

    @Override
    public WorkflowResult<Long> create(final RoleTO roleTO)
            throws UnauthorizedRoleException, WorkflowException {

        SyncopeRole role = new SyncopeRole();
        dataBinder.create(role, roleTO);
        role = roleDAO.save(role);

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, role.getResourceNames());

        return new WorkflowResult<Long>(role.getId(), propByRes, "create");
    }

    @Override
    protected WorkflowResult<Long> doUpdate(final SyncopeRole role, final RoleMod roleMod)
            throws WorkflowException {

        PropagationByResource propByRes = dataBinder.update(role, roleMod);

        SyncopeRole updated = roleDAO.save(role);

        return new WorkflowResult<Long>(updated.getId(), propByRes, "update");
    }

    @Override
    protected void doDelete(final SyncopeRole role)
            throws WorkflowException {

        roleDAO.delete(role);
    }

    @Override
    public WorkflowResult<Long> execute(RoleTO roleTO, String taskId) throws UnauthorizedRoleException,
            NotFoundException, WorkflowException {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        return new WorkflowDefinitionTO();
    }

    @Override
    public void updateDefinition(final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<String> getDefinedTasks()
            throws WorkflowException {

        return TASKS;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.emptyList();
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId)
            throws NotFoundException, WorkflowException {

        return null;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<Map.Entry<Long, String>> submitForm(final WorkflowFormTO form, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }
}
