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
package org.apache.syncope.client.enduser.rest;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.syncope.common.rest.api.service.UserRequestService;

public class UserRequestRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4785231164900813921L;

    public static int countUserRequests() {
        return getService(UserRequestService.class).
                list(new UserRequestQuery.Builder()
                        .user(SyncopeEnduserSession.get().getSelfTO().getUsername())
                        .page(1)
                        .size(1)
                        .build()).getTotalCount();
    }

    public static List<UserRequest> getUserRequests(
        final int page,
        final int size,
        final String username,
        final SortParam<String> sort) {
        return getService(UserRequestService.class).list(new UserRequestQuery.Builder().
                user(StringUtils.isBlank(username)
                        ? SyncopeEnduserSession.get().getSelfTO().getUsername()
                        : username).
                page(page).size(size).build()).getResult();
    }

    public static void cancelRequest(final String executionId, final String reason) {
        getService(UserRequestService.class).cancel(executionId, reason);
    }

    public static int countForms() {
        return getService(UserRequestService.class).
                getForms(new UserRequestFormQuery.Builder().page(1).size(1).build()).
                getTotalCount();
    }

    public static List<UserRequestForm> getForms(final int page, final int size, final SortParam<String> sort) {
        return getService(UserRequestService.class).
                getForms(new UserRequestFormQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public static Optional<UserRequestForm> getForm(final String username, final String taskId) {
        return Optional.ofNullable(getService(UserRequestService.class).getForm(StringUtils.isBlank(username)
                ? SyncopeEnduserSession.get().getSelfTO().getUsername()
                : username,
                taskId));
    }

    public static void submitForm(final UserRequestForm form) {
        getService(UserRequestService.class).submitForm(form);
    }

    public static void start(final String bpmnProcess, final String user) {
        getService(UserRequestService.class).start(bpmnProcess, user, null);
    }

    public static UserRequestForm claimForm(final String taskKey) {
        return getService(UserRequestService.class).claimForm(taskKey);
    }
}
