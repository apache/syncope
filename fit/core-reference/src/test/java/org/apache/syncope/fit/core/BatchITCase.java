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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.lib.batch.BatchResponse;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class BatchITCase extends AbstractITCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String requestBody(final String boundary) throws JsonProcessingException, JAXBException {
        List<BatchRequestItem> reqItems = new ArrayList<>();

        // 1. create user as JSON
        UserTO user = UserITCase.getUniqueSampleTO("batch@syncope.apache.org");
        assertNotEquals("/odd", user.getRealm());
        String createUserPayload = MAPPER.writeValueAsString(user);

        BatchRequestItem createUser = new BatchRequestItem();
        createUser.setMethod(HttpMethod.POST);
        createUser.setRequestURI("/users");
        createUser.setHeaders(new HashMap<>());
        createUser.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_JSON));
        createUser.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_JSON));
        createUser.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList(createUserPayload.length()));
        createUser.setContent(createUserPayload);
        reqItems.add(createUser);

        // 2. create group as XML
        GroupTO group = GroupITCase.getBasicSampleTO("batch");
        JAXBContext context = JAXBContext.newInstance(GroupTO.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(group, writer);
        String createGroupPayload = writer.toString();

        BatchRequestItem createGroup = new BatchRequestItem();
        createGroup.setMethod(HttpMethod.POST);
        createGroup.setRequestURI("/groups");
        createGroup.setHeaders(new HashMap<>());
        createGroup.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_XML));
        createGroup.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_XML));
        createGroup.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList(createGroupPayload.length()));
        createGroup.setContent(createGroupPayload);
        reqItems.add(createGroup);

        // 3. update the user above as JSON, request for no user data being returned
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getUsername());
        userPatch.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
        String updateUserPayload = MAPPER.writeValueAsString(userPatch);

        BatchRequestItem updateUser = new BatchRequestItem();
        updateUser.setMethod(HttpMethod.PATCH);
        updateUser.setRequestURI("/users/" + user.getUsername());
        updateUser.setHeaders(new HashMap<>());
        updateUser.getHeaders().put(RESTHeaders.PREFER, Arrays.asList(Preference.RETURN_NO_CONTENT.toString()));
        updateUser.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList(updateUserPayload.length()));
        updateUser.setContent(updateUserPayload);
        reqItems.add(updateUser);

        // 4. attempt to invoke an unexisting endpoint
        BatchRequestItem endpointNotFound = new BatchRequestItem();
        endpointNotFound.setMethod(HttpMethod.PATCH);
        endpointNotFound.setRequestURI("/missing");
        reqItems.add(endpointNotFound);

        // 5. attempt to delete an unexisting group
        BatchRequestItem groupNotFound = new BatchRequestItem();
        groupNotFound.setMethod(HttpMethod.DELETE);
        groupNotFound.setRequestURI("/groups/" + UUID.randomUUID());
        reqItems.add(groupNotFound);

        // 6, delete the group created above, expect deleted group as JSON
        BatchRequestItem deleteGroup = new BatchRequestItem();
        deleteGroup.setMethod(HttpMethod.DELETE);
        deleteGroup.setRequestURI("/groups/" + group.getName());
        reqItems.add(deleteGroup);

        String body = BatchPayloadGenerator.generate(reqItems, boundary);
        LOG.debug("Batch request body:\n{}", body);

        return body;
    }

    private void check(final List<BatchResponseItem> resItems) throws IOException, JAXBException {
        assertEquals(6, resItems.size());

        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.get(0).getStatus());
        assertNotNull(resItems.get(0).getHeaders().get(HttpHeaders.LOCATION));
        assertNotNull(resItems.get(0).getHeaders().get(HttpHeaders.ETAG));
        assertNotNull(resItems.get(0).getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.get(0).getHeaders().get(RESTHeaders.RESOURCE_KEY));
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(0).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        ProvisioningResult<UserTO> user = MAPPER.readValue(
                resItems.get(0).getContent(), new TypeReference<ProvisioningResult<UserTO>>() {
        });
        assertNotNull(user.getEntity().getKey());

        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.get(1).getStatus());
        assertNotNull(resItems.get(1).getHeaders().get(HttpHeaders.LOCATION));
        assertNotNull(resItems.get(1).getHeaders().get(HttpHeaders.ETAG));
        assertNotNull(resItems.get(1).getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.get(1).getHeaders().get(RESTHeaders.RESOURCE_KEY));
        assertEquals(MediaType.APPLICATION_XML, resItems.get(1).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));

        JAXBContext context = JAXBContext.newInstance(ProvisioningResult.class, GroupTO.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        @SuppressWarnings("unchecked")
        ProvisioningResult<GroupTO> group = (ProvisioningResult<GroupTO>) unmarshaller.unmarshal(
                IOUtils.toInputStream(resItems.get(1).getContent(), StandardCharsets.UTF_8));
        assertNotNull(group.getEntity().getKey());

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resItems.get(2).getStatus());
        assertNotNull(resItems.get(2).getHeaders().get(RESTHeaders.DOMAIN));
        assertEquals(
                Preference.RETURN_NO_CONTENT.toString(),
                resItems.get(2).getHeaders().get(RESTHeaders.PREFERENCE_APPLIED).get(0));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(3).getStatus());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(4).getStatus());
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.ERROR_CODE));
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.ERROR_INFO));
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(4).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));

        assertEquals(Response.Status.OK.getStatusCode(), resItems.get(5).getStatus());
        assertNotNull(resItems.get(5).getHeaders().get(RESTHeaders.DOMAIN));
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(5).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        group = MAPPER.readValue(
                resItems.get(5).getContent(), new TypeReference<ProvisioningResult<GroupTO>>() {
        });
        assertNotNull(group);
    }

    @Test
    public void webClientSync() throws IOException, JAXBException {
        String boundary = "--batch_" + UUID.randomUUID().toString();

        Response response = WebClient.create(ADDRESS).path("batch").
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT()).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).
                post(requestBody(boundary));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));

        String body = IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8);
        LOG.debug("Batch response body:\n{}", body);

        check(BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem()));
    }

    @Test
    public void webClientAsync() throws IOException, JAXBException {
        String boundary = "--batch_" + UUID.randomUUID().toString();

        // request async processing
        Response response = WebClient.create(ADDRESS).path("batch").
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT()).
                header(RESTHeaders.PREFER, Preference.RESPOND_ASYNC).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).
                post(requestBody(boundary));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));
        assertEquals(Preference.RESPOND_ASYNC.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        URI monitor = response.getLocation();
        assertNotNull(monitor);

        for (int i = 0; i < 10 && response.getStatus() == Response.Status.ACCEPTED.getStatusCode(); i++) {
            // wait a bit...
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            // check results
            response = WebClient.create(monitor).
                    header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT()).
                    type(RESTHeaders.multipartMixedWith(boundary.substring(2))).get();
        }
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));

        String body = IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8);
        LOG.debug("Batch response body:\n{}", body);

        check(BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem()));

        // check results again: removed since they were returned above
        response = WebClient.create(monitor).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT()).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private BatchRequest batchRequest() {
        BatchRequest batchRequest = adminClient.batch();

        // 1. create user as JSON
        UserService batchUserService = batchRequest.getService(UserService.class);
        UserTO user = UserITCase.getUniqueSampleTO("batch@syncope.apache.org");
        assertNotEquals("/odd", user.getRealm());
        batchUserService.create(user, true);

        // 2. create group as XML
        GroupService batchGroupService = batchRequest.getService(GroupService.class);
        Client client = WebClient.client(batchGroupService).reset();
        client.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        GroupTO group = GroupITCase.getBasicSampleTO("batch");
        batchGroupService.create(group);

        // 3. update the user above as JSON, request for no user data being returned
        client = WebClient.client(batchUserService).reset();
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        client.header(RESTHeaders.PREFER, Preference.RETURN_NO_CONTENT.toString());
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getUsername());
        userPatch.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
        batchUserService.update(userPatch);

        // 4. generate not found
        batchRequest.getService(ResourceService.class).read(UUID.randomUUID().toString());

        // 5. attempt to delete an unexisting group
        client = WebClient.client(batchGroupService).reset();
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        batchGroupService.delete(UUID.randomUUID().toString());

        // 6, delete the group created above, expect deleted group as JSON
        batchGroupService.delete(group.getName());

        return batchRequest;
    }

    @Test
    public void syncopeClientSync() throws IOException, JAXBException {
        BatchResponse batchResponse = batchRequest().commit();

        Response response = batchResponse.getResponse();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(RESTHeaders.MULTIPART_MIXED));

        check(batchResponse.getItems());
    }

    @Test
    public void syncopeClientAsync() throws IOException, JAXBException {
        // request async processing
        BatchResponse batchResponse = batchRequest().commit(true);

        Response response = batchResponse.getResponse();
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(RESTHeaders.MULTIPART_MIXED));

        for (int i = 0; i < 10 && response.getStatus() == Response.Status.ACCEPTED.getStatusCode(); i++) {
            // wait a bit...
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            // check results
            response = batchResponse.poll();
        }
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(RESTHeaders.MULTIPART_MIXED));

        check(batchResponse.getItems());

        // check results again: removed since they were returned above
        response = batchResponse.poll();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
