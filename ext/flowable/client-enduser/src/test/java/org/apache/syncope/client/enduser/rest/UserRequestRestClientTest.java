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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

class UserRequestRestClientTest {

    private UserRequestService service;

    private UserRequestRestClient client;

    @BeforeEach
    void setup() {
        service = mock(UserRequestService.class);
        client = new TestUserRequestRestClient(service);
    }

    @Test
    void countRequests() {
        UserTO selfTO = new UserTO();
        selfTO.setUsername("username");

        PagedResult<UserRequest> result = new PagedResult<>();
        result.setTotalCount(3);

        try (MockedStatic<SyncopeEnduserSession> session = mockStatic(SyncopeEnduserSession.class)) {
            SyncopeEnduserSession enduserSession = mock(SyncopeEnduserSession.class);
            session.when(SyncopeEnduserSession::get).thenReturn(enduserSession);
            when(enduserSession.getSelfTO()).thenReturn(selfTO);
            when(service.listRequests(any())).thenReturn(result);

            assertEquals(3, client.countRequests());

            ArgumentCaptor<UserRequestQuery> query = ArgumentCaptor.forClass(UserRequestQuery.class);
            verify(service).listRequests(query.capture());
            assertEquals("username", query.getValue().getUser());
        }
    }

    @Test
    void listRequests() {
        UserTO selfTO = new UserTO();
        selfTO.setUsername("username");

        UserRequest userRequest = new UserRequest();
        PagedResult<UserRequest> result = new PagedResult<>();
        result.getResult().add(userRequest);

        try (MockedStatic<SyncopeEnduserSession> session = mockStatic(SyncopeEnduserSession.class)) {
            SyncopeEnduserSession enduserSession = mock(SyncopeEnduserSession.class);
            session.when(SyncopeEnduserSession::get).thenReturn(enduserSession);
            when(enduserSession.getSelfTO()).thenReturn(selfTO);
            when(service.listRequests(any())).thenReturn(result);

            assertEquals(List.of(userRequest), client.listRequests(2, 10, new SortParam<>("key", false)));

            ArgumentCaptor<UserRequestQuery> query = ArgumentCaptor.forClass(UserRequestQuery.class);
            verify(service).listRequests(query.capture());
            assertEquals("username", query.getValue().getUser());
            assertEquals(2, query.getValue().getPage());
            assertEquals(10, query.getValue().getSize());
            assertEquals("key DESC", query.getValue().getOrderBy());
        }
    }

    @Test
    void countForms() {
        PagedResult<UserRequestForm> result = new PagedResult<>();
        result.setTotalCount(2);
        when(service.listForms(any())).thenReturn(result);

        assertEquals(2, client.countForms());
    }

    @Test
    void listForms() {
        UserRequestForm form = new UserRequestForm();
        PagedResult<UserRequestForm> result = new PagedResult<>();
        result.getResult().add(form);
        when(service.listForms(any())).thenReturn(result);

        assertEquals(List.of(form), client.listForms(3, 15, new SortParam<>("bpmnProcess", true)));

        ArgumentCaptor<UserRequestQuery> query = ArgumentCaptor.forClass(UserRequestQuery.class);
        verify(service).listForms(query.capture());
        assertEquals(3, query.getValue().getPage());
        assertEquals(15, query.getValue().getSize());
        assertEquals("bpmnProcess ASC", query.getValue().getOrderBy());
    }

    @Test
    void getForm() {
        UserTO selfTO = new UserTO();
        selfTO.setUsername("username");

        try (MockedStatic<SyncopeEnduserSession> session = mockStatic(SyncopeEnduserSession.class)) {
            SyncopeEnduserSession enduserSession = mock(SyncopeEnduserSession.class);
            session.when(SyncopeEnduserSession::get).thenReturn(enduserSession);
            when(enduserSession.getSelfTO()).thenReturn(selfTO);

            UserRequestForm form = new UserRequestForm();
            when(service.getForm("username", "task")).thenReturn(form);
            assertEquals(Optional.of(form), client.getForm("task"));

            when(service.getForm("username", "task")).thenReturn(null);
            assertEquals(Optional.empty(), client.getForm("task"));
        }
    }

    @Test
    void submitForm() {
        UserRequestForm form = new UserRequestForm();
        Response response = mock(Response.class);
        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        when(service.submitForm(form)).thenReturn(response);
        when(response.readEntity(
                ArgumentMatchers.<GenericType<ProvisioningResult<UserTO>>>any())).thenReturn(result);

        assertSame(result, client.submitForm(form));
    }

    @Test
    void startRequest() {
        client.startRequest("process");

        verify(service).startRequest("process", null, null);
    }

    @Test
    void claimForm() {
        UserRequestForm form = new UserRequestForm();
        when(service.claimForm("task")).thenReturn(form);

        assertSame(form, client.claimForm("task"));
    }

    private static final class TestUserRequestRestClient extends UserRequestRestClient {

        private static final long serialVersionUID = 1L;

        private final UserRequestService service;

        private TestUserRequestRestClient(final UserRequestService service) {
            this.service = service;
        }

        @Override
        protected <T> T getService(final Class<T> serviceClass) {
            return serviceClass.cast(service);
        }
    }
}
