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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.lib.batch.BatchResponse;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        PagedResult<UserRequest> result = new PagedResult<>();
        result.setTotalCount(5);
        when(service.listRequests(any())).thenReturn(result);

        assertEquals(5, client.countRequests("keyword"));

        ArgumentCaptor<UserRequestQuery> query = ArgumentCaptor.forClass(UserRequestQuery.class);
        verify(service).listRequests(query.capture());
        assertEquals("keyword", query.getValue().getUser());
        assertEquals(1, query.getValue().getPage());
        assertEquals(1, query.getValue().getSize());
    }

    @Test
    void countRequestsOnFailure() {
        when(service.listRequests(any())).thenThrow(new IllegalStateException("failure"));

        assertEquals(0, client.countRequests("keyword"));
    }

    @Test
    void listRequests() {
        UserRequest userRequest = new UserRequest();
        PagedResult<UserRequest> result = new PagedResult<>();
        result.getResult().add(userRequest);
        when(service.listRequests(any())).thenReturn(result);

        List<UserRequest> requests = client.listRequests("keyword", 2, 10, new SortParam<>("key", true));

        assertEquals(List.of(userRequest), requests);

        ArgumentCaptor<UserRequestQuery> query = ArgumentCaptor.forClass(UserRequestQuery.class);
        verify(service).listRequests(query.capture());
        assertEquals("keyword", query.getValue().getUser());
        assertEquals(2, query.getValue().getPage());
        assertEquals(10, query.getValue().getSize());
        assertEquals("key ASC", query.getValue().getOrderBy());
    }

    @Test
    void listRequestsOnSyncopeClientException() {
        when(service.listRequests(any())).
                thenThrow(SyncopeClientException.build(ClientExceptionType.InvalidRequest));

        assertTrue(client.listRequests("keyword", 1, 10, new SortParam<>("key", true)).isEmpty());
    }

    @Test
    void getForm() {
        when(service.listForms(any())).thenReturn(new PagedResult<>());
        assertEquals(Optional.empty(), client.getForm("userKey"));

        UserRequestForm form = new UserRequestForm();
        PagedResult<UserRequestForm> result = new PagedResult<>();
        result.getResult().add(form);
        when(service.listForms(any())).thenReturn(result);

        assertSame(form, client.getForm("userKey").orElseThrow());
    }

    @Test
    void claimAndUnclaimForm() {
        UserRequestForm form = new UserRequestForm();
        when(service.claimForm("task")).thenReturn(form);
        when(service.unclaimForm("task")).thenReturn(form);

        assertSame(form, client.claimForm("task"));
        assertSame(form, client.unclaimForm("task"));
    }

    @Test
    void submitForm() {
        UserRequestForm form = new UserRequestForm();

        client.submitForm(form);

        verify(service).submitForm(form);
    }

    @Test
    void batch() throws IOException {
        BatchRequestItem requestItem = new BatchRequestItem();
        requestItem.setRequestURI("users/userKey");
        BatchRequest batchRequest = mock(BatchRequest.class);
        when(batchRequest.getItems()).thenReturn(List.of(requestItem));

        BatchResponseItem responseItem = new BatchResponseItem();
        responseItem.setStatus(200);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getItems()).thenReturn(List.of(responseItem));
        when(batchRequest.commit()).thenReturn(batchResponse);

        assertEquals(Map.of("userKey", ExecStatus.SUCCESS.name()), client.batch(batchRequest));
    }

    @Test
    void batchOnFailure() throws IOException {
        BatchRequest batchRequest = mock(BatchRequest.class);
        when(batchRequest.getItems()).thenReturn(List.of());

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getItems()).thenThrow(new IOException("failure"));
        when(batchRequest.commit()).thenReturn(batchResponse);

        assertTrue(client.batch(batchRequest).isEmpty());
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
