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
package org.apache.syncope.core.workflow.user;

import java.util.Map;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowResult;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface UserWorkflowAdapter extends WorkflowAdapter {

    /**
     * Create an user.
     *
     * @param userTO user to be created and wether to propagate it as active
     * @return user just created
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO) throws UnauthorizedRoleException, WorkflowException;

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and wether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @return user just created
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO, boolean disablePwdPolicyCheck)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and wether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param enabled specify true/false to force active/supended status
     * @return user just created
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO, boolean disablePwdPolicyCheck, final Boolean enabled)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Execute a task on an user.
     *
     * @param userTO user to be subject to task
     * @param taskId to be executed
     * @return user just updated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> execute(UserTO userTO, String taskId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    /**
     * Activate an user.
     *
     * @param userId user to be activated
     * @param token to be verified for activation
     * @return user just updated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> activate(Long userId, String token)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    /**
     * Update an user.
     *
     * @param userMod modification set to be performed
     * @return user just updated and propagations to be performed
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> update(UserMod userMod)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    /**
     * Suspend an user.
     *
     * @param userId user to be suspended
     * @return user just suspended
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> suspend(Long userId) throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    /**
     * Suspend an user.
     *
     * @param user to be suspended
     * @return user just suspended
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> suspend(SyncopeUser user) throws UnauthorizedRoleException, WorkflowException;

    /**
     * Reactivate an user.
     *
     * @param userId user to be reactivated
     * @return user just reactivated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> reactivate(Long userId) throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    /**
     * Delete an user.
     *
     * @param userId user to be deleted
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    void delete(Long userId) throws UnauthorizedRoleException, NotFoundException, WorkflowException;
}
