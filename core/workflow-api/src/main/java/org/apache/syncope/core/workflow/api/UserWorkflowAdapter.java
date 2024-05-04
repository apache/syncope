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
     * @param creator username that requested this operation
     * @param context context information
     * @return user just created
     */
    UserWorkflowResult<Pair<String, Boolean>> create(UserCR userCR, String creator, String context);

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userCR user to be created and whether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @param enabled specify true/false to force active/supended status
     * @param creator username that requested this operation
     * @param context context information
     * @return user just created
     */
    UserWorkflowResult<Pair<String, Boolean>> create(
            UserCR userCR, boolean disablePwdPolicyCheck, Boolean enabled, String creator, String context);

    /**
     * Activate an user.
     *
     * @param userKey user to be activated
     * @param token to be verified for activation
     * @param updater username that requested this operation
     * @param context context information
     * @return user just updated
     */
    UserWorkflowResult<String> activate(String userKey, String token, String updater, String context);

    /**
     * Update an user.
     *
     * @param userUR modification set to be performed
     * @param enabled whether status shall be changed or not
     * @param updater username that requested this operation
     * @param context context information
     * @return user just updated and propagations to be performed
     */
    UserWorkflowResult<Pair<UserUR, Boolean>> update(UserUR userUR, Boolean enabled, String updater, String context);

    /**
     * Suspend an user.
     *
     * @param key to be suspended
     * @param updater username that requested this operation
     * @param context context information
     * @return user just suspended
     */
    UserWorkflowResult<String> suspend(String key, String updater, String context);

    /**
     * Suspend an user (used by internal authentication process)
     *
     * @param key to be suspended
     * @param updater username that requested this operation
     * @param context context information
     * @return user just suspended and information whether to propagate suspension
     */
    Pair<UserWorkflowResult<String>, Boolean> internalSuspend(String key, String updater, String context);

    /**
     * Reactivate an user.
     *
     * @param userKey user to be reactivated
     * @param updater username that requested this operation
     * @param context context information
     * @return user just reactivated
     */
    UserWorkflowResult<String> reactivate(String userKey, String updater, String context);

    /**
     * Request password reset for an user.
     *
     * @param userKey user requesting password reset
     * @param updater username that requested this operation
     * @param context context information
     */
    void requestPasswordReset(String userKey, String updater, String context);

    /**
     * Confirm password reset for an user.
     *
     * @param userKey user confirming password reset
     * @param token security token
     * @param password new password value
     * @param updater username that requested this operation
     * @param context context information
     * @return user just updated and propagations to be performed
     */
    UserWorkflowResult<Pair<UserUR, Boolean>> confirmPasswordReset(
            String userKey, String token, String password, String updater, String context);

    /**
     * Delete an user.
     *
     * @param userKey user to be deleted
     * @param eraser username that requested this operation
     * @param context context information
     */
    void delete(String userKey, String eraser, String context);
}
