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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.junit.jupiter.api.Assertions;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.Test;

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
        matchingUsers.getResult().stream().forEach(Assertions::assertNotNull);

        // ISNULL
        matchingUsers = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertEquals(2, matchingUsers.getResult().stream().
                filter(user -> "74cd8ece-715a-44a4-a736-e17b46c4e7e6".equals(user.getKey())
                || "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee".equals(user.getKey())).count());
    }

    @Test
    public void searchUserIgnoreCase() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalToIgnoreCase("RoSsINI").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().get(0).getUsername());

        matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("fullname=~*oSsINi").page(1).size(2).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().get(0).getUsername());

        matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("fullname=~*ino*rossini*").page(1).size(2).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().get(0).getUsername());
    }

    @Test
    public void searchByUsernameAndKey() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalTo("rossini").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().get(0).getUsername());
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
        PagedResult<UserTO> matchingChild = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("child").query()).
                        build());
        assertTrue(matchingChild.getResult().stream().anyMatch(user -> "verdi".equals(user.getUsername())));

        PagedResult<UserTO> matchingOtherChild = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("otherchild").query()).
                        build());
        assertTrue(matchingOtherChild.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        Set<String> union = Stream.concat(
                matchingChild.getResult().stream().map(UserTO::getUsername),
                matchingOtherChild.getResult().stream().map(UserTO::getUsername)).
                collect(Collectors.toSet());

        PagedResult<UserTO> matchingStar = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("*child").query()).
                        build());
        assertTrue(matchingStar.getResult().stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
        assertEquals(union, matchingStar.getResult().stream().map(UserTO::getUsername).collect(Collectors.toSet()));
    }

    @Test
    public void searchByDynGroup() {
        GroupCR groupCR = GroupITCase.getBasicSample("dynMembership");
        groupCR.setUDynMembershipCond("cool==true");
        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<GroupTO> matchingGroups = groupService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getGroupSearchConditionBuilder().
                                is("creationContext").equalTo("REST").query()).
                        build());
        assertNotNull(matchingGroups);
        assertTrue(matchingGroups.getTotalCount() > 0);

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

        assertTrue(matchingUsers.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByPrivilege() {
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().withPrivileges("postMighty").query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByDynRole() {
        RoleTO role = RoleITCase.getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = roleService.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
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
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                hasResources(RESOURCE_NAME_MAPPINGS2).query()).
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
    public void searchByRealm() {
        PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").
                        equalTo("c5b75db1-fce7-470f-b780-3b9934d82a9d").query()).build());
        assertTrue(users.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").equalTo("/even").query()).build());
        assertTrue(users.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
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

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
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

        // SYNCOPE-1321
        PagedResult<UserTO> issueSYNCOPE1321 = userService.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("lastLoginDate").lexicalNotBefore("2016-03-02T15:21:22+0300").
                        and("username").equalTo("bellini").query()).
                build());
        assertEquals(users, issueSYNCOPE1321);
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
                        fiql("((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)").
                        page(1).size(2).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void searchByType() {
        PagedResult<AnyObjectTO> matching = anyObjectService.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).query()).build());
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
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
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
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
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
    public void searchBySecurityAnswer() {
        String securityAnswer = RandomStringUtils.randomAlphanumeric(10);
        UserCR userCR = UserITCase.getUniqueSample("securityAnswer@syncope.apache.org");
        userCR.setSecurityQuestion("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        userCR.setSecurityAnswer(securityAnswer);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO.getSecurityQuestion());

        try {
            userService.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getUserSearchConditionBuilder().
                                    is("securityAnswer").equalTo(securityAnswer).query()).build());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
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
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).isAssignable().
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
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(
                        SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query()).
                        orderBy(SyncopeClient.getOrderByClauseBuilder().asc("status").desc("firstname").build()).
                        build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void searchConnObjectsBrowsePagedResult() {
        List<String> groupKeys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupCR groupCR = GroupITCase.getSample("group");
            groupCR.getResources().add(RESOURCE_NAME_LDAP);
            GroupTO group = createGroup(groupCR).getEntity();
            groupKeys.add(group.getKey());
        }

        int totalRead = 0;
        Set<String> read = new HashSet<>();
        try {
            // 1. first search with no filters
            ConnObjectTOQuery.Builder builder = new ConnObjectTOQuery.Builder().size(10);
            PagedConnObjectTOResult matches;
            do {
                matches = null;

                boolean succeeded = false;
                // needed because ApacheDS seems to randomly fail when searching with cookie
                for (int i = 0; i < 5 && !succeeded; i++) {
                    try {
                        matches = resourceService.searchConnObjects(
                                RESOURCE_NAME_LDAP,
                                AnyTypeKind.GROUP.name(),
                                builder.build());
                        succeeded = true;
                    } catch (SyncopeClientException e) {
                        assertEquals(ClientExceptionType.ConnectorException, e.getType());
                    }
                }
                assertNotNull(matches);

                totalRead += matches.getResult().size();
                read.addAll(matches.getResult().stream().
                        map(input -> input.getAttr(ConnIdSpecialName.NAME).get().getValues().get(0)).
                        collect(Collectors.toList()));

                if (matches.getPagedResultsCookie() != null) {
                    builder.pagedResultsCookie(matches.getPagedResultsCookie());
                }
            } while (matches.getPagedResultsCookie() != null);

            assertEquals(totalRead, read.size());
            assertTrue(totalRead >= 10);
        } finally {
            groupKeys.forEach(key -> {
                groupService.delete(key);
            });
        }
    }

    @Test
    public void searchConnObjectsWithFilter() {
        PagedConnObjectTOResult matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").equalTo("pullFromLDAP").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("givenName").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("mail").equalTo("pullFromLDAP*").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("cn").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("mail").equalTo("*@syncope.apache.org").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("cn").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").equalToIgnoreCase("pullfromldap").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("givenName").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is(Name.NAME).equalTo("uid=pullFromLDAP%252Cou=people%252Co=isp").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("cn").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").notEqualTo("pullFromLDAP").query()).build());
        assertFalse(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("givenName").get().getValues().contains("pullFromLDAP")));

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("homePhone").notNullValue().query()).build());
        assertTrue(matches.getResult().isEmpty());

        matches = resourceService.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("homePhone").nullValue().query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("homePhone").isEmpty()));
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
        matchingUsers.getResult().forEach(user -> assertTrue(user.getUsername().startsWith("bellini")));
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
            AnyObjectCR anyObjectCR = new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM, service.getKey(), "one").
                    membership(new MembershipTO.Builder("29f96485-729e-4d31-88a1-6fc60e4677f3").build()).
                    build();
            serviceKey = createAnyObject(anyObjectCR).getEntity().getKey();

            AnyObjectUR anyObjectUR = new AnyObjectUR.Builder("fc6dbc3a-6c07-4965-8781-921e7401a4a5").
                    membership(new MembershipUR.Builder("29f96485-729e-4d31-88a1-6fc60e4677f3").build()).
                    build();
            updateAnyObject(anyObjectUR);

            if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

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

    @Test
    public void issueSYNCOPE1223() {
        UserUR req = new UserUR();
        req.setKey("vivaldi");
        req.getPlainAttrs().add(new AttrPatch.Builder(attr("ctype", "ou=sample,o=isp")).build());
        userService.update(req);

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        try {
            PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().fiql(
                    SyncopeClient.getUserSearchConditionBuilder().is("ctype").equalTo("ou=sample%252Co=isp").query()).
                    build());
            assertEquals(1, users.getTotalCount());
            assertEquals("vivaldi", users.getResult().get(0).getUsername());
        } finally {
            req.getPlainAttrs().clear();
            req.getPlainAttrs().add(new AttrPatch.Builder(attr("ctype", "F")).build());
            userService.update(req);
        }
    }

    @Test
    public void issueSYNCOPE1304() {
        PagedResult<GroupTO> groups = groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                orderBy("userOwner DESC").build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());
    }

    @Test
    public void issueSYNCOPE1416() {
        // check the search for attributes of type different from stringvalue
        PagedResult<UserTO> issueSYNCOPE1416 = userService.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("loginDate").lexicalNotBefore("2009-05-26").
                        and("username").equalTo("rossini").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        assertEquals(1, issueSYNCOPE1416.getSize());
        assertEquals("rossini", issueSYNCOPE1416.getResult().get(0).getUsername());

        // search by attribute with unique constraint
        issueSYNCOPE1416 = userService.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("fullname").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        // some identities could have been imported by pull tasks executions
        assertTrue(issueSYNCOPE1416.getSize() >= 5);

        issueSYNCOPE1416 = userService.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("fullname").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        assertEquals(0, issueSYNCOPE1416.getSize());
    }

    @Test
    public void issueSYNCOPE1417() {
        try {
            userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query()).
                    orderBy(SyncopeClient.getOrderByClauseBuilder().asc("surname").desc("firstname").build()).build());
            if (!ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
                fail();
            }
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE1419() {
        UserTO rossini = userService.read("rossini");
        assertNotNull(rossini);

        UserUR req = new UserUR();
        req.setKey(rossini.getKey());
        req.getPlainAttrs().add(new AttrPatch.Builder(
                new Attr.Builder("loginDate").value("2009-05-26").build()).build());
        rossini = updateUser(req).getEntity();
        assertNotNull(rossini);
        assertEquals("2009-05-26", rossini.getPlainAttr("loginDate").get().getValues().get(0));

        PagedResult<UserTO> total = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).page(1).size(1).build());

        PagedResult<UserTO> matching = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("loginDate").equalTo("2009-05-26").query()).page(1).size(1).build());
        assertTrue(matching.getSize() > 0);

        PagedResult<UserTO> unmatching = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("loginDate").notEqualTo("2009-05-26").query()).page(1).size(1).build());
        assertTrue(unmatching.getSize() > 0);

        assertEquals(total.getTotalCount(), matching.getTotalCount() + unmatching.getTotalCount());
    }

    @Test
    public void issueSYNCOPE1648() {
        PagedResult<UserTO> matching = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("username").notEqualTo("verdi").query()).build());
        assertTrue(matching.getResult().stream().noneMatch(user -> "verdi".equals(user.getUsername())));
    }

    @Test
    public void issueSYNCOPE1663() {
        PagedResult<UserTO> matching1 = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("lastChangeDate=ge=2022-01-25T17:00:06Z").build());
        assertNotNull(matching1);
        assertFalse(matching1.getResult().isEmpty());

        PagedResult<UserTO> matching2 = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("lastChangeDate=ge=2022-01-25T17:00:06+0000").build());
        assertNotNull(matching2);
        assertFalse(matching2.getResult().isEmpty());
    }
}
