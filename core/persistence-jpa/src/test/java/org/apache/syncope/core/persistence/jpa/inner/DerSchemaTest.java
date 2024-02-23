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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerSchemaTest extends AbstractTest {

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void findAll() {
        List<? extends DerSchema> list = derSchemaDAO.findAll();
        assertEquals(10, list.size());
    }

    @Test
    public void findByIdLike() {
        List<? extends DerSchema> schemas = derSchemaDAO.findByIdLike("mderivedd%");
        assertEquals(1, schemas.size());
    }

    @Test
    public void findByName() {
        assertTrue(derSchemaDAO.findById("cn").isPresent());
    }

    @Test
    public void save() {
        DerSchema derivedAttributeSchema = entityFactory.newEntity(DerSchema.class);
        derivedAttributeSchema.setKey("cn2");
        derivedAttributeSchema.setExpression("firstname surname");

        derSchemaDAO.save(derivedAttributeSchema);

        DerSchema actual = derSchemaDAO.findById("cn2").orElseThrow();
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public void delete() {
        DerSchema cn = derSchemaDAO.findById("cn").orElseThrow();

        derSchemaDAO.deleteById(cn.getKey());

        assertTrue(derSchemaDAO.findById("cn").isEmpty());

        // ------------- //
        DerSchema rderiveddata = derSchemaDAO.findById("rderiveddata").orElseThrow();

        derSchemaDAO.deleteById(rderiveddata.getKey());

        assertTrue(derSchemaDAO.findById("rderiveddata").isEmpty());
    }

    @Test
    public void issueSYNCOPE418() {
        DerSchema schema = entityFactory.newEntity(DerSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            derSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
