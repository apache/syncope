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
package org.apache.syncope.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.persistence.api.entity.ConnInstance;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.user.UMapping;
import org.apache.syncope.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.apache.syncope.persistence.jpa.entity.user.JPAUMapping;
import org.apache.syncope.persistence.jpa.entity.user.JPAUMappingItem;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Test
    public void findById() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull("findById did not work", resource);

        ConnInstance connector = resource.getConnector();
        assertNotNull("connector not found", connector);
        assertEquals("invalid connector name",
                "net.tirasa.connid.bundles.soap.WebServiceConnector", connector.getConnectorName());
        assertEquals("invalid bundle name", "net.tirasa.connid.bundles.soap", connector.getBundleName());

        assertFalse("no mapping specified", resource.getUmapping().getItems().isEmpty());

        List<Long> mappingIds = new ArrayList<>();
        for (UMappingItem item : resource.getUmapping().getItems()) {
            mappingIds.add(item.getKey());
        }
        assertTrue(mappingIds.contains(100L));
    }

    @Test
    public void findAll() {
        List<ExternalResource> resources = resourceDAO.findAll();
        assertNotNull(resources);
        assertEquals(18, resources.size());
    }

    @Test
    public void findAllByPriority() {
        List<ExternalResource> resources = resourceDAO.findAllByPriority();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());
    }

    @Test
    public void getAccountId() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);
        assertEquals("fullname", resource.getUmapping().getAccountIdItem().getIntAttrName());
    }

    @Test
    public void save() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("ws-target-resource-basic-save");
        resource.setPropagationPriority(2);
        resource.setPropagationPrimary(true);

        UMapping mapping = new JPAUMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new JPAUMappingItem();
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("fullname");
        accountId.setIntMappingType(IntMappingType.UserId);
        accountId.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(accountId);

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);

        assertNotNull(actual);
        assertNotNull(actual.getConnector());
        assertNotNull(actual.getUmapping());
        assertFalse(actual.getUmapping().getItems().isEmpty());
        assertEquals(Integer.valueOf(2), actual.getPropagationPriority());
        assertTrue(actual.isPropagationPrimary());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingIntAttr() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new JPAUMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new JPAUMappingItem();
        accountId.setAccountid(true);
        accountId.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(accountId);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveInvalidAccountIdMapping() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new JPAUMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new JPAUMappingItem();
        accountId.setAccountid(true);
        accountId.setIntMappingType(IntMappingType.UserVirtualSchema);
        mapping.setAccountIdItem(accountId);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingExtAttr() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new JPAUMapping();
        resource.setUmapping(mapping);

        UMappingItem item = new JPAUMappingItem();
        item.setAccountid(true);
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        item = new JPAUMappingItem();
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test
    public void saveWithRoleMappingType() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new JPAUMapping();
        resource.setUmapping(mapping);

        UMappingItem item = new JPAUMappingItem();
        item.setIntAttrName("fullname");
        item.setExtAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        item = new JPAUMappingItem();
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setIntMappingType(IntMappingType.RoleSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        item = new JPAUMappingItem();
        item.setIntAttrName("mderiveddata");
        item.setExtAttrName("mderiveddata");
        item.setIntMappingType(IntMappingType.MembershipDerivedSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.addItem(item);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        int items = 0;
        for (UMappingItem mapItem : actual.getUmapping().getItems()) {
            items++;

            if ("icon".equals(mapItem.getIntAttrName())) {
                assertTrue(IntMappingType.contains(AttributableType.ROLE,
                        mapItem.getIntMappingType().toString()));
            }
            if ("mderiveddata".equals(mapItem.getIntAttrName())) {
                assertTrue(IntMappingType.contains(AttributableType.MEMBERSHIP,
                        mapItem.getIntMappingType().toString()));
            }
        }
        assertEquals(3, items);
    }

    @Test
    public void delete() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        resourceDAO.delete(resource.getKey());

        ExternalResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull(actual);
    }

    @Test
    public void issueSYNCOPE418() {
        ExternalResource resource = new JPAExternalResource();
        resource.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            resourceDAO.save(resource);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidName));
        }
    }
}
