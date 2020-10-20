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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.syncope.common.rest.api.service.UserRequestService;

public class UserRequestRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4785231164900813921L;

    public int countRequests(final String keyword) {
        try {
            return getService(UserRequestService.class).
                    listRequests(new UserRequestQuery.Builder().user(keyword).page(1).size(0).build()).
                    getTotalCount();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<UserRequest> listRequests(
            final String keyword, final int page, final int size, final SortParam<String> sort) {

        try {
            return getService(UserRequestService.class).listRequests(
                    new UserRequestQuery.Builder().user(keyword).page(page).size(size).
                            orderBy(toOrderBy(sort)).build()).
                    getResult();
        } catch (SyncopeClientException e) {
            return Collections.emptyList();
        }
    }

    public void cancelRequest(final String executionId, final String reason) {
        getService(UserRequestService.class).cancelRequest(executionId, reason);
    }

    public int countForms(final String keyword) {
        try {
            return getService(UserRequestService.class).
                    listForms(new UserRequestQuery.Builder().user(keyword).page(1).size(0).build()).
                    getTotalCount();
        } catch (SyncopeClientException e) {
            return 0;
        }
    }

    public List<UserRequestForm> listForms(
            final String keyword, final int page, final int size, final SortParam<String> sort) {

        try {
            return getService(UserRequestService.class).listForms(
                    new UserRequestQuery.Builder().user(keyword).page(page).size(size).
                            orderBy(toOrderBy(sort)).build()).
                    getResult();
        } catch (SyncopeClientException e) {
            return Collections.emptyList();
        }
    }

    public Optional<UserRequestForm> getForm(final String userKey) {
        PagedResult<UserRequestForm> forms = getService(UserRequestService.class).
                listForms(new UserRequestQuery.Builder().user(userKey).page(1).size(1).build());
        UserRequestForm form = forms.getResult().isEmpty()
                ? null
                : forms.getResult().get(0);
        return Optional.ofNullable(form);
    }

    public UserRequestForm claimForm(final String taskKey) {
        return getService(UserRequestService.class).claimForm(taskKey);
    }

    public UserRequestForm unclaimForm(final String taskKey) {
        return getService(UserRequestService.class).unclaimForm(taskKey);
    }

    public void submitForm(final UserRequestForm form) {
        getService(UserRequestService.class).submitForm(form);
    }
}
