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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class VirSchemaTest extends AbstractTest {

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void deal() {
        Provision provision = resourceDAO.find("ws-target-resource-1").getProvision(anyTypeDAO.findUser()).get();
        assertNotNull(provision);
        assertTrue(virSchemaDAO.findByProvision(provision).isEmpty());

        VirSchema virSchema = entityFactory.newEntity(VirSchema.class);
        virSchema.setKey("vSchema");
        virSchema.setReadonly(true);
        virSchema.setExtAttrName("EXT_ATTR");
        virSchema.setProvision(provision);

        virSchemaDAO.save(virSchema);
        virSchemaDAO.flush();

        virSchema = virSchemaDAO.find("vSchema");
        assertNotNull("expected save to work", virSchema);
        assertTrue(virSchema.isReadonly());
        assertEquals("EXT_ATTR", virSchema.getExtAttrName());

        provision = resourceDAO.find("ws-target-resource-1").getProvision(anyTypeDAO.findUser()).get();
        assertNotNull(provision);
        assertFalse(virSchemaDAO.findByProvision(provision).isEmpty());
        assertTrue(virSchemaDAO.findByProvision(provision).contains(virSchema));

        MappingItem item = virSchema.asLinkingMappingItem();
        assertNotNull(item);
        assertEquals(virSchema.getKey(), item.getIntAttrName());
    }
}
