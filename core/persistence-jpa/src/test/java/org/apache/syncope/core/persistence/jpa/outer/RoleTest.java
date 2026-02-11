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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private DelegationDAO delegationDAO;

    @Test
    public void delete() {
        // 0. create role
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_LIST);
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);

        role = roleDAO.save(role);
        assertNotNull(role);

        // 1. create user and assign that role
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        user.add(role);

        user = userDAO.save(user);
        assertNotNull(user);

        // 2. remove role
        roleDAO.delete(role);

        entityManager.flush();

        // 3. verify that role was removed from user
        user = userDAO.findById(user.getKey()).orElseThrow();
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    public void deleteCascadeOnDelegations() {
        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        User rossini = userDAO.findByUsername("rossini").orElseThrow();

        Role reviewer = roleDAO.findById("User reviewer").orElseThrow();

        Delegation delegation = entityFactory.newEntity(Delegation.class);
        delegation.setDelegating(bellini);
        delegation.setDelegated(rossini);
        delegation.setStart(OffsetDateTime.now());
        delegation.add(reviewer);
        delegation = delegationDAO.save(delegation);

        entityManager.flush();

        delegation = delegationDAO.findById(delegation.getKey()).orElseThrow();

        assertEquals(List.of(delegation), delegationDAO.findByRoles(reviewer));

        roleDAO.delete(reviewer);

        entityManager.flush();

        assertTrue(delegationDAO.findById(delegation.getKey()).orElseThrow().getRoles().isEmpty());
    }
}
