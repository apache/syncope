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

import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.entity.EntityFactory;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.provisioning.api.WorkflowResult;
import org.apache.syncope.server.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.server.workflow.api.RoleWorkflowAdapter;
import org.apache.syncope.server.workflow.api.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractRoleWorkflowAdapter implements RoleWorkflowAdapter {

    @Autowired
    protected RoleDataBinder dataBinder;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Override
    public String getPrefix() {
        return null;
    }

    protected abstract WorkflowResult<Long> doUpdate(Role role, RoleMod roleMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Long> update(final RoleMod roleMod)
            throws NotFoundException, WorkflowException {

        return doUpdate(roleDAO.authFetch(roleMod.getKey()), roleMod);
    }

    protected abstract void doDelete(Role role) throws WorkflowException;

    @Override
    public void delete(final Long roleKey) throws NotFoundException, WorkflowException {
        doDelete(roleDAO.authFetch(roleKey));
    }
}
