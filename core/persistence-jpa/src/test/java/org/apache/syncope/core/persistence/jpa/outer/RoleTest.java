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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.persistence.Query;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.dao.JPARoleDAO;
import org.apache.syncope.core.persistence.jpa.entity.user.JPADynRoleMembership;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    /**
     * Static copy of {@link org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO} method with same signature:
     * required for avoiding creating new transaction - good for general use case but bad for the way how
     * this test class is architected.
     */
    private List<Role> findDynRoles(final User user) {
        Query query = entityManager().createNativeQuery(
                "SELECT role_id FROM " + JPARoleDAO.DYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, user.getKey());

        List<Role> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            Role role = roleDAO.find(actualKey);
            if (role != null && !result.contains(role)) {
                result.add(role);
            }
        }
        return result;
    }

    @Test
    public void dynMembership() {
        // 0. create user matching the condition below
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.add(anyTypeClassDAO.find("other"));

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("cool"));
        attr.add("true", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        user = userDAO.save(user);
        String newUserKey = user.getKey();
        assertNotNull(newUserKey);

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

        Role actual = roleDAO.save(role);
        assertNotNull(actual);

        roleDAO.flush();

        // 2. verify that dynamic membership is there
        actual = roleDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getDynMembership());
        assertNotNull(actual.getDynMembership().getKey());
        assertEquals(actual, actual.getDynMembership().getRole());

        // 3. verify that expected users have the created role dynamically assigned
        List<String> members = roleDAO.findDynMembers(actual);
        assertEquals(2, members.size());
        assertEquals(
                new HashSet<>(Arrays.asList("c9b2dec2-00a7-4855-97c0-d854842b4b24", newUserKey)),
                new HashSet<>(members));

        user = userDAO.find("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        assertNotNull(user);
        Collection<Role> dynRoleMemberships = findDynRoles(user);
        assertEquals(1, dynRoleMemberships.size());
        assertTrue(dynRoleMemberships.contains(actual.getDynMembership().getRole()));

        // 4. delete the new user and verify that dynamic membership was updated
        userDAO.delete(newUserKey);

        userDAO.flush();

        actual = roleDAO.find(actual.getKey());
        members = roleDAO.findDynMembers(actual);
        assertEquals(1, members.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", members.get(0));

        // 5. delete role and verify that dynamic membership was also removed
        String dynMembershipKey = actual.getDynMembership().getKey();

        roleDAO.delete(actual);

        roleDAO.flush();

        assertNull(entityManager().find(JPADynRoleMembership.class, dynMembershipKey));

        dynRoleMemberships = findDynRoles(user);
        assertTrue(dynRoleMemberships.isEmpty());
    }

    @Test
    public void delete() {
        // 0. create role
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmDAO.findByFullPath("/even/two"));
        role.getEntitlements().add(StandardEntitlement.LOG_LIST);
        role.getEntitlements().add(StandardEntitlement.LOG_SET_LEVEL);

        role = roleDAO.save(role);
        assertNotNull(role);

        // 1. create user and assign that role
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.add(role);

        user = userDAO.save(user);
        assertNotNull(user);

        // 2. remove role
        roleDAO.delete(role);

        userDAO.flush();

        // 3. verify that role was removed from user
        user = userDAO.find(user.getKey());
        assertNotNull(user);
        assertTrue(user.getRoles().isEmpty());
    }
}
