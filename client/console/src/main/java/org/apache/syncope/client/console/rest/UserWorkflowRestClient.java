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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.rest.api.beans.WorkflowFormQuery;
import org.apache.syncope.common.rest.api.service.UserWorkflowService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class UserWorkflowRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4785231164900813921L;

    public int countForms() {
        return getService(UserWorkflowService.class).
                getForms(new WorkflowFormQuery.Builder().page(1).size(1).build()).
                getTotalCount();
    }

    public List<WorkflowFormTO> getForms(final int page, final int size, final SortParam<String> sort) {
        return getService(UserWorkflowService.class).
                getForms(new WorkflowFormQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public WorkflowFormTO getFormForUser(final String userKey) {
        return getService(UserWorkflowService.class).getFormForUser(userKey);
    }

    public WorkflowFormTO claimForm(final String taskKey) {
        return getService(UserWorkflowService.class).claimForm(taskKey);
    }

    public WorkflowFormTO unclaimForm(final String taskKey) {
        return getService(UserWorkflowService.class).unclaimForm(taskKey);
    }

    public void submitForm(final WorkflowFormTO form) {
        getService(UserWorkflowService.class).submitForm(form);
    }

    public UserTO executeTask(final String taskId, final UserTO form) {
        return getService(UserWorkflowService.class).executeTask(taskId, form);
    }
}
