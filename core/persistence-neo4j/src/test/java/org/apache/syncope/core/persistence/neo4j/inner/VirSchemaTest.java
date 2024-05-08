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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
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
    public void findAll() {
        List<? extends VirSchema> list = virSchemaDAO.findAll();
        assertEquals(3, list.size());
    }

    @Test
    public void findByIdLike() {
        List<? extends VirSchema> schemas = virSchemaDAO.findByIdLike("rvirtuald%");
        assertEquals(1, schemas.size());
    }

    @Test
    public void findByName() {
        VirSchema attributeSchema = virSchemaDAO.findById("virtualdata").orElseThrow();
        assertNotNull(attributeSchema);
    }

    @Test
    public void save() {
        VirSchema virSchema = entityFactory.newEntity(VirSchema.class);
        virSchema.setKey("virtual");
        virSchema.setResource(resourceDAO.findById("resource-csv").orElseThrow());
        virSchema.setAnyType(anyTypeDAO.getUser());
        virSchema.setReadonly(true);
        virSchema.setExtAttrName("EXT_ATTR");

        virSchemaDAO.save(virSchema);

        VirSchema actual = virSchemaDAO.findById("virtual").orElseThrow();
        assertNotNull(actual);
        assertTrue(actual.isReadonly());
        assertEquals("EXT_ATTR", actual.getExtAttrName());
    }

    @Test
    public void delete() {
        VirSchema virtualdata = virSchemaDAO.findById("virtualdata").orElseThrow();

        virSchemaDAO.deleteById(virtualdata.getKey());

        assertTrue(virSchemaDAO.findById("virtualdata").isEmpty());

        // ------------- //
        VirSchema rvirtualdata = virSchemaDAO.findById("rvirtualdata").orElseThrow();

        virSchemaDAO.deleteById(rvirtualdata.getKey());

        assertTrue(virSchemaDAO.findById("rvirtualdata").isEmpty());
    }

    @Test
    public void issueSYNCOPE418() {
        VirSchema schema = entityFactory.newEntity(VirSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            virSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
