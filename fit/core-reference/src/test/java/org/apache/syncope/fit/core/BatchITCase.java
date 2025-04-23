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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.lib.batch.BatchResponse;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
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

    private static String requestBody(final String boundary) throws JsonProcessingException {
        List<BatchRequestItem> reqItems = new ArrayList<>();

        // 1. create user as YAML
        UserCR userCR = UserITCase.getUniqueSample("batch@syncope.apache.org");
        assertNotEquals("/odd", userCR.getRealm());
        String createUserPayload = YAML_MAPPER.writeValueAsString(userCR);

        BatchRequestItem createUser = new BatchRequestItem();
        createUser.setMethod(HttpMethod.POST);
        createUser.setRequestURI("/users");
        createUser.setHeaders(new HashMap<>());
        createUser.getHeaders().put(HttpHeaders.ACCEPT, List.of(RESTHeaders.APPLICATION_YAML));
        createUser.getHeaders().put(HttpHeaders.CONTENT_TYPE, List.of(RESTHeaders.APPLICATION_YAML));
        createUser.getHeaders().put(HttpHeaders.CONTENT_LENGTH, List.of(createUserPayload.length()));
        createUser.setContent(createUserPayload);
        reqItems.add(createUser);

        // 2. create group as XML
        GroupCR groupCR = GroupITCase.getBasicSample("batch");
        String createGroupPayload = XML_MAPPER.writeValueAsString(groupCR);

        BatchRequestItem createGroup = new BatchRequestItem();
        createGroup.setMethod(HttpMethod.POST);
        createGroup.setRequestURI("/groups");
        createGroup.setHeaders(new HashMap<>());
        createGroup.getHeaders().put(HttpHeaders.ACCEPT, List.of(MediaType.APPLICATION_XML));
        createGroup.getHeaders().put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_XML));
        createGroup.getHeaders().put(HttpHeaders.CONTENT_LENGTH, List.of(createGroupPayload.length()));
        createGroup.setContent(createGroupPayload);
        reqItems.add(createGroup);

        // 3. update the user above as JSON, request for no user data being returned
        UserUR userUR = new UserUR();
        userUR.setKey(userCR.getUsername());
        userUR.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
        String updateUserPayload = JSON_MAPPER.writeValueAsString(userUR);

        BatchRequestItem updateUser = new BatchRequestItem();
        updateUser.setMethod(HttpMethod.PATCH);
        updateUser.setRequestURI("/users/" + userCR.getUsername());
        updateUser.setHeaders(new HashMap<>());
        updateUser.getHeaders().put(RESTHeaders.PREFER, List.of(Preference.RETURN_NO_CONTENT.toString()));
        updateUser.getHeaders().put(HttpHeaders.ACCEPT, List.of(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_LENGTH, List.of(updateUserPayload.length()));
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
        deleteGroup.setRequestURI("/groups/" + groupCR.getName());
        reqItems.add(deleteGroup);

        String body = BatchPayloadGenerator.generate(reqItems, boundary);
        LOG.debug("Batch request body:\n{}", body);

        return body;
    }

    private static void check(final List<BatchResponseItem> resItems) throws IOException {
        assertEquals(6, resItems.size());

        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.getFirst().getStatus());
        assertNotNull(resItems.getFirst().getHeaders().get(HttpHeaders.LOCATION));
        assertNotNull(resItems.getFirst().getHeaders().get(HttpHeaders.ETAG));
        assertNotNull(resItems.getFirst().getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.getFirst().getHeaders().get(RESTHeaders.RESOURCE_KEY));
        assertEquals(RESTHeaders.APPLICATION_YAML, resItems.getFirst().
                getHeaders().get(HttpHeaders.CONTENT_TYPE).getFirst());
        ProvisioningResult<UserTO> user = YAML_MAPPER.readValue(
                resItems.get(0).getContent(), new TypeReference<>() {
        });
        assertNotNull(user.getEntity().getKey());

        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.get(1).getStatus());
        assertNotNull(resItems.get(1).getHeaders().get(HttpHeaders.LOCATION));
        assertNotNull(resItems.get(1).getHeaders().get(HttpHeaders.ETAG));
        assertNotNull(resItems.get(1).getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.get(1).getHeaders().get(RESTHeaders.RESOURCE_KEY));
        assertEquals(MediaType.APPLICATION_XML, resItems.get(1).getHeaders().get(HttpHeaders.CONTENT_TYPE).getFirst());

        ProvisioningResult<GroupTO> group = XML_MAPPER.readValue(
                resItems.get(1).getContent(), new TypeReference<>() {
        });
        assertNotNull(group.getEntity().getKey());

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resItems.get(2).getStatus());
        assertNotNull(resItems.get(2).getHeaders().get(RESTHeaders.DOMAIN));
        assertEquals(
                Preference.RETURN_NO_CONTENT.toString(),
                resItems.get(2).getHeaders().get(RESTHeaders.PREFERENCE_APPLIED).getFirst());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(3).getStatus());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(4).getStatus());
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.DOMAIN));
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.ERROR_CODE));
        assertNotNull(resItems.get(4).getHeaders().get(RESTHeaders.ERROR_INFO));
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(4).getHeaders().get(HttpHeaders.CONTENT_TYPE).getFirst());

        assertEquals(Response.Status.OK.getStatusCode(), resItems.get(5).getStatus());
        assertNotNull(resItems.get(5).getHeaders().get(RESTHeaders.DOMAIN));
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(5).getHeaders().get(HttpHeaders.CONTENT_TYPE).getFirst());
        group = JSON_MAPPER.readValue(
                resItems.get(5).getContent(), new TypeReference<>() {
        });
        assertNotNull(group);
    }

    @Test
    public void webClientSync() throws IOException {
        String boundary = "--batch_" + UUID.randomUUID();

        Response response = WebClient.create(ADDRESS).path("batch").
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT()).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).
                post(requestBody(boundary));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));

        String body = response.readEntity(String.class);
        LOG.debug("Batch response body:\n{}", body);

        check(BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem()));
    }

    @Test
    public void webClientAsync() throws IOException {
        String boundary = "--batch_" + UUID.randomUUID();

        // request async processing
        Response response = WebClient.create(ADDRESS).path("batch").
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT()).
                header(RESTHeaders.PREFER, Preference.RESPOND_ASYNC).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).
                post(requestBody(boundary));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));
        assertEquals(Preference.RESPOND_ASYNC.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        URI monitor = response.getLocation();
        assertNotNull(monitor);

        WebClient client = WebClient.create(monitor).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT()).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2)));

        Mutable<Response> holder = new MutableObject<>();
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                holder.setValue(client.get());
                return holder.getValue().getStatus() != Response.Status.ACCEPTED.getStatusCode();
            } catch (Exception e) {
                return false;
            }
        });
        response = holder.getValue();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().
                startsWith(RESTHeaders.multipartMixedWith(boundary.substring(2))));

        String body = response.readEntity(String.class);
        LOG.debug("Batch response body:\n{}", body);

        check(BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem()));

        // check results again: removed since they were returned above
        response = WebClient.create(monitor).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT()).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2))).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private static BatchRequest batchRequest() {
        BatchRequest batchRequest = ADMIN_CLIENT.batch();

        // 1. create user as YAML
        UserService batchUserService = batchRequest.getService(UserService.class);
        Client client = WebClient.client(batchUserService).reset();
        client.type(RESTHeaders.APPLICATION_YAML).accept(RESTHeaders.APPLICATION_YAML);
        UserCR userCR = UserITCase.getUniqueSample("batch@syncope.apache.org");
        assertNotEquals("/odd", userCR.getRealm());
        batchUserService.create(userCR);

        // 2. create group as XML
        GroupService batchGroupService = batchRequest.getService(GroupService.class);
        client = WebClient.client(batchGroupService).reset();
        client.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        GroupCR groupCR = GroupITCase.getBasicSample("batch");
        batchGroupService.create(groupCR);

        // 3. update the user above as JSON, request for no user data being returned
        client = WebClient.client(batchUserService).reset();
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        client.header(RESTHeaders.PREFER, Preference.RETURN_NO_CONTENT.toString());
        UserUR userUR = new UserUR();
        userUR.setKey(userCR.getUsername());
        userUR.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
        batchUserService.update(userUR);

        // 4. generate not found
        batchRequest.getService(ResourceService.class).read(UUID.randomUUID().toString());

        // 5. attempt to delete an unexisting group
        client = WebClient.client(batchGroupService).reset();
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        batchGroupService.delete(UUID.randomUUID().toString());

        // 6, delete the group created above, expect deleted group as JSON
        batchGroupService.delete(groupCR.getName());

        return batchRequest;
    }

    @Test
    public void syncopeClientSync() throws IOException {
        BatchResponse batchResponse = batchRequest().commit();

        Response response = batchResponse.getResponse();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(RESTHeaders.MULTIPART_MIXED));

        check(batchResponse.getItems());
    }

    @Test
    public void syncopeClientAsync() throws IOException {
        // request async processing
        BatchResponse batchResponse = batchRequest().commit(true);

        Response response = batchResponse.getResponse();
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(RESTHeaders.MULTIPART_MIXED));

        await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).
                until(() -> batchResponse.poll().getStatus() == Response.Status.OK.getStatusCode());

        check(batchResponse.getItems());

        // check results again: removed since they were returned above
        response = batchResponse.poll();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
