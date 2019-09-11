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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.apache.tika.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class DynRealmITCase extends AbstractITCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void misc() {
        DynRealmTO dynRealm = null;
        try {
            dynRealm = new DynRealmTO();
            dynRealm.setKey("/name" + getUUIDString());
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "cool==true");

            // invalid key (starts with /)
            try {
                dynRealmService.create(dynRealm);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidDynRealm, e.getType());
            }
            dynRealm.setKey("name" + getUUIDString());

            Response response = dynRealmService.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            PagedResult<UserTO> matching = userService.search(new AnyQuery.Builder().fiql("cool==true").build());
            assertNotNull(matching);
            assertNotEquals(0, matching.getSize());

            UserTO user = matching.getResult().get(0);

            assertTrue(user.getDynRealms().contains(dynRealm.getKey()));
        } finally {
            if (dynRealm != null) {
                dynRealmService.delete(dynRealm.getKey());
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

            Response response = dynRealmService.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            // 2. create role for such dynamic realm
            role = new RoleTO();
            role.setKey("Administer LDAP" + getUUIDString());
            role.getEntitlements().add(StandardEntitlement.USER_SEARCH);
            role.getEntitlements().add(StandardEntitlement.USER_READ);
            role.getEntitlements().add(StandardEntitlement.USER_UPDATE);
            role.getEntitlements().add(StandardEntitlement.GROUP_READ);
            role.getEntitlements().add(StandardEntitlement.GROUP_UPDATE);
            role.getDynRealms().add(dynRealm.getKey());

            role = createRole(role);
            assertNotNull(role);

            // 3. create new user and assign the new role
            UserTO dynRealmAdmin = UserITCase.getUniqueSampleTO("dynRealmAdmin@apache.org");
            dynRealmAdmin.setPassword("password123");
            dynRealmAdmin.getRoles().add(role.getKey());
            dynRealmAdmin = createUser(dynRealmAdmin).getEntity();
            assertNotNull(dynRealmAdmin);

            // 4. create new user and group, assign resource-ldap
            UserTO user = UserITCase.getUniqueSampleTO("dynRealmUser@apache.org");
            user.setRealm("/even/two");
            user.getResources().clear();
            user.getResources().add(RESOURCE_NAME_LDAP);
            user = createUser(user).getEntity();
            assertNotNull(user);
            final String userKey = user.getKey();

            GroupTO group = GroupITCase.getSampleTO("dynRealmGroup");
            group.setRealm("/odd");
            group.getResources().clear();
            group.getResources().add(RESOURCE_NAME_LDAP);
            group = createGroup(group).getEntity();
            assertNotNull(group);
            final String groupKey = group.getKey();

            if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            // 5. verify that the new user and group are found when searching by dynamic realm
            PagedResult<UserTO> matchingUsers = userService.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertTrue(matchingUsers.getResult().stream().anyMatch(object -> object.getKey().equals(userKey)));

            PagedResult<GroupTO> matchingGroups = groupService.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getGroupSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertTrue(matchingGroups.getResult().stream().anyMatch(object -> object.getKey().equals(groupKey)));

            // 6. prepare to act as delegated admin
            SyncopeClient delegatedClient = clientFactory.create(dynRealmAdmin.getUsername(), "password123");
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
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userKey);
            userPatch.getResources().add(new StringPatchItem.Builder().
                    value(RESOURCE_NAME_LDAP).operation(PatchOperation.DELETE).build());
            // this will fail because unassigning resource-ldap would result in removing the user from the dynamic realm
            try {
                delegatedUserService.update(userPatch);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }
            // this will succeed instead
            userPatch.getResources().clear();
            userPatch.getResources().add(new StringPatchItem.Builder().value(RESOURCE_NAME_NOPROPAGATION).build());
            user = delegatedUserService.update(userPatch).
                    readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                    }).getEntity();
            assertNotNull(user);
            assertTrue(user.getResources().contains(RESOURCE_NAME_NOPROPAGATION));

            // GROUP_UPDATE
            GroupPatch groupPatch = new GroupPatch();
            groupPatch.setKey(groupKey);
            groupPatch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO("icon", "modified")).build());
            group = delegatedGroupService.update(groupPatch).
                    readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
                    }).getEntity();
            assertNotNull(group);
            assertEquals("modified", group.getPlainAttr("icon").get().getValues().get(0));
        } finally {
            if (role != null) {
                roleService.delete(role.getKey());
            }
            if (dynRealm != null) {
                dynRealmService.delete(dynRealm.getKey());
            }
        }
    }

    private ArrayNode fetchDynRealmsFromElasticsearch(final String userKey) throws Exception {
        String body =
                "{"
                + "    \"query\": {"
                + "        \"match\": {\"_id\": \"" + userKey + "\"}"
                + "    }"
                + "}";

        HttpClient httpClient = new HttpClient();
        httpClient.start();
        ContentResponse response = httpClient.newRequest("http://localhost:9200/master_user/_search").
                method(HttpMethod.GET).
                header(HttpHeader.CONTENT_TYPE, MediaType.APPLICATION_JSON).
                content(new InputStreamContentProvider(IOUtils.toInputStream(body))).
                send();
        assertEquals(HttpStatus.OK_200, response.getStatus());

        return (ArrayNode) MAPPER.readTree(response.getContent()).
                get("hits").get("hits").get(0).get("_source").get("dynRealms");
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
            dynRealmService.create(dynRealm);

            Response response = dynRealmService.create(dynRealm);
            dynRealm = getObject(response.getLocation(), DynRealmService.class, DynRealmTO.class);
            assertNotNull(dynRealm);

            // 2. no dyn realm members
            PagedResult<UserTO> matching = userService.search(new AnyQuery.Builder().realm("/").fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(0, matching.getSize());

            // 3. create user with that attribute value
            UserTO user = UserITCase.getUniqueSampleTO("syncope1480@syncope.apache.org");
            user.getPlainAttr("ctype").get().getValues().set(0, ctype);
            user = createUser(user).getEntity();
            assertNotNull(user.getKey());

            // 4a. check that Elasticsearch index was updated correctly
            if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
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
            matching = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(1, matching.getSize());

            // 5. change dyn realm condition
            dynRealm.getDynMembershipConds().put(AnyTypeKind.USER.name(), "ctype==ANY");
            dynRealmService.update(dynRealm);

            // 6a. check that Elasticsearch index was updated correctly
            if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }

                ArrayNode dynRealms = fetchDynRealmsFromElasticsearch(user.getKey());
                assertTrue(dynRealms.isEmpty());
            }

            // 6b. no more dyn realm members
            matching = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(
                    SyncopeClient.getUserSearchConditionBuilder().inDynRealms(dynRealm.getKey()).query()).build());
            assertEquals(0, matching.getSize());
        } finally {
            if (dynRealm != null) {
                dynRealmService.delete(dynRealm.getKey());
            }
        }
    }
}
