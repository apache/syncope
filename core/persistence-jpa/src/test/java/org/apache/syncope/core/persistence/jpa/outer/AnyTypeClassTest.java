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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnyTypeClassTest extends AbstractTest {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void create() {
        PlainSchema newSchema = entityFactory.newEntity(PlainSchema.class);
        newSchema.setKey("new_plain_schema");
        newSchema.setType(AttrSchemaType.String);

        plainSchemaDAO.save(newSchema);

        entityManager.flush();

        newSchema = plainSchemaDAO.findById(newSchema.getKey()).orElseThrow();

        AnyTypeClass newClass = entityFactory.newEntity(AnyTypeClass.class);
        newClass.setKey("new class");
        newClass.add(newSchema);

        anyTypeClassDAO.save(newClass);

        entityManager.flush();

        newClass = anyTypeClassDAO.findById(newClass.getKey()).orElseThrow();
        assertNotNull(newClass);
        assertEquals(1, newClass.getPlainSchemas().size());
        assertEquals(newSchema, newClass.getPlainSchemas().getFirst());
        assertEquals(newClass, newClass.getPlainSchemas().getFirst().getAnyTypeClass());

        assertTrue(plainSchemaDAO.findById(newSchema.getKey()).isPresent());
    }

    @Test
    public void delete() {
        AnyTypeClass minimalUser = anyTypeClassDAO.findById("minimal user").orElseThrow();

        PlainSchema surname = plainSchemaDAO.findById("surname").orElseThrow();
        assertTrue(minimalUser.getPlainSchemas().contains(surname));
        int before = minimalUser.getPlainSchemas().size();

        plainSchemaDAO.deleteById("surname");

        entityManager.flush();

        minimalUser = anyTypeClassDAO.findById("minimal user").orElseThrow();
        assertEquals(before, minimalUser.getPlainSchemas().size() + 1);
    }
}
