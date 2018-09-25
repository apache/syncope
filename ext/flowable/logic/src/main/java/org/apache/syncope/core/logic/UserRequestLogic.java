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
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.UserRequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRequestLogic extends AbstractTransactionalLogic<UserRequestForm> {

    @Autowired
    private BpmnProcessManager bpmnProcessManager;

    @Autowired
    private UserRequestHandler userRequestHandler;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private UserDataBinder binder;

    @Autowired
    private UserDAO userDAO;

    protected UserRequestTO doStart(final String bpmnProcess, final User user) {
        // check if BPMN process exists
        bpmnProcessManager.exportProcess(bpmnProcess, BpmnProcessFormat.XML, new NullOutputStream());

        return userRequestHandler.start(bpmnProcess, user);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestTO start(final String bpmnProcess) {
        return doStart(bpmnProcess, userDAO.findByUsername(AuthContextUtils.getUsername()));
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.USER_REQUEST_START + "')")
    public UserRequestTO start(final String bpmnProcess, final String userKey) {
        return doStart(bpmnProcess, userDAO.authFind(userKey));
    }

    @PreAuthorize("isAuthenticated()")
    public void cancel(final String executionId, final String reason) {
        Pair<ProcessInstance, String> parsed = userRequestHandler.parse(executionId);

        if (!AuthContextUtils.getUsername().equals(userDAO.find(parsed.getRight()).getUsername())
                && !AuthContextUtils.getAuthorities().stream().
                        anyMatch(auth -> FlowableEntitlement.USER_REQUEST_CANCEL.equals(auth.getAuthority()))) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Canceling " + executionId + " not allowed");
            throw sce;
        }

        userRequestHandler.cancel(parsed.getLeft(), reason);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.WORKFLOW_FORM_CLAIM + "')")
    public UserRequestForm claimForm(final String taskId) {
        return userRequestHandler.claimForm(taskId);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.WORKFLOW_FORM_READ + "') "
            + "and hasRole('" + StandardEntitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public List<UserRequestForm> getForms(final String key) {
        User user = userDAO.authFind(key);
        return userRequestHandler.getForms(user.getKey());
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.WORKFLOW_FORM_LIST + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<UserRequestForm>> getForms(
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        return userRequestHandler.getForms(page, size, orderByClauses);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.WORKFLOW_FORM_SUBMIT + "')")
    public UserTO submitForm(final UserRequestForm form) {
        WorkflowResult<UserPatch> wfResult = userRequestHandler.submitForm(form);

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (wfResult.getPropByRes() != null && !wfResult.getPropByRes().isEmpty()) {
            List<PropagationTaskTO> tasks = propagationManager.getUserUpdateTasks(
                    new WorkflowResult<>(
                            Pair.of(wfResult.getResult(), Boolean.TRUE),
                            wfResult.getPropByRes(),
                            wfResult.getPerformedTasks()));

            taskExecutor.execute(tasks, false);
        }

        UserTO userTO;
        if (userDAO.find(wfResult.getResult().getKey()) == null) {
            userTO = new UserTO();
            userTO.setKey(wfResult.getResult().getKey());
        } else {
            userTO = binder.getUserTO(wfResult.getResult().getKey());
        }
        return userTO;
    }

    @Override
    protected UserRequestForm resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
