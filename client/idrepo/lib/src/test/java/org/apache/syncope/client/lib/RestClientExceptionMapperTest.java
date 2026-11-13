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
package org.apache.syncope.client.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.junit.jupiter.api.Test;

class RestClientExceptionMapperTest {

    @Test
    void unauthorized() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.UNAUTHORIZED.getStatusCode());
        when(response.getHeaderString(RESTHeaders.ERROR_INFO)).thenReturn(null);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(NotAuthorizedException.class, ex);
        assertEquals("Remote unauthorized exception", ex.getMessage());
    }

    @Test
    void forbidden() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.FORBIDDEN.getStatusCode());
        when(response.getHeaderString(RESTHeaders.ERROR_INFO)).thenReturn(null);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(ForbiddenException.class, ex);
        assertEquals("Remote forbidden exception", ex.getMessage());
    }

    @Test
    void badRequestWithoutMessage() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.BAD_REQUEST.getStatusCode());
        when(response.getHeaderString(RESTHeaders.ERROR_INFO)).thenReturn(null);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(BadRequestException.class, ex);
    }

    @Test
    void badRequestWithMessage() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.BAD_REQUEST.getStatusCode());
        when(response.getHeaderString(RESTHeaders.ERROR_INFO)).thenReturn("invalid request");

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(BadRequestException.class, ex);
        assertEquals("invalid request", ex.getMessage());
    }

    @Test
    void notFound() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.NOT_FOUND.getStatusCode());

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(WebApplicationException.class, ex);
        assertEquals("Remote exception with status code: NOT_FOUND", ex.getMessage());
    }

    @Test
    void internalServerError() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(WebApplicationException.class, ex);
        assertEquals("Remote exception with status code: INTERNAL_SERVER_ERROR", ex.getMessage());
    }

    @Test
    void compositeExceptionFromHeadersSingle() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.BAD_REQUEST.getStatusCode());
        MultivaluedMap<String, String> headers = headers(
                List.of(ClientExceptionType.NotFound.name()),
                List.of(ClientExceptionType.NotFound.name() + ":element"));
        when(response.getStringHeaders()).thenReturn(headers);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(SyncopeClientException.class, ex);
        SyncopeClientException clientException = (SyncopeClientException) ex;
        assertEquals(ClientExceptionType.NotFound, clientException.getType());
    }

    @Test
    void compositeExceptionFromHeadersMultiple() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.BAD_REQUEST.getStatusCode());
        MultivaluedMap<String, String> headers = headers(
                List.of(
                        ClientExceptionType.NotFound.name(),
                        ClientExceptionType.InvalidRealm.name()),
                List.of(
                        ClientExceptionType.NotFound.name() + ":element1",
                        ClientExceptionType.InvalidRealm.name() + ":element2"));
        when(response.getStringHeaders()).thenReturn(headers);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(SyncopeClientCompositeException.class, ex);
        SyncopeClientCompositeException composite = (SyncopeClientCompositeException) ex;
        assertTrue(composite.hasExceptions());
        assertEquals(2, composite.getExceptions().size());
    }

    @Test
    void compositeExceptionFromEntity() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.NOT_FOUND.getStatusCode());

        ErrorTO error = new ErrorTO();
        error.setType(ClientExceptionType.NotFound);
        error.getElements().add("element");
        when(response.readEntity(ErrorTO.class)).thenReturn(error);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(SyncopeClientException.class, ex);
        SyncopeClientException clientException = (SyncopeClientException) ex;
        assertEquals(ClientExceptionType.NotFound, clientException.getType());
        assertEquals(Set.of("element"), clientException.getElements());
    }

    @Test
    void unmanagedExceptionTypeInHeaders() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.BAD_REQUEST.getStatusCode());
        MultivaluedMap<String, String> headers = headers(
                List.of(ClientExceptionType.NotFound.name(), "UnexpectedType"),
                List.of());
        when(response.getStringHeaders()).thenReturn(headers);

        Exception ex = mapper.fromResponse(response);

        assertInstanceOf(SyncopeClientException.class, ex);
        SyncopeClientException clientException = (SyncopeClientException) ex;
        assertEquals(ClientExceptionType.NotFound, clientException.getType());
    }

    @Test
    void fromResponseNeverReturnsNull() {
        RestClientExceptionMapper mapper = new RestClientExceptionMapper();
        Response response = mockResponse(Response.Status.OK.getStatusCode());

        assertNotNull(mapper.fromResponse(response));
    }

    private static Response mockResponse(final int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getStatusInfo()).thenReturn(Response.Status.fromStatusCode(status));
        when(response.getStringHeaders()).thenReturn(emptyHeaders());
        return response;
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> emptyHeaders() {
        return mock(MultivaluedMap.class);
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> headers(
            final List<String> codes,
            final List<String> infos) {

        MultivaluedMap<String, String> headers = mock(MultivaluedMap.class);
        when(headers.get(RESTHeaders.ERROR_CODE)).thenReturn(new ArrayList<>(codes));
        when(headers.get(RESTHeaders.ERROR_INFO)).thenReturn(new ArrayList<>(infos));
        return headers;
    }
}
