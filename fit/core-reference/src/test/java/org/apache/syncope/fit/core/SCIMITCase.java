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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.SCIMDetector;
import org.junit.jupiter.api.Test;

public class SCIMITCase extends AbstractITCase {

    public static final String SCIM_ADDRESS = "http://localhost:9080/syncope/scim";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WebClient webClient() {
        return WebClient.create(SCIM_ADDRESS).
                accept(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT());
    }

    @Test
    public void read() throws IOException {
        assumeTrue(SCIMDetector.isSCIMAvailable());

        Response response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        SCIMUser user = MAPPER.readValue((InputStream) response.getEntity(), SCIMUser.class);
        assertNotNull(user);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", user.getId());
        assertEquals("rossini", user.getUserName());
        assertFalse(user.getGroups().isEmpty());
        assertFalse(user.getRoles().isEmpty());
    }

    @Test
    public void list() throws IOException {
        assumeTrue(SCIMDetector.isSCIMAvailable());

        Response response = webClient().path("Groups").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> result = MAPPER.readValue(
                (InputStream) response.getEntity(),
                new TypeReference<ListResponse<SCIMGroup>>() {
        });
        assertNotNull(result);
        assertTrue(result.getTotalResults() > 0);
        assertFalse(result.getResources().isEmpty());

        result.getResources().forEach(group -> {
            assertNotNull(group.getId());
            assertNotNull(group.getDisplayName());
        });
    }
}
