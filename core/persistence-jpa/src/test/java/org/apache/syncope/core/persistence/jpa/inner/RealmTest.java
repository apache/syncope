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
import org.apache.commons.collections4.CollectionUtils;
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
        Realm realm = realmDAO.find(1L);
        assertNotNull(realm);
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getName());
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getFullPath());

        realm = realmDAO.find(3L);
        assertNotNull(realm);
        assertEquals("even", realm.getName());
        assertEquals("/even", realm.getFullPath());
        assertEquals(1, realm.getParent().getKey(), 0);
        assertEquals(realmDAO.getRoot(), realm.getParent());

        realm = realmDAO.find("/even/two");
        assertNotNull(realm);
        assertEquals(4, realm.getKey(), 0);
        assertEquals("two", realm.getName());
        assertEquals("/even/two", realm.getFullPath());
    }

    @Test(expected = MalformedPathException.class)
    public void findInvalidPath() {
        realmDAO.find("even/two");
    }

    @Test
    public void findChildren() {
        List<Realm> children = realmDAO.findChildren(realmDAO.find(SyncopeConstants.ROOT_REALM));
        assertEquals(2, children.size());
        assertTrue(children.contains(realmDAO.find("/odd")));
        assertTrue(children.contains(realmDAO.find("/even")));

        children = realmDAO.findChildren(realmDAO.find("/odd"));
        assertTrue(children.isEmpty());
    }

    @Test
    public void findDescendants() {
        assertTrue(CollectionUtils.disjunction(realmDAO.findAll(), realmDAO.findDescendants(realmDAO.getRoot())).
                isEmpty());
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
        realm.setParent(realmDAO.find("/even/two"));
        assertNull(realm.getKey());

        Realm actual = realmDAO.save(realm);
        assertNotNull(actual.getKey());
        assertEquals("last", actual.getName());
        assertEquals("/even/two/last", actual.getFullPath());
        assertEquals(realmDAO.find("/even/two"), actual.getParent());
        assertEquals(5L, realm.getAccountPolicy().getKey(), 0);
        assertEquals(2L, realm.getPasswordPolicy().getKey(), 0);

        realm = actual;
        realm.setAccountPolicy((AccountPolicy) policyDAO.find(6L));
        realm.setPasswordPolicy((PasswordPolicy) policyDAO.find(4L));

        actual = realmDAO.save(realm);
        assertEquals(6L, actual.getAccountPolicy().getKey(), 0);
        assertEquals(4L, actual.getPasswordPolicy().getKey(), 0);
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

        Long key = actual.getKey();
        assertNotNull(realmDAO.find(key));

        realmDAO.delete(key);
        assertNull(realmDAO.find(key));
    }
}
