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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class VirSchemaTest extends AbstractTest {

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void deal() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-1").orElseThrow();
        assertTrue(virSchemaDAO.findByResource(resource).isEmpty());
        assertTrue(virSchemaDAO.findByResourceAndAnyType(resource.getKey(), AnyTypeKind.USER.name()).isEmpty());

        VirSchema virSchema = entityFactory.newEntity(VirSchema.class);
        virSchema.setKey("vSchema");
        virSchema.setReadonly(true);
        virSchema.setExtAttrName("EXT_ATTR");
        virSchema.setResource(resource);
        virSchema.setAnyType(anyTypeDAO.getUser());

        virSchemaDAO.save(virSchema);

        virSchema = virSchemaDAO.findById("vSchema").orElseThrow();
        assertTrue(virSchema.isReadonly());
        assertEquals("EXT_ATTR", virSchema.getExtAttrName());

        assertFalse(virSchemaDAO.findByResource(resource).isEmpty());
        assertTrue(virSchemaDAO.findByResource(resource).contains(virSchema));

        assertFalse(virSchemaDAO.findByResourceAndAnyType(
                resource.getKey(), AnyTypeKind.USER.name()).isEmpty());
        assertTrue(virSchemaDAO.findByResourceAndAnyType(
                resource.getKey(), AnyTypeKind.USER.name()).contains(virSchema));

        Item item = virSchema.asLinkingMappingItem();
        assertNotNull(item);
        assertEquals(virSchema.getKey(), item.getIntAttrName());
    }
}
