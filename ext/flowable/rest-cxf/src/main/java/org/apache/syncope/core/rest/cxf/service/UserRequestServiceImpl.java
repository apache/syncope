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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.core.logic.UserRequestLogic;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRequestServiceImpl extends AbstractServiceImpl implements UserRequestService {

    @Autowired
    private UserRequestLogic logic;

    @Autowired
    private UserDAO userDAO;

    @Override
    public PagedResult<UserRequest> list(final UserRequestQuery query) {
        if (query.getUser() != null) {
            query.setUser(getActualKey(userDAO, query.getUser()));
        }

        Pair<Integer, List<UserRequest>> result = logic.list(
                query.getUser(), query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public UserRequest start(final String bpmnProcess, final String user, final WorkflowTaskExecInput inputVariables) {
        return user == null
                ? logic.start(bpmnProcess, inputVariables)
                : logic.start(bpmnProcess, getActualKey(userDAO, user), inputVariables);
    }

    @Override
    public void cancel(final String executionId, final String reason) {
        logic.cancel(executionId, reason);
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
    public PagedResult<UserRequestForm> getForms(final UserRequestFormQuery query) {
        if (query.getUser() != null) {
            query.setUser(getActualKey(userDAO, query.getUser()));
        }

        Pair<Integer, List<UserRequestForm>> result = logic.getForms(
                query.getUser(), query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public UserTO submitForm(final UserRequestForm form) {
        return logic.submitForm(form);
    }
}
