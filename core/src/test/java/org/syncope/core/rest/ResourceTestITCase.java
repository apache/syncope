/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.IntMappingType;
import org.syncope.types.SyncopeClientExceptionType;

public class ResourceTestITCase extends AbstractTest {

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createExistingResource() {
        final String resourceName = "ws-target-resource-1";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(100L);

        restTemplate.postForObject(BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);
    }

    @Test
    public void create() {
        String resourceName = "ws-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("uid");
        schemaMappingTO.setIntAttrName("userId");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("username");
        schemaMappingTO.setIntAttrName("fullname");
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("fullname");
        schemaMappingTO.setIntAttrName("cn");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setAccountid(false);
        resourceTO.addMapping(schemaMappingTO);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(
                BASE_URL + "resource/read/{resourceName}.json",
                ResourceTO.class,
                resourceName);

        assertNotNull(actual);
    }

    @Test
    public void createOverridingProps() {
        String resourceName = "overriding-conn-conf-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("uid");
        schemaMappingTO.setIntAttrName("userId");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("username");
        schemaMappingTO.setIntAttrName("fullname");
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("fullname");
        schemaMappingTO.setIntAttrName("cn");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setAccountid(false);
        resourceTO.addMapping(schemaMappingTO);

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        ConnConfProperty p = new ConnConfProperty();
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setType("java.lang.String");
        schema.setName("endpoint");
        schema.setRequired(true);
        p.setSchema(schema);
        p.setValue("http://invalidurl/");

        Set<ConnConfProperty> connectorConfigurationProperties =
                new HashSet<ConnConfProperty>(Arrays.asList(p));

        resourceTO.setConnectorConfigurationProperties(
                connectorConfigurationProperties);

        ResourceTO actual = restTemplate.postForObject(BASE_URL
                + "resource/create.json", resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(BASE_URL
                + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);

        assertNotNull(actual);
    }

    @Test
    public void createWithSingleMapping() {
        String resourceName = "ws-target-resource-create-single";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);
    }

    @Test
    public void createWithWrongMapping() {
        String resourceName = "ws-target-resource-create-wrong";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setExtAttrName("email");
        // missing intAttrName ...
        resourceTO.addMapping(schemaMappingTO);


        Throwable t = null;

        try {

            restTemplate.postForObject(
                    BASE_URL + "resource/create.json",
                    resourceTO, ResourceTO.class);

        } catch (SyncopeClientCompositeErrorException e) {
            t = e;

            SyncopeClientException requiredValueMissing = e.getException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
            assertNotNull(requiredValueMissing);
            assertNotNull(requiredValueMissing.getElements());
            assertEquals(1, requiredValueMissing.getElements().size());
            assertEquals("intAttrName",
                    requiredValueMissing.getElements().iterator().next());
        }
        assertNotNull(t);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithoutExtAttr() {
        String resourceName = "ws-target-resource-create-wrong";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setIntAttrName("usernane");
        // missing extAttrName ...
        resourceTO.addMapping(schemaMappingTO);

        restTemplate.postForObject(
                BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);
    }

    @Test
    public void createWithPasswordPolicy() {
        String resourceName = "res-with-password-policy";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);
        resourceTO.setPasswordPolicy(4L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("uid");
        schemaMappingTO.setIntAttrName("userId");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(
                BASE_URL + "resource/read/{resourceName}.json",
                ResourceTO.class,
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

            restTemplate.postForObject(BASE_URL + "resource/update.json",
                    resourceTO, ResourceTO.class);
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

        List<SchemaMappingTO> schemaMappingTOs =
                new ArrayList<SchemaMappingTO>();

        // Update with an existing and already assigned mapping
        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setId(112L);
        schemaMappingTO.setExtAttrName("test3");
        schemaMappingTO.setIntAttrName("fullname");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTOs.add(schemaMappingTO);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            schemaMappingTO = new SchemaMappingTO();
            schemaMappingTO.setExtAttrName("test" + i);
            schemaMappingTO.setIntAttrName("fullname");
            schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
            schemaMappingTOs.add(schemaMappingTO);
        }
        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("username");
        schemaMappingTO.setIntAttrName("fullname");
        schemaMappingTO.setIntMappingType(IntMappingType.SyncopeUserId);
        schemaMappingTO.setAccountid(true);
        schemaMappingTOs.add(schemaMappingTO);

        resourceTO.setMappings(schemaMappingTOs);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/update.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        List<SchemaMappingTO> mappings = actual.getMappings();

        assertNotNull(mappings);

        assertEquals(4, mappings.size());
    }

    @Test
    public void deleteWithException() {
        try {
            restTemplate.delete(
                    BASE_URL + "resource/delete/{resourceName}.json",
                    "resourcenotfound");
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void updateResetSyncToken() {
	// pre condition: sync token is set
        String resourceName = "ws-target-resource-update-resetsynctoken";
	ResourceTO pre = restTemplate.getForObject(
                BASE_URL + "/resource/read/{resourceName}.json",
                ResourceTO.class, resourceName);
        assertNotNull(pre.getSyncToken());
        
        pre.setSyncToken(null);
        
        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/update.json",
                pre, ResourceTO.class);

        // check that the synctoken has been reset
        assertNull(actual.getSyncToken());
    }
    
    @Test
    public void delete() {
        final String resourceName = "ws-target-resource-delete";

        restTemplate.delete(
                BASE_URL + "resource/delete/{resourceName}.json",
                resourceName);

        try {
            restTemplate.getForObject(
                    BASE_URL + "resource/read/{resourceName}.json",
                    ResourceTO.class,
                    resourceName);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "resource/list.json", ResourceTO[].class));
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
        }
    }

    @Test
    public void read() {
        ResourceTO actual = restTemplate.getForObject(
                BASE_URL + "/resource/read/{resourceName}.json",
                ResourceTO.class, "resource-testdb");

        assertNotNull(actual);
    }
}
