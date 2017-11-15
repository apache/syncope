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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
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
import org.junit.jupiter.api.Test;

public class SCIMITCase extends AbstractITCase {

    public static final String SCIM_ADDRESS = "http://localhost:9080/syncope/scim/v2";

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyPattern(SyncopeConstants.DEFAULT_DATE_PATTERN);
            return sdf;
        }
    };

    private WebClient webClient() {
        return WebClient.create(SCIM_ADDRESS, Arrays.asList(new JacksonSCIMJsonProvider())).
                accept(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                type(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT());
    }

    @Test
    public void serviceProviderConfig() {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("ServiceProviderConfig").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ServiceProviderConfig serviceProviderConfig = response.readEntity(ServiceProviderConfig.class);
        assertNotNull(serviceProviderConfig);
        assertTrue(serviceProviderConfig.getEtag().isSupported());
    }

    @Test
    public void resourceTypes() {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("ResourceTypes").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        List<ResourceType> resourceTypes = response.readEntity(new GenericType<List<ResourceType>>() {
        });
        assertNotNull(resourceTypes);
        assertEquals(2, resourceTypes.size());

        response = webClient().path("ResourceTypes").path("User").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ResourceType user = response.readEntity(ResourceType.class);
        assertNotNull(user);
        assertEquals(Resource.User.schema(), user.getSchema());
        assertFalse(user.getSchemaExtensions().isEmpty());
    }

    @Test
    public void schemas() {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Schemas").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ArrayNode schemas = response.readEntity(ArrayNode.class);
        assertNotNull(schemas);
        assertEquals(3, schemas.size());

        response = webClient().path("Schemas").path("none").get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = webClient().path("Schemas").path(Resource.EnterpriseUser.schema()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectNode enterpriseUser = response.readEntity(ObjectNode.class);
        assertNotNull(enterpriseUser);
        assertEquals(Resource.EnterpriseUser.schema(), enterpriseUser.get("id").textValue());
    }

    @Test
    public void read() throws IOException {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
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
    public void conf() {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        SCIMConf conf = scimConfService.get();
        assertNotNull(conf);

        SCIMUserConf userConf = conf.getUserConf();
        if (userConf == null) {
            userConf = new SCIMUserConf();
            conf.setUserConf(userConf);
        }
        assertNull(userConf.getDisplayName());
        userConf.setDisplayName("cn");

        scimConfService.set(conf);

        Response response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        SCIMUser user = response.readEntity(SCIMUser.class);
        assertNotNull(user);
        assertEquals("Rossini, Gioacchino", user.getDisplayName());
    }

    @Test
    public void list() throws IOException {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        Response response = webClient().path("Groups").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> result = response.readEntity(new GenericType<ListResponse<SCIMGroup>>() {
        });
        assertNotNull(result);
        assertTrue(result.getTotalResults() > 0);
        assertFalse(result.getResources().isEmpty());

        result.getResources().forEach(group -> {
            assertNotNull(group.getId());
            assertNotNull(group.getDisplayName());
        });
    }

    @Test
    public void search() {
        assumeTrue(SCIMDetector.isSCIMAvailable(webClient()));

        // eq
        Response response = webClient().path("Groups").query("filter", "displayName eq \"additional\"").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> groups = response.readEntity(new GenericType<ListResponse<SCIMGroup>>() {
        });
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());

        SCIMGroup additional = groups.getResources().get(0);
        assertEquals("additional", additional.getDisplayName());

        // eq via POST
        String request = "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:SearchRequest\"],"
                + "     \"filter\": \"displayName eq \\\"additional\\\"\""
                + "   }";
        response = webClient().path("Groups").path("/.search").post(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        groups = response.readEntity(new GenericType<ListResponse<SCIMGroup>>() {
        });
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());

        additional = groups.getResources().get(0);
        assertEquals("additional", additional.getDisplayName());

        // gt
        UserTO newUser = userService.create(UserITCase.getUniqueSampleTO("scimsearch@syncope.apache.org")).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        Date value = new Date(newUser.getCreationDate().getTime() - 1000);
        response = webClient().path("Users").query("filter", "meta.created gt \""
                + DATE_FORMAT.get().format(value) + "\"").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMUser> users = response.readEntity(new GenericType<ListResponse<SCIMUser>>() {
        });
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());

        SCIMUser newSCIMUser = users.getResources().get(0);
        assertEquals(newUser.getUsername(), newSCIMUser.getUserName());
    }
}
