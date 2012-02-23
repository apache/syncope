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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.connid.bundles.soap.WebServiceConnector;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.Entity;
import org.syncope.types.IntMappingType;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public void findById() {
        ExternalResource resource =
                resourceDAO.find("ws-target-resource-1");

        assertNotNull("findById did not work", resource);

        ConnInstance connector = resource.getConnector();

        assertNotNull("connector not found", connector);

        assertEquals("invalid connector name",
                WebServiceConnector.class.getName(),
                connector.getConnectorName());

        assertEquals("invalid bundle name", "org.connid.bundles.soap",
                connector.getBundleName());

        assertEquals("invalid bundle version",
                connidSoapVersion, connector.getVersion());

        Set<SchemaMapping> mappings = resource.getMappings();
        assertNotNull("mappings not found", mappings);
        assertFalse("no mapping specified", mappings.isEmpty());

        List<Long> mappingIds = new ArrayList<Long>();
        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }
        assertTrue(mappingIds.contains(100L));
    }

    @Test
    public void findAllByPriority() {
        List<ExternalResource> resources = resourceDAO.findAllByPriority();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());
    }

    @Test
    public void getAccountId() {
        SchemaMapping mapping = resourceDAO.getMappingForAccountId(
                "ws-target-resource-2");
        assertEquals("fullname", mapping.getIntAttrName());
    }

    @Test
    public void save() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save");
        resource.setPropagationPriority(2);
        resource.setPropagationPrimary(true);

        SchemaMapping accountId = new SchemaMapping();
        accountId.setResource(resource);
        accountId.setAccountid(true);
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("fullname");
        accountId.setIntMappingType(IntMappingType.SyncopeUserId);

        resource.addMapping(accountId);

        ConnInstance connector =
                resourceDAO.find("ws-target-resource-1").getConnector();

        resource.setConnector(connector);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);

        assertNotNull(actual);
        assertNotNull(actual.getConnector());
        assertEquals(Integer.valueOf(2), actual.getPropagationPriority());
        assertTrue(actual.isPropagationPrimary());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingIntAttr() {

        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector =
                resourceDAO.find("ws-target-resource-1").getConnector();

        resource.setConnector(connector);

        SchemaMapping accountId = new SchemaMapping();
        accountId.setResource(resource);
        accountId.setAccountid(true);
        accountId.setIntMappingType(IntMappingType.UserSchema);

        resource.addMapping(accountId);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);

        assertNotNull(actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingExtAttr() {

        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector =
                resourceDAO.find("ws-target-resource-1").getConnector();

        resource.setConnector(connector);

        SchemaMapping mapping = new SchemaMapping();
        mapping.setResource(resource);
        mapping.setAccountid(true);
        mapping.setIntAttrName("fullname");
        mapping.setIntMappingType(IntMappingType.UserSchema);

        resource.addMapping(mapping);

        mapping = new SchemaMapping();
        mapping.setResource(resource);
        mapping.setIntAttrName("userId");
        mapping.setIntMappingType(IntMappingType.UserSchema);

        resource.addMapping(mapping);

        resourceDAO.save(resource);
    }

    @Test
    public void saveWithRoleMappingType() {

        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector =
                resourceDAO.find("ws-target-resource-1").getConnector();

        resource.setConnector(connector);

        SchemaMapping mapping = new SchemaMapping();
        mapping.setResource(resource);
        mapping.setAccountid(true);
        mapping.setIntAttrName("fullname");
        mapping.setIntMappingType(IntMappingType.UserSchema);

        resource.addMapping(mapping);

        mapping = new SchemaMapping();
        mapping.setResource(resource);
        mapping.setIntAttrName("icon");
        mapping.setExtAttrName("icon");
        mapping.setIntMappingType(IntMappingType.RoleSchema);

        resource.addMapping(mapping);

        mapping = new SchemaMapping();
        mapping.setResource(resource);
        mapping.setIntAttrName("mderiveddata");
        mapping.setExtAttrName("mderiveddata");
        mapping.setIntMappingType(IntMappingType.MembershipDerivedSchema);

        resource.addMapping(mapping);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);

        assertNotNull(actual);

        assertEquals(3, actual.getMappings().size());

        for (SchemaMapping schemaMapping : actual.getMappings()) {

            if ("icon".equals(schemaMapping.getIntAttrName())) {
                assertTrue(IntMappingType.contains(
                        Entity.ROLE,
                        schemaMapping.getIntMappingType().toString()));
            }

            if ("mderiveddata".equals(schemaMapping.getIntAttrName())) {
                assertTrue(IntMappingType.contains(
                        Entity.MEMBERSHIP,
                        schemaMapping.getIntMappingType().toString()));
            }
        }
    }

    @Test
    public void delete() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        resourceDAO.delete(resource.getName());

        ExternalResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull(actual);
    }
}
