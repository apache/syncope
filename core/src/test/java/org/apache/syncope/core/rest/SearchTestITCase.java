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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SearchTestITCase extends AbstractTest {

    @Test
    public void searchUser() {
        // LIKE
        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                        is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());

        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());

        Set<Long> userIds = new HashSet<Long>(users.getResult().size());
        for (UserTO user : users.getResult()) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(2L));
        assertTrue(userIds.contains(3L));
    }

    @Test
    public void searchUserIgnoreCase() {
        PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                        is("username").equalToIgnoreCase("RoSsINI").and("id").lessThan(2).query());

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());

        matchingUsers = userService.search("(fullname=~*oSsINi)");
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
    }

    @Test
    public void searchByUsernameAndId() {
        PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                        is("username").equalTo("rossini").and("id").lessThan(2).query());

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals(1L, matchingUsers.getResult().iterator().next().getId());
    }

    @Test
    public void searchByRolenameAndId() {
        PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("name").equalTo("root").and("id").lessThan(2).query());

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());
        assertEquals("root", matchingRoles.getResult().iterator().next().getName());
        assertEquals(1L, matchingRoles.getResult().iterator().next().getId());
    }

    @Test
    public void searchUserByResourceName() {
        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().hasResources(RESOURCE_NAME_MAPPINGS2).query());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());

        Set<Long> userIds = new HashSet<Long>(users.getResult().size());
        for (UserTO user : users.getResult()) {
            userIds.add(user.getId());
        }

        assertEquals(1, userIds.size());
        assertTrue(userIds.contains(2L));
    }

    @Test
    public void paginatedSearch() {
        // LIKE
        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                        is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query(), 1, 2);
        assertNotNull(users);

        assertFalse(users.getResult().isEmpty());
        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query(), 1, 2);

        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());
        Set<Long> userIds = new HashSet<Long>(users.getResult().size());
        for (UserTO user : users.getResult()) {
            userIds.add(user.getId());
        }
        assertEquals(2, userIds.size());
    }

    @Test
    public void searchByBooleanSubjectCond() {
        PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("inheritAttrs").equalTo("true").query());
        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.getResult().isEmpty());
    }

    @Test
    public void searchByDate() {
        clientFactory.create("bellini", "password").getService(UserSelfService.class).read();

        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                        is("lastLoginDate").lexicalNotBefore("2016-03-02 15:21:22").
                        and("username").equalTo("bellini").query());
        assertNotNull(users);
        assertEquals(1, users.getTotalCount());
        assertEquals(1, users.getResult().size());
    }

    @Test
    public void searchByEntitlement() {
        PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().hasEntitlements("USER_LIST", "USER_READ").query());
        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.getResult().isEmpty());
    }

    @Test
    public void searchByRelationshipSubjectCond() {
        PagedResult<RoleTO> matchingRoles = roleService.search(SyncopeClient.getRoleSearchConditionBuilder().
                isNotNull("passwordPolicy").and("userOwner").equalTo(5).query());

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());
        assertEquals("director", matchingRoles.getResult().iterator().next().getName());
        assertEquals(6L, matchingRoles.getResult().iterator().next().getId());
    }

    @Test
    public void nested() {
        PagedResult<UserTO> users = userService.search(
                "((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)", 1, 2);
        assertNotNull(users);

        assertFalse(users.getResult().isEmpty());
        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void orderBy() {
        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query(),
                SyncopeClient.getOrderByClauseBuilder().asc("status").desc("firstname").build());

        assertFalse(users.getResult().isEmpty());
        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void issueSYNCOPE712() {
        PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("parent").equalTo(1L).query());

        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.getResult().isEmpty());
    }

    @Test
    public void issueSYNCOPE768() {
        final List<UserTO> usersWithType = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("type").notNullValue().query()).getResult();

        assertFalse(usersWithType.isEmpty());

        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query(),
                SyncopeClient.getOrderByClauseBuilder().asc("type").build());

        assertTrue(users.getResult().size() > usersWithType.size());
    }

    @Test
    public void issueSYNCOPE929() {
        PagedResult<UserTO> matchingUsers = userService.search("(surname==Rossini,gender==M);surname==Bellini");

        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertTrue(user.getUsername().startsWith("bellini"));
        }
    }

    @Test
    public void issueSYNCOPE983() {
        PagedResult<UserTO> users = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("surname").equalTo("*o*").query(),
                SyncopeClient.getOrderByClauseBuilder().asc("surname").desc("username").build());
        assertNotEquals(0, users.getTotalCount());
    }
}
