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
import org.apache.syncope.common.lib.to.UserRequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
import org.apache.syncope.core.logic.UserRequestLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.syncope.common.rest.api.service.UserRequestService;

@Service
public class UserRequestServiceImpl extends AbstractServiceImpl implements UserRequestService {

    @Autowired
    private UserRequestLogic logic;

    @Override
    public UserRequestTO start(final String bpmnProcess, final String userKey) {
        return userKey == null
                ? logic.start(bpmnProcess)
                : logic.start(bpmnProcess, userKey);
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
    public List<UserRequestForm> getForms(final String userKey) {
        return logic.getForms(userKey);
    }

    @Override
    public PagedResult<UserRequestForm> getForms(final UserRequestFormQuery query) {
        Pair<Integer, List<UserRequestForm>> result = logic.getForms(
                query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public UserTO submitForm(final UserRequestForm form) {
        return logic.submitForm(form);
    }

}
