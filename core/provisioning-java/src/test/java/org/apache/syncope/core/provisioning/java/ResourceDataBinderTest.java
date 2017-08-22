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
package org.apache.syncope.core.provisioning.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ResourceDataBinderTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @BeforeClass
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = StandardEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails("Master"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterClass
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void issue42() {
        PlainSchema userId = plainSchemaDAO.find("userId");

        Set<MappingItem> beforeUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvision(anyTypeDAO.findUser()).isPresent()
                    && res.getProvision(anyTypeDAO.findUser()).get().getMapping() != null) {

                for (MappingItem mapItem : res.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {
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

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("userId");
        item.setExtAttrName("campo1");
        item.setConnObjectKey(true);
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        assertNotNull(resource);
        assertNotNull(resource.getProvision(anyTypeDAO.findUser()).get().getMapping());
        assertEquals(1, resource.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems().size());

        resourceDAO.flush();

        ExternalResource actual = resourceDAO.find("resource-issue42");
        assertNotNull(actual);
        assertEquals(resource, actual);

        userId = plainSchemaDAO.find("userId");

        Set<MappingItem> afterUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvision(anyTypeDAO.findUser()).isPresent()
                    && res.getProvision(anyTypeDAO.findUser()).get().getMapping() != null) {

                for (MappingItem mapItem : res.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        afterUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
    }
}
