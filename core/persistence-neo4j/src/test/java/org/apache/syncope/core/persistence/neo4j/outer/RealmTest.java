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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RealmTest extends AbstractTest {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void test() {
        Realm realm = realmSearchDAO.findByFullPath("/odd").orElseThrow();

        // need to remove this group in order to remove the realm, which is otherwise empty
        Group group = groupDAO.findByName("fake").orElseThrow();
        assertEquals(realm, group.getRealm());
        groupDAO.delete(group);

        Role role = roleDAO.findById("User reviewer").orElseThrow();
        assertTrue(role.getRealms().contains(realm));

        int beforeSize = role.getRealms().size();

        realmDAO.delete(realm);

        role = roleDAO.findById("User reviewer").orElseThrow();
        assertEquals(beforeSize - 1, role.getRealms().size());
    }

    @Test
    public void addAndRemoveLogicActions() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdRepoImplementationType.LOGIC_ACTIONS);
        implementation.setBody("TestLogicActions");
        implementation = implementationDAO.save(implementation);

        Realm realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertTrue(realm.getActions().isEmpty());

        realm.add(implementation);
        realmDAO.save(realm);

        realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertEquals(1, realm.getActions().size());
        assertEquals(implementation, realm.getActions().getFirst());

        realm.getActions().clear();
        realm = realmDAO.save(realm);
        assertTrue(realm.getActions().isEmpty());

        realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertTrue(realm.getActions().isEmpty());
    }
}
