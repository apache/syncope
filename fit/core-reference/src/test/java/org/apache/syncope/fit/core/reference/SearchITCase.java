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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SearchITCase extends AbstractITCase {

    @Test
    public void searchUser() {
        // LIKE
        PagedResult<UserTO> matchedUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query());
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getResult().isEmpty());

        for (UserTO user : matchedUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchedUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query());
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getResult().isEmpty());

        Set<Long> userIds = new HashSet<Long>(matchedUsers.getResult().size());
        for (UserTO user : matchedUsers.getResult()) {
            userIds.add(user.getKey());
        }
        assertTrue(userIds.contains(2L));
        assertTrue(userIds.contains(3L));
    }

    @Test
    public void searchByUsernameAndKey() {
        final PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                is("username").equalTo("rossini").and("key").lessThan(2).query());

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals(1L, matchingUsers.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByRolenameAndKey() {
        final PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().
                is("name").equalTo("root").and("key").lessThan(2).query());

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());
        assertEquals("root", matchingRoles.getResult().iterator().next().getName());
        assertEquals(1L, matchingRoles.getResult().iterator().next().getKey());
    }

    @Test
    public void searchUserByResourceName() {
        PagedResult<UserTO> matchedUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().hasResources(RESOURCE_NAME_MAPPINGS2).query());
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getResult().isEmpty());

        Set<Long> userIds = new HashSet<Long>(matchedUsers.getResult().size());
        for (UserTO user : matchedUsers.getResult()) {
            userIds.add(user.getKey());
        }

        assertEquals(1, userIds.size());
        assertTrue(userIds.contains(2L));
    }

    @Test
    public void paginatedSearch() {
        // LIKE
        PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query(), 1, 2);
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query(), 1, 2);

        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());
        Set<Long> userIds = new HashSet<>(matchingUsers.getResult().size());
        for (UserTO user : matchingUsers.getResult()) {
            userIds.add(user.getKey());
        }
        assertEquals(2, userIds.size());
    }

    @Test
    public void searchByBooleanSubjectCond() {
        final PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("inheritPlainAttrs").equalTo("true").query());
        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.getResult().isEmpty());
    }

    @Test
    public void searchByEntitlement() {
        final PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().hasEntitlements("USER_LIST", "USER_READ").query());
        assertNotNull(matchingRoles);
        assertFalse(matchingRoles.getResult().isEmpty());
    }

    @Test
    public void searchByRelationshipSubjectCond() {
        final PagedResult<RoleTO> matchingRoles = roleService.search(SyncopeClient.getRoleSearchConditionBuilder().
                isNotNull("passwordPolicy").and("userOwner").equalTo(5).query());

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());
        assertEquals("director", matchingRoles.getResult().iterator().next().getName());
        assertEquals(6L, matchingRoles.getResult().iterator().next().getKey());
    }

    @Test
    public void nested() {
        PagedResult<UserTO> matchedUsers = userService.search(
                "((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)", 1, 2);
        assertNotNull(matchedUsers);

        assertFalse(matchedUsers.getResult().isEmpty());
        for (UserTO user : matchedUsers.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void orderBy() {
        final PagedResult<UserTO> matchedUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query(),
                SyncopeClient.getOrderByClauseBuilder().asc("status").desc("firstname").build());

        assertFalse(matchedUsers.getResult().isEmpty());
        for (UserTO user : matchedUsers.getResult()) {
            assertNotNull(user);
        }
    }
}
