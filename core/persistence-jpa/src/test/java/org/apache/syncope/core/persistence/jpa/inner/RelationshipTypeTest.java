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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RelationshipTypeTest extends AbstractTest {

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Test
    public void find() {
        RelationshipType inclusion = relationshipTypeDAO.find("inclusion");
        assertNotNull(inclusion);
        assertEquals("inclusion", inclusion.getKey());
    }

    @Test
    public void findAll() {
        List<RelationshipType> list = relationshipTypeDAO.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    public void save() {
        RelationshipType newType = entityFactory.newEntity(RelationshipType.class);
        newType.setKey("new type");
        newType.setDescription("description");

        newType = relationshipTypeDAO.save(newType);
        assertNotNull(newType);
        assertEquals("description", newType.getDescription());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidName() {
        RelationshipType newType = entityFactory.newEntity(RelationshipType.class);
        newType.setKey("membership");
        relationshipTypeDAO.save(newType);
    }

    @Test
    public void delete() {
        RelationshipType type = relationshipTypeDAO.find("neighborhood");
        assertNotNull(type);

        relationshipTypeDAO.delete(type.getKey());
        assertNull(relationshipTypeDAO.find("neighborhood"));
    }

    @Test
    public void deleteOnAnyObject() {
        RelationshipType neighborhood = relationshipTypeDAO.find("neighborhood");
        assertNotNull(neighborhood);

        AnyObject anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObject);
        assertNotNull(anyObject.getRelationships(neighborhood));
        assertFalse(anyObject.getRelationships(neighborhood).isEmpty());

        relationshipTypeDAO.delete("neighborhood");

        relationshipTypeDAO.flush();

        anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObject);
        assertTrue(anyObject.getRelationships().isEmpty());
    }
}
