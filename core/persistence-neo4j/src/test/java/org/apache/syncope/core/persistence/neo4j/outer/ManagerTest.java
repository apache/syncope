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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ManagerTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmDAO realmDAO;

    /**
     * (a) U2 has uManager set to U1
     */
    @Test
    void findUManagedUsersA() {
        User vivaldi = userDAO.findByUsername("vivaldi").orElseThrow();
        assertNull(vivaldi.getgManager());
        assertNull(vivaldi.getuManager());

        User rossini = userDAO.findByUsername("rossini").orElseThrow();
        assertFalse(userDAO.isManager(rossini.getKey()));

        vivaldi.setuManager(rossini);
        vivaldi = userDAO.save(vivaldi);
        assertEquals(rossini, vivaldi.getuManager());
        assertTrue(userDAO.isManager(rossini.getKey()));

        List<User> managed = userDAO.findManagedUsers(rossini.getKey());
        assertEquals(List.of(vivaldi), managed);
    }

    /**
     * (b) G2 has uManager set to U1
     */
    @Test
    void findUManagedUsersB() {
        Group artDirector = groupDAO.findByName("artDirector").orElseThrow();
        assertNull(artDirector.getuManager());
        assertNull(artDirector.getgManager());

        User vivaldi = userDAO.findByUsername("vivaldi").orElseThrow();
        assertFalse(userDAO.isManager(vivaldi.getKey()));

        artDirector.setuManager(vivaldi);
        artDirector = groupDAO.save(artDirector);
        assertEquals(vivaldi, artDirector.getuManager());
        assertTrue(userDAO.isManager(vivaldi.getKey()));

        User puccini = userDAO.findByUsername("puccini").orElseThrow();
        assertNull(puccini.getgManager());
        assertNull(puccini.getuManager());
        assertTrue(puccini.getMembership(artDirector.getKey()).isPresent());

        List<User> managed = userDAO.findManagedUsers(vivaldi.getKey());
        assertEquals(List.of(puccini), managed);
    }

    /**
     * (c) U2 has gManager set to G1
     */
    @Test
    void findUManagedUsersC() {
        Group managingDirector = groupDAO.findByName("managingDirector").orElseThrow();
        assertFalse(groupDAO.isManager(managingDirector.getKey()));

        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        assertTrue(bellini.getMembership(managingDirector.getKey()).isPresent());
        assertFalse(userDAO.isManager(bellini.getKey()));

        User rossini = userDAO.findByUsername("rossini").orElseThrow();
        assertNull(rossini.getgManager());
        assertNull(rossini.getuManager());

        rossini.setgManager(managingDirector);
        rossini = userDAO.save(rossini);
        assertEquals(managingDirector, rossini.getgManager());
        assertTrue(groupDAO.isManager(managingDirector.getKey()));

        List<User> managed = userDAO.findManagedUsers(bellini.getKey());
        assertEquals(List.of(rossini), managed);
    }

    /**
     * (d) G2 has gManager set to G1
     */
    @Test
    void findUManagedUsersD() {
        Group managingDirector = groupDAO.findByName("managingDirector").orElseThrow();
        assertFalse(groupDAO.isManager(managingDirector.getKey()));

        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        assertTrue(bellini.getMembership(managingDirector.getKey()).isPresent());
        assertFalse(userDAO.isManager(bellini.getKey()));

        Group otherchild = groupDAO.findByName("otherchild").orElseThrow();
        assertNull(otherchild.getgManager());
        assertNull(otherchild.getuManager());

        User rossini = userDAO.findByUsername("rossini").orElseThrow();
        assertTrue(rossini.getMembership(otherchild.getKey()).isPresent());
        rossini.setuManager(null);
        rossini.setgManager(null);
        rossini = userDAO.save(rossini);
        assertNull(rossini.getgManager());
        assertNull(rossini.getuManager());

        otherchild.setgManager(managingDirector);
        otherchild = groupDAO.save(otherchild);
        assertEquals(managingDirector, otherchild.getgManager());
        assertTrue(groupDAO.isManager(managingDirector.getKey()));

        List<User> managed = userDAO.findManagedUsers(bellini.getKey());
        assertEquals(List.of(rossini), managed);
    }

    /**
     * (a) G2 has uManager set to U1 - see UserDAO#findManagedGroups
     */
    @Test
    void findUManagedGroupsA() {
        Group director = groupDAO.findByName("director").orElseThrow();
        User puccini = userDAO.findByUsername("puccini").orElseThrow();
        assertEquals(puccini, director.getuManager());
        assertTrue(userDAO.isManager(puccini.getKey()));

        List<Group> managed = userDAO.findManagedGroups(puccini.getKey());
        assertEquals(List.of(director), managed);
    }

    /**
     * (b) G2 has gManager set to G1 - see UserDAO#findManagedGroups
     */
    @Test
    void findUManagedGroupsB() {
        Group managingDirector = groupDAO.findByName("managingDirector").orElseThrow();
        assertFalse(groupDAO.isManager(managingDirector.getKey()));

        Group root = groupDAO.findByName("root").orElseThrow();
        assertNull(root.getgManager());
        assertNull(root.getuManager());

        root.setgManager(managingDirector);
        root = groupDAO.save(root);
        assertEquals(managingDirector, root.getgManager());
        assertTrue(groupDAO.isManager(managingDirector.getKey()));

        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        assertTrue(bellini.getMembership(managingDirector.getKey()).isPresent());

        List<Group> managed = userDAO.findManagedGroups(bellini.getKey());
        assertEquals(List.of(root), managed);
    }

    /**
     * (a) U has gManager set to G1
     */
    @Test
    void findGManagedUsersA() {
        Group citizen = groupDAO.findByName("citizen").orElseThrow();
        assertFalse(groupDAO.isManager(citizen.getKey()));

        User verdi = userDAO.findByUsername("verdi").orElseThrow();
        assertNull(verdi.getgManager());
        assertNull(verdi.getuManager());

        verdi.setgManager(citizen);
        verdi = userDAO.save(verdi);
        assertEquals(citizen, verdi.getgManager());
        assertTrue(groupDAO.isManager(citizen.getKey()));

        List<User> managed = groupDAO.findManagedUsers(citizen.getKey());
        assertEquals(List.of(verdi), managed);
    }

    /**
     * (b) G2 has gManager set to G1
     */
    @Test
    void findGManagedUsersB() {
        Group managingDirector = groupDAO.findByName("managingDirector").orElseThrow();
        assertFalse(groupDAO.isManager(managingDirector.getKey()));

        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        assertTrue(bellini.getMembership(managingDirector.getKey()).isPresent());

        Group citizen = groupDAO.findByName("citizen").orElseThrow();
        assertNull(citizen.getgManager());
        assertNull(citizen.getuManager());

        User verdi = userDAO.findByUsername("verdi").orElseThrow();
        assertTrue(verdi.getMembership(citizen.getKey()).isPresent());

        citizen.setgManager(managingDirector);
        citizen = groupDAO.save(citizen);
        assertEquals(managingDirector, citizen.getgManager());
        assertTrue(groupDAO.isManager(managingDirector.getKey()));

        List<User> managed = groupDAO.findManagedUsers(managingDirector.getKey());
        assertEquals(List.of(verdi), managed);
    }

    /**
     * (a) G2 has gManager set to G1 - see GroupDAO#findManagedGroups
     */
    @Test
    void findGManagedGroups() {
        Group root = groupDAO.findByName("root").orElseThrow();
        assertFalse(groupDAO.isManager(root.getKey()));

        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("error");
        group.setgManager(root);
        group = groupDAO.save(group);
        assertEquals(root, group.getgManager());
        assertTrue(groupDAO.isManager(root.getKey()));

        List<Group> managed = groupDAO.findManagedGroups(root.getKey());
        assertEquals(List.of(group), managed);
    }
}
