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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Test
    public void findById() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-1").orElseThrow();

        ConnInstance connector = resource.getConnector();
        assertNotNull(connector);
        assertEquals("net.tirasa.connid.bundles.soap.WebServiceConnector", connector.getConnectorName());
        assertEquals("net.tirasa.connid.bundles.soap", connector.getBundleName());

        Mapping mapping = resource.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping();
        assertFalse(mapping.getItems().isEmpty());

        assertTrue(mapping.getItems().stream().
                anyMatch(item -> "email".equals(item.getExtAttrName()) && "email".equals(item.getIntAttrName())));

        try {
            resourceDAO.authFind("ws-target-resource-1");
            fail("This should not happen");
        } catch (DelegatedAdministrationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void findWithOrgUnit() {
        ExternalResource resource = resourceDAO.findById("resource-ldap-orgunit").orElseThrow();
        assertNotNull(resource.getOrgUnit());
    }

    @Test
    public void findAll() {
        List<GrantedAuthority> authorities = IdMEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            List<? extends ExternalResource> resources = resourceDAO.findAll();
            assertNotNull(resources);
            assertFalse(resources.isEmpty());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void getConnObjectKey() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-2").orElseThrow();
        assertEquals("fullname", resource.getProvisionByAnyType(AnyTypeKind.USER.name()).get().
                getMapping().getConnObjectKeyItem().get().getIntAttrName());
    }

    @Test
    public void save() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save");
        resource.setPropagationPriority(2);

        Provision provision = new Provision();
        provision.setAnyType(AnyTypeKind.USER.name());
        provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resource.getProvisions().add(provision);

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        Item connObjectKey = new Item();
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("fullname");
        connObjectKey.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(connObjectKey);

        ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
        resource.setConnector(connector);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        entityManager.flush();
        assertNotNull(actual);
        assertNotNull(actual.getConnector());
        assertNotNull(actual.getProvisionByAnyType(AnyTypeKind.USER.name()).
                get().getMapping());
        assertFalse(actual.getProvisionByAnyType(AnyTypeKind.USER.name()).
                get().getMapping().getItems().isEmpty());
        assertEquals(Integer.valueOf(2), actual.getPropagationPriority());
    }

    @Test
    public void saveInvalidMappingIntAttr() {
        assertThrows(InvalidEntityException.class, () -> {
            ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
            resource.setKey("ws-target-resource-basic-save-invalid");

            ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
            resource.setConnector(connector);

            Provision provision = new Provision();
            provision.setAnyType(AnyTypeKind.USER.name());
            provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provision);

            Mapping mapping = new Mapping();
            provision.setMapping(mapping);

            Item connObjectKey = new Item();
            connObjectKey.setConnObjectKey(true);
            mapping.add(connObjectKey);

            // save the resource
            resourceDAO.save(resource);
        });
    }

    @Test
    public void saveInvalidMappingExtAttr() {
        assertThrows(InvalidEntityException.class, () -> {
            ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
            resource.setKey("ws-target-resource-basic-save-invalid");

            ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
            resource.setConnector(connector);

            Provision provision = new Provision();
            provision.setAnyType(AnyTypeKind.USER.name());
            provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provision);

            Mapping mapping = new Mapping();
            provision.setMapping(mapping);

            Item item = new Item();
            item.setConnObjectKey(true);
            item.setIntAttrName("fullname");
            mapping.add(item);

            item = new Item();
            item.setIntAttrName("userId");
            mapping.add(item);

            resourceDAO.save(resource);
        });
    }

    @Test
    public void saveInvalidProvision() {
        assertThrows(InvalidEntityException.class, () -> {
            ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
            resource.setKey("invalidProvision");

            Provision provision = new Provision();
            provision.setAnyType(AnyTypeKind.USER.name());
            provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provision);

            Mapping mapping = new Mapping();
            provision.setMapping(mapping);

            Item connObjectKey = new Item();
            connObjectKey.setExtAttrName("username");
            connObjectKey.setIntAttrName("fullname");
            connObjectKey.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(connObjectKey);

            provision = new Provision();
            provision.setAnyType(AnyTypeKind.GROUP.name());
            provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provision);

            ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
            resource.setConnector(connector);

            // save the resource
            resourceDAO.save(resource);
        });
    }

    @Test
    public void saveVirtualMapping() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-virtual-mapping");
        resource.setPropagationPriority(2);

        Provision provision = new Provision();
        provision.setAnyType(AnyTypeKind.USER.name());
        provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resource.getProvisions().add(provision);

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        Item connObjectKey = new Item();
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("fullname");
        connObjectKey.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(connObjectKey);

        Item virtualMapItem = new Item();
        virtualMapItem.setIntAttrName("virtualReadOnly");
        virtualMapItem.setExtAttrName("TEST");
        virtualMapItem.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(virtualMapItem);

        ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
        resource.setConnector(connector);

        resourceDAO.save(resource);
    }

    @Test
    public void saveWithGroupMappingType() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-basic-save-invalid");

        ConnInstance connector = resourceDAO.findById("ws-target-resource-1").orElseThrow().getConnector();
        resource.setConnector(connector);

        Provision provision = new Provision();
        provision.setAnyType(AnyTypeKind.USER.name());
        provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resource.getProvisions().add(provision);

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        Item item = new Item();
        item.setIntAttrName("fullname");
        item.setExtAttrName("fullname");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new Item();
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new Item();
        item.setIntAttrName("mderiveddata");
        item.setExtAttrName("mderiveddata");
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(item);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        entityManager.flush();
        assertNotNull(actual);

        assertEquals(3, actual.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().size());
    }

    @Test
    public void delete() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-2").orElseThrow();

        resourceDAO.deleteById(resource.getKey());

        assertTrue(resourceDAO.findById("ws-target-resource-2").isEmpty());
    }

    @Test
    public void issueSYNCOPE418() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            resourceDAO.save(resource);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
