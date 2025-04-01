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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RealmTest extends AbstractTest {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void getRoot() {
        assertNotNull(realmDAO.getRoot());
    }

    @Test
    public void find() {
        Realm realm = realmDAO.findById("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28").orElseThrow();
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getName());
        assertEquals(SyncopeConstants.ROOT_REALM, realm.getFullPath());

        realm = realmDAO.findById("c5b75db1-fce7-470f-b780-3b9934d82a9d").orElseThrow();
        assertEquals("even", realm.getName());
        assertEquals("/even", realm.getFullPath());
        assertEquals("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28", realm.getParent().getKey());
        assertEquals(realmDAO.getRoot(), realm.getParent());

        realm = realmSearchDAO.findByFullPath("/even/two").orElseThrow();
        assertEquals("0679e069-7355-4b20-bd11-a5a0a5453c7c", realm.getKey());
        assertEquals("two", realm.getName());
        assertEquals("/even/two", realm.getFullPath());
    }

    @Test
    public void findInvalidPath() {
        assertThrows(MalformedPathException.class, () -> realmSearchDAO.findByFullPath("even/two"));
    }

    @Test
    public void findChildren() {
        List<Realm> children = realmSearchDAO.findChildren(
                realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());
        assertEquals(2, children.size());
        assertTrue(children.contains(realmSearchDAO.findByFullPath("/odd").orElseThrow()));
        assertTrue(children.contains(realmSearchDAO.findByFullPath("/even").orElseThrow()));

        children = realmSearchDAO.findChildren(realmSearchDAO.findByFullPath("/odd").orElseThrow());
        assertTrue(children.isEmpty());
    }

    @Test
    public void findAll() {
        List<Realm> list = realmSearchDAO.findDescendants(realmDAO.getRoot().getFullPath(), null, Pageable.unpaged());
        assertNotNull(list);
        assertFalse(list.isEmpty());
        list.forEach(Assertions::assertNotNull);

        assertEquals(4, realmDAO.findAll(Pageable.unpaged()).stream().count());

        list = realmSearchDAO.findDescendants(Set.of("/even", "/odd"), null, Pageable.unpaged());
        assertEquals(3, list.size());
        assertNotNull(list.stream().filter(realm -> "even".equals(realm.getName())).findFirst().orElseThrow());
        assertNotNull(list.stream().filter(realm -> "two".equals(realm.getName())).findFirst().orElseThrow());
        assertNotNull(list.stream().filter(realm -> "odd".equals(realm.getName())).findFirst().orElseThrow());
    }

    @Test
    public void save() {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setName("last");
        realm.setParent(realmSearchDAO.findByFullPath("/even/two").orElseThrow());

        Realm actual = realmDAO.save(realm);
        assertNotNull(actual.getKey());
        assertEquals("last", actual.getName());
        assertEquals("/even/two/last", actual.getFullPath());
        assertEquals(realmSearchDAO.findByFullPath("/even/two").orElseThrow(), actual.getParent());
        assertEquals("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", realm.getAccountPolicy().getKey());
        assertEquals("ce93fcda-dc3a-4369-a7b0-a6108c261c85", realm.getPasswordPolicy().getKey());

        realm = actual;
        realm.setAccountPolicy(policyDAO.findById(
                "06e2ed52-6966-44aa-a177-a0ca7434201f", AccountPolicy.class).orElseThrow());
        realm.setPasswordPolicy(policyDAO.findById(
                "986d1236-3ac5-4a19-810c-5ab21d79cba1", PasswordPolicy.class).orElseThrow());

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
            fail("This should not happen");
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
            fail("This should not happen");
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

        actual = realmDAO.findById(actual.getKey()).orElseThrow();
        assertNotNull(actual);

        realmDAO.delete(actual);
        assertTrue(realmDAO.findById(actual.getKey()).isEmpty());
    }
}
