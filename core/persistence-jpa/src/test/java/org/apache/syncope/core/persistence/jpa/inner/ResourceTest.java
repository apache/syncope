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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ResourceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void findById() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull("findById did not work", resource);

        ConnInstance connector = resource.getConnector();
        assertNotNull("connector not found", connector);
        assertEquals("invalid connector name",
                "net.tirasa.connid.bundles.soap.WebServiceConnector", connector.getConnectorName());
        assertEquals("invalid bundle name", "net.tirasa.connid.bundles.soap", connector.getBundleName());

        Mapping mapping = resource.getProvision(anyTypeDAO.findUser()).get().getMapping();
        assertFalse("no mapping specified", mapping.getItems().isEmpty());

        assertTrue(mapping.getItems().stream().
                anyMatch(item -> "7f55b09c-b573-41dc-a9eb-ccd80bd3ea7a".equals(item.getKey())));

        try {
            resourceDAO.authFind("ws-target-resource-1");
            fail();
        } catch (DelegatedAdministrationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void findWithOrgUnit() {
        ExternalResource resource = resourceDAO.find("resource-ldap-orgunit");
        assertNotNull(resource);
        assertNotNull(resource.getOrgUnit());
    }

    @Test
    public void findAll() {
        List<GrantedAuthority> authorities = StandardEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails("Master"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            List<ExternalResource> resources = resourceDAO.findAll();
            assertNotNull(resources);
            assertFalse(resources.isEmpty());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void getConnObjectKey() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);
        assertEquals("fullname", resource.getProvision(anyTypeDAO.findUser()).get().
                getMapping().getConnObjectKeyItem().get().getIntAttrName());
    }

    @Test
    public void save() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save");
        resource.setPropagationPriority(2);

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem connObjectKey = entityFactory.newEntity(MappingItem.class);
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("fullname");
        connObjectKey.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(connObjectKey);

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);

        assertNotNull(actual);
        assertNotNull(actual.getConnector());
        assertNotNull(actual.getProvision(anyTypeDAO.findUser()).get().getMapping());
        assertFalse(actual.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems().isEmpty());
        assertEquals(Integer.valueOf(2), actual.getPropagationPriority());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingIntAttr() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem connObjectKey = entityFactory.newEntity(MappingItem.class);
        connObjectKey.setConnObjectKey(true);
        mapping.add(connObjectKey);

        // save the resource
        resourceDAO.save(resource);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidMappingExtAttr() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem item = entityFactory.newEntity(MappingItem.class);
        item.setConnObjectKey(true);
        item.setIntAttrName("fullname");
        mapping.add(item);

        item = entityFactory.newEntity(MappingItem.class);
        item.setIntAttrName("userId");
        mapping.add(item);

        resourceDAO.save(resource);
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidProvision() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("invalidProvision");

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem connObjectKey = entityFactory.newEntity(MappingItem.class);
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("fullname");
        connObjectKey.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(connObjectKey);

        provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findGroup());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        // save the resource
        resourceDAO.save(resource);
    }

    @Test
    public void saveVirtualMapping() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-virtual-mapping");
        resource.setPropagationPriority(2);

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem connObjectKey = entityFactory.newEntity(MappingItem.class);
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("fullname");
        connObjectKey.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(connObjectKey);

        MappingItem virtualMapItem = entityFactory.newEntity(MappingItem.class);
        virtualMapItem.setIntAttrName("virtualReadOnly");
        virtualMapItem.setExtAttrName("TEST");
        virtualMapItem.setPurpose(MappingPurpose.PROPAGATION);
        virtualMapItem.setMapping(mapping);
        mapping.add(virtualMapItem);

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        resourceDAO.save(resource);
    }

    @Test
    public void saveWithGroupMappingType() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.find("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyTypeDAO.findUser());
        provision.setObjectClass(ObjectClass.ACCOUNT);
        provision.setResource(resource);
        resource.add(provision);

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        mapping.setProvision(provision);
        provision.setMapping(mapping);

        MappingItem item = entityFactory.newEntity(MappingItem.class);
        item.setIntAttrName("fullname");
        item.setExtAttrName("fullname");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = entityFactory.newEntity(MappingItem.class);
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = entityFactory.newEntity(MappingItem.class);
        item.setIntAttrName("mderiveddata");
        item.setExtAttrName("mderiveddata");
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(item);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        assertEquals(3, actual.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems().size());
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
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            resourceDAO.save(resource);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
