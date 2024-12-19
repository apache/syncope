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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceLogicTest extends AbstractTest {

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = IdMEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private static ResourceTO buildResourceTO(final String resourceKey) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        Provision provisionTO = new Provision();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        Mapping mapping = new Mapping();
        provisionTO.setMapping(mapping);

        Item item = new Item();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new Item();
        item.setExtAttrName("username");
        item.setIntAttrName("key");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new Item();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setConnObjectKey(false);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(item);

        return resourceTO;
    }

    @Autowired
    private ResourceLogic logic;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void updateChangePurpose() {
        ResourceTO ws1 = logic.read("ws-target-resource-1");
        assertNotNull(ws1);

        Mapping ws1NewUMapping = ws1.getProvision(AnyTypeKind.USER.name()).get().getMapping();
        // change purpose from NONE to BOTH
        ws1NewUMapping.getItems().stream().
                filter(itemTO -> "firstname".equals(itemTO.getIntAttrName())).
                forEach(itemTO -> itemTO.setPurpose(MappingPurpose.BOTH));
        ws1.getProvision(AnyTypeKind.USER.name()).get().setMapping(ws1NewUMapping);

        ws1 = logic.update(ws1);
        assertNotNull(ws1);
    }

    @Test
    public void updateChangeOverrideCapabilities() {
        ResourceTO ldap = logic.read("resource-ldap");
        assertNotNull(ldap);
        assertTrue(ldap.getCapabilitiesOverride().isEmpty());

        ldap.setCapabilitiesOverride(Optional.of(Set.of(ConnectorCapability.SEARCH)));
        ldap = logic.update(ldap);
        assertNotNull(ldap);
        assertEquals(1, ldap.getCapabilitiesOverride().orElseThrow().size());
        assertTrue(ldap.getCapabilitiesOverride().orElseThrow().contains(ConnectorCapability.SEARCH));

        ldap.setCapabilitiesOverride(Optional.empty());
        logic.update(ldap);
    }

    @Test
    public void orgUnit() {
        ResourceTO resourceTO = buildResourceTO("ws-orgunit");
        assertNull(resourceTO.getOrgUnit());
        assertNull(resourceTO.getPropagationPriority());

        resourceTO = logic.create(resourceTO);
        entityManager.flush();
        assertNull(resourceTO.getOrgUnit());

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setConnObjectLink("'ou=' + name + ',o=isp'");
        orgUnit.setObjectClass("organizationalUnit");

        Item item = new Item();
        item.setIntAttrName("name");
        item.setExtAttrName("ou");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.BOTH);
        orgUnit.setConnObjectKeyItem(item);

        resourceTO.setOrgUnit(orgUnit);
        logic.update(resourceTO);
        entityManager.flush();
        assertNull(resourceTO.getPropagationPriority());

        resourceTO = logic.read("ws-orgunit");
        assertNotNull(resourceTO.getOrgUnit());

        resourceTO.setOrgUnit(null);
        resourceTO.setPropagationPriority(11);
        logic.update(resourceTO);
        entityManager.flush();

        resourceTO = logic.read("ws-orgunit");
        assertNull(resourceTO.getOrgUnit());
        assertEquals(11, resourceTO.getPropagationPriority());
    }

    @Test
    public void setLatestSyncToken() {
        ConnectorManager connectorManager = mock(ConnectorManager.class);
        when(connectorManager.getConnector(any(ExternalResource.class))).thenAnswer(ic -> {
            Connector connector = mock(Connector.class);
            when(connector.getLatestSyncToken(any(ObjectClass.class))).thenAnswer(ic2 -> new SyncToken("tokenValue"));
            return connector;
        });

        ResourceTO resourceTO = logic.create(buildResourceTO("lss"));
        assertNotNull(resourceTO);
        assertNull(resourceTO.getProvision(AnyTypeKind.USER.name()).get().getSyncToken());

        ResourceLogic resourceLogic = new ResourceLogic(
                resourceDAO, anyTypeDAO, null, null, null, null, null, null, null, connectorManager, null);

        resourceLogic.setLatestSyncToken(resourceTO.getKey(), AnyTypeKind.USER.name());
        entityManager.flush();

        resourceTO = logic.read(resourceTO.getKey());
        assertNotNull(resourceTO.getProvision(AnyTypeKind.USER.name()).get().getSyncToken());
    }
}
