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
package org.apache.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.to.MappingItemTO;
import org.apache.syncope.client.to.MappingTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.types.ConnConfPropSchema;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ResourceTestITCase extends AbstractTest {

    @Test
    public void getPropagationActionsClasses() {
        Set<String> actions = restTemplate.getForObject(
                BASE_URL + "resource/propagationActionsClasses.json", Set.class);
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        String resourceName = "ws-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
        assertNotNull(actual);

        // check for existence
        actual = restTemplate.getForObject(BASE_URL + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createOverridingProps() {
        String resourceName = "overriding-conn-conf-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        mapping.addItem(item);

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        resourceTO.setUmapping(mapping);

        ConnConfProperty p = new ConnConfProperty();
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setType("java.lang.String");
        schema.setName("endpoint");
        schema.setRequired(true);
        p.setSchema(schema);
        p.setValues(Collections.singletonList("http://invalidurl/"));

        Set<ConnConfProperty> connectorConfigurationProperties = new HashSet<ConnConfProperty>(Arrays.asList(p));
        resourceTO.setConnectorConfigurationProperties(connectorConfigurationProperties);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(BASE_URL + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createWithSingleMapping() {
        String resourceName = "ws-target-resource-create-single";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
        assertNotNull(actual);
    }

    @Test
    public void createWithInvalidMapping() {
        String resourceName = "ws-target-resource-create-wrong";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setExtAttrName("email");
        // missing intAttrName ...
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);

        Throwable t = null;
        try {
            restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;

            SyncopeClientException requiredValueMissing = e
                    .getException(SyncopeClientExceptionType.RequiredValuesMissing);
            assertNotNull(requiredValueMissing);
            assertNotNull(requiredValueMissing.getElements());
            assertEquals(1, requiredValueMissing.getElements().size());
            assertEquals("intAttrName", requiredValueMissing.getElements().iterator().next());
        }
        assertNotNull(t);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithoutExtAttr() {
        String resourceName = "ws-target-resource-create-wrong";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setIntAttrName("usernane");
        // missing extAttrName ...
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);

        restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
    }

    @Test
    public void createWithPasswordPolicy() {
        String resourceName = "res-with-password-policy";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);
        resourceTO.setPasswordPolicy(4L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(BASE_URL + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void updateWithException() {
        try {
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setName("resourcenotfound");

            restTemplate.postForObject(BASE_URL + "resource/update.json", resourceTO, ResourceTO.class);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void update() {
        final String resourceName = "ws-target-resource-update";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(101L);

        MappingTO mapping = new MappingTO();

        // Update with an existing and already assigned mapping
        MappingItemTO item = new MappingItemTO();
        item.setId(112L);
        item.setExtAttrName("test3");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            item = new MappingItemTO();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("fullname");
            item.setIntMappingType(IntMappingType.UserSchema);
            mapping.addItem(item);
        }
        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/update.json", resourceTO, ResourceTO.class);
        assertNotNull(actual);

        // check for existence
        Collection<MappingItemTO> mapItems = actual.getUmapping().getItems();
        assertNotNull(mapItems);
        assertEquals(4, mapItems.size());
    }

    @Test
    public void deleteWithException() {
        try {
            restTemplate.getForObject(
                    BASE_URL + "resource/delete/{resourceName}.json", ResourceTO.class, "resourcenotfound");
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void updateResetSyncToken() {
        // pre condition: sync token is set
        String resourceName = "ws-target-resource-update-resetsynctoken";
        ResourceTO pre = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
        assertNotNull(pre.getUsyncToken());

        pre.setUsyncToken(null);

        ResourceTO actual = restTemplate.postForObject(BASE_URL + "resource/update.json", pre, ResourceTO.class);

        // check that the synctoken has been reset
        assertNull(actual.getUsyncToken());
    }

    @Test
    public void delete() {
        final String resourceName = "ws-target-resource-delete";

        ResourceTO deletedResource =
                restTemplate.getForObject(BASE_URL + "resource/delete/{resourceName}.json", ResourceTO.class,
                resourceName);
        assertNotNull(deletedResource);

        try {
            restTemplate.getForObject(BASE_URL + "resource/read/{resourceName}.json", ResourceTO.class, resourceName);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = Arrays.asList(restTemplate.getForObject(BASE_URL + "resource/list.json",
                ResourceTO[].class));
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
        }
    }

    @Test
    public void listByType() {
        List<ResourceTO> actuals = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "resource/list.json?connInstanceId=105", ResourceTO[].class));

        assertNotNull(actuals);
        assertEquals(1, actuals.size());
        assertNotNull(actuals.get(0));
    }

    @Test
    public void read() {
        ResourceTO actual = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json",
                ResourceTO.class, "resource-testdb");

        assertNotNull(actual);
    }
}
