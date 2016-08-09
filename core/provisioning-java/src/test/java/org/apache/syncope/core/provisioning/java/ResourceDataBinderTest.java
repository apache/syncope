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
import java.util.Set;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Test
    public void issue42() {
        PlainSchema userId = plainSchemaDAO.find("userId");

        Set<MappingItem> beforeUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvision(anyTypeDAO.findUser()) != null
                    && res.getProvision(anyTypeDAO.findUser()).getMapping() != null) {

                for (MappingItem mapItem : res.getProvision(anyTypeDAO.findUser()).getMapping().getItems()) {
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

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("userId");
        item.setExtAttrName("campo1");
        item.setConnObjectKey(true);
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        assertNotNull(resource);
        assertNotNull(resource.getProvision(anyTypeDAO.findUser()).getMapping());
        assertEquals(1, resource.getProvision(anyTypeDAO.findUser()).getMapping().getItems().size());

        resourceDAO.flush();

        ExternalResource actual = resourceDAO.find("resource-issue42");
        assertNotNull(actual);
        assertEquals(resource, actual);

        userId = plainSchemaDAO.find("userId");

        Set<MappingItem> afterUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getProvision(anyTypeDAO.findUser()) != null
                    && res.getProvision(anyTypeDAO.findUser()).getMapping() != null) {

                for (MappingItem mapItem : res.getProvision(anyTypeDAO.findUser()).getMapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        afterUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
    }
}
