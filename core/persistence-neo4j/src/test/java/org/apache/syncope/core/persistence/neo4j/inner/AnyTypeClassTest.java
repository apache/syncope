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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
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
    public void find() {
        AnyTypeClass minimalGroup = anyTypeClassDAO.findById("minimal group").orElseThrow();

        assertFalse(minimalGroup.getPlainSchemas().isEmpty());
        assertFalse(minimalGroup.getDerSchemas().isEmpty());
        assertFalse(minimalGroup.getVirSchemas().isEmpty());
    }

    @Test
    public void findAll() {
        List<? extends AnyTypeClass> list = anyTypeClassDAO.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    public void save() {
        PlainSchema firstname = plainSchemaDAO.findById("firstname").orElseThrow();

        AnyTypeClass newClass = entityFactory.newEntity(AnyTypeClass.class);
        newClass.setKey("new class");
        newClass.add(firstname);

        firstname.setAnyTypeClass(newClass);
        plainSchemaDAO.save(firstname);

        newClass = anyTypeClassDAO.save(newClass);
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertTrue(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());

        firstname = plainSchemaDAO.findById("firstname").orElseThrow();
        assertEquals(newClass, firstname.getAnyTypeClass());
    }

    @Test
    public void delete() {
        AnyTypeClass minimalUser = anyTypeClassDAO.findById("minimal user").orElseThrow();
        assertNotNull(minimalUser);

        anyTypeClassDAO.deleteById(minimalUser.getKey());
        assertTrue(anyTypeClassDAO.findById("minimal user").isEmpty());
    }
}
