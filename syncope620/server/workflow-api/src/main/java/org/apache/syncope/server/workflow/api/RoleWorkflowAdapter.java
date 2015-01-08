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
package org.apache.syncope.server.workflow.api;

import org.apache.syncope.provisioning.api.WorkflowResult;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.persistence.api.dao.NotFoundException;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface RoleWorkflowAdapter extends WorkflowAdapter {

    /**
     * Create a role.
     *
     * @param roleTO role to be created and wether to propagate it as active
     * @return role just created
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> create(RoleTO roleTO) throws WorkflowException;

    /**
     * Execute a task on a role.
     *
     * @param roleTO role to be subject to task
     * @param taskId to be executed
     * @return role just updated
     * @throws NotFoundException role not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> execute(RoleTO roleTO, String taskId)
            throws NotFoundException, WorkflowException;

    /**
     * Update a role.
     *
     * @param roleMod modification set to be performed
     * @return role just updated and propagations to be performed
     * @throws NotFoundException role not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> update(RoleMod roleMod)
            throws NotFoundException, WorkflowException;

    /**
     * Delete a role.
     *
     * @param roleKey role to be deleted
     * @throws NotFoundException role not found exception
     * @throws WorkflowException workflow exception
     */
    void delete(Long roleKey) throws NotFoundException, WorkflowException;
}
