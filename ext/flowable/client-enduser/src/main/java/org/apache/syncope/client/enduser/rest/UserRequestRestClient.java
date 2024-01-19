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

import jakarta.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class UserRequestRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4785231164900813921L;

    public long countRequests() {
        return getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder()
                        .user(SyncopeEnduserSession.get().getSelfTO().getUsername())
                        .page(1)
                        .size(0)
                        .build()).getTotalCount();
    }

    public List<UserRequest> listRequests(
            final int page,
            final int size,
            final String username,
            final SortParam<String> sort) {

        return getService(UserRequestService.class).listRequests(new UserRequestQuery.Builder().
                user(StringUtils.isBlank(username)
                        ? SyncopeEnduserSession.get().getSelfTO().getUsername()
                        : username).
                page(page).size(size).orderBy(toOrderBy(sort)).build()).getResult();
    }

    public void cancelRequest(final String executionId, final String reason) {
        getService(UserRequestService.class).cancelRequest(executionId, reason);
    }

    public long countForms() {
        return getService(UserRequestService.class).
                listForms(new UserRequestQuery.Builder().page(1).size(0).build()).
                getTotalCount();
    }

    public List<UserRequestForm> listForms(final int page, final int size, final SortParam<String> sort) {
        return getService(UserRequestService.class).
                listForms(new UserRequestQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public Optional<UserRequestForm> getForm(final String username, final String taskId) {
        return Optional.ofNullable(getService(UserRequestService.class).getForm(StringUtils.isBlank(username)
                ? SyncopeEnduserSession.get().getSelfTO().getUsername()
                : username,
                taskId));
    }

    public ProvisioningResult<UserTO> submitForm(final UserRequestForm form) {
        return getService(UserRequestService.class).submitForm(form).readEntity(
                new GenericType<>() {
        });
    }

    public void startRequest(final String bpmnProcess, final String user) {
        UserRequestService service = getService(UserRequestService.class);
        WebClient.getConfig(WebClient.client(service)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.FALSE);

        service.startRequest(bpmnProcess, user, null);

        WebClient.getConfig(WebClient.client(service)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
    }

    public UserRequestForm claimForm(final String taskKey) {
        return getService(UserRequestService.class).claimForm(taskKey);
    }
}
