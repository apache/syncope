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

import static org.apache.syncope.core.rest.AbstractTest.getUUIDString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import org.apache.syncope.common.to.BulkAction;

import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class ResourceTestITCase extends AbstractTest {

    @Test
    public void getPropagationActionsClasses() {
        Set<PropagationActionClassTO> actions = resourceService.getPropagationActionsClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        String resourceName = "ws-target-resource-create";
        ResourceTO resourceTO = buildResourceTO(resourceName);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response, ResourceTO.class, resourceService);
        assertNotNull(actual);

        // check for existence
        actual = resourceService.read(resourceName);
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
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        item.setPurpose(MappingPurpose.BOTH);
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

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response, ResourceTO.class, resourceService);
        assertNotNull(actual);

        // check the existence

        actual = resourceService.read(resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createWithSingleMappingItem() {
        String resourceName = "ws-target-resource-create-single";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO umapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        umapping.setAccountIdItem(item);

        resourceTO.setUmapping(umapping);

        MappingTO rmapping = new MappingTO();

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.SYNCHRONIZATION);
        rmapping.setAccountIdItem(item);

        resourceTO.setRmapping(rmapping);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response, ResourceTO.class, resourceService);

        assertNotNull(actual);
        assertNotNull(actual.getUmapping());
        assertNotNull(actual.getUmapping().getItems());
        assertNotNull(actual.getRmapping());
        assertNotNull(actual.getRmapping().getItems());
        assertEquals(MappingPurpose.SYNCHRONIZATION, actual.getRmapping().getAccountIdItem().getPurpose());
        assertEquals(MappingPurpose.PROPAGATION, actual.getUmapping().getAccountIdItem().getPurpose());
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

        try {
            createResource(resourceTO);
            fail("Create should not have worked");
        } catch (SyncopeClientCompositeErrorException e) {
            SyncopeClientException requiredValueMissing = e
                    .getException(SyncopeClientExceptionType.RequiredValuesMissing);
            assertNotNull(requiredValueMissing);
            assertNotNull(requiredValueMissing.getElements());
            assertEquals(1, requiredValueMissing.getElements().size());
            assertEquals("intAttrName", requiredValueMissing.getElements().iterator().next());
        }
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

        createResource(resourceTO);
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
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response, ResourceTO.class, resourceService);
        assertNotNull(actual);

        // check the existence

        actual = resourceService.read(resourceName);
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void updateWithException() {
        try {
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setName("resourcenotfound");

            resourceService.update(resourceTO.getName(), resourceTO);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void update() {
        String resourceName = "ws-target-resource-update";
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
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            item = new MappingItemTO();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("fullname");
            item.setIntMappingType(IntMappingType.UserSchema);
            item.setPurpose(MappingPurpose.BOTH);
            mapping.addItem(item);
        }
        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setAccountid(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        resourceTO.setUmapping(mapping);

        resourceService.update(resourceTO.getName(), resourceTO);
        ResourceTO actual = resourceService.read(resourceTO.getName());
        assertNotNull(actual);

        // check for existence
        Collection<MappingItemTO> mapItems = actual.getUmapping().getItems();
        assertNotNull(mapItems);
        assertEquals(4, mapItems.size());
    }

    @Test
    public void deleteWithException() {
        try {
            resourceService.delete("resourcenotfound");
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void updateResetSyncToken() {
        // create resource with sync token
        String resourceName = "ws-target-resource-update-resetsynctoken" + getUUIDString();
        ResourceTO pre = buildResourceTO(resourceName);
        pre.setUsyncToken("test");
        resourceService.create(pre);

        pre.setUsyncToken(null);
        resourceService.update(pre.getName(), pre);
        ResourceTO actual = resourceService.read(pre.getName());
        // check that the synctoken has been reset
        assertNull(actual.getUsyncToken());
    }

    @Test
    public void delete() {
        String resourceName = "tobedeleted";

        ResourceTO resource = buildResourceTO(resourceName);
        Response response = resourceService.create(resource);
        ResourceTO actual = getObject(response, ResourceTO.class, resourceService);
        assertNotNull(actual);

        resourceService.delete(resourceName);

        try {
            resourceService.read(resourceName);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = resourceService.list(null);
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
        }
    }

    @Test
    public void listByType() {
        List<ResourceTO> actuals = resourceService.list(105L);

        assertNotNull(actuals);
        assertEquals(1, actuals.size());
        assertNotNull(actuals.get(0));
    }

    @Test
    public void read() {
        ResourceTO actual = resourceService.read("resource-testdb");
        assertNotNull(actual);
    }

    @Test
    public void issueSYNCOPE323() {
        ResourceTO actual = resourceService.read("resource-testdb");
        assertNotNull(actual);

        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientCompositeErrorException scce) {
            assertEquals(HttpStatus.CONFLICT, scce.getStatusCode());
            assertTrue(scce.hasException(SyncopeClientExceptionType.EntityExists));
        }

        actual.setName(null);
        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientCompositeErrorException scce) {
            assertEquals(HttpStatus.BAD_REQUEST, scce.getStatusCode());
            assertTrue(scce.hasException(SyncopeClientExceptionType.RequiredValuesMissing));
        }
    }

    @Test
    public void bulkAction() {
        resourceService.create(buildResourceTO("forBulk1"));
        resourceService.create(buildResourceTO("forBulk2"));

        assertNotNull(resourceService.read("forBulk1"));
        assertNotNull(resourceService.read("forBulk2"));

        final BulkAction bulkAction = new BulkAction();
        bulkAction.setOperation(BulkAction.Type.DELETE);

        bulkAction.addTarget(String.valueOf("forBulk1"));
        bulkAction.addTarget(String.valueOf("forBulk2"));

        resourceService.bulkAction(bulkAction);

        try {
            resourceService.read("forBulk1");
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
        }

        try {
            resourceService.read("forBulk2");
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
        }
    }

    @Test
    public void issueSYNCOPE360() {
        final String name = "SYNCOPE360-" + getUUIDString();
        resourceService.create(buildResourceTO(name));

        ResourceTO resource = resourceService.read(name);
        assertNotNull(resource);
        assertNotNull(resource.getUmapping());

        resource.setUmapping(new MappingTO());
        resourceService.update(name, resource);

        resource = resourceService.read(name);
        assertNotNull(resource);
        assertNull(resource.getUmapping());
    }

    @Test
    public void issueSYNCOPE368() {
        final String name = "SYNCOPE368-" + getUUIDString();

        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(name);
        resourceTO.setConnectorId(105L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleName);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.RoleOwnerSchema);
        item.setExtAttrName("owner");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        resourceTO.setRmapping(mapping);
        
        resourceTO = createResource(resourceTO);
        assertNotNull(resourceTO);
        assertEquals(2, resourceTO.getRmapping().getItems().size());
    }

    private ResourceTO buildResourceTO(String resourceName) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        MappingTO mapping = new MappingTO();

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserId);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setAccountid(false);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        resourceTO.setUmapping(mapping);
        return resourceTO;
    }
}
