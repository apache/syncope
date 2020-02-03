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
import java.util.Optional;
import org.apache.syncope.common.lib.to.PagedResult;
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
                list(new UserRequestQuery.Builder().page(1).size(1).build()).
                getTotalCount();
    }

    public static List<UserRequest> getUserRequests(final int page, final int size, final SortParam<String> sort) {
        return getService(UserRequestService.class).
                list(new UserRequestQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
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

    public static Optional<UserRequestForm> getForm(final String userKey) {
        PagedResult<UserRequestForm> forms = getService(UserRequestService.class).
                getForms(new UserRequestFormQuery.Builder().user(userKey).page(1).size(1).build());
        UserRequestForm form = forms.getResult().isEmpty()
                ? null
                : forms.getResult().get(0);
        return Optional.ofNullable(form);
    }

    public static UserRequestForm claimForm(final String taskKey) {
        return getService(UserRequestService.class).claimForm(taskKey);
    }

    public static UserRequestForm unclaimForm(final String taskKey) {
        return getService(UserRequestService.class).unclaimForm(taskKey);
    }

    public static void submitForm(final UserRequestForm form) {
        getService(UserRequestService.class).submitForm(form);
    }
}
