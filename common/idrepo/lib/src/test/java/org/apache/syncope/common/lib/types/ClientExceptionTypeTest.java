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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ClientExceptionTypeTest {

    @Test
    void fromHeaderValue() {
        for (ClientExceptionType type : ClientExceptionType.values()) {
            assertEquals(type, ClientExceptionType.fromHeaderValue(type.name()));
        }
    }

    @Test
    void fromUnknownHeaderValue() {
        assertThrows(IllegalArgumentException.class, () -> ClientExceptionType.fromHeaderValue("UnknownType"));
        assertThrows(IllegalArgumentException.class, () -> ClientExceptionType.fromHeaderValue(""));
    }

    @Test
    void infoHeaderValue() {
        assertEquals("NotFound:not found", ClientExceptionType.NotFound.getInfoHeaderValue("not found"));
        assertEquals("NotFound:not  found", ClientExceptionType.NotFound.getInfoHeaderValue("not\r\nfound"));
    }

    @Test
    void responseStatus() {
        assertEquals(Response.Status.NOT_FOUND, ClientExceptionType.NotFound.getResponseStatus());
        assertEquals(Response.Status.CONFLICT, ClientExceptionType.EntityExists.getResponseStatus());
        assertEquals(Response.Status.FORBIDDEN, ClientExceptionType.DelegatedAdministration.getResponseStatus());
        assertEquals(Response.Status.TOO_MANY_REQUESTS, ClientExceptionType.TooManyRequests.getResponseStatus());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR, ClientExceptionType.ExecutionError.getResponseStatus());
        assertEquals(Response.Status.BAD_REQUEST, ClientExceptionType.Unknown.getResponseStatus());
    }
}
