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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class DynRealmITCase extends AbstractITCase {

    private static ArrayNode fetchDynRealmsFromElasticsearch(final String userKey) throws Exception {
        String body =
                '{'
                + "    \"query\": {"
                + "        \"match\": {\"_id\": \"" + userKey + "\"}"
                + "    }"
                + '}';

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:9200/master_user/_search")).
                        header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).
                        method("GET", BodyPublishers.ofString(body)).
                        build(),
                BodyHandlers.ofString());
        assertEquals(Response.Status.OK.getStatusCode(), response.statusCode());

        return (ArrayNode) JSON_MAPPER.readTree(response.body()).
                get("hits").get("hits").get(0).get("_source").get("dynRealms");
    }

    @Test
    public void misc() {
        DynRealmTO dynRealm = null;
        try {
            dynRealm = new DynRealmTO();
            dynRealm.setKey("/name" + getUUIDString());
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "cool==true");

            // invalid key (starts with /)
            try {
                DYN_REALM_SERVICE.create(dynRealm);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidDynRealm, e.getType());
            }
            dynRealm.setKey("name" + getUUIDString());

            Response response = DYN_REALM_SERVICE.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            PagedResult<UserTO> matching = USER_SERVICE.search(new AnyQuery.Builder().fiql("cool==true").build());
            assertNotNull(matching);
            assertNotEquals(0, matching.getSize());

            UserTO user = matching.getResult().getFirst();

            assertTrue(user.getDynRealms().contains(dynRealm.getKey()));
        } finally {
            if (dynRealm != null) {
                DYN_REALM_SERVICE.delete(dynRealm.getKey());
            }
        }
    }

    @Test
    public void delegatedAdmin() {
        DynRealmTO dynRealm = null;
        RoleTO role = null;
        try {
            // 1. create dynamic realm for all users and groups having resource-ldap assigned
            dynRealm = new DynRealmTO();
            dynRealm.setKey("LDAPLovers" + getUUIDString());
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "$resources==resource-ldap");
            dynRealm.getDynMembershipConds().put(AnyTypeKind.GROUP.name(), "$resources==resource-ldap");

            Response response = DYN_REALM_SERVICE.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            // 2. create role for such dynamic realm
            role = new RoleTO();
            role.setKey("Administer LDAP" + getUUIDString());
            role.getEntitlements().add(IdRepoEntitlement.USER_SEARCH);
            role.getEntitlements().add(IdRepoEntitlement.USER_READ);
            role.getEntitlements().add(IdRepoEntitlement.USER_UPDATE);
            role.getEntitlements().add(IdRepoEntitlement.GROUP_READ);
            role.getEntitlements().add(IdRepoEntitlement.GROUP_UPDATE);
            role.getDynRealms().add(dynRealm.getKey());

            role = createRole(role);
            assertNotNull(role);

            // 3. create new user and assign the new role
            UserCR dynRealmAdmin = UserITCase.getUniqueSample("dynRealmAdmin@apache.org");
            dynRealmAdmin.setPassword("password123");
            dynRealmAdmin.getRoles().add(role.getKey());
            assertNotNull(createUser(dynRealmAdmin).getEntity());

            // 4. create new user and group, assign resource-ldap
            UserCR userCR = UserITCase.getUniqueSample("dynRealmUser@apache.org");
            userCR.setRealm("/even/two");
            userCR.getResources().clear();
            userCR.getResources().add(RESOURCE_NAME_LDAP);
            UserTO user = createUser(userCR).getEntity();
            assertNotNull(user);
            final String userKey = user.getKey();

            GroupCR groupCR = GroupITCase.getSample("dynRealmGroup");
            groupCR.setRealm("/odd");
            groupCR.getResources().clear();
            groupCR.getResources().add(RESOURCE_NAME_LDAP);
            GroupTO group = createGroup(groupCR).getEntity();
            assertNotNull(group);
            final String groupKey = group.getKey();

            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            // 5. verify that the new user and group are found when searching by dynamic realm
            PagedResult<UserTO> matchingUsers = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertTrue(matchingUsers.getResult().stream().anyMatch(object -> object.getKey().equals(userKey)));

            PagedResult<GroupTO> matchingGroups = GROUP_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getGroupSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertTrue(matchingGroups.getResult().stream().anyMatch(object -> object.getKey().equals(groupKey)));

            // 6. prepare to act as delegated admin
            SyncopeClient delegatedClient = CLIENT_FACTORY.create(dynRealmAdmin.getUsername(), "password123");
            UserService delegatedUserService = delegatedClient.getService(UserService.class);
            GroupService delegatedGroupService = delegatedClient.getService(GroupService.class);

            // 7. verify delegated administration
            // USER_READ
            assertNotNull(delegatedUserService.read(userKey));

            // GROUP_READ
            assertNotNull(delegatedGroupService.read(groupKey));

            // USER_SEARCH
            matchingUsers = delegatedUserService.search(new AnyQuery.Builder().realm("/").build());
            assertTrue(matchingUsers.getResult().stream().anyMatch(object -> object.getKey().equals(userKey)));

            // USER_UPDATE
            UserUR userUR = new UserUR();
            userUR.setKey(userKey);
            userUR.getResources().add(new StringPatchItem.Builder().
                    value(RESOURCE_NAME_LDAP).operation(PatchOperation.DELETE).build());
            // this will fail because unassigning resource-ldap would result in removing the user from the dynamic realm
            try {
                delegatedUserService.update(userUR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }
            // this will succeed instead
            userUR.getResources().clear();
            userUR.getResources().add(new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION).build());
            user = delegatedUserService.update(userUR).
                    readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                    }).getEntity();
            assertNotNull(user);
            assertTrue(user.getResources().contains(RESOURCE_NAME_NOPROPAGATION));

            // GROUP_UPDATE
            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupKey);
            groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr("icon", "modified")).build());
            group = delegatedGroupService.update(groupUR).readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
            }).getEntity();
            assertNotNull(group);
            assertEquals("modified", group.getPlainAttr("icon").get().getValues().getFirst());
        } finally {
            if (role != null) {
                ROLE_SERVICE.delete(role.getKey());
            }
            if (dynRealm != null) {
                DYN_REALM_SERVICE.delete(dynRealm.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1480() throws Exception {
        String ctype = getUUIDString();

        DynRealmTO dynRealm = null;
        try {
            // 1. create new dyn realm matching a very specific attribute value
            dynRealm = new DynRealmTO();
            dynRealm.setKey("name" + getUUIDString());
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "ctype==" + ctype);

            Response response = DYN_REALM_SERVICE.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            // 2. no dyn realm members
            PagedResult<UserTO> matching = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(0, matching.getSize());

            // 3. create user with that attribute value
            UserCR userCR = UserITCase.getUniqueSample("syncope1480@syncope.apache.org");
            userCR.getPlainAttr("ctype").get().getValues().set(0, ctype);
            UserTO user = createUser(userCR).getEntity();
            assertNotNull(user.getKey());

            // 4a. check that Elasticsearch index was updated correctly
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }

                ArrayNode dynRealms = fetchDynRealmsFromElasticsearch(user.getKey());
                assertEquals(1, dynRealms.size());
                assertEquals(dynRealm.getKey(), dynRealms.get(0).asText());
            }

            // 4b. now there is 1 realm member
            matching = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(1, matching.getSize());

            // 5. change dyn realm condition
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "ctype==ANY");
            DYN_REALM_SERVICE.update(dynRealm);

            // 6a. check that Elasticsearch index was updated correctly
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }

                ArrayNode dynRealms = fetchDynRealmsFromElasticsearch(user.getKey());
                assertTrue(dynRealms.isEmpty());
            }

            // 6b. no more dyn realm members
            matching = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(0, matching.getSize());
        } finally {
            if (dynRealm != null) {
                DYN_REALM_SERVICE.delete(dynRealm.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1806() {
        DynRealmTO realm1 = null;
        DynRealmTO realm2 = null;
        try {
            // 1. create two dyn realms with same condition
            realm1 = new DynRealmTO();
            realm1.setKey("realm1");
            realm1.getDynMembershipConds().put(AnyTypeKind.USER.name(), "cool==true");
            realm1 = getObject(DYN_REALM_SERVICE.create(realm1).getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(realm1);

            realm2 = new DynRealmTO();
            realm2.setKey("realm2");
            realm2.getDynMembershipConds().put(AnyTypeKind.USER.name(), "cool==true");
            realm2 = getObject(DYN_REALM_SERVICE.create(realm2).getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(realm2);

            // 2. verify that dynamic members are the same
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            PagedResult<UserTO> matching1 = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(realm1.getKey()).query()).build());
            PagedResult<UserTO> matching2 = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(realm2.getKey()).query()).build());

            assertEquals(matching1, matching2);
            assertEquals(1, matching1.getResult().size());
            assertTrue(matching1.getResult().stream().
                    anyMatch(u -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(u.getKey())));

            // 3. update an user to let them become part of both dyn realms
            UserUR userUR = new UserUR();
            userUR.setKey("823074dc-d280-436d-a7dd-07399fae48ec");
            userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("cool").value("true").build()).build());
            updateUser(userUR);

            // 4. verify that dynamic members are still the same
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            matching1 = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(realm1.getKey()).query()).build());
            matching2 = USER_SERVICE.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(realm2.getKey()).query()).build());
            assertEquals(matching1, matching2);
            assertEquals(2, matching1.getResult().size());
            assertTrue(matching1.getResult().stream().
                    anyMatch(u -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(u.getKey())));
            assertTrue(matching1.getResult().stream().
                    anyMatch(u -> "823074dc-d280-436d-a7dd-07399fae48ec".equals(u.getKey())));
        } finally {
            if (realm1 != null) {
                DYN_REALM_SERVICE.delete(realm1.getKey());
            }
            if (realm2 != null) {
                DYN_REALM_SERVICE.delete(realm2.getKey());
            }
            UserUR userUR = new UserUR();
            userUR.setKey("823074dc-d280-436d-a7dd-07399fae48ec");
            userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("cool").build()).
                    operation(PatchOperation.DELETE).build());
            updateUser(userUR);
        }
    }
}
