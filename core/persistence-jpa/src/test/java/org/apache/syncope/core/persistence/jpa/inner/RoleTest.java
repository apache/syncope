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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Assertions;
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

    @Test
    public void find() {
        Role role = roleDAO.findById("User manager").orElseThrow();
        assertNotNull(role.getKey());
        assertFalse(role.getRealms().isEmpty());
        assertFalse(role.getEntitlements().isEmpty());
        assertTrue(role.getEntitlements().contains(IdRepoEntitlement.USER_SEARCH));
    }

    @Test
    public void findAll() {
        List<? extends Role> list = roleDAO.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        list.forEach(Assertions::assertNotNull);
    }

    @Test
    public void save() {
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_LIST);
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);

        Role actual = roleDAO.save(role);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        assertTrue(roleDAO.findById("Other").isPresent());

        roleDAO.deleteById("Other");

        assertTrue(roleDAO.findById("Other").isEmpty());
    }
}
