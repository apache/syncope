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
package org.apache.syncope.core.workflow.api;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.user.User;

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
     */
    WorkflowResult<Pair<String, Boolean>> create(UserTO userTO, boolean storePassword);

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param storePassword whether password shall be stored into the internal storage
     * @return user just created
     */
    WorkflowResult<Pair<String, Boolean>> create(
            UserTO userTO, boolean disablePwdPolicyCheck, boolean storePassword);

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param enabled specify true/false to force active/supended status
     * @param storePassword whether password shall be stored into the internal storage
     * @return user just created
     */
    WorkflowResult<Pair<String, Boolean>> create(
            UserTO userTO, boolean disablePwdPolicyCheck, final Boolean enabled, boolean storePassword);

    /**
     * Request certification for the given user.
     * 
     * @param user user to be subject to recertification
     * @return user just updated
     */
    WorkflowResult<String> requestCertify(User user);

    /**
     * Execute a task on an user.
     *
     * @param userTO user to be subject to task
     * @param taskId to be executed
     * @return user just updated
     */
    WorkflowResult<String> execute(UserTO userTO, String taskId);

    /**
     * Activate an user.
     *
     * @param userKey user to be activated
     * @param token to be verified for activation
     * @return user just updated
     */
    WorkflowResult<String> activate(String userKey, String token);

    /**
     * Update an user.
     *
     * @param userPatch modification set to be performed
     * @return user just updated and propagations to be performed
     */
    WorkflowResult<Pair<UserPatch, Boolean>> update(UserPatch userPatch);

    /**
     * Suspend an user.
     *
     * @param key to be suspended
     * @return user just suspended
     */
    WorkflowResult<String> suspend(String key);

    /**
     * Suspend an user (used by internal authentication process)
     *
     * @param key to be suspended
     * @return user just suspended and information whether to propagate suspension
     */
    Pair<WorkflowResult<String>, Boolean> internalSuspend(String key);

    /**
     * Reactivate an user.
     *
     * @param userKey user to be reactivated
     * @return user just reactivated
     */
    WorkflowResult<String> reactivate(String userKey);

    /**
     * Request password reset for an user.
     *
     * @param userKey user requesting password reset
     */
    void requestPasswordReset(String userKey);

    /**
     * Confirm password reset for an user.
     *
     * @param userKey user confirming password reset
     * @param token security token
     * @param password new password value
     * @return user just updated and propagations to be performed
     */
    WorkflowResult<Pair<UserPatch, Boolean>> confirmPasswordReset(String userKey, String token, String password);

    /**
     * Delete an user.
     *
     * @param userKey user to be deleted
     */
    void delete(String userKey);
}
