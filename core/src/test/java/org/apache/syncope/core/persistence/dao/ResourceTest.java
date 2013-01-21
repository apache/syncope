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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceTest extends AbstractDAOTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public void findById() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull("findById did not work", resource);

        ConnInstance connector = resource.getConnector();
        assertNotNull("connector not found", connector);
        assertEquals("invalid connector name",
                "org.connid.bundles.soap.WebServiceConnector", connector.getConnectorName());
        assertEquals("invalid bundle name", "org.connid.bundles.soap", connector.getBundleName());
        assertEquals("invalid bundle version", connidSoapVersion, connector.getVersion());

        assertFalse("no mapping specified", resource.getUmapping().getItems().isEmpty());

        List<Long> mappingIds = new ArrayList<Long>();
        for (AbstractMappingItem item : resource.getUmapping().getItems()) {
            mappingIds.add(item.getId());
        }
        assertTrue(mappingIds.contains(100L));
    }

    @Test
    public void findAll() {
        List<ExternalResource> resources = resourceDAO.findAll();
        assertNotNull(resources);
        assertEquals(17, resources.size());
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
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save");
        resource.setPropagationPriority(2);
        resource.setPropagationPrimary(true);

        UMapping mapping = new UMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new UMappingItem();
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("fullname");
        accountId.setIntMappingType(IntMappingType.UserId);
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
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new UMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new UMappingItem();
        accountId.setAccountid(true);
        accountId.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(accountId);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveInvalidAccountIdMapping() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new UMapping();
        resource.setUmapping(mapping);

        UMappingItem accountId = new UMappingItem();
        accountId.setAccountid(true);
        accountId.setIntMappingType(IntMappingType.UserVirtualSchema);
        mapping.setAccountIdItem(accountId);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingExtAttr() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new UMapping();
        resource.setUmapping(mapping);

        UMappingItem item = new UMappingItem();
        item.setAccountid(true);
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        item = new UMappingItem();
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.addItem(item);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
    }

    @Test
    public void saveWithRoleMappingType() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        UMapping mapping = new UMapping();
        resource.setUmapping(mapping);

        UMappingItem item = new UMappingItem();
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserSchema);
        mapping.setAccountIdItem(item);

        item = new UMappingItem();
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setIntMappingType(IntMappingType.RoleSchema);
        mapping.addItem(item);

        item = new UMappingItem();
        item.setIntAttrName("mderiveddata");
        item.setExtAttrName("mderiveddata");
        item.setIntMappingType(IntMappingType.MembershipDerivedSchema);
        mapping.addItem(item);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        int items = 0;
        for (AbstractMappingItem mapItem : actual.getUmapping().getItems()) {
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

        resourceDAO.delete(resource.getName());

        ExternalResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull(actual);
    }
}
