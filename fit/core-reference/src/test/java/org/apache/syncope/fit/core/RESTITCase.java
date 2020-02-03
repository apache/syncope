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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.util.List;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.BasicAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class RESTITCase extends AbstractITCase {

    @Test
    public void unauthorizedOrForbidden() {
        // service as admin: it works
        List<ConnInstanceTO> connectors = connectorService.list(null);
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty());

        // service with bad password: 401 unauthorized
        try {
            clientFactory.create("bellini", "passwor");
            fail("This should not happen");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // service with invalid JWT string: 401 unauthorized
        try {
            clientFactory.create(RandomStringUtils.random(20, true, true)).self();
            fail("This should not happen");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // service with good password, but no entitlements owned: 403 forbidden
        SyncopeClient goodClient = clientFactory.create("bellini", "password");
        try {
            goodClient.getService(ConnectorService.class).list(null);
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void noContent() throws IOException {
        SyncopeClient noContentclient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        GroupService noContentService = SyncopeClient.prefer(
                noContentclient.getService(GroupService.class), Preference.RETURN_NO_CONTENT);

        GroupCR groupCR = GroupITCase.getSample("noContent");

        Response response = noContentService.create(groupCR);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(
                StringUtils.EMPTY,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        GroupTO group = getObject(response.getLocation(), GroupService.class, GroupTO.class);
        assertNotNull(group);

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(group.getKey());
        groupUR.getPlainAttrs().add(attrAddReplacePatch("badge", "xxxxxxxxxx"));

        response = noContentService.update(groupUR);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(
                StringUtils.EMPTY,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        response = noContentService.delete(group.getKey());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(
                StringUtils.EMPTY,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));
    }

    @Test
    public void ifMatch() {
        UserTO userTO = userService.create(UserITCase.getUniqueSample("ifmatch@syncope.apache.org")).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(userTO);
        assertNotNull(userTO.getKey());

        EntityTag etag = SyncopeClient.getLatestEntityTag(userService);
        assertNotNull(etag);
        assertTrue(StringUtils.isNotBlank(etag.getValue()));

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setUsername(new StringReplacePatchItem.Builder().value(userTO.getUsername() + "XX").build());
        userTO = userService.update(userUR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertTrue(userTO.getUsername().endsWith("XX"));
        EntityTag etag1 = SyncopeClient.getLatestEntityTag(userService);
        assertFalse(etag.getValue().equals(etag1.getValue()));

        UserService ifMatchService = SyncopeClient.ifMatch(adminClient.getService(UserService.class), etag);
        userUR.setUsername(new StringReplacePatchItem.Builder().value(userTO.getUsername() + "YY").build());
        try {
            ifMatchService.update(userUR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.ConcurrentModification, e.getType());
        }

        userTO = userService.read(userTO.getKey());
        assertTrue(userTO.getUsername().endsWith("XX"));
    }

    @Test
    public void defaultContentType() {
        // manualy instantiate SyncopeClient so that media type can be set to */*
        SyncopeClientFactoryBean factory = new SyncopeClientFactoryBean().setAddress(ADDRESS);
        SyncopeClient client = new SyncopeClient(
                MediaType.WILDCARD_TYPE,
                factory.getRestClientFactoryBean(),
                factory.getExceptionMapper(),
                new BasicAuthenticationHandler(ADMIN_UNAME, ADMIN_PWD),
                false,
                null);

        // perform operation
        AnyTypeClassService service = client.getService(AnyTypeClassService.class);
        service.list();

        // check that */* was actually sent
        MultivaluedMap<String, String> requestHeaders = WebClient.client(service).getHeaders();
        assertEquals(MediaType.WILDCARD, requestHeaders.getFirst(HttpHeaders.ACCEPT));

        // check that application/json was received
        String contentType = WebClient.client(service).getResponse().getHeaderString(HttpHeaders.CONTENT_TYPE);
        assertTrue(contentType.startsWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void exportInternalStorageContent() throws IOException {
        Response response = syncopeService.exportInternalStorageContent();
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_XML));
        String contentDisposition = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        String configExport = IOUtils.toString((InputStream) entity, StandardCharsets.UTF_8.name());
        assertFalse(configExport.isEmpty());
        assertTrue(configExport.length() > 1000);
    }
}
