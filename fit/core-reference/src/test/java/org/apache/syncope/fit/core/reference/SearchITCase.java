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

import java.util.Collection;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SearchITCase extends AbstractITCase {

    @Test
    public void searchUser() {
        // LIKE
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        Collection<UserTO> found = CollectionUtils.select(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 2L || user.getKey() == 3L;
            }
        });
        assertEquals(2, found.size());
    }

    @Test
    public void searchByUsernameAndKey() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("username").equalTo("rossini").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals(1L, matchingUsers.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByGroupNameAndKey() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().
                        is("name").equalTo("root").and("key").lessThan(2).query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals("root", groups.getResult().iterator().next().getName());
        assertEquals(1L, groups.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByGroup() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(1L).query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 1;
            }
        }));
    }

    @Test
    public void searchByDynGroup() {
        GroupTO group = GroupITCase.getBasicSampleTO("dynMembership");
        group.setUDynMembershipCond("cool==true");
        group = createGroup(group).getAny();
        assertNotNull(group);

        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(group.getKey()).query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void searchByRole() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles("Other").query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 1;
            }
        }));
    }

    @Test
    public void searchByDynRole() {
        RoleTO role = RoleITCase.getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = roleService.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles(role.getKey()).query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void searchUserByResourceName() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().hasResources(RESOURCE_NAME_MAPPINGS2).query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 2;
            }
        }));
    }

    @Test
    public void paginatedSearch() {
        // LIKE
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query()).page(1).size(2).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).page(2).size(2).
                build());
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.getPage());
        assertEquals(2, matchingUsers.getResult().size());
    }

    @Test
    public void searchByBooleanAnyCond() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("show").equalTo("true").query()).build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());
    }

    @Test
    public void searchByRelationshipAnyCond() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("userOwner").equalTo(5).query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals(6L, groups.getResult().iterator().next().getKey());
    }

    @Test
    public void nested() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)").page(1).size(2).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void searchByType() {
        PagedResult<AnyObjectTO> matching = anyObjectService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").query()).build());
        assertNotNull(matching);

        assertFalse(matching.getResult().isEmpty());
        for (AnyObjectTO printer : matching.getResult()) {
            assertNotNull(printer);
        }

        matching = anyObjectService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("UNEXISTING").query()).build());
        assertNotNull(matching);

        assertTrue(matching.getResult().isEmpty());
    }

    @Test
    public void searchByRelationship() {
        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                        inRelationships(2L).query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return anyObject.getKey() == 1L;
            }
        }));

        PagedResult<UserTO> users = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationships(1L).query()).
                build());
        assertNotNull(users);
        assertTrue(IterableUtils.matchesAny(users.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 4L;
            }
        }));
    }

    @Test
    public void searchByRelationshipType() {
        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                        inRelationshipTypes("neighborhood").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return anyObject.getKey() == 1L;
            }
        }));
        assertTrue(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return anyObject.getKey() == 2L;
            }
        }));

        PagedResult<UserTO> users = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationshipTypes("neighborhood").query()).
                build());
        assertNotNull(users);
        assertTrue(IterableUtils.matchesAny(users.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return user.getKey() == 4L;
            }
        }));
    }

    @Test
    public void assignable() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm("/even/two").page(1).size(1000).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().isAssignable().
                        and("name").equalTo("*").query()).
                build());
        assertNotNull(groups);
        assertTrue(IterableUtils.matchesAny(groups.getResult(), new Predicate<GroupTO>() {

            @Override
            public boolean evaluate(final GroupTO group) {
                return group.getKey() == 15L;
            }
        }));
        assertFalse(IterableUtils.matchesAny(groups.getResult(), new Predicate<GroupTO>() {

            @Override
            public boolean evaluate(final GroupTO group) {
                return group.getKey() == 16L;
            }
        }));

        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(
                new AnySearchQuery.Builder().realm("/odd").
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").isAssignable().
                        and("name").equalTo("*").query()).
                build());
        assertNotNull(anyObjects);
        assertFalse(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return anyObject.getKey() == 3L;
            }
        }));
    }

    @Test
    public void orderBy() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("status").desc("firstname").build()).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }
    }
}
