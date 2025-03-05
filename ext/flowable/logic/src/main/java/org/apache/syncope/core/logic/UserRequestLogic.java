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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class UserRequestLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final BpmnProcessManager bpmnProcessManager;

    protected final UserRequestHandler userRequestHandler;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    protected final UserDataBinder binder;

    protected final UserDAO userDAO;

    public UserRequestLogic(
            final BpmnProcessManager bpmnProcessManager,
            final UserRequestHandler userRequestHandler,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final UserDataBinder binder,
            final UserDAO userDAO) {

        this.bpmnProcessManager = bpmnProcessManager;
        this.userRequestHandler = userRequestHandler;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
        this.binder = binder;
        this.userDAO = userDAO;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<UserRequest> listRequests(final String userKey, final Pageable pageable) {
        if (userKey == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_LIST,
                    "Listing user requests not allowed");
        } else {
            User user = userDAO.findById(userKey).
                    orElseThrow(() -> new NotFoundException("User " + userKey));

            securityChecks(user.getUsername(),
                    FlowableEntitlement.USER_REQUEST_LIST,
                    "Listing requests for user" + user.getUsername() + " not allowed");
        }

        return userRequestHandler.getUserRequests(userKey, pageable);
    }

    protected UserRequest doStart(
            final String bpmnProcess,
            final User user,
            final WorkflowTaskExecInput inputVariables) {

        // check if BPMN process exists
        bpmnProcessManager.exportProcess(bpmnProcess, BpmnProcessFormat.XML, NullOutputStream.INSTANCE);

        return userRequestHandler.start(bpmnProcess, user, inputVariables);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequest startRequest(final String bpmnProcess, final WorkflowTaskExecInput inputVariables) {
        return doStart(
                bpmnProcess,
                userDAO.findByUsername(AuthContextUtils.getUsername()).
                        orElseThrow(() -> new NotFoundException("Authenticated user")),
                inputVariables);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.USER_REQUEST_START + "')")
    public UserRequest startRequest(
            final String bpmnProcess,
            final String userKey,
            final WorkflowTaskExecInput inputVariables) {
        return doStart(bpmnProcess, userDAO.authFind(userKey), inputVariables);
    }

    protected static void securityChecks(final String username, final String entitlement, final String errorMessage) {
        if (!AuthContextUtils.getUsername().equals(username)
                && AuthContextUtils.getAuthorities().stream().
            noneMatch(auth -> entitlement.equals(auth.getAuthority()))) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add(errorMessage);
            throw sce;
        }
    }

    @PreAuthorize("isAuthenticated()")
    public void cancelRequest(final String executionId, final String reason) {
        Pair<ProcessInstance, String> parsed = userRequestHandler.parse(executionId);

        securityChecks(
                userDAO.findUsername(parsed.getRight()).
                        orElseThrow(() -> new NotFoundException("User " + parsed.getRight())),
                FlowableEntitlement.USER_REQUEST_CANCEL,
                "Canceling " + executionId + " not allowed");

        userRequestHandler.cancel(parsed.getLeft(), reason);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestForm claimForm(final String taskId) {
        UserRequestForm form = userRequestHandler.claimForm(taskId);
        securityChecks(form.getUsername(),
                FlowableEntitlement.USER_REQUEST_FORM_CLAIM,
                "Claiming form " + taskId + " not allowed");
        return form;
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestForm unclaimForm(final String taskId) {
        UserRequestForm form = userRequestHandler.unclaimForm(taskId);
        securityChecks(form.getUsername(),
                FlowableEntitlement.USER_REQUEST_FORM_UNCLAIM,
                "Unclaiming form " + taskId + " not allowed");
        return form;
    }

    protected void evaluateKey(final String userKey) {
        if (userKey == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_FORM_LIST,
                    "Listing forms not allowed");
        } else {
            User user = userDAO.findById(userKey).
                    orElseThrow(() -> new NotFoundException("User " + userKey));

            securityChecks(user.getUsername(),
                    FlowableEntitlement.USER_REQUEST_FORM_LIST,
                    "Listing forms for user" + user.getUsername() + " not allowed");
        }
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestForm getForm(final String userKey, final String taskId) {
        evaluateKey(userKey);

        return userRequestHandler.getForm(userKey, taskId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<UserRequestForm> listForms(final String userKey, final Pageable pageable) {
        evaluateKey(userKey);

        return userRequestHandler.getForms(userKey, pageable);
    }

    @PreAuthorize("isAuthenticated()")
    public ProvisioningResult<UserTO> submitForm(final UserRequestForm form, final boolean nullPriorityAsync) {
        if (form.getUsername() == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_FORM_SUBMIT,
                    "Submitting forms not allowed");
        } else {
            securityChecks(form.getUsername(),
                    FlowableEntitlement.USER_REQUEST_FORM_SUBMIT,
                    "Submitting forms for user" + form.getUsername() + " not allowed");
        }

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();

        UserWorkflowResult<UserUR> wfResult = userRequestHandler.submitForm(form);

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (wfResult.getPropByRes() != null && !wfResult.getPropByRes().isEmpty()) {
            List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(
                    new UserWorkflowResult<>(
                            Pair.of(wfResult.getResult(), Boolean.TRUE),
                            wfResult.getPropByRes(),
                            wfResult.getPropByLinkedAccount(),
                            wfResult.getPerformedTasks()));

            PropagationReporter propagationReporter = taskExecutor.execute(
                    taskInfos, nullPriorityAsync, AuthContextUtils.getUsername());
            result.getPropagationStatuses().addAll(propagationReporter.getStatuses());
        }

        UserTO userTO;
        if (userDAO.findById(wfResult.getResult().getKey()).isEmpty()) {
            userTO = new UserTO();
            userTO.setKey(wfResult.getResult().getKey());
        } else {
            userTO = binder.getUserTO(wfResult.getResult().getKey());
        }
        result.setEntity(userTO);

        return result;
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
