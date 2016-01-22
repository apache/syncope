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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
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

        plainSchemaDAO.flush();

        newSchema = plainSchemaDAO.find(newSchema.getKey());
        assertNotNull(newSchema);

        AnyTypeClass newClass = entityFactory.newEntity(AnyTypeClass.class);
        newClass.setKey("new class");
        newClass.add(newSchema);

        anyTypeClassDAO.save(newClass);

        anyTypeClassDAO.flush();

        newClass = anyTypeClassDAO.find(newClass.getKey());
        assertNotNull(newClass);
        assertEquals(1, newClass.getPlainSchemas().size());
        assertEquals(newSchema, newClass.getPlainSchemas().get(0));
        assertEquals(newClass, newClass.getPlainSchemas().get(0).getAnyTypeClass());

        newSchema = plainSchemaDAO.find(newSchema.getKey());
        assertNotNull(newSchema.getAnyTypeClass());
    }

    @Test
    public void delete() {
        AnyTypeClass minimalUser = anyTypeClassDAO.find("minimal user");
        assertNotNull(minimalUser);

        PlainSchema surname = plainSchemaDAO.find("surname");
        assertNotNull(surname);
        assertTrue(minimalUser.getPlainSchemas().contains(surname));
        int before = minimalUser.getPlainSchemas().size();

        plainSchemaDAO.delete("surname");

        anyTypeClassDAO.flush();

        minimalUser = anyTypeClassDAO.find("minimal user");
        assertNotNull(minimalUser);
        assertEquals(before, minimalUser.getPlainSchemas().size() + 1);
    }
}
