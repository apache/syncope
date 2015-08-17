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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
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
    public void find() {
        AnyTypeClass minimalGroup = anyTypeClassDAO.find("minimal group");
        assertNotNull(minimalGroup);

        assertFalse(minimalGroup.getPlainSchemas().isEmpty());
        assertFalse(minimalGroup.getDerSchemas().isEmpty());
        assertFalse(minimalGroup.getVirSchemas().isEmpty());
    }

    @Test
    public void findAll() {
        List<AnyTypeClass> list = anyTypeClassDAO.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    public void save() {
        AnyTypeClass newClass = entityFactory.newEntity(AnyTypeClass.class);
        newClass.setKey("new class");
        newClass.add(plainSchemaDAO.find("firstname"));

        newClass = anyTypeClassDAO.save(newClass);
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertTrue(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());
    }

    @Test
    public void delete() {
        AnyTypeClass minimalUser = anyTypeClassDAO.find("minimal user");
        assertNotNull(minimalUser);

        anyTypeClassDAO.delete(minimalUser.getKey());
        assertNull(anyTypeClassDAO.find("minimal user"));
    }
}
