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
package org.apache.syncope.server.provisioning.java.data;

import org.apache.syncope.server.provisioning.java.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.server.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.server.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.MappingItem;
import org.apache.syncope.server.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.server.provisioning.api.data.ResourceDataBinder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceDataBinderTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void databinding() throws IOException {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        ResourceTO resourceTO = resourceDataBinder.getResourceTO(resource);
        assertNotNull(resourceTO);

        ExternalResource fromto = resourceDataBinder.update(resource, resourceTO);
        assertNotNull(fromto);
        assertEquals(resource, fromto);

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, resourceTO);

        assertEquals(resourceTO, mapper.readValue(writer.toString(), ResourceTO.class));

        List<ResourceTO> resourceTOs = resourceDataBinder.getResourceTOs(resourceDAO.findAll());
        assertNotNull(resourceTOs);
        assertFalse(resourceTOs.isEmpty());

        writer = new StringWriter();
        mapper.writeValue(writer, resourceTOs);

        ResourceTO[] actual = mapper.readValue(writer.toString(), ResourceTO[].class);
        assertEquals(resourceTOs, Arrays.asList(actual));
    }

    @Test
    public void issue42() {
        UPlainSchema userId = plainSchemaDAO.find("userId", UPlainSchema.class);

        Set<MappingItem> beforeUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getUmapping() != null) {
                for (MappingItem mapItem : res.getUmapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        beforeUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey("resource-issue42");
        resourceTO.setConnectorId(100L);
        resourceTO.setPropagationMode(PropagationMode.ONE_PHASE);
        resourceTO.setEnforceMandatoryCondition(true);

        MappingTO mapping = new MappingTO();
        resourceTO.setUmapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setExtAttrName("campo1");
        item.setAccountid(true);
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setAccountIdItem(item);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        assertNotNull(resource);
        assertNotNull(resource.getUmapping());
        assertEquals(1, resource.getUmapping().getItems().size());

        resourceDAO.flush();

        ExternalResource actual = resourceDAO.find("resource-issue42");
        assertNotNull(actual);
        assertEquals(resource, actual);

        userId = plainSchemaDAO.find("userId", UPlainSchema.class);

        Set<MappingItem> afterUserIdMappings = new HashSet<>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getUmapping() != null) {
                for (MappingItem mapItem : res.getUmapping().getItems()) {
                    if (userId.getKey().equals(mapItem.getIntAttrName())) {
                        afterUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
    }
}
