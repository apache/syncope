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
package org.apache.syncope.fit.core;

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
import org.apache.syncope.fit.AbstractITCase;
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
                return "74cd8ece-715a-44a4-a736-e17b46c4e7e6".equals(user.getKey())
                        || "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee".equals(user.getKey());
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
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matchingUsers.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByGroupNameAndKey() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().
                        is("name").equalTo("root").and("key").equalTo("37d15e4c-cdc1-460b-a591-8505c8133806").
                        query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals("root", groups.getResult().iterator().next().getName());
        assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", groups.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByGroup() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        inGroups("37d15e4c-cdc1-460b-a591-8505c8133806").query()).
                build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(IterableUtils.matchesAny(matchingUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return "1417acbe-cbf6-4277-9372-e75e04f97000".equals(user.getKey());
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
                return "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey());
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
                return "1417acbe-cbf6-4277-9372-e75e04f97000".equals(user.getKey());
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
                return "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey());
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
                return "74cd8ece-715a-44a4-a736-e17b46c4e7e6".equals(user.getKey());
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
        assertFalse(matchingUsers.getResult().isEmpty());
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
    public void searchByDate() {
        clientFactory.create("bellini", "password").self();

        PagedResult<UserTO> users = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("lastLoginDate").lexicalNotBefore("2016-03-02 15:21:22").
                        and("username").equalTo("bellini").query()).
                build());
        assertNotNull(users);
        assertEquals(1, users.getTotalCount());
        assertEquals(1, users.getResult().size());
    }

    @Test
    public void searchByRelationshipAnyCond() {
        PagedResult<GroupTO> groups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().
                        is("userOwner").equalTo("823074dc-d280-436d-a7dd-07399fae48ec").query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals(
                "ebf97068-aa4b-4a85-9f01-680e8c4cf227",
                groups.getResult().iterator().next().getKey());
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
                        inRelationships("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey());
            }
        }));

        PagedResult<UserTO> users = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        inRelationships("fc6dbc3a-6c07-4965-8781-921e7401a4a5").query()).
                build());
        assertNotNull(users);
        assertTrue(IterableUtils.matchesAny(users.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO user) {
                return "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey());
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
                return "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey());
            }
        }));
        assertTrue(IterableUtils.matchesAny(anyObjects.getResult(), new Predicate<AnyObjectTO>() {

            @Override
            public boolean evaluate(final AnyObjectTO anyObject) {
                return "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(anyObject.getKey());
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
                return "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey());
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
                return "034740a9-fa10-453b-af37-dc7897e98fb1".equals(group.getKey());
            }
        }));
        assertFalse(IterableUtils.matchesAny(groups.getResult(), new Predicate<GroupTO>() {

            @Override
            public boolean evaluate(final GroupTO group) {
                return "e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed".equals(group.getKey());
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
                return "9e1d130c-d6a3-48b1-98b3-182477ed0688".equals(anyObject.getKey());
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

    @Test
    public void issueSYNCOPE768() {
        int usersWithNullable = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("ctype").nullValue().query()).build()).
                getTotalCount();
        assertTrue(usersWithNullable > 0);

        int nonOrdered = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).build()).
                getTotalCount();
        assertTrue(nonOrdered > 0);

        int orderedByNullable = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("ctype").build()).build()).
                getTotalCount();
        assertEquals(nonOrdered, orderedByNullable);
    }
}
