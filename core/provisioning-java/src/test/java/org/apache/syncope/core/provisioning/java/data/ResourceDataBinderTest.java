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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceDataBinderTest extends AbstractTest {

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

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void issue42() {
        PlainSchema userId = plainSchemaDAO.findById("userId").orElseThrow();

        Set<Item> beforeUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvisionByAnyType(AnyTypeKind.USER.name()).isPresent()
                    && res.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping() != null) {

                for (Item mapItem : res.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        beforeUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey("resource-issue42");
        resourceTO.setConnector("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        resourceTO.setEnforceMandatoryCondition(true);

        Provision provisionTO = new Provision();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        Mapping mapping = new Mapping();
        provisionTO.setMapping(mapping);

        Item item = new Item();
        item.setIntAttrName("userId");
        item.setExtAttrName("campo1");
        item.setConnObjectKey(true);
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        entityManager.flush();
        assertNotNull(resource);
        assertNotNull(resource.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping());
        assertEquals(1, resource.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().size());

        ExternalResource actual = resourceDAO.findById("resource-issue42").orElseThrow();
        entityManager.flush();
        assertNotNull(actual);
        assertEquals(resource, actual);

        userId = plainSchemaDAO.findById("userId").orElseThrow();

        Set<Item> afterUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvisionByAnyType(AnyTypeKind.USER.name()).isPresent()
                    && res.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping() != null) {

                for (Item mapItem : res.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        afterUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
    }
}
