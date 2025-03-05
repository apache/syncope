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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedConnObjectResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SearchITCase extends AbstractITCase {

    @Test
    public void searchUser() {
        // LIKE
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(Assertions::assertNotNull);

        // ISNULL
        matchingUsers = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());
    }

    @Test
    public void searchUserIgnoreCase() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalToIgnoreCase("RoSsINI").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().getFirst().getUsername());

        matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("fullname=~*oSsINi").page(1).size(2).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().getFirst().getUsername());

        matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("fullname=~*ino*rossini*").page(1).size(2).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().getFirst().getUsername());
    }

    @Test
    public void searchByUsernameAndKey() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalTo("rossini").and("key").lessThan(2).query()).build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        assertEquals("rossini", matchingUsers.getResult().getFirst().getUsername());
    }

    @Test
    public void searchByGroupNameAndKey() {
        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().
                        is("name").equalTo("root").and("key").equalTo("37d15e4c-cdc1-460b-a591-8505c8133806").
                        query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals("root", groups.getResult().getFirst().getName());
        assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", groups.getResult().getFirst().getKey());
    }

    @Test
    public void searchByGroup() {
        PagedResult<UserTO> matchingChild = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("child").query()).
                        build());
        assertTrue(matchingChild.getResult().stream().anyMatch(user -> "verdi".equals(user.getUsername())));

        PagedResult<UserTO> matchingOtherChild = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("otherchild").query()).
                        build());
        assertTrue(matchingOtherChild.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        Set<String> union = Stream.concat(
                matchingChild.getResult().stream().map(UserTO::getUsername),
                matchingOtherChild.getResult().stream().map(UserTO::getUsername)).
                collect(Collectors.toSet());

        PagedResult<UserTO> matchingStar = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("*child").query()).
                        build());
        assertTrue(matchingStar.getResult().stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
        assertEquals(union, matchingStar.getResult().stream().map(UserTO::getUsername).collect(Collectors.toSet()));

        matchingStar = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).recursive(false).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups("*child").query()).
                        build());
        assertTrue(matchingStar.getResult().stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.getResult().stream().noneMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByDynGroup() {
        GroupCR groupCR = GroupITCase.getBasicSample("dynMembership");
        groupCR.setUDynMembershipCond("cool==true");
        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<GroupTO> matchingGroups = GROUP_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getGroupSearchConditionBuilder().
                                is("creationContext").equalTo("REST").query()).
                        build());
        assertNotNull(matchingGroups);
        assertTrue(matchingGroups.getTotalCount() > 0);

        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles("Other").query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByDynRole() {
        RoleTO role = RoleITCase.getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = ROLE_SERVICE.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles(role.getKey()).query()).
                        build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());

        assertTrue(matchingUsers.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchByAuxClass() {
        PagedResult<GroupTO> matchingGroups = GROUP_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getGroupSearchConditionBuilder().
                                hasAuxClasses("csv").query()).
                        build());
        assertNotNull(matchingGroups);
        assertFalse(matchingGroups.getResult().isEmpty());

        assertTrue(matchingGroups.getResult().stream().
                anyMatch(group -> "0626100b-a4ba-4e00-9971-86fad52a6216".equals(group.getKey())));
        assertTrue(matchingGroups.getResult().stream().
                anyMatch(group -> "ba9ed509-b1f5-48ab-a334-c8530a6422dc".equals(group.getKey())));
    }

    @Test
    public void searchByResource() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        matchingUsers = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("loginDate").query()).page(2).size(2).
                build());
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.getPage());
        assertFalse(matchingUsers.getResult().isEmpty());
    }

    @Test
    public void searchByRealm() {
        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").
                        equalTo("c5b75db1-fce7-470f-b780-3b9934d82a9d").query()).build());
        assertTrue(users.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").equalTo("/even").query()).build());
        assertTrue(users.getResult().stream().anyMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByBooleanAnyCond() {
        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("show").equalTo("true").query()).build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());
    }

    @Test
    public void searchByDate() {
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("creationDate").lexicalNotBefore("2009-03-02 15:21:22").
                        and("username").equalTo("bellini").query()).
                build());
        assertNotNull(users);
        assertEquals(1, users.getTotalCount());
        assertEquals(1, users.getResult().size());

        // SYNCOPE-1321
        PagedResult<UserTO> issueSYNCOPE1321 = USER_SERVICE.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("lastChangeDate").lexicalNotBefore("2010-03-02T15:21:22%2B0300").
                        and("username").equalTo("bellini").query()).
                build());
        assertEquals(users, issueSYNCOPE1321);
    }

    @Test
    public void searchByRelationshipAnyCond() {
        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().
                        is("userOwner").equalTo("823074dc-d280-436d-a7dd-07399fae48ec").query()).build());
        assertNotNull(groups);
        assertEquals(1, groups.getResult().size());
        assertEquals("ebf97068-aa4b-4a85-9f01-680e8c4cf227", groups.getResult().getFirst().getKey());
    }

    @Test
    public void nested() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql("((fullname==*o*,fullname==*i*);$resources!=ws-target-resource-1)").
                        page(1).size(2).build());
        assertNotNull(matchingUsers);

        assertFalse(matchingUsers.getResult().isEmpty());
        matchingUsers.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void searchByType() {
        PagedResult<AnyObjectTO> matching = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).query()).build());
        assertNotNull(matching);

        assertFalse(matching.getResult().isEmpty());
        for (AnyObjectTO printer : matching.getResult()) {
            assertNotNull(printer);
        }

        matching = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("UNEXISTING").query()).build());
        assertNotNull(matching);

        assertTrue(matching.getResult().isEmpty());
    }

    @Test
    public void searchByRelationship() {
        PagedResult<AnyObjectTO> anyObjects = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
                        inRelationships("Canon MF 8030cn").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey())));

        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationships("HP LJ 1300n").query()).
                build());
        assertNotNull(users);
        assertTrue(users.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchByRelationshipType() {
        PagedResult<AnyObjectTO> anyObjects = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
                        inRelationshipTypes("inclusion").query()).
                build());
        assertNotNull(anyObjects);
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(anyObject.getKey())));
        assertTrue(anyObjects.getResult().stream().
                anyMatch(anyObject -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(anyObject.getKey())));

        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRelationshipTypes("neighborhood").query()).
                build());
        assertNotNull(users);
        assertTrue(users.getResult().stream().
                anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void searchBySecurityAnswer() {
        String securityAnswer = RandomStringUtils.insecure().nextAlphanumeric(10);
        UserCR userCR = UserITCase.getUniqueSample("securityAnswer@syncope.apache.org");
        userCR.setSecurityQuestion("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        userCR.setSecurityAnswer(securityAnswer);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO.getSecurityQuestion());

        try {
            USER_SERVICE.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getUserSearchConditionBuilder().
                                    is("securityAnswer").equalTo(securityAnswer).query()).build());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void member() {
        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm("/").
                fiql(SyncopeClient.getGroupSearchConditionBuilder().withMembers("rossini").query()).
                build());
        assertNotNull(groups);

        assertTrue(groups.getResult().stream().anyMatch(group -> "root".equals(group.getName())));
        assertTrue(groups.getResult().stream().anyMatch(group -> "otherchild".equals(group.getName())));

        String printer = createAnyObject(new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM, PRINTER, getUUIDString()).
                membership(new MembershipTO.Builder("29f96485-729e-4d31-88a1-6fc60e4677f3").build()).
                build()).getEntity().getKey();

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm("/").
                fiql(SyncopeClient.getGroupSearchConditionBuilder().withMembers(printer).query()).
                build());
        assertTrue(groups.getResult().stream().
                anyMatch(group -> "29f96485-729e-4d31-88a1-6fc60e4677f3".equals(group.getKey())));
    }

    @Test
    public void orderBy() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        for (int i = 0; i < 11; i++) {
            GroupCR groupCR = GroupITCase.getSample("group");
            groupCR.getResources().add(RESOURCE_NAME_LDAP);
            GroupTO group = createGroup(groupCR).getEntity();
            groupKeys.add(group.getKey());
        }

        ConnObjectTOQuery.Builder builder = new ConnObjectTOQuery.Builder().size(10);

        try {
            PagedConnObjectResult matches = RESOURCE_SERVICE.searchConnObjects(
                    RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), builder.build());
            assertNotNull(matches);

            // the test LDAP server sometimes does not return the expected cookie
            if (matches.getPagedResultsCookie() != null) {
                int firstRound = matches.getResult().size();

                builder.pagedResultsCookie(matches.getPagedResultsCookie());
                matches = RESOURCE_SERVICE.searchConnObjects(
                        RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), builder.build());
                assertNotNull(matches);
                int secondRound = matches.getResult().size();

                assertTrue(firstRound + secondRound >= groupKeys.size());
            }
        } finally {
            BatchRequest batchRequest = ADMIN_CLIENT.batch();
            GroupService batchGroupService = batchRequest.getService(GroupService.class);
            groupKeys.forEach(batchGroupService::delete);
            batchRequest.commit();
        }
    }

    @Test
    public void searchConnObjectsWithFilter() {
        PagedConnObjectResult matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").equalTo("pullFromLDAP").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("givenName").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("mail").equalTo("pullFromLDAP*").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("cn").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("mail").equalTo("*@syncope.apache.org").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("cn").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").equalToIgnoreCase("pullfromldap").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("givenName").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is(Name.NAME).equalTo("uid=pullFromLDAP%252Cou=people%252Co=isp").query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("cn").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("givenName").notEqualTo("pullFromLDAP").query()).build());
        assertFalse(matches.getResult().stream().
                anyMatch(connObj -> connObj.getAttr("givenName").orElseThrow().getValues().contains("pullFromLDAP")));

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("homePhone").notNullValue().query()).build());
        assertTrue(matches.getResult().isEmpty());

        matches = RESOURCE_SERVICE.searchConnObjects(
                RESOURCE_NAME_LDAP,
                AnyTypeKind.USER.name(),
                new ConnObjectTOQuery.Builder().size(100).fiql(
                        SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder().
                                is("homePhone").nullValue().query()).build());
        assertTrue(matches.getResult().stream().
                anyMatch(connObject -> connObject.getAttr("homePhone").isEmpty()));
    }

    @Test
    public void changePwdDate() {
        long users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("status!~suspended;changePwdDate==$null").build()).
                getTotalCount();
        assertTrue(users > 0);
    }

    @Test
    public void userByMembershipAttribute() {
        // create type extension for the 'employee' group, if not present
        GroupTO employee = GROUP_SERVICE.read("employee");
        if (employee.getTypeExtension(AnyTypeKind.USER.name()).isEmpty()) {
            TypeExtensionTO typeExtensionTO = new TypeExtensionTO();
            typeExtensionTO.setAnyType(AnyTypeKind.USER.name());
            typeExtensionTO.getAuxClasses().add("other");
            updateGroup(new GroupUR.Builder(employee.getKey()).typeExtension(typeExtensionTO).build());
        }

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matching = USER_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("ctype").equalTo("additionalctype").query()).build());
        assertEquals(0, matching.getTotalCount());
        matching = USER_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("ctype").equalTo("myownctype").query()).build());
        assertEquals(0, matching.getTotalCount());

        // add user membership and its plain attribute
        updateUser(new UserUR.Builder(USER_SERVICE.read("puccini").getKey())
                .plainAttr(attrAddReplacePatch("ctype", "myownctype"))
                .membership(new MembershipUR.Builder(GROUP_SERVICE.read("additional").getKey()).
                        plainAttrs(attr("ctype", "additionalctype")).build())
                .membership(new MembershipUR.Builder(employee.getKey())
                        .plainAttrs(attr("ctype", "additionalemployeectype")).build())
                .build());

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        matching = USER_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("ctype").equalTo("additionalctype").query()).build());
        assertEquals(1, matching.getTotalCount());
        assertTrue(matching.getResult().stream().anyMatch(u -> "puccini".equals(u.getUsername())));

        // check also that search on user plain attribute (not in membership) works
        matching = USER_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("ctype").equalTo("myownctype").query()).build());
        assertEquals(1, matching.getTotalCount());
        assertTrue(matching.getResult().stream().anyMatch(u -> "puccini".equals(u.getUsername())));
    }

    @Test
    public void anyObjectByMembershipAttribute() {
        PagedResult<AnyObjectTO> matching = ANY_OBJECT_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER)
                        .is("ctype").equalTo("otherchildctype").query()).build());
        assertEquals(0, matching.getTotalCount());

        // add any object membership and its plain attribute
        updateAnyObject(new AnyObjectUR.Builder("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").
                membership(new MembershipUR.Builder(GROUP_SERVICE.read("otherchild").getKey()).
                        plainAttrs(attr("ctype", "otherchildctype")).
                        build()).build());

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        matching = ANY_OBJECT_SERVICE.search(
                new AnyQuery.Builder().fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER)
                        .is("ctype").equalTo("otherchildctype").query()).build());
        assertEquals(1, matching.getTotalCount());

        assertTrue(matching.getResult().stream().
                anyMatch(a -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(a.getKey())));
    }

    @Test
    public void issueSYNCOPE768() {
        long usersWithNullable = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("ctype").nullValue().query()).build()).
                getTotalCount();
        assertTrue(usersWithNullable > 0);

        long nonOrdered = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).build()).
                getTotalCount();
        assertTrue(nonOrdered > 0);

        long orderedByNullable = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").notNullValue().query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("ctype").build()).build()).
                getTotalCount();
        assertEquals(nonOrdered, orderedByNullable);
    }

    @Test
    public void issueSYNCOPE929() {
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        Response response = ANY_TYPE_SERVICE.create(service);
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

            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            PagedResult<AnyObjectTO> matching = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().fiql(
                    SyncopeClient.getAnyObjectSearchConditionBuilder(service.getKey()).
                            inGroups("29f96485-729e-4d31-88a1-6fc60e4677f3").
                            query()).build());
            assertEquals(1, matching.getSize());
            assertEquals(serviceKey, matching.getResult().getFirst().getKey());
        } finally {
            if (serviceKey != null) {
                ANY_OBJECT_SERVICE.delete(serviceKey);
            }
            ANY_TYPE_SERVICE.delete(service.getKey());
        }
    }

    @Test
    public void issueSYNCOPE983() {
        PagedResult<UserTO> users = USER_SERVICE.search(
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
        USER_SERVICE.update(req);

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        try {
            PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().fiql(
                    SyncopeClient.getUserSearchConditionBuilder().is("ctype").equalTo("ou=sample%252Co=isp").query()).
                    build());
            assertEquals(1, users.getTotalCount());
            assertEquals("vivaldi", users.getResult().getFirst().getUsername());
        } finally {
            req.getPlainAttrs().clear();
            req.getPlainAttrs().add(new AttrPatch.Builder(attr("ctype", "F")).build());
            USER_SERVICE.update(req);
        }
    }

    @Test
    public void issueSYNCOPE1304() {
        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                orderBy("userOwner DESC").build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());
    }

    @Test
    public void issueSYNCOPE1416() {
        // check the search for attributes of type different from stringvalue
        PagedResult<UserTO> issueSYNCOPE1416 = USER_SERVICE.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("loginDate").lexicalNotBefore("2009-05-26").
                        and("username").equalTo("rossini").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        assertEquals(1, issueSYNCOPE1416.getSize());
        assertEquals("rossini", issueSYNCOPE1416.getResult().getFirst().getUsername());

        // search by attribute with unique constraint
        issueSYNCOPE1416 = USER_SERVICE.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("fullname").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        // some identities could have been imported by pull tasks executions
        assertTrue(issueSYNCOPE1416.getSize() >= 5);

        issueSYNCOPE1416 = USER_SERVICE.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNull("fullname").query()).
                orderBy(SyncopeClient.getOrderByClauseBuilder().asc("loginDate").build()).
                build());
        assertEquals(0, issueSYNCOPE1416.getSize());
    }

    @Test
    public void issueSYNCOPE1417() {
        try {
            USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("userId").equalTo("*@apache.org").query()).
                    orderBy(SyncopeClient.getOrderByClauseBuilder().asc("surname").desc("firstname").build()).build());
            if (!IS_EXT_SEARCH_ENABLED) {
                fail();
            }
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE1419() {
        UserTO rossini = USER_SERVICE.read("rossini");
        assertNotNull(rossini);

        UserUR req = new UserUR();
        req.setKey(rossini.getKey());
        req.getPlainAttrs().add(new AttrPatch.Builder(
                new Attr.Builder("loginDate").value("2009-05-26").build()).build());
        rossini = updateUser(req).getEntity();
        assertNotNull(rossini);
        assertEquals("2009-05-26", rossini.getPlainAttr("loginDate").orElseThrow().getValues().getFirst());

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> total = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).page(1).size(1).build());

        PagedResult<UserTO> matching = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("loginDate").equalTo("2009-05-26").query()).page(1).size(1).build());
        assertTrue(matching.getSize() > 0);

        PagedResult<UserTO> unmatching = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("loginDate").notEqualTo("2009-05-26").query()).page(1).size(1).build());
        assertTrue(unmatching.getSize() > 0);

        assertEquals(total.getTotalCount(), matching.getTotalCount() + unmatching.getTotalCount());
    }

    @Test
    public void issueSYNCOPE1648() {
        PagedResult<UserTO> matching = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("username").notEqualTo("verdi").query()).build());
        assertTrue(matching.getResult().stream().noneMatch(user -> "verdi".equals(user.getUsername())));
    }

    @Test
    public void issueSYNCOPE1663() {
        PagedResult<UserTO> matching1 = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("lastChangeDate=ge=2022-01-25T17:00:06Z").build());
        assertNotNull(matching1);
        assertFalse(matching1.getResult().isEmpty());

        PagedResult<UserTO> matching2 = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("lastChangeDate=ge=2022-01-25T17:00:06+0000").build());
        assertNotNull(matching2);
        assertFalse(matching2.getResult().isEmpty());
    }

    @Test
    public void issueSYNCOPE1727() {
        RealmTO realm = new RealmTO();
        realm.setName("syncope1727");

        // 1. create Realm
        Response response = REALM_SERVICE.create("/even/two", realm);
        realm = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertNotNull(realm.getKey());
        assertEquals("syncope1727", realm.getName());
        assertEquals("/even/two/syncope1727", realm.getFullPath());
        assertEquals(realm.getParent(), getRealm("/even/two").orElseThrow().getKey());

        // 2. create user
        UserCR userCR = UserITCase.getUniqueSample("syncope1727@syncope.apache.org");
        userCR.setRealm(realm.getFullPath());
        UserTO user = createUser(userCR).getEntity();

        // 3. search for user
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").
                        equalTo(realm.getKey()).query()).build());
        assertEquals(1, users.getResult().size());
        assertEquals(user.getKey(), users.getResult().getFirst().getKey());

        // 4. update parent Realm
        realm.setParent(getRealm("/odd").orElseThrow().getKey());
        REALM_SERVICE.update(realm);
        realm = getRealm("/odd/syncope1727").orElseThrow();

        // 5. search again for user
        users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("realm").
                        equalTo(realm.getKey()).query()).build());
        assertEquals(1, users.getResult().size());
        assertEquals(user.getKey(), users.getResult().getFirst().getKey());
    }

    @Test
    public void issueSYNCOPE1779() {
        // 1. create user with underscore
        UserTO userWith = createUser(UserITCase.getSample("syncope1779_test@syncope.apache.org")).getEntity();
        // 2 create second user without underscore
        UserTO userWithout = createUser(UserITCase.getSample("syncope1779test@syncope.apache.org")).getEntity();
        // 3. create printer with underscore
        AnyObjectTO printer = createAnyObject(
                new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM, PRINTER, "_syncope1779").build()).getEntity();

        // 4. search
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        try {
            // Search by username
            PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncope1779_*").
                            and().is("firstname").equalTo("syncope1779_*").
                            and().is("userId").equalTo("syncope1779_*").query()).
                    build());
            assertEquals(1, users.getResult().size());
            assertEquals(userWith.getKey(), users.getResult().getFirst().getKey());
            // Search also by attribute
            users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("email").equalTo("syncope1779_*").query()).
                    build());
            assertEquals(1, users.getResult().size());
            assertEquals(userWith.getKey(), users.getResult().getFirst().getKey());
            // search for both
            users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("email").equalTo("syncope1779*").query()).
                    build());
            assertEquals(2, users.getResult().size());

            // search for printer
            PagedResult<AnyObjectTO> printers = ANY_OBJECT_SERVICE.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql("$type==PRINTER;name==_syncope1779").build());
            assertEquals(1, printers.getResult().size());
            assertEquals(printer.getKey(), printers.getResult().getFirst().getKey());
        } finally {
            USER_SERVICE.delete(userWith.getKey());
            USER_SERVICE.delete(userWithout.getKey());
            ANY_OBJECT_SERVICE.delete(printer.getKey());
        }
    }

    @Test
    public void issueSYNCOPE1790() {
        // 0. search by email verdi@syncope.org
        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("email").equalTo("verdi@syncope.org").query()).
                build());
        long before = users.getTotalCount();
        assertTrue(before > 0);
        assertFalse(users.getResult().isEmpty());
        assertTrue(users.getResult().stream().
                allMatch(u -> "verdi@syncope.org".equals(u.getPlainAttr("email").
                    orElseThrow().getValues().getFirst())));

        // 1. create user with similar email
        UserTO user = createUser(UserITCase.getSample("bisverdi@syncope.org")).getEntity();
        assertNotNull(user);

        // 2. search again
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("email").equalTo("verdi@syncope.org").query()).
                build());
        assertEquals(before, users.getTotalCount());
        assertFalse(users.getResult().isEmpty());
        assertTrue(users.getResult().stream().
                allMatch(u -> "verdi@syncope.org".equals(u.getPlainAttr("email").
                    orElseThrow().getValues().getFirst())));
        assertTrue(users.getResult().stream().noneMatch(u -> user.getKey().equals(u.getKey())));
    }

    @Test
    public void issueSYNCOPE1800() {
        // 1. create user
        UserCR userCR = UserITCase.getUniqueSample("syncope800@syncope.apache.org");
        Attr surname = userCR.getPlainAttr("surname").orElseThrow();
        surname.getValues().clear();
        surname.getValues().add("D'Amico");
        UserTO user = createUser(userCR).getEntity();
        assertEquals("D'Amico", user.getPlainAttr("surname").orElseThrow().getValues().getFirst());

        // 2. search for user
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql("surname=~D'*").build());
        assertEquals(1, users.getResult().size());
        assertEquals(user.getKey(), users.getResult().getFirst().getKey());
    }

    @Test
    void issueSYNCOPE1826() {
        UserCR userCR = UserITCase.getUniqueSample("issueSearch1@syncope.apache.org");
        userCR.setUsername("user test 1826");
        createUser(userCR);

        AnyObjectTO anotherPrinter = createAnyObject(new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM,
                PRINTER,
                "obj test 1826").build()).getEntity();

        userCR = UserITCase.getUniqueSample("issueSearch2@syncope.apache.org");
        userCR.setUsername("user 1826 test");
        createUser(userCR);

        userCR = UserITCase.getUniqueSample("issueSearch3@syncope.apache.org");
        userCR.setUsername("user test 182");
        createUser(userCR);

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        try {
            assertFalse(USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).details(false)
                    .fiql(SyncopeClient.getUserSearchConditionBuilder().is("username")
                            .equalToIgnoreCase("user test 1826").query()).build()).getResult().isEmpty());
            assertFalse(ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM)
                    .details(false).fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).is("name")
                    .equalToIgnoreCase("obj test 1826").query()).build()).getResult().isEmpty());
            assertFalse(USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).details(false)
                    .fiql(SyncopeClient.getUserSearchConditionBuilder().is("username")
                            .equalToIgnoreCase("user 1826 test").query()).build()).getResult().isEmpty());
            assertFalse(USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).details(false)
                    .fiql(SyncopeClient.getUserSearchConditionBuilder().is("username")
                            .equalToIgnoreCase("user test 182").query()).build()).getResult().isEmpty());
        } finally {
            deleteUser("user test 1826");
            deleteAnyObject(anotherPrinter.getKey());
            deleteUser("user 1826 test");
            deleteUser("user test 182");
        }
    }
}
