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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnySearchTest extends AbstractTest {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Test
    public void issueSYNCOPE95() {
        for (Group group : groupDAO.findAll(1, 100)) {
            groupDAO.delete(group.getKey());
        }
        groupDAO.flush();

        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        SearchCond cond = SearchCond.getLeafCond(coolLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.get(0).getKey());
    }

    @Test
    public void searchByDynMembership() {
        // 1. create role with dynamic membership
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmDAO.findByFullPath("/even/two"));
        role.getEntitlements().add(StandardEntitlement.LOG_LIST);
        role.getEntitlements().add(StandardEntitlement.LOG_SET_LEVEL);

        DynRoleMembership dynMembership = entityFactory.newEntity(DynRoleMembership.class);
        dynMembership.setFIQLCond("cool==true");
        dynMembership.setRole(role);

        role.setDynMembership(dynMembership);

        role = roleDAO.save(role);
        assertNotNull(role);

        roleDAO.flush();

        // 2. search user by this dynamic role
        RoleCond roleCond = new RoleCond();
        roleCond.setRole(role.getKey());

        List<User> users = searchDAO.search(SearchCond.getLeafCond(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.get(0).getKey());
    }
}
