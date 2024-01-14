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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
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
    private RealmDAO realmDAO;

    @Test
    public void find() {
        AnyObject anyObject = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        assertNotNull(anyObject.getType());
        assertFalse(anyObject.getType().getClasses().isEmpty());
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
    }

    @Test
    public void findAll() {
        List<AnyObject> anyObjects = anyObjectDAO.findAll(1, 100);
        assertNotNull(anyObjects);

        List<String> anyObjectKeys = anyObjectDAO.findAllKeys(1, 100);
        assertNotNull(anyObjectKeys);

        assertEquals(anyObjects.size(), anyObjectKeys.size());
    }

    @Test
    public void save() {
        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("a name");
        anyObject.setType(anyTypeDAO.findById("PRINTER").orElseThrow());
        anyObject.setRealm(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());

        anyObject = anyObjectDAO.save(anyObject);
        assertNotNull(anyObject);
    }

    @Test
    public void delete() {
        AnyObject anyObject = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        anyObjectDAO.deleteById(anyObject.getKey());

        AnyObject actual = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElse(null);
        assertNull(actual);
    }
}
