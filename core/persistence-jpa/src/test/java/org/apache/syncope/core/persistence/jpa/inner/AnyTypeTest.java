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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnyTypeTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void find() {
        AnyType userType = anyTypeDAO.getUser();
        assertNotNull(userType);
        assertEquals(AnyTypeKind.USER, userType.getKind());
        assertEquals(AnyTypeKind.USER.name(), userType.getKey());
        assertFalse(userType.getClasses().isEmpty());

        AnyType groupType = anyTypeDAO.getGroup();
        assertNotNull(groupType);
        assertEquals(AnyTypeKind.GROUP, groupType.getKind());
        assertEquals(AnyTypeKind.GROUP.name(), groupType.getKey());
        assertFalse(groupType.getClasses().isEmpty());

        AnyType otherType = anyTypeDAO.findById("PRINTER").orElseThrow();
        assertEquals(AnyTypeKind.ANY_OBJECT, otherType.getKind());
        assertEquals("PRINTER", otherType.getKey());
    }

    @Test
    public void findAll() {
        List<? extends AnyType> list = anyTypeDAO.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    public void save() {
        AnyType newType = entityFactory.newEntity(AnyType.class);
        newType.setKey("new type");
        newType.setKind(AnyTypeKind.ANY_OBJECT);
        newType.add(anyTypeClassDAO.findById("generic membership").orElseThrow());
        newType.add(anyTypeClassDAO.findById("csv").orElseThrow());

        newType = anyTypeDAO.save(newType);
        assertNotNull(newType);
        assertFalse(newType.getClasses().isEmpty());
    }

    @Test
    public void saveInvalidKind() {
        assertThrows(InvalidEntityException.class, () -> {
            AnyType newType = entityFactory.newEntity(AnyType.class);
            newType.setKey("new type");
            newType.setKind(AnyTypeKind.USER);
            anyTypeDAO.save(newType);
            entityManager.flush();
        });
    }

    @Test
    public void saveInvalidName() {
        assertThrows(InvalidEntityException.class, () -> {
            AnyType newType = entityFactory.newEntity(AnyType.class);
            newType.setKey("group");
            newType.setKind(AnyTypeKind.ANY_OBJECT);
            anyTypeDAO.save(newType);
            entityManager.flush();
        });
    }

    @Test
    public void delete() {
        AnyType otherType = anyTypeDAO.findById("PRINTER").orElseThrow();
        assertNotNull(otherType);

        anyTypeDAO.deleteById(otherType.getKey());
        assertTrue(anyTypeDAO.findById("PRINTER").isEmpty());
    }

    @Test
    public void deleteInvalid() {
        assertThrows(IllegalArgumentException.class, () -> anyTypeDAO.deleteById(AnyTypeKind.USER.name()));
    }
}
