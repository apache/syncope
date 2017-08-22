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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.junit.Test;

public class SearchITCase extends AbstractITCase {

    @Test
    public void searchUser() {
        // LIKE
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchingUsers = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertEquals(2, matchingUsers.getResult().stream().filter(user -> {
            return "74cd8ece-715a-44a4-a736-e17b46c4e7e6".equals(user.getKey())
                    || "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee".equals(user.getKey());
        }).count());
    }

    @Test
    public void searchUserIgnoreCase() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalToIgnoreCase("RoSsINI").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matchingUsers.getResult().iterator().next().getKey());

        matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("(fullname=~*oSsINi)").page(1).size(2).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matchingUsers.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByUsernameAndKey() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalTo("rossini").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().iterator().next().getUsername());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matchingUsers.getResult().iterator().next().getKey());
    }

    @Test
    public void searchByGroupNameAndKey() {
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
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
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("root").query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "1417acbe-cbf6-4277-9372-e75e04f97000".equals(user.getKey())));
    }

    @Test
    public void searchByDynGroup() {
        GroupTO group = GroupITCase.getBasicSampleTO("dynMembership");
        group.setUDynMembershipCond("cool==true");
        group = createGroup(group).getEntity();
        assertNotNull(group);

        if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(group.getKey()).query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchByRole() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles("Other").query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "1417acbe-cbf6-4277-9372-e75e04f97000".equals(user.getKey())));
    }

    @Test
    public void searchByDynRole() {
        RoleTO role = RoleITCase.getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = roleService.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles(role.getKey()).query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchUserByResourceName() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().hasResources(RESOURCE_NAME_MAPPINGS2).query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "74cd8ece-715a-44a4-a736-e17b46c4e7e6".equals(user.getKey())));
    }

    @Test
    public void paginatedSearch() {
        // LIKE
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query()).page(1).size(2).
                        build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        for (UserTO user : matchingUsers.getResult()) {
            assertNotNull(user);
        }

        // ISNULL
        matchingUsers = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).page(2).size(2).
                build());
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.getPage());
        assertFalse(matchingUsers.getResult().isEmpty());
    }

    @Test
    public void searchByBooleanAnyCond() {
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("show").equalTo("true").query()).build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());
    }

    @Test
    public void searchByDate() {
        clientFactory.create("bellini", "password").self();

        if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
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
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
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
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)").page(1).size(2).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(user -> {
            assertNotNull(user);
        });
    }

    @Test
    public void searchByType() {
        PagedResult<AnyObjectTO> matching = anyObjectService.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").query()).build());
        assertNotNull(matching);

        assertFalse(matching.getResult().isEmpty());
        for (AnyObjectTO printer : matching.getResult()) {
            assertNotNull(printer);
        }

        matching = anyObjectService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("UNEXISTING").query()).build());
        assertNotNull(matching);

        assertTrue(matching.getResult().isEmpty());
    }

    @Test
    public void searchByRelationship() {
        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                        inRelationships("Canon MF 8030cn").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey())));

        PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationships("HP LJ 1300n").query()).
                build());
        assertNotNull(users);
        assertTrue(users.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchByRelationshipType() {
        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                        inRelationshipTypes("neighborhood").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey())));
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(anyObject.getKey())));

        PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationshipTypes("neighborhood").query()).
                build());
        assertNotNull(users);
        assertTrue(users.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void assignable() {
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm("/even/two").page(1).size(1000).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().isAssignable().
                        and("name").equalTo("*").query()).
                build());
        assertNotNull(groups);
        assertTrue(groups.getResult().stream().
                anyMatch(group -> "034740a9-fa10-453b-af37-dc7897e98fb1".equals(group.getKey())));
        assertFalse(groups.getResult().stream().
                anyMatch(group -> "e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed".equals(group.getKey())));

        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(new AnyQuery.Builder().realm("/odd").
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").isAssignable().
                        and("name").equalTo("*").query()).
                build());
        assertNotNull(anyObjects);
        assertFalse(anyObjects.getResult().stream().
                anyMatch(anyObject -> "9e1d130c-d6a3-48b1-98b3-182477ed0688".equals(anyObject.getKey())));
    }

    @Test
    public void member() {
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm("/").
                fiql(SyncopeClient.getGroupSearchConditionBuilder().withMembers("rossini").query()).
                build());
        assertNotNull(groups);

        assertTrue(groups.getResult().stream().anyMatch(group -> "root".equals(group.getName())));
        assertTrue(groups.getResult().stream().anyMatch(group -> "otherchild".equals(group.getName())));
    }

    @Test
    public void orderBy() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
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
        int usersWithNullable = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("ctype").nullValue().query()).build()).
                getTotalCount();
        assertTrue(usersWithNullable > 0);

        int nonOrdered = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).build()).
                getTotalCount();
        assertTrue(nonOrdered > 0);

        int orderedByNullable = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("ctype").build()).build()).
                getTotalCount();
        assertEquals(nonOrdered, orderedByNullable);
    }

    @Test
    public void issueSYNCOPE929() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("(surname==Rossini,gender==M);surname==Bellini").build());

        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(user -> {
            assertTrue(user.getUsername().startsWith("bellini"));
        });
    }

    @Test
    public void issueSYNCOPE980() {
        AnyTypeTO service = new AnyTypeTO();
        service.setKey("SERVICE");
        service.setKind(AnyTypeKind.ANY_OBJECT);
        Response response = anyTypeService.create(service);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        String serviceKey = null;
        try {
            AnyObjectTO anyObjectTO = new AnyObjectTO();
            anyObjectTO.setName("one");
            anyObjectTO.setRealm(SyncopeConstants.ROOT_REALM);
            anyObjectTO.setType(service.getKey());
            anyObjectTO.getMemberships().add(
                    new MembershipTO.Builder().group("29f96485-729e-4d31-88a1-6fc60e4677f3").build());
            serviceKey = createAnyObject(anyObjectTO).getEntity().getKey();

            AnyObjectPatch anyObjectPatch = new AnyObjectPatch();
            anyObjectPatch.setKey("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
            anyObjectPatch.getMemberships().add(
                    new MembershipPatch.Builder().group("29f96485-729e-4d31-88a1-6fc60e4677f3").build());
            updateAnyObject(anyObjectPatch);

            PagedResult<AnyObjectTO> matching = anyObjectService.search(new AnyQuery.Builder().fiql(
                    SyncopeClient.getAnyObjectSearchConditionBuilder(service.getKey()).
                            inGroups("29f96485-729e-4d31-88a1-6fc60e4677f3").
                            query()).build());
            assertEquals(1, matching.getSize());
            assertEquals(serviceKey, matching.getResult().get(0).getKey());
        } finally {
            if (serviceKey != null) {
                anyObjectService.delete(serviceKey);
            }
            anyTypeService.delete(service.getKey());
        }
    }

    @Test
    public void issueSYNCOPE983() {
        PagedResult<UserTO> users = userService.search(
                new AnyQuery.Builder().
                        fiql(SyncopeClient.getUserSearchConditionBuilder().is("surname").equalTo("*o*").query()).
                        orderBy(SyncopeClient.getOrderByClauseBuilder().asc("surname").desc("username").build()).
                        build());
        assertNotEquals(0, users.getTotalCount());
    }
}
