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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RelationshipTypeTest extends AbstractTest {

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void find() {
        RelationshipType inclusion = relationshipTypeDAO.findById("inclusion").orElseThrow();
        assertEquals("inclusion", inclusion.getKey());
        assertEquals(anyTypeDAO.findById("PRINTER").orElseThrow(), inclusion.getLeftEndAnyType());
        assertEquals(anyTypeDAO.findById("PRINTER").orElseThrow(), inclusion.getRightEndAnyType());
    }

    @Test
    public void findByEndAnyType() {
        List<String> relationshipTypes =
                relationshipTypeDAO.findByEndAnyType(anyTypeDAO.findById("PRINTER").orElseThrow());
        assertEquals(2, relationshipTypes.size());
        assertTrue(relationshipTypes.contains("inclusion"));
        assertTrue(relationshipTypes.contains("neighborhood"));
    }

    @Test
    public void findAll() {
        List<? extends RelationshipType> list = relationshipTypeDAO.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    public void save() {
        RelationshipType newType = entityFactory.newEntity(RelationshipType.class);
        newType.setKey("new type");
        newType.setDescription("description");
        newType.setLeftEndAnyType(anyTypeDAO.getGroup());
        newType.setRightEndAnyType(anyTypeDAO.findById("PRINTER").orElseThrow());

        newType = relationshipTypeDAO.save(newType);
        assertNotNull(newType);
        assertEquals("description", newType.getDescription());
    }

    @Test
    public void saveInvalidName() {
        assertThrows(InvalidEntityException.class, () -> {
            RelationshipType newType = entityFactory.newEntity(RelationshipType.class);
            newType.setKey("membership");
            relationshipTypeDAO.save(newType);
        });
    }

    @Test
    public void delete() {
        RelationshipType type = relationshipTypeDAO.findById("neighborhood").orElseThrow();

        relationshipTypeDAO.deleteById(type.getKey());

        assertTrue(relationshipTypeDAO.findById("neighborhood").isEmpty());
    }

    @Test
    public void deleteOnAnyObject() {
        RelationshipType neighborhood = relationshipTypeDAO.findById("inclusion").orElseThrow();

        AnyObject anyObject = anyObjectDAO.findById("fc6dbc3a-6c07-4965-8781-921e7401a4a5").orElseThrow();
        assertNotNull(anyObject.getRelationships(neighborhood));
        assertFalse(anyObject.getRelationships(neighborhood).isEmpty());

        relationshipTypeDAO.deleteById("inclusion");

        anyObject = anyObjectDAO.findById("fc6dbc3a-6c07-4965-8781-921e7401a4a5").orElseThrow();
        assertNotNull(anyObject);
        assertTrue(anyObject.getRelationships().isEmpty());
    }
}
