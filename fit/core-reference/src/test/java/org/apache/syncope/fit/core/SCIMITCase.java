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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionUserConf;
import org.apache.syncope.common.lib.scim.SCIMGroupConf;
import org.apache.syncope.common.lib.scim.SCIMItem;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.ext.scimv2.api.data.Group;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.Member;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.SCIMComplexValue;
import org.apache.syncope.ext.scimv2.api.data.SCIMError;
import org.apache.syncope.ext.scimv2.api.data.SCIMExtensionInfo;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserName;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.data.Value;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SCIMITCase extends AbstractITCase {

    public static final String SCIM_ADDRESS = "http://localhost:9080/syncope/rest/scim/v2";

    private static final SCIMConf CONF;

    private static Boolean ENABLED;

    static {
        CONF = new SCIMConf();

        CONF.setGroupConf(new SCIMGroupConf());

        CONF.getGroupConf().setExternalId("originalName");

        CONF.setUserConf(new SCIMUserConf());

        CONF.getUserConf().setNickName("ctype");
        CONF.getUserConf().setDisplayName("cn");

        CONF.getUserConf().setName(new SCIMUserNameConf());
        CONF.getUserConf().getName().setGivenName("firstname");
        CONF.getUserConf().getName().setFamilyName("surname");
        CONF.getUserConf().getName().setFormatted("fullname");

        SCIMComplexConf<EmailCanonicalType> email = new SCIMComplexConf<>();
        email.setValue("userId");
        email.setType(EmailCanonicalType.work);
        CONF.getUserConf().getEmails().add(email);
        email = new SCIMComplexConf<>();
        email.setValue("email");
        email.setType(EmailCanonicalType.home);
        CONF.getUserConf().getEmails().add(email);
    }

    private static SCIMUser getSampleUser(final String username, final List<String> schemas) {
        SCIMUser user = new SCIMUser(null, schemas, null, username, true);
        user.setPassword("password123");

        SCIMUserName name = new SCIMUserName();
        name.setGivenName(username);
        name.setFamilyName("surname");
        name.setFormatted(username);
        user.setName(name);

        SCIMComplexValue userId = new SCIMComplexValue();
        userId.setType(EmailCanonicalType.work.name());
        userId.setValue(username + "@syncope.apache.org");
        user.getEmails().add(userId);

        SCIMComplexValue email = new SCIMComplexValue();
        email.setType(EmailCanonicalType.home.name());
        email.setValue(username + "@syncope.apache.org");
        user.getEmails().add(email);

        return user;
    }

    @BeforeAll
    public static void isSCIMAvailable() {
        if (ENABLED == null) {
            try {
                Response response = webClient().path("ServiceProviderConfig").get();
                ENABLED = response.getStatus() == 200;
            } catch (Exception e) {
                // ignore
                ENABLED = false;
            }
        }

        assumeTrue(ENABLED);
    }

    private static WebClient webClient() {
        return WebClient.create(
                SCIM_ADDRESS,
                List.of(new JacksonJsonProvider(JsonMapper.builder().
                        findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))).
                accept(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                type(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT());
    }

    @Test
    public void serviceProviderConfig() {
        Response response = webClient().path("ServiceProviderConfig").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ServiceProviderConfig serviceProviderConfig = response.readEntity(ServiceProviderConfig.class);
        assertNotNull(serviceProviderConfig);
        assertTrue(serviceProviderConfig.getPatch().isSupported());
        assertFalse(serviceProviderConfig.getBulk().isSupported());
        assertTrue(serviceProviderConfig.getChangePassword().isSupported());
        assertTrue(serviceProviderConfig.getEtag().isSupported());
        assertTrue(serviceProviderConfig.getSort().isSupported());
    }

    @Test
    public void resourceTypes() {
        Response response = webClient().path("ResourceTypes").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        List<ResourceType> resourceTypes = response.readEntity(new GenericType<>() {
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
        SCIMExtensionUserConf extensionUserConf = new SCIMExtensionUserConf();
        extensionUserConf.setName("syncope");
        extensionUserConf.setDescription("syncope user");
        SCIMItem scimItem = new SCIMItem();
        scimItem.setIntAttrName("gender");
        scimItem.setExtAttrName("gender");
        extensionUserConf.add(scimItem);
        CONF.setExtensionUserConf(extensionUserConf);
        SCIM_CONF_SERVICE.set(CONF);

        Response response = webClient().path("Schemas").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ArrayNode schemas = response.readEntity(ArrayNode.class);
        assertNotNull(schemas);
        assertEquals(4, schemas.size());

        response = webClient().path("Schemas").path("none").get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = webClient().path("Schemas").path(Resource.EnterpriseUser.schema()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectNode enterpriseUser = response.readEntity(ObjectNode.class);
        assertNotNull(enterpriseUser);
        assertEquals(Resource.EnterpriseUser.schema(), enterpriseUser.get("id").textValue());

        response = webClient().path("Schemas").path(Resource.ExtensionUser.schema()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectNode extensionUser = response.readEntity(ObjectNode.class);
        assertNotNull(extensionUser);
        assertEquals(Resource.ExtensionUser.schema(), extensionUser.get("id").textValue());

        CONF.setExtensionUserConf(null);
        SCIM_CONF_SERVICE.set(CONF);
    }

    @Test
    public void read() throws IOException {
        Response response = webClient().path("Users").path("missing").get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        SCIMError error = response.readEntity(SCIMError.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), error.getStatus());

        response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
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

        response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").
                query("attributes", "groups").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", user.getId());
        assertNull(user.getUserName());
        assertFalse(user.getGroups().isEmpty());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    public void conf() {
        SCIMConf conf = SCIM_CONF_SERVICE.get();
        assertNotNull(conf);

        SCIM_CONF_SERVICE.set(CONF);

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
    void invalidConf() {
        SCIMExtensionUserConf extensionUserConf = new SCIMExtensionUserConf();
        extensionUserConf.setName("syncope");
        extensionUserConf.setDescription("syncope user");
        SCIMItem scimItem = new SCIMItem();
        scimItem.setIntAttrName("gender");
        scimItem.setExtAttrName("gender");
        scimItem.setMultiValued(true);
        extensionUserConf.add(scimItem);
        CONF.setExtensionUserConf(extensionUserConf);
        try {
            SCIM_CONF_SERVICE.set(CONF);
            fail();
        } catch (Exception ignored) {
            CONF.setExtensionUserConf(null);
        }
    }

    @Test
    public void list() throws IOException {
        Response response = webClient().path("Groups").query("count", 1100000).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        SCIMError error = response.readEntity(SCIMError.class);
        assertEquals(ErrorType.tooMany, error.getScimType());

        response = webClient().path("Groups").
                query("sortBy", "displayName").
                query("count", 11).
                get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> result = response.readEntity(new GenericType<>() {
        });
        assertNotNull(result);
        assertTrue(result.getTotalResults() > 0);
        assertEquals(11, result.getItemsPerPage());

        assertFalse(result.getResources().isEmpty());
        result.getResources().forEach(group -> {
            assertNotNull(group.getId());
            assertNotNull(group.getDisplayName());
        });
    }

    @Test
    public void search() {
        // invalid filter
        Response response = webClient().path("Groups").query("filter", "invalid").get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        SCIMError error = response.readEntity(SCIMError.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), error.getStatus());
        assertEquals(ErrorType.invalidFilter, error.getScimType());

        // eq
        response = webClient().path("Groups").query("filter", "displayName eq \"additional\"").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMGroup> groups = response.readEntity(new GenericType<>() {
        });
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());

        SCIMGroup additional = groups.getResources().get(0);
        assertEquals("additional", additional.getDisplayName());

        // eq via POST
        SCIMSearchRequest request = new SCIMSearchRequest("displayName eq \"additional\"", null, null, null, null);
        response = webClient().path("Groups").path("/.search").post(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        groups = response.readEntity(new GenericType<>() {
        });
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());

        additional = groups.getResources().get(0);
        assertEquals("additional", additional.getDisplayName());

        // gt
        UserTO newUser = USER_SERVICE.create(UserITCase.getUniqueSample("scimsearch@syncope.apache.org")).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();

        OffsetDateTime value = newUser.getCreationDate().minusSeconds(1).truncatedTo(ChronoUnit.SECONDS);
        response = webClient().path("Users").query("filter", "meta.created gt \""
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value) + '"').get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                SCIMConstants.APPLICATION_SCIM_JSON,
                StringUtils.substringBefore(response.getHeaderString(HttpHeaders.CONTENT_TYPE), ";"));

        ListResponse<SCIMUser> users = response.readEntity(new GenericType<>() {
        });
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());

        SCIMUser newSCIMUser = users.getResources().get(0);
        assertEquals(newUser.getUsername(), newSCIMUser.getUserName());
    }

    @Test
    public void createUser() throws JsonProcessingException {
        SCIM_CONF_SERVICE.set(CONF);

        SCIMUser user = getSampleUser(UUID.randomUUID().toString(), List.of(Resource.User.schema()));
        user.getRoles().add(new Value("User reviewer"));
        user.getGroups().add(new Group("37d15e4c-cdc1-460b-a591-8505c8133806", null, null, null));

        Response response = webClient().path("Users").post(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user.getId());
        assertTrue(response.getLocation().toASCIIString().endsWith(user.getId()));

        UserTO userTO = USER_SERVICE.read(user.getId());
        assertEquals(user.getUserName(), userTO.getUsername());
        assertTrue(user.isActive());
        assertEquals(user.getDisplayName(), userTO.getDerAttr("cn").get().getValues().get(0));
        assertEquals(user.getName().getGivenName(), userTO.getPlainAttr("firstname").get().getValues().get(0));
        assertEquals(user.getName().getFamilyName(), userTO.getPlainAttr("surname").get().getValues().get(0));
        assertEquals(user.getName().getFormatted(), userTO.getPlainAttr("fullname").get().getValues().get(0));
        assertEquals(user.getEmails().get(0).getValue(), userTO.getPlainAttr("userId").get().getValues().get(0));
        assertEquals(user.getEmails().get(1).getValue(), userTO.getPlainAttr("email").get().getValues().get(0));
        assertEquals(user.getRoles().get(0).getValue(), userTO.getRoles().get(0));
        assertEquals(user.getGroups().get(0).getValue(), userTO.getMemberships().get(0).getGroupKey());
    }

    @Test
    void crudExtensionUser() {
        SCIMExtensionUserConf extensionUserConf = new SCIMExtensionUserConf();
        extensionUserConf.setName("syncope");
        extensionUserConf.setDescription("syncope user");
        SCIMItem scimItem = new SCIMItem();
        scimItem.setIntAttrName("gender");
        scimItem.setExtAttrName("gender");
        extensionUserConf.add(scimItem);
        CONF.setExtensionUserConf(extensionUserConf);
        SCIM_CONF_SERVICE.set(CONF);

        SCIMUser user = getSampleUser(
                UUID.randomUUID().toString(), List.of(Resource.User.schema(), Resource.ExtensionUser.schema()));
        SCIMExtensionInfo scimExtensionInfo = new SCIMExtensionInfo();
        scimExtensionInfo.getAttributes().put("gender", "M");
        user.setExtensionInfo(scimExtensionInfo);

        Response response = webClient().path("Users").post(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user.getId());
        assertTrue(response.getLocation().toASCIIString().endsWith(user.getId()));

        UserTO userTO = USER_SERVICE.read(user.getId());
        assertEquals(user.getUserName(), userTO.getUsername());
        assertTrue(user.isActive());
        assertEquals(user.getDisplayName(), userTO.getDerAttr("cn").get().getValues().get(0));
        assertEquals(user.getName().getGivenName(), userTO.getPlainAttr("firstname").get().getValues().get(0));
        assertEquals(user.getName().getFamilyName(), userTO.getPlainAttr("surname").get().getValues().get(0));
        assertEquals(user.getName().getFormatted(), userTO.getPlainAttr("fullname").get().getValues().get(0));
        assertEquals(user.getEmails().get(0).getValue(), userTO.getPlainAttr("userId").get().getValues().get(0));
        assertEquals(user.getEmails().get(1).getValue(), userTO.getPlainAttr("email").get().getValues().get(0));
        assertEquals(user.getExtensionInfo().getAttributes().get("gender"),
                userTO.getPlainAttr("gender").get().getValues().get(0));

        response = webClient().path("Users").path(user.getId()).delete();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = webClient().path("Users").path(user.getId()).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        CONF.setExtensionUserConf(null);
        SCIM_CONF_SERVICE.set(CONF);
    }

    @Test
    public void updateUser() {
        SCIM_CONF_SERVICE.set(CONF);

        SCIMUser user = getSampleUser(UUID.randomUUID().toString(), List.of(Resource.User.schema()));

        Response response = webClient().path("Users").post(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user.getId());
        assertNull(user.getNickName());
        assertTrue(user.isActive());

        // 1. update no path, add value and suspend
        String body =
                "{"
                + "  \"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "  \"Operations\": ["
                + "    {"
                + "      \"op\": \"add\","
                + "      \"value\": {"
                + "        \"nickName\": \"" + user.getUserName() + "\","
                + "        \"active\": false"
                + "      }"
                + "    }"
                + "  ]"
                + "}";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertEquals(user.getUserName(), user.getNickName());
        assertFalse(user.isActive());

        // 2. update with path, reactivate
        body =
                "{"
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "\"Operations\":[{"
                + "\"op\":\"Replace\","
                + "\"path\":\"active\","
                + "\"value\":true"
                + "}]"
                + "}";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertTrue(user.isActive());

        // 3. update with path, replace simple value
        assertNotEquals("newSurname", user.getName().getFamilyName());
        body =
                "{"
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "\"Operations\":["
                + "{"
                + "\"op\":\"Replace\","
                + "\"path\":\"name.familyName\","
                + "\"value\":\"newSurname\""
                + "},"
                + "{"
                + "\"op\":\"remove\","
                + "\"path\":\"nickName\""
                + "}"
                + "]"
                + "}";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertEquals("newSurname", user.getName().getFamilyName());
        assertNull(user.getNickName());

        // 4. update with path, replace complex value
        String newMail = UUID.randomUUID().toString() + "@syncope.apache.org";
        assertNotEquals(
                newMail,
                user.getEmails().stream().filter(v -> "work".equals(v.getType())).findFirst().get().getValue());
        body =
                "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"replace\","
                + "       \"path\":\"emails[type eq \\\"work\\\"]\","
                + "       \"value\":"
                + "       {"
                + "         \"type\": \"work\","
                + "         \"value\": \"" + newMail + "\","
                + "         \"primary\": true"
                + "       }"
                + "     }]"
                + "   }";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertEquals(
                newMail,
                user.getEmails().stream().filter(v -> "work".equals(v.getType())).findFirst().get().getValue());

        // 5. update with path, filter and sub
        newMail = "verycomplex" + UUID.randomUUID().toString() + "@syncope.apache.org";
        body =
                "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"replace\","
                + "       \"path\":\"emails[type eq \\\"work\\\"].value\","
                + "       \"value\":\"" + newMail + "\""
                + "     }]"
                + "   }";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertEquals(
                newMail,
                user.getEmails().stream().filter(v -> "work".equals(v.getType())).findFirst().get().getValue());

        // 6. remove with path and filter
        body =
                "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"remove\","
                + "       \"path\":\"emails[type eq \\\"home\\\"]\""
                + "     }]"
                + "   }";
        response = webClient().path("Users").path(user.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertTrue(user.getEmails().stream().noneMatch(v -> "home".equals(v.getType())));
    }

    @Test
    public void replaceUser() {
        SCIM_CONF_SERVICE.set(CONF);

        SCIMUser user = getSampleUser(UUID.randomUUID().toString(), List.of(Resource.User.schema()));

        Response response = webClient().path("Users").post(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user.getId());

        user.getName().setFormatted("new" + user.getUserName());

        response = webClient().path("Users").path(user.getId()).put(user);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertTrue(user.getName().getFormatted().startsWith("new"));
    }

    @Test
    public void deleteUser() {
        SCIM_CONF_SERVICE.set(CONF);

        SCIMUser user = getSampleUser(UUID.randomUUID().toString(), List.of(Resource.User.schema()));

        Response response = webClient().path("Users").post(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(SCIMUser.class);
        assertNotNull(user.getId());

        response = webClient().path("Users").path(user.getId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = webClient().path("Users").path(user.getId()).delete();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = webClient().path("Users").path(user.getId()).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void createGroup() {
        String displayName = UUID.randomUUID().toString();

        SCIMGroup group = new SCIMGroup(null, null, displayName);
        group.getMembers().add(new Member("1417acbe-cbf6-4277-9372-e75e04f97000", null, null));
        assertNull(group.getId());
        assertEquals(displayName, group.getDisplayName());

        Response response = webClient().path("Groups").post(group);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertNotNull(group.getId());
        assertTrue(response.getLocation().toASCIIString().endsWith(group.getId()));
        assertEquals(1, group.getMembers().size());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", group.getMembers().get(0).getValue());

        response = webClient().path("Users").path("1417acbe-cbf6-4277-9372-e75e04f97000").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        SCIMUser user = response.readEntity(SCIMUser.class);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", user.getId());

        response = webClient().path("Groups").post(group);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        SCIMError error = response.readEntity(SCIMError.class);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), error.getStatus());
        assertEquals(ErrorType.uniqueness, error.getScimType());
    }

    @Test
    public void updateGroup() {
        SCIM_CONF_SERVICE.set(CONF);

        SCIMGroup group = new SCIMGroup(null, null, UUID.randomUUID().toString());
        group.getMembers().add(new Member("74cd8ece-715a-44a4-a736-e17b46c4e7e6", null, null));
        group.getMembers().add(new Member("1417acbe-cbf6-4277-9372-e75e04f97000", null, null));
        Response response = webClient().path("Groups").post(group);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertNotNull(group.getId());
        assertNull(group.getExternalId());
        assertEquals(2, group.getMembers().size());

        // 1. update with path, add value
        String body =
                "{"
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "\"Operations\":[{"
                + "\"op\":\"Add\","
                + "\"path\":\"externalId\","
                + "\"value\":\"" + group.getId() + "\""
                + "}]"
                + "}";
        response = webClient().path("Groups").path(group.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertEquals(group.getId(), group.getExternalId());

        // 2. add member, remove member, remove attribute
        body =
                "{"
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "\"Operations\":["
                + "{"
                + "\"op\":\"Add\","
                + "\"path\":\"members\","
                + "\"value\":[{"
                + "\"value\":\"b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee\"}]"
                + "},"
                + "{"
                + "\"op\":\"remove\","
                + "\"path\":\"members[value eq \\\"74cd8ece-715a-44a4-a736-e17b46c4e7e6\\\"]\""
                + "},"
                + "{"
                + "\"op\":\"remove\","
                + "\"path\":\"externalId\""
                + "}"
                + "]"
                + "}";
        response = webClient().path("Groups").path(group.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertEquals(2, group.getMembers().size());
        assertTrue(group.getMembers().stream().
                anyMatch(m -> "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee".equals(m.getValue())));
        assertTrue(group.getMembers().stream().
                anyMatch(m -> "1417acbe-cbf6-4277-9372-e75e04f97000".equals(m.getValue())));
        assertNull(group.getExternalId());

        // 3. remove all members
        body =
                "{"
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "\"Operations\":["
                + "{"
                + "\"op\":\"remove\","
                + "\"path\":\"members\""
                + "}"
                + "]"
                + "}";
        response = webClient().path("Groups").path(group.getId()).invoke(HttpMethod.PATCH, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertTrue(group.getMembers().isEmpty());
    }

    @Test
    public void replaceGroup() {
        SCIMGroup group = new SCIMGroup(null, null, UUID.randomUUID().toString());
        group.getMembers().add(new Member("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee", null, null));
        Response response = webClient().path("Groups").post(group);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertNotNull(group.getId());
        assertEquals(1, group.getMembers().size());
        assertEquals("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee", group.getMembers().get(0).getValue());

        group.setDisplayName("other" + group.getId());
        group.getMembers().add(new Member("c9b2dec2-00a7-4855-97c0-d854842b4b24", null, null));

        response = webClient().path("Groups").path(group.getId()).put(group);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertTrue(group.getDisplayName().startsWith("other"));
        assertEquals(2, group.getMembers().size());

        group.getMembers().clear();
        group.getMembers().add(new Member("c9b2dec2-00a7-4855-97c0-d854842b4b24", null, null));

        response = webClient().path("Groups").path(group.getId()).put(group);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertEquals(1, group.getMembers().size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", group.getMembers().get(0).getValue());
    }

    @Test
    public void deleteGroup() {
        SCIMGroup group = new SCIMGroup(null, null, UUID.randomUUID().toString());
        Response response = webClient().path("Groups").post(group);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        group = response.readEntity(SCIMGroup.class);
        assertNotNull(group.getId());

        response = webClient().path("Groups").path(group.getId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = webClient().path("Groups").path(group.getId()).delete();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = webClient().path("Groups").path(group.getId()).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
