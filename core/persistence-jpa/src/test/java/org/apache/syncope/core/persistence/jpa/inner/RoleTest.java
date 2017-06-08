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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void find() {
        Role role = roleDAO.find("User manager");
        assertNotNull(role);
        assertNotNull(role.getKey());
        assertFalse(role.getRealms().isEmpty());
        assertFalse(role.getEntitlements().isEmpty());
        assertTrue(role.getEntitlements().contains(StandardEntitlement.USER_SEARCH));
    }

    @Test
    public void findAll() {
        List<Role> list = roleDAO.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        for (Role role : list) {
            assertNotNull(role);
        }
    }

    @Test
    public void save() {
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmDAO.findByFullPath("/even/two"));
        role.getEntitlements().add(StandardEntitlement.LOG_LIST);
        role.getEntitlements().add(StandardEntitlement.LOG_SET_LEVEL);

        Role actual = roleDAO.save(role);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        assertNotNull(roleDAO.find("Other"));

        roleDAO.delete("Other");
        assertNull(roleDAO.find("Other"));
    }
}
