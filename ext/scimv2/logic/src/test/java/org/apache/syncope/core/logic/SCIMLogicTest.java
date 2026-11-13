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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriBuilder;
import java.util.List;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyObjectConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.syncope.common.lib.scim.SCIMItem;
import org.apache.syncope.common.lib.scim.SCIMReturned;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.Test;

class SCIMLogicTest {

    private static final String BASE_URI = "http://localhost:8080/syncope/rest/scimv2/";

    private static SCIMLogic logic(final SCIMConf conf) {
        SCIMConfManager confManager = mock(SCIMConfManager.class);
        when(confManager.get()).thenReturn(conf);
        return new SCIMLogic(confManager);
    }

    private static SCIMItem item(final String intAttrName) {
        SCIMItem item = new SCIMItem();
        item.setIntAttrName(intAttrName);
        item.setExtAttrName(intAttrName);
        item.setReturned(SCIMReturned.ALWAYS);
        return item;
    }

    private static SCIMConf confWithExtensions() {
        SCIMConf conf = new SCIMConf();
        SCIMGeneralConf generalConf = new SCIMGeneralConf();
        generalConf.setBulkMaxOperations(42);
        generalConf.setBulkMaxPayloadSize(1024);
        generalConf.setFilterMaxResults(7);
        conf.setGeneralConf(generalConf);

        SCIMExtensionAnyConf extensionUserConf = new SCIMExtensionAnyConf();
        extensionUserConf.setName("Syncope User Extension");
        extensionUserConf.setDescription("Syncope user extension");
        extensionUserConf.add(item("surname"));
        conf.setExtensionUserConf(extensionUserConf);

        SCIMExtensionAnyConf extensionGroupConf = new SCIMExtensionAnyConf();
        extensionGroupConf.setName("Syncope Group Extension");
        extensionGroupConf.add(item("groupOwner"));
        conf.setExtensionGroupConf(extensionGroupConf);

        SCIMExtensionAnyObjectConf extensionAnyObjectConf = new SCIMExtensionAnyObjectConf();
        extensionAnyObjectConf.setType("other");
        extensionAnyObjectConf.add(item("attribute"));
        conf.getExtensionAnyObjectsConf().add(extensionAnyObjectConf);

        return conf;
    }

    @Test
    void serviceProviderConfigAndResourceTypes() {
        UriBuilder uriBuilder = UriBuilder.fromUri(BASE_URI);
        SCIMLogic logic = logic(confWithExtensions());

        ServiceProviderConfig config = logic.serviceProviderConfig(uriBuilder);
        assertNotNull(config);
        assertEquals(List.of(Resource.ServiceProviderConfig.schema()), config.getSchemas());
        assertEquals(BASE_URI, config.getMeta().getLocation());
        assertTrue(config.getPatch().isSupported());
        assertFalse(config.getBulk().isSupported());
        assertEquals(42, config.getBulk().getMaxOperations());
        assertEquals(1024, config.getBulk().getMaxPayloadSize());
        assertEquals(7, config.getFilter().getMaxResults());
        assertEquals(2, config.getAuthenticationSchemes().size());
        assertEquals("JSON Web Token", config.getAuthenticationSchemes().getFirst().getName());

        List<ResourceType> resourceTypes = logic.resourceTypes(uriBuilder);
        assertEquals(2, resourceTypes.size());
        assertEquals("User", resourceTypes.getFirst().getName());
        assertEquals(Resource.User.schema(), resourceTypes.getFirst().getSchema());
        assertEquals(1, resourceTypes.getFirst().getSchemaExtensions().size());
        assertTrue(resourceTypes.getFirst().getSchemaExtensions().getFirst().isRequired());
        assertEquals("Group", resourceTypes.get(1).getName());

        assertEquals("User", logic.resourceType(uriBuilder, Resource.User.name()).getId());
        assertEquals("Group", logic.resourceType(uriBuilder, Resource.Group.name()).getId());
        assertThrows(IllegalArgumentException.class,
                () -> logic.resourceType(uriBuilder, "Unsupported"));
    }

    @Test
    void schemasIncludeConfiguredExtensions() {
        SCIMLogic logic = logic(confWithExtensions());

        String schemas = logic.schemas();
        assertNotNull(schemas);
        assertTrue(schemas.contains(Resource.User.schema()));
        assertTrue(schemas.contains(Resource.Group.schema()));
        assertTrue(schemas.contains(Resource.EnterpriseUser.schema()));
        assertTrue(schemas.contains(Resource.ExtensionUser.schema()));
        assertTrue(schemas.contains(Resource.ExtensionGroup.schema()));
        assertTrue(schemas.contains("urn:ietf:params:scim:schemas:extension:syncope:2.0:other"));

        assertTrue(schemas.contains("\"name\" : \"surname\""));
        assertTrue(schemas.contains("\"returned\" : \"always\""));
        assertTrue(schemas.contains("Syncope User Extension"));
    }

    @Test
    void schema() {
        SCIMLogic logic = logic(new SCIMConf());

        String user = logic.schema(Resource.User.schema());
        assertNotNull(user);
        assertTrue(user.contains(Resource.User.schema()));

        assertThrows(NotFoundException.class, () -> logic.schema("urn:not:existing"));
    }
}
