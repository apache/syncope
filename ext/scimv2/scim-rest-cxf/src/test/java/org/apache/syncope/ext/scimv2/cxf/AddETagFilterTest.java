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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.data.Meta;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddETagFilterTest {

    private final AddETagFilter filter = new AddETagFilter();

    private MultivaluedMap<String, Object> headers;

    private ContainerResponseContext resCtx;

    @BeforeEach
    void setup() {
        headers = new MultivaluedHashMap<>();
        resCtx = mock(ContainerResponseContext.class);
        when(resCtx.getHeaders()).thenReturn(headers);
    }

    private static Meta meta(final OffsetDateTime lastModified) {
        return new Meta("User", lastModified, lastModified, null, "http://localhost/Users/1");
    }

    @Test
    void noETagIfAlreadySet() {
        when(resCtx.getEntityTag()).thenReturn(new EntityTag("existing"));

        filter.filter(mock(ContainerRequestContext.class), resCtx);

        assertFalse(headers.containsKey(HttpHeaders.ETAG));
    }

    @Test
    void addETagForUser() {
        OffsetDateTime lastModified = OffsetDateTime.parse("2021-02-03T04:05:06Z");
        when(resCtx.getEntity()).thenReturn(new SCIMUser(
                "1", List.of(Resource.User.schema()), meta(lastModified), "username", true));

        filter.filter(mock(ContainerRequestContext.class), resCtx);

        Object etag = headers.getFirst(HttpHeaders.ETAG);
        assertNotNull(etag);
        assertEquals(String.valueOf(lastModified.toInstant().toEpochMilli()), new EntityTag(
                etag.toString().replace("\"", "")).getValue());
    }

    @Test
    void addETagForGroup() {
        OffsetDateTime lastModified = OffsetDateTime.parse("2022-03-04T05:06:07Z");
        when(resCtx.getEntity()).thenReturn(new SCIMGroup(
                "1", List.of(Resource.Group.schema()), meta(lastModified), "group"));

        filter.filter(mock(ContainerRequestContext.class), resCtx);

        Object etag = headers.getFirst(HttpHeaders.ETAG);
        assertNotNull(etag);
        assertEquals(String.valueOf(lastModified.toInstant().toEpochMilli()), new EntityTag(
                etag.toString().replace("\"", "")).getValue());
    }

    @Test
    void noETagForOtherEntities() {
        when(resCtx.getEntity()).thenReturn("not a SCIM resource");

        filter.filter(mock(ContainerRequestContext.class), resCtx);

        assertFalse(headers.containsKey(HttpHeaders.ETAG));
    }
}
