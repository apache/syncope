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
package org.apache.syncope.ext.scimv2.cxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.SCIMError;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SCIMExceptionMapperTest {

    private final SCIMExceptionMapper mapper = new SCIMExceptionMapper();

    @Test
    void notFound() {
        Response response = mapper.toResponse(new NotFoundException("Resource unknown not found"));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        SCIMError error = (SCIMError) response.getEntity();
        assertNull(error.getScimType());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), error.getStatus());
        assertTrue(error.getDetail().contains("Resource unknown not found"));
    }

    @Test
    void badRequest() {
        Response response = mapper.toResponse(new BadRequestException(ErrorType.invalidFilter, "bad filter"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        SCIMError error = (SCIMError) response.getEntity();
        assertEquals(ErrorType.invalidFilter, error.getScimType());
        assertEquals("bad filter", error.getDetail());
    }

    @Test
    void syncopeClientException() {
        SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.EntityExists);
        ex.getElements().add("already there");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        SCIMError error = (SCIMError) response.getEntity();
        assertEquals(ErrorType.uniqueness, error.getScimType());
        assertTrue(error.getDetail().contains("already there"));
    }

    @Test
    void unsupportedOperation() {
        Response response = mapper.toResponse(new UnsupportedOperationException("not supported"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        SCIMError error = (SCIMError) response.getEntity();
        assertEquals(ErrorType.invalidSyntax, error.getScimType());
    }

    @Test
    void generic() {
        Response response = mapper.toResponse(new IllegalStateException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("IllegalStateException: boom", response.getEntity());
    }

    @Test
    void accessDenied() {
        assertNull(mapper.toResponse(new AccessDeniedException("denied")));
    }
}
