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
package org.apache.syncope.core.flowable.api;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.event.TransactionalEventListener;

public interface UserRequestHandler {

    /**
     * Get the running user requests matching the provided parameters.
     *
     * @param userKey user key (optional)
     * @param pageable paging information
     * @return total number of user requests, list of user requests matching the provided parameters
     */
    Page<UserRequest> getUserRequests(String userKey, Pageable pageable);

    /**
     * Starts a new user request, for the given BPMN process and user.
     *
     * @param bpmnProcess BPMN process
     * @param user user
     * @param inputVariables variables
     * @return data about the started request service, including execution id
     */
    UserRequest start(String bpmnProcess, User user, WorkflowTaskExecInput inputVariables);

    /**
     * Parses the given execution id to find matching user request and owner.
     *
     * @param executionId execution id
     * @return matching user request and owner
     */
    Pair<ProcessInstance, String> parse(String executionId);

    /**
     * Cancel a running user request.
     *
     * @param procInst process instance for user request
     * @param reason reason to cancel the user request
     */
    void cancel(ProcessInstance procInst, String reason);

    /**
     * Cancel all running user requests for the given process definition id.
     *
     * @param processDefinitionId process definition id
     */
    void cancelByProcessDefinition(String processDefinitionId);

    /**
     * Cancel all running user requests for the user in the given delete event.
     *
     * @param event delete event
     */
    @TransactionalEventListener
    void cancelByUser(EntityLifecycleEvent<Entity> event);

    /**
     * Get the form matching the provided task id.
     *
     * @param userKey user key
     * @param taskId task id
     * @return the form for the given task id
     */
    UserRequestForm getForm(String userKey, String taskId);

    /**
     * Get the forms matching the provided parameters.
     *
     * @param userKey user key (optional)
     * @param pageable paging information
     * @return total number of forms, list of forms matching the provided parameters
     */
    Page<UserRequestForm> getForms(String userKey, Pageable pageable);

    /**
     * Claim a form for a given object.
     *
     * @param taskId Workflow task to which the form is associated
     * @return updated form
     */
    UserRequestForm claimForm(String taskId);

    /**
     * Unclaim a form for a given object.
     *
     * @param taskId Workflow task to which the form is associated
     * @return updated form
     */
    UserRequestForm unclaimForm(String taskId);

    /**
     * Submit a form.
     *
     * @param form to be submitted
     * @return user updated by this form submit
     */
    UserWorkflowResult<UserUR> submitForm(UserRequestForm form);
}
