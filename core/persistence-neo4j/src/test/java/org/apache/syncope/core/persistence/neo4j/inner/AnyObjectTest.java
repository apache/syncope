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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnyObjectTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Test
    public void findAll() {
        List<? extends AnyObject> anyObjects = anyObjectDAO.findAll();
        assertEquals(3, anyObjects.size());
    }

    @Test
    public void find() {
        AnyObject anyObject = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        assertNotNull(anyObject.getType());
        assertFalse(anyObject.getType().getClasses().isEmpty());

        PlainAttr model = anyObject.getPlainAttr("model").orElseThrow();
        assertEquals("HP Laserjet 1300n", model.getValuesAsStrings().getFirst());

        PlainAttr location = anyObject.getPlainAttr("location").orElseThrow();
        assertEquals("2nd floor", location.getValuesAsStrings().getFirst());
    }

    @Test
    public void findByName() {
        AnyObject anyObject = anyObjectDAO.findByName("PRINTER", "HP LJ 1300n").orElseThrow();
        assertNotNull(anyObject);
        assertEquals("fc6dbc3a-6c07-4965-8781-921e7401a4a5", anyObject.getKey());

        assertEquals(1, anyObjectDAO.findByName("HP LJ 1300n").size());
    }

    @Test
    public void findKey() {
        assertEquals(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5",
                anyObjectDAO.findKey("PRINTER", "HP LJ 1300n").orElseThrow());

        assertTrue(anyObjectDAO.findKey("PRINTER", "any").isEmpty());
    }

    @Test
    public void findByKeys() {
        List<AnyObject> found = anyObjectDAO.findByKeys(List.of(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8", "9e1d130c-d6a3-48b1-98b3-182477ed0688", "none"));
        assertEquals(2, found.size());
        assertTrue(found.contains(anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow()));
        assertTrue(found.contains(anyObjectDAO.findById("9e1d130c-d6a3-48b1-98b3-182477ed0688").orElseThrow()));
    }

    @Test
    public void countByType() {
        Map<AnyType, Long> byType = anyObjectDAO.countByType();
        assertEquals(1, byType.size());
        Long count = byType.get(anyTypeDAO.findById("PRINTER").orElseThrow());
        assertEquals(3, count);
    }

    @Test
    public void countByRealm() {
        Map<String, Long> byRealm = anyObjectDAO.countByRealm(anyTypeDAO.findById("PRINTER").orElseThrow());
        assertEquals(2, byRealm.size());
        Long count = byRealm.get(SyncopeConstants.ROOT_REALM);
        assertEquals(2, count);
    }

    @Test
    public void save() {
        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("a name");
        anyObject.setType(anyTypeDAO.findById("PRINTER").orElseThrow());
        anyObject.setRealm(realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());

        anyObject = anyObjectDAO.save(anyObject);
        assertNotNull(anyObject);
    }

    @Test
    public void deleteAttr() {
        AnyObject anyObject = anyObjectDAO.findById("fc6dbc3a-6c07-4965-8781-921e7401a4a5").orElseThrow();
        PlainAttr attr = anyObject.getPlainAttr("location").orElseThrow();
        anyObject.remove(attr);

        anyObjectDAO.save(anyObject);

        anyObject = anyObjectDAO.findById("fc6dbc3a-6c07-4965-8781-921e7401a4a5").orElseThrow();
        assertTrue(anyObject.getPlainAttr("location").isEmpty());
    }

    @Test
    public void delete() {
        AnyObject anyObject = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        anyObjectDAO.deleteById(anyObject.getKey());

        AnyObject actual = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElse(null);
        assertNull(actual);
    }
}
