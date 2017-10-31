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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.cxf.JacksonSCIMJsonProvider;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.SCIMDetector;
import org.junit.Assume;
import org.junit.Test;

public class SCIMITCase extends AbstractITCase {

    public static final String SCIM_ADDRESS = "http://localhost:9080/syncope/scim/v2";

    private WebClient webClient() {
        return WebClient.create(SCIM_ADDRESS, Arrays.asList(new JacksonSCIMJsonProvider())).
                accept(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT());
    }

    @Test
    public void serviceProviderConfig() {
        Assume.assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("ServiceProviderConfig").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ServiceProviderConfig serviceProviderConfig = response.readEntity(ServiceProviderConfig.class);
        assertNotNull(serviceProviderConfig);
        assertTrue(serviceProviderConfig.getEtag().isSupported());
    }

    @Test
    public void resourceTypes() {
        Assume.assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("ResourceTypes").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        List<ResourceType> resourceTypes = response.readEntity(new GenericType<List<ResourceType>>() {
        });
        assertNotNull(resourceTypes);
        assertEquals(2, resourceTypes.size());

        response = webClient().path("ResourceTypes").path("User").get();
        assertEquals(200, response.getStatus());

        ResourceType user = response.readEntity(ResourceType.class);
        assertNotNull(user);
        assertEquals(Resource.User.schema(), user.getSchema());
        assertFalse(user.getSchemaExtensions().isEmpty());
    }

    @Test
    public void schemas() {
        Assume.assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Schemas").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ArrayNode schemas = response.readEntity(ArrayNode.class);
        assertNotNull(schemas);
        assertEquals(3, schemas.size());

        response = webClient().path("Schemas").path(Resource.EnterpriseUser.schema()).get();
        assertEquals(200, response.getStatus());

        ObjectNode enterpriseUser = response.readEntity(ObjectNode.class);
        assertNotNull(enterpriseUser);
        assertEquals(Resource.EnterpriseUser.schema(), enterpriseUser.get("id").textValue());
    }

    @Test
    public void read() throws IOException {
        Assume.assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        SCIMUser user = response.readEntity(SCIMUser.class);
        assertNotNull(user);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", user.getId());
        assertEquals("rossini", user.getUserName());
        assertFalse(user.getGroups().isEmpty());
        assertFalse(user.getRoles().isEmpty());
    }

    @Test
    public void list() throws IOException {
        Assume.assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Groups").get();
        assertEquals(200, response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> result = response.readEntity(new GenericType<ListResponse<SCIMGroup>>() {
        });
        assertNotNull(result);
        assertTrue(result.getTotalResults() > 0);
        assertFalse(result.getResources().isEmpty());

        for (SCIMGroup group : result.getResources()) {
            assertNotNull(group.getId());
            assertNotNull(group.getDisplayName());
        }
    }
}
