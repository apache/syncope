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

import org.apache.syncope.server.provisioning.api.WorkflowResult;
import java.util.Map;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.server.persistence.api.entity.user.User;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface UserWorkflowAdapter extends WorkflowAdapter {

    /**
     * Create an user.
     *
     * @param userTO user to be created and whether to propagate it as active
     * @param storePassword whether password shall be stored into the internal storage
     * @return user just created
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO, boolean storePassword) throws
            WorkflowException;

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param storePassword whether password shall be stored into the internal storage
     * @return user just created
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO, boolean disablePwdPolicyCheck,
            boolean storePassword) throws WorkflowException;

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param enabled specify true/false to force active/supended status
     * @param storePassword whether password shall be stored into the internal storage
     * @return user just created
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO, boolean disablePwdPolicyCheck, final Boolean enabled,
            boolean storePassword) throws WorkflowException;

    /**
     * Execute a task on an user.
     *
     * @param userTO user to be subject to task
     * @param taskId to be executed
     * @return user just updated
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> execute(UserTO userTO, String taskId) throws WorkflowException;

    /**
     * Activate an user.
     *
     * @param userKey user to be activated
     * @param token to be verified for activation
     * @return user just updated
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> activate(Long userKey, String token) throws WorkflowException;

    /**
     * Update an user.
     *
     * @param userMod modification set to be performed
     * @return user just updated and propagations to be performed
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<UserMod, Boolean>> update(UserMod userMod)
            throws WorkflowException;

    /**
     * Suspend an user.
     *
     * @param userKey user to be suspended
     * @return user just suspended
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> suspend(Long userKey) throws WorkflowException;

    /**
     * Suspend an user.
     *
     * @param user to be suspended
     * @return user just suspended
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> suspend(User user) throws WorkflowException;

    /**
     * Reactivate an user.
     *
     * @param userKey user to be reactivated
     * @return user just reactivated
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> reactivate(Long userKey) throws WorkflowException;

    /**
     * Request password reset for an user.
     *
     * @param userKey user requesting password reset
     * @throws WorkflowException workflow exception
     */
    void requestPasswordReset(Long userKey) throws WorkflowException;

    /**
     * Confirm password reset for an user.
     *
     * @param userKey user confirming password reset
     * @param token security token
     * @param password new password value
     * @throws WorkflowException workflow exception
     */
    void confirmPasswordReset(Long userKey, String token, String password)
            throws WorkflowException;

    /**
     * Delete an user.
     *
     * @param userKey user to be deleted
     * @throws WorkflowException workflow exception
     */
    void delete(Long userKey) throws WorkflowException;
}
