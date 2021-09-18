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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.core.logic.UserRequestLogic;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.stereotype.Service;

@Service
public class UserRequestServiceImpl extends AbstractService implements UserRequestService {

    protected final UserRequestLogic logic;

    protected final UserDAO userDAO;

    public UserRequestServiceImpl(final UserRequestLogic logic, final UserDAO userDAO) {
        this.logic = logic;
        this.userDAO = userDAO;
    }

    @Override
    public PagedResult<UserRequest> listRequests(final UserRequestQuery query) {
        if (query.getUser() != null) {
            query.setUser(Optional.ofNullable(getActualKey(userDAO, query.getUser())).orElse(query.getUser()));
        }

        Pair<Integer, List<UserRequest>> result = logic.listRequests(
                query.getUser(), query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public UserRequest startRequest(
            final String bpmnProcess, final String user, final WorkflowTaskExecInput inputVariables) {

        return user == null
                ? logic.startRequest(bpmnProcess, inputVariables)
                : logic.startRequest(bpmnProcess, getActualKey(userDAO, user), inputVariables);
    }

    @Override
    public void cancelRequest(final String executionId, final String reason) {
        logic.cancelRequest(executionId, reason);
    }

    @Override
    public UserRequestForm claimForm(final String taskId) {
        return logic.claimForm(taskId);
    }

    @Override
    public UserRequestForm unclaimForm(final String taskId) {
        return logic.unclaimForm(taskId);
    }

    @Override
    public UserRequestForm getForm(final String username, final String taskId) {
        return logic.getForm(getActualKey(userDAO, username), taskId);
    }

    @Override
    public PagedResult<UserRequestForm> listForms(final UserRequestQuery query) {
        if (query.getUser() != null) {
            query.setUser(Optional.ofNullable(getActualKey(userDAO, query.getUser())).orElse(query.getUser()));
        }

        Pair<Integer, List<UserRequestForm>> result = logic.listForms(
                query.getUser(), query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public Response submitForm(final UserRequestForm form) {
        ProvisioningResult<UserTO> submitted = logic.submitForm(form, isNullPriorityAsync());
        return modificationResponse(submitted);
    }
}
