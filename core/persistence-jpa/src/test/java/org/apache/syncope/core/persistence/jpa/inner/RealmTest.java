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
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RealmTest extends AbstractTest {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void getRoot() {
        assertNotNull(realmDAO.getRoot());
    }

    @Test
    public void find() {
        Realm realm = realmDAO.find("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28");
        assertNotNull(realm);
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getName());
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getFullPath());

        realm = realmDAO.find("c5b75db1-fce7-470f-b780-3b9934d82a9d");
        assertNotNull(realm);
        assertEquals("even", realm.getName());
        assertEquals("/even", realm.getFullPath());
        assertEquals("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28", realm.getParent().getKey());
        assertEquals(realmDAO.getRoot(), realm.getParent());

        realm = realmDAO.findByFullPath("/even/two");
        assertNotNull(realm);
        assertEquals("0679e069-7355-4b20-bd11-a5a0a5453c7c", realm.getKey());
        assertEquals("two", realm.getName());
        assertEquals("/even/two", realm.getFullPath());
    }

    @Test(expected = MalformedPathException.class)
    public void findInvalidPath() {
        realmDAO.findByFullPath("even/two");
    }

    @Test
    public void findChildren() {
        List<Realm> children = realmDAO.findChildren(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM));
        assertEquals(2, children.size());
        assertTrue(children.contains(realmDAO.findByFullPath("/odd")));
        assertTrue(children.contains(realmDAO.findByFullPath("/even")));

        children = realmDAO.findChildren(realmDAO.findByFullPath("/odd"));
        assertTrue(children.isEmpty());
    }

    @Test
    public void findAll() {
        List<Realm> list = realmDAO.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        for (Realm realm : list) {
            assertNotNull(realm);
        }
    }

    @Test
    public void save() {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setName("last");
        realm.setParent(realmDAO.findByFullPath("/even/two"));
        assertNull(realm.getKey());

        Realm actual = realmDAO.save(realm);
        assertNotNull(actual.getKey());
        assertEquals("last", actual.getName());
        assertEquals("/even/two/last", actual.getFullPath());
        assertEquals(realmDAO.findByFullPath("/even/two"), actual.getParent());
        assertEquals("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", realm.getAccountPolicy().getKey());
        assertEquals("ce93fcda-dc3a-4369-a7b0-a6108c261c85", realm.getPasswordPolicy().getKey());

        realm = actual;
        realm.setAccountPolicy(
                (AccountPolicy) policyDAO.find("06e2ed52-6966-44aa-a177-a0ca7434201f"));
        realm.setPasswordPolicy(
                (PasswordPolicy) policyDAO.find("986d1236-3ac5-4a19-810c-5ab21d79cba1"));

        actual = realmDAO.save(realm);
        assertEquals("06e2ed52-6966-44aa-a177-a0ca7434201f", actual.getAccountPolicy().getKey());
        assertEquals("986d1236-3ac5-4a19-810c-5ab21d79cba1", actual.getPasswordPolicy().getKey());
    }

    @Test
    public void saveInvalidName() {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setName(" a name");
        realm.setParent(realmDAO.getRoot());

        try {
            realmDAO.save(realm);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidRealm));
        }
    }

    @Test
    public void saveNullParent() {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setName("name");
        realm.setParent(null);

        try {
            realmDAO.save(realm);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidRealm));
        }
    }

    @Test
    public void delete() {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setName("name");
        realm.setParent(realmDAO.getRoot());

        Realm actual = realmDAO.save(realm);
        assertNotNull(actual);

        String id = actual.getKey();
        assertNotNull(realmDAO.find(id));

        realmDAO.delete(id);
        assertNull(realmDAO.find(id));
    }
}
