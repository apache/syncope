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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.BasicAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class AuthenticationITCase extends AbstractITCase {

    @Test
    public void readEntitlements() {
        // 1. as anonymous
        Triple<Map<String, Set<String>>, List<String>, UserTO> self = ANONYMOUS_CLIENT.self();
        assertEquals(1, self.getLeft().size());
        assertTrue(self.getLeft().containsKey(IdRepoEntitlement.ANONYMOUS));
        assertEquals(List.of(), self.getMiddle());
        assertEquals(ANONYMOUS_UNAME, self.getRight().getUsername());

        // 3. as admin
        self = ADMIN_CLIENT.self();
        assertEquals(ANONYMOUS_CLIENT.platform().getEntitlements().size(), self.getLeft().size());
        assertFalse(self.getLeft().containsKey(IdRepoEntitlement.ANONYMOUS));
        assertEquals(List.of(), self.getMiddle());
        assertEquals(ADMIN_UNAME, self.getRight().getUsername());

        // 4. as user
        self = CLIENT_FACTORY.create("bellini", ADMIN_PWD).self();
        assertFalse(self.getLeft().isEmpty());
        assertFalse(self.getLeft().containsKey(IdRepoEntitlement.ANONYMOUS));
        assertEquals(List.of(), self.getMiddle());
        assertEquals("bellini", self.getRight().getUsername());
    }

    @Test
    public void userSchemaAuthorization() {
        String schemaName = "authTestSchema" + getUUIDString();

        // 1. create a schema (as admin)
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(schemaName);
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(AttrSchemaType.String);

        PlainSchemaTO newPlainSchemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserCR userCR = UserITCase.getUniqueSample("auth@test.org");
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = SCHEMA_SERVICE.read(SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = CLIENT_FACTORY.create(userTO.getUsername(), "password123").
                getService(SchemaService.class);
        schemaTO = schemaService2.read(SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        try {
            schemaService2.update(SchemaType.PLAIN, schemaTO);
            fail("Schema update as user should not work");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        assertEquals(0, USER_SERVICE.read(userTO.getKey()).getFailedLogins());
    }

    @Test
    public void userRead() {
        UserCR userCR = UserITCase.getUniqueSample("testuserread@test.org");
        userCR.getRoles().add("User manager");

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserService userService2 = CLIENT_FACTORY.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        UserTO readUserTO = userService2.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(readUserTO);

        UserService userService3 = CLIENT_FACTORY.create("puccini", ADMIN_PWD).getService(UserService.class);

        try {
            userService3.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }
    }

    @Test
    public void userSearch() {
        UserCR userCR = UserITCase.getUniqueSample("testusersearch@test.org");
        userCR.getRoles().add("User reviewer");

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 1. user assigned to role 1, with search entitlement on realms /odd and /even: won't find anything with 
        // root realm
        UserService userService2 = CLIENT_FACTORY.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matchingUsers = userService2.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("key").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());
        Set<String> matchingUserKeys = matchingUsers.getResult().stream().
                map(AnyTO::getKey).collect(Collectors.toSet());
        assertTrue(matchingUserKeys.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertFalse(matchingUserKeys.contains("74cd8ece-715a-44a4-a736-e17b46c4e7e6"));
        assertFalse(matchingUserKeys.contains("823074dc-d280-436d-a7dd-07399fae48ec"));

        // 2. user assigned to role 4, with search entitlement on realm /even/two
        UserService userService3 = CLIENT_FACTORY.create("puccini", ADMIN_PWD).getService(UserService.class);

        matchingUsers = userService3.search(new AnyQuery.Builder().realm("/even/two").
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("loginDate").query()).build());
        assertNotNull(matchingUsers);
        assertTrue(matchingUsers.getResult().stream().allMatch(matching -> "/even/two".equals(matching.getRealm())));
    }

    @Test
    public void delegatedUserCRUD() {
        String roleKey = null;
        String delegatedAdminKey = null;
        try {
            // 1. create role for full user administration, under realm /even/two
            RoleTO role = new RoleTO();
            role.setKey("Delegated user admin");
            role.getEntitlements().add(IdRepoEntitlement.USER_CREATE);
            role.getEntitlements().add(IdRepoEntitlement.USER_UPDATE);
            role.getEntitlements().add(IdRepoEntitlement.USER_DELETE);
            role.getEntitlements().add(IdRepoEntitlement.USER_SEARCH);
            role.getEntitlements().add(IdRepoEntitlement.USER_READ);
            role.getRealms().add("/even/two");

            roleKey = ROLE_SERVICE.create(role).getHeaderString(RESTHeaders.RESOURCE_KEY);
            assertNotNull(roleKey);

            // 2. as admin, create delegated admin user, and assign the role just created
            UserCR delegatedAdminCR = UserITCase.getUniqueSample("admin@syncope.apache.org");
            delegatedAdminCR.getRoles().add(roleKey);
            UserTO delegatedAdmin = createUser(delegatedAdminCR).getEntity();
            delegatedAdminKey = delegatedAdmin.getKey();

            // 3. instantiate a delegate user service client, for further operations
            UserService delegatedUserService =
                    CLIENT_FACTORY.create(delegatedAdmin.getUsername(), "password123").getService(UserService.class);

            // 4. as delegated, create user under realm / -> fail
            UserCR userCR = UserITCase.getUniqueSample("delegated@syncope.apache.org");
            try {
                delegatedUserService.create(userCR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            // 5. set realm to /even/two -> succeed
            userCR.setRealm("/even/two");

            Response response = delegatedUserService.create(userCR);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            UserTO user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
            }).getEntity();
            assertEquals("/even/two", user.getRealm());
            assertEquals("surname", user.getPlainAttr("surname").get().getValues().getFirst());

            // 5. as delegated, update user attempting to move under realm /odd -> fail
            UserUR userUR = new UserUR();
            userUR.setKey(user.getKey());
            userUR.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
            userUR.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

            try {
                delegatedUserService.update(userUR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            // 6. revert realm change -> succeed
            userUR.setRealm(null);

            response = delegatedUserService.update(userUR);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
            }).getEntity();
            assertEquals("/even/two", user.getRealm());
            assertEquals("surname2", user.getPlainAttr("surname").get().getValues().getFirst());

            // 7. as delegated, delete user
            delegatedUserService.delete(user.getKey());

            try {
                USER_SERVICE.read(user.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            if (roleKey != null) {
                ROLE_SERVICE.delete(roleKey);
            }
            if (delegatedAdminKey != null) {
                USER_SERVICE.delete(delegatedAdminKey);
            }
        }
    }

    @Test
    public void checkFailedLogins() {
        UserCR userCR = UserITCase.getUniqueSample("checkFailedLogin@syncope.apache.org");
        userCR.getRoles().add("User manager");

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        String userKey = userTO.getKey();

        UserService userService2 = CLIENT_FACTORY.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, userService2.read(userKey).getFailedLogins());

        // authentications failed ...
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }
        assertEquals(2, USER_SERVICE.read(userKey).getFailedLogins());

        UserService userService4 = CLIENT_FACTORY.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, userService4.read(userKey).getFailedLogins());
    }

    @Test
    public void checkUserSuspension() {
        UserCR userCR = UserITCase.getUniqueSample("checkSuspension@syncope.apache.org");
        userCR.setRealm("/odd");
        userCR.getRoles().add("User manager");

        UserTO userTO = createUser(userCR).getEntity();
        String userKey = userTO.getKey();

        assertEquals(0, USER_SERVICE.read(userKey).getFailedLogins());

        // authentications failed ...
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }

        assertEquals(3, USER_SERVICE.read(userKey).getFailedLogins());

        // last authentication before suspension
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "wrongpwd1");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }

        userTO = USER_SERVICE.read(userTO.getKey());
        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(3, userTO.getFailedLogins().intValue());
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "password123");
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }

        StatusR reactivate = new StatusR.Builder(userTO.getKey(), StatusRType.REACTIVATE).build();
        userTO = USER_SERVICE.status(reactivate).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        SyncopeClient goodPwdClient = CLIENT_FACTORY.create(userTO.getUsername(), "password123");
        assertEquals(0, goodPwdClient.self().getRight().getFailedLogins().intValue());
    }

    @Test
    public void anyTypeEntitlement() {
        String anyTypeKey = "FOLDER " + getUUIDString();

        // 1. no entitlement exists (yet) for the any type to be created
        assertFalse(ANONYMOUS_CLIENT.platform().getEntitlements().stream().
                anyMatch(entitlement -> entitlement.contains(anyTypeKey)));

        // 2. create plain schema, any type class and any type
        PlainSchemaTO path = new PlainSchemaTO();
        path.setKey("path" + getUUIDString());
        path.setType(AttrSchemaType.String);
        path = createSchema(SchemaType.PLAIN, path);

        AnyTypeClassTO anyTypeClass = new AnyTypeClassTO();
        anyTypeClass.setKey("folder" + getUUIDString());
        anyTypeClass.getPlainSchemas().add(path.getKey());
        ANY_TYPE_CLASS_SERVICE.create(anyTypeClass);

        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey(anyTypeKey);
        anyTypeTO.setKind(AnyTypeKind.ANY_OBJECT);
        anyTypeTO.getClasses().add(anyTypeClass.getKey());
        ANY_TYPE_SERVICE.create(anyTypeTO);

        // 2. now entitlement exists for the any type just created
        assertTrue(ANONYMOUS_CLIENT.platform().getEntitlements().stream().
                anyMatch(entitlement -> entitlement.contains(anyTypeKey)));

        // 3. attempt to create an instance of the type above: fail because no entitlement was assigned
        AnyObjectCR folder = new AnyObjectCR();
        folder.setName("home");
        folder.setRealm(SyncopeConstants.ROOT_REALM);
        folder.setType(anyTypeKey);
        folder.getPlainAttrs().add(attr(path.getKey(), "/home"));

        SyncopeClient belliniClient = CLIENT_FACTORY.create("bellini", ADMIN_PWD);
        try {
            belliniClient.getService(AnyObjectService.class).create(folder);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 4. give create entitlement for the any type just created
        RoleTO role = new RoleTO();
        role.setKey("role" + getUUIDString());
        role.getRealms().add(SyncopeConstants.ROOT_REALM);
        role.getEntitlements().add(anyTypeKey + "_READ");
        role.getEntitlements().add(anyTypeKey + "_CREATE");
        role = createRole(role);

        UserTO bellini = USER_SERVICE.read("bellini");
        UserUR req = new UserUR();
        req.setKey(bellini.getKey());
        req.getRoles().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(role.getKey()).build());
        bellini = updateUser(req).getEntity();
        assertTrue(bellini.getRoles().contains(role.getKey()));

        // 5. now the instance of the type above can be created successfully
        belliniClient.logout();
        belliniClient.login(new BasicAuthenticationHandler("bellini", ADMIN_PWD));
        belliniClient.getService(AnyObjectService.class).create(folder);
    }

    @Test
    public void asGroupOwner() {
        // 0. prepare
        UserTO owner = createUser(UserITCase.getUniqueSample("owner@syncope.org")).getEntity();
        assertNotNull(owner);

        GroupCR groupCR = GroupITCase.getSample("forgroupownership");
        groupCR.setUserOwner(owner.getKey());
        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);
        assertEquals(owner.getKey(), group.getUserOwner());

        UserCR memberCR = UserITCase.getUniqueSample("forgroupownership@syncope.org");
        memberCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());
        memberCR.getMemberships().add(new MembershipTO.Builder("37d15e4c-cdc1-460b-a591-8505c8133806").build());
        UserTO member = createUser(memberCR).getEntity();
        assertEquals(2, member.getMemberships().size());
        String memberKey = member.getKey();

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matching = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(group.getKey()).query()).
                        page(1).size(1000).build());
        int fullMatchSize = matching.getResult().size();
        assertTrue(matching.getResult().stream().anyMatch(user -> memberKey.equals(user.getKey())));

        UserService groupOwnerService = CLIENT_FACTORY.create(owner.getUsername(), "password123").
                getService(UserService.class);

        // 1. search
        matching = groupOwnerService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("key").query()).
                        page(1).size(1000).build());
        assertEquals(fullMatchSize, matching.getResult().size());
        assertTrue(matching.getResult().stream().anyMatch(user -> memberKey.equals(user.getKey())));

        // 2. update and read
        UserUR memberUR = new UserUR();
        memberUR.setKey(memberKey);
        memberUR.setUsername(new StringReplacePatchItem.Builder().value("new" + getUUIDString()).build());

        Response response = groupOwnerService.update(memberUR);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        member = groupOwnerService.read(memberKey);
        assertEquals(memberUR.getUsername().getValue(), member.getUsername());
        assertEquals(2, member.getMemberships().size());

        // 3. update with membership removal -> fail
        memberUR.setUsername(null);
        memberUR.getMemberships().add(new MembershipUR.Builder(group.getKey()).
                operation(PatchOperation.DELETE).build());
        try {
            groupOwnerService.update(memberUR);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 4. update non-member -> fail
        UserTO nonmember = createUser(UserITCase.getUniqueSample("nonmember@syncope.org")).getEntity();
        UserUR nonmemberUR = new UserUR();
        nonmemberUR.setKey(nonmember.getKey());
        nonmemberUR.setUsername(new StringReplacePatchItem.Builder().value("new" + getUUIDString()).build());
        try {
            groupOwnerService.update(nonmemberUR);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 5. update user under /even
        memberCR = UserITCase.getUniqueSample("forgroupownership2@syncope.org");
        memberCR.setRealm("/even");
        memberCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());
        member = createUser(memberCR).getEntity();

        memberUR = new UserUR();
        memberUR.setKey(member.getKey());
        memberUR.setUsername(new StringReplacePatchItem.Builder().value("new" + getUUIDString()).build());
        response = groupOwnerService.update(memberUR);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // 6 delete
        groupOwnerService.delete(memberKey);
        try {
            USER_SERVICE.read(memberKey);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE434() {
        assumeTrue(IS_FLOWABLE_ENABLED);

        // 1. create user with group 'groupForWorkflowApproval' 
        // (users with group groupForWorkflowApproval are defined in workflow as subject to approval)
        UserCR userCR = UserITCase.getUniqueSample("createWithReject@syncope.apache.org");
        userCR.getMemberships().add(new MembershipTO.Builder("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());

        // 2. try to authenticate: fail
        try {
            CLIENT_FACTORY.create(userTO.getUsername(), "password123").self();
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }

        // 3. approve user
        UserRequestForm form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = USER_REQUEST_SERVICE.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // 4. try to authenticate again: success
        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                CLIENT_FACTORY.create(userTO.getUsername(), "password123").self();
        assertNotNull(self);
        assertNotNull(self.getLeft());
        assertEquals(List.of(), self.getMiddle());
        assertNotNull(self.getRight());
    }

    @Test
    public void issueSYNCOPE164() throws Exception {
        // 1. create user with db resource
        UserCR userCR = UserITCase.getUniqueSample("syncope164@syncope.apache.org");
        userCR.setRealm("/even/two");
        userCR.setPassword("password123");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user);

        // 2. unlink the resource from the created user
        ResourceDR resourceDR = new ResourceDR.Builder().key(user.getKey()).
                action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_TESTDB).build();
        assertNotNull(parseBatchResponse(USER_SERVICE.deassociate(resourceDR)));

        // 3. change password on Syncope
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("password234").build());
        user = updateUser(userUR).getEntity();
        assertNotNull(user);

        // 4. check that the db resource has still the initial password value
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(jdbcTemplate, MAX_WAIT_SECONDS,
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(encryptorManager.getInstance().encode("password123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. successfully authenticate with old (on db resource) and new (on internal storage) password values
        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                CLIENT_FACTORY.create(user.getUsername(), "password123").self();
        assertNotNull(self);
        self = CLIENT_FACTORY.create(user.getUsername(), "password234").self();
        assertNotNull(self);
    }

    @Test
    public void issueSYNCOPE706() {
        String username = getUUIDString();
        try {
            USER_SERVICE.read(username);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        try {
            CLIENT_FACTORY.create(username, "anypassword").self();
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertNotNull(e.getMessage());
        }
    }
}
