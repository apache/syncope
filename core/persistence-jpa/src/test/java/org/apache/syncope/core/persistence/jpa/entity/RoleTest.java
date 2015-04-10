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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void find() {
        Role role1 = roleDAO.find(2L);
        assertNotNull(role1);
        assertNotNull(role1.getName());
        assertFalse(searchDAO.<User>search(
                GroupEntitlementUtil.getGroupKeys(entitlementDAO.findAll()),
                SearchCondConverter.convert(role1.getCriteria()),
                Collections.<OrderByClause>emptyList(), SubjectType.USER).isEmpty());
        assertFalse(role1.getRealms().isEmpty());
        assertFalse(role1.getEntitlements().isEmpty());
        assertTrue(role1.getEntitlements().contains(Entitlement.USER_LIST));

        Role role2 = roleDAO.find(role1.getName());
        assertEquals(role1, role2);
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
        role.setName("new");
        role.setCriteria(new UserFiqlSearchConditionBuilder().inGroups(2L).query());
        role.addRealm(realmDAO.getRoot());
        role.addRealm(realmDAO.find("/even/two"));
        role.getEntitlements().add(Entitlement.LOG_LIST);
        role.getEntitlements().add(Entitlement.LOG_SET_LEVEL);

        Role actual = roleDAO.save(role);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        assertNotNull(roleDAO.find(3L));

        roleDAO.delete(3L);
        assertNull(roleDAO.find(3L));
    }
}
