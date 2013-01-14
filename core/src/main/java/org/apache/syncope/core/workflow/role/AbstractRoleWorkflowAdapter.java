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

import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowInstanceLoader;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = {Throwable.class})
public abstract class AbstractRoleWorkflowAdapter implements RoleWorkflowAdapter {

    @Autowired
    protected RoleDataBinder dataBinder;

    @Autowired
    protected RoleDAO roleDAO;

    @Override
    public Class<? extends WorkflowInstanceLoader> getLoaderClass() {
        return null;
    }

    protected abstract WorkflowResult<Long> doUpdate(SyncopeRole role, RoleMod roleMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Long> update(final RoleMod roleMod)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        return doUpdate(dataBinder.getRoleFromId(roleMod.getId()), roleMod);
    }

    protected abstract void doDelete(SyncopeRole role) throws WorkflowException;

    @Override
    public void delete(final Long roleId) throws UnauthorizedRoleException, NotFoundException, WorkflowException {
        doDelete(dataBinder.getRoleFromId(roleId));
    }
}
