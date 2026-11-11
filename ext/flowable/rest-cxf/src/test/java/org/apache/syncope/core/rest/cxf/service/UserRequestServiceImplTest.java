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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;
import java.util.UUID;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.core.logic.UserRequestLogic;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class UserRequestServiceImplTest {

    private UserRequestLogic logic;

    private UserDAO userDAO;

    private UriInfo uriInfo;

    private MessageContext messageContext;

    private UserRequestServiceImpl service;

    @BeforeEach
    void setup() {
        logic = mock(UserRequestLogic.class);
        userDAO = mock(UserDAO.class);
        uriInfo = mock(UriInfo.class);
        messageContext = mock(MessageContext.class);

        when(uriInfo.getPathParameters(true)).thenReturn(new MultivaluedHashMap<>());
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("http://localhost/flowable"));
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        service = new UserRequestServiceImpl(logic, userDAO);
        service.uriInfo = uriInfo;
        service.messageContext = messageContext;
    }

    @Test
    void listRequests() {
        UserRequestQuery query = new UserRequestQuery.Builder().page(1).size(10).build();
        when(logic.listRequests(isNull(), any(Pageable.class))).thenReturn(Page.empty());

        PagedResult<UserRequest> result = service.listRequests(query);

        assertEquals(0, result.getTotalCount());
        assertEquals(1, result.getPage());
        verify(logic).listRequests(isNull(), any(Pageable.class));
    }

    @Test
    void listRequestsForUser() {
        UserRequestQuery query = new UserRequestQuery.Builder().user("username").page(1).size(10).build();
        when(userDAO.findKey("username")).thenReturn(Optional.of("userKey"));
        when(logic.listRequests(any(), any(Pageable.class))).thenReturn(Page.empty());

        service.listRequests(query);

        verify(logic).listRequests(any(), any(Pageable.class));
    }

    @Test
    void startRequestWithoutUser() {
        WorkflowTaskExecInput input = new WorkflowTaskExecInput();
        UserRequest userRequest = new UserRequest();
        when(logic.startRequest("process", input)).thenReturn(userRequest);

        assertSame(userRequest, service.startRequest("process", null, input));
    }

    @Test
    void startRequestWithKey() {
        String userKey = UUID.randomUUID().toString();
        WorkflowTaskExecInput input = new WorkflowTaskExecInput();
        UserRequest userRequest = new UserRequest();
        when(logic.startRequest("process", userKey, input)).thenReturn(userRequest);

        assertSame(userRequest, service.startRequest("process", userKey, input));
    }

    @Test
    void startRequestWithUsername() {
        WorkflowTaskExecInput input = new WorkflowTaskExecInput();
        UserRequest userRequest = new UserRequest();
        when(userDAO.findKey("username")).thenReturn(Optional.of("userKey"));
        when(logic.startRequest("process", "userKey", input)).thenReturn(userRequest);

        assertSame(userRequest, service.startRequest("process", "username", input));
    }

    @Test
    void cancelRequest() {
        service.cancelRequest("execution", "reason");

        verify(logic).cancelRequest("execution", "reason");
    }

    @Test
    void claimAndUnclaimForm() {
        UserRequestForm form = new UserRequestForm();
        when(logic.claimForm("task")).thenReturn(form);
        when(logic.unclaimForm("task")).thenReturn(form);

        assertSame(form, service.claimForm("task"));
        assertSame(form, service.unclaimForm("task"));
    }

    @Test
    void getForm() {
        UserRequestForm form = new UserRequestForm();
        when(userDAO.findKey("username")).thenReturn(Optional.of("userKey"));
        when(logic.getForm("userKey", "task")).thenReturn(form);

        assertSame(form, service.getForm("username", "task"));
    }

    @Test
    void submitForm() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(messageContext.getHttpServletRequest()).thenReturn(request);

        UserRequestForm form = new UserRequestForm();
        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        UserTO userTO = new UserTO();
        userTO.setKey("userKey");
        result.setEntity(userTO);
        when(logic.submitForm(form, false)).thenReturn(result);

        Response response = service.submitForm(form);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertSame(result, response.getEntity());
    }
}
