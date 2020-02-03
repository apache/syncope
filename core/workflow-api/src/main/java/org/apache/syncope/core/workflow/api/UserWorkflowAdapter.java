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
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface UserWorkflowAdapter extends WorkflowAdapter {

    /**
     * Create an user.
     *
     * @param userCR user to be created and whether to propagate it as active
     * @return user just created
     */
    UserWorkflowResult<Pair<String, Boolean>> create(UserCR userCR);

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userCR user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param enabled specify true/false to force active/supended status
     * @return user just created
     */
    UserWorkflowResult<Pair<String, Boolean>> create(UserCR userCR, boolean disablePwdPolicyCheck, Boolean enabled);

    /**
     * Activate an user.
     *
     * @param userKey user to be activated
     * @param token to be verified for activation
     * @return user just updated
     */
    UserWorkflowResult<String> activate(String userKey, String token);

    /**
     * Update an user.
     *
     * @param userUR modification set to be performed
     * @return user just updated and propagations to be performed
     */
    UserWorkflowResult<Pair<UserUR, Boolean>> update(UserUR userUR);

    /**
     * Suspend an user.
     *
     * @param key to be suspended
     * @return user just suspended
     */
    UserWorkflowResult<String> suspend(String key);

    /**
     * Suspend an user (used by internal authentication process)
     *
     * @param key to be suspended
     * @return user just suspended and information whether to propagate suspension
     */
    Pair<UserWorkflowResult<String>, Boolean> internalSuspend(String key);

    /**
     * Reactivate an user.
     *
     * @param userKey user to be reactivated
     * @return user just reactivated
     */
    UserWorkflowResult<String> reactivate(String userKey);

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
    UserWorkflowResult<Pair<UserUR, Boolean>> confirmPasswordReset(String userKey, String token, String password);

    /**
     * Delete an user.
     *
     * @param userKey user to be deleted
     */
    void delete(String userKey);
}
