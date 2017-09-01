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
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.BasicAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class AuthenticationITCase extends AbstractITCase {

    @Autowired
    private DataSource testDataSource;

    private int getFailedLogins(final UserService userService, final String userKey) {
        UserTO readUserTO = userService.read(userKey);
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        return readUserTO.getFailedLogins();
    }

    @Test
    public void readEntitlements() {
        // 1. as not authenticated (not allowed)
        try {
            clientFactory.create().self();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 2. as anonymous
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY)).self();
        assertEquals(1, self.getKey().size());
        assertTrue(self.getKey().keySet().contains(StandardEntitlement.ANONYMOUS));
        assertEquals(ANONYMOUS_UNAME, self.getValue().getUsername());

        // 3. as admin
        self = adminClient.self();
        assertEquals(syncopeService.platform().getEntitlements().size(), self.getKey().size());
        assertFalse(self.getKey().keySet().contains(StandardEntitlement.ANONYMOUS));
        assertEquals(ADMIN_UNAME, self.getValue().getUsername());

        // 4. as user
        self = clientFactory.create("bellini", ADMIN_PWD).self();
        assertFalse(self.getKey().isEmpty());
        assertFalse(self.getKey().keySet().contains(StandardEntitlement.ANONYMOUS));
        assertEquals("bellini", self.getValue().getUsername());
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
        UserTO userTO = UserITCase.getUniqueSampleTO("auth@test.org");
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = clientFactory.create(userTO.getUsername(), "password123").
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

        assertEquals(0, getFailedLogins(userService, userTO.getKey()));
    }

    @Test
    public void userRead() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testuserread@test.org");
        userTO.getRoles().add("User manager");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        UserTO readUserTO = userService2.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(readUserTO);

        UserService userService3 = clientFactory.create("puccini", ADMIN_PWD).getService(UserService.class);

        try {
            userService3.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
            fail();
        } catch (SyncopeClientException e) {
            assertNotNull(e);
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }
    }

    @Test
    public void userSearch() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testusersearch@test.org");
        userTO.getRoles().add("User reviewer");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 1. user assigned to role 1, with search entitlement on realms /odd and /even: won't find anything with 
        // root realm
        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        PagedResult<UserTO> matchingUsers = userService2.search(new AnyQuery.Builder().
                realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("key").query()).build());
        assertNotNull(matchingUsers);
        assertFalse(matchingUsers.getResult().isEmpty());
        Set<String> matchingUserKeys = matchingUsers.getResult().stream().
                map(user -> user.getKey()).collect(Collectors.toSet());
        assertTrue(matchingUserKeys.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertFalse(matchingUserKeys.contains("74cd8ece-715a-44a4-a736-e17b46c4e7e6"));
        assertFalse(matchingUserKeys.contains("823074dc-d280-436d-a7dd-07399fae48ec"));

        // 2. user assigned to role 4, with search entitlement on realm /even/two
        UserService userService3 = clientFactory.create("puccini", ADMIN_PWD).getService(UserService.class);

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
            role.getEntitlements().add(StandardEntitlement.USER_CREATE);
            role.getEntitlements().add(StandardEntitlement.USER_UPDATE);
            role.getEntitlements().add(StandardEntitlement.USER_DELETE);
            role.getEntitlements().add(StandardEntitlement.USER_SEARCH);
            role.getEntitlements().add(StandardEntitlement.USER_READ);
            role.getRealms().add("/even/two");

            roleKey = roleService.create(role).getHeaderString(RESTHeaders.RESOURCE_KEY);
            assertNotNull(roleKey);

            // 2. as admin, create delegated admin user, and assign the role just created
            UserTO delegatedAdmin = UserITCase.getUniqueSampleTO("admin@syncope.apache.org");
            delegatedAdmin.getRoles().add(roleKey);
            delegatedAdmin = createUser(delegatedAdmin).getEntity();
            delegatedAdminKey = delegatedAdmin.getKey();

            // 3. instantiate a delegate user service client, for further operatins
            UserService delegatedUserService =
                    clientFactory.create(delegatedAdmin.getUsername(), "password123").getService(UserService.class);

            // 4. as delegated, create user under realm / -> fail
            UserTO user = UserITCase.getUniqueSampleTO("delegated@syncope.apache.org");
            try {
                delegatedUserService.create(user);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            // 5. set realm to /even/two -> succeed
            user.setRealm("/even/two");

            Response response = delegatedUserService.create(user);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
            }).getEntity();
            assertEquals("surname", user.getPlainAttr("surname").get().getValues().get(0));

            // 5. as delegated, update user attempting to move under realm / -> fail
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(user.getKey());
            userPatch.setRealm(new StringReplacePatchItem.Builder().value("/odd").build());
            userPatch.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

            try {
                delegatedUserService.update(userPatch);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            // 6. revert realm change -> succeed
            userPatch.setRealm(null);

            response = delegatedUserService.update(userPatch);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
            }).getEntity();
            assertEquals("surname2", user.getPlainAttr("surname").get().getValues().get(0));

            // 7. as delegated, delete user
            delegatedUserService.delete(user.getKey());

            try {
                userService.read(user.getKey());
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            if (roleKey != null) {
                roleService.delete(roleKey);
            }
            if (delegatedAdminKey != null) {
                userService.delete(delegatedAdminKey);
            }
        }
    }

    @Test
    public void checkFailedLogins() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");
        userTO.getRoles().add("User manager");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        String userKey = userTO.getKey();

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, getFailedLogins(userService2, userKey));

        // authentications failed ...
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }
        assertEquals(2, getFailedLogins(userService, userKey));

        UserService userService4 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, getFailedLogins(userService4, userKey));
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkSuspension@syncope.apache.org");
        userTO.setRealm("/odd");
        userTO.getRoles().add("User manager");

        userTO = createUser(userTO).getEntity();
        String userKey = userTO.getKey();
        assertNotNull(userTO);

        assertEquals(0, getFailedLogins(userService, userKey));

        // authentications failed ...
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        assertEquals(3, getFailedLogins(userService, userKey));

        // last authentication before suspension
        try {
            clientFactory.create(userTO.getUsername(), "wrongpwd1");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(3, userTO.getFailedLogins(), 0);
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        try {
            clientFactory.create(userTO.getUsername(), "password123");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        StatusPatch reactivate = new StatusPatch();
        reactivate.setKey(userTO.getKey());
        reactivate.setType(StatusPatchType.REACTIVATE);
        userTO = userService.status(reactivate).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        SyncopeClient goodPwdClient = clientFactory.create(userTO.getUsername(), "password123");
        assertEquals(0, goodPwdClient.self().getValue().getFailedLogins(), 0);
    }

    @Test
    public void anyTypeEntitlement() {
        final String anyTypeKey = "FOLDER " + getUUIDString();

        // 1. no entitlement exists (yet) for the any type to be created
        assertFalse(syncopeService.platform().getEntitlements().stream().
                anyMatch(entitlement -> entitlement.contains(anyTypeKey)));

        // 2. create plain schema, any type class and any type
        PlainSchemaTO path = new PlainSchemaTO();
        path.setKey("path" + getUUIDString());
        path.setType(AttrSchemaType.String);
        path = createSchema(SchemaType.PLAIN, path);

        AnyTypeClassTO anyTypeClass = new AnyTypeClassTO();
        anyTypeClass.setKey("folder" + getUUIDString());
        anyTypeClass.getPlainSchemas().add(path.getKey());
        anyTypeClassService.create(anyTypeClass);

        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey(anyTypeKey);
        anyTypeTO.setKind(AnyTypeKind.ANY_OBJECT);
        anyTypeTO.getClasses().add(anyTypeClass.getKey());
        anyTypeService.create(anyTypeTO);

        // 2. now entitlement exists for the any type just created
        assertTrue(syncopeService.platform().getEntitlements().stream().
                anyMatch(entitlement -> entitlement.contains(anyTypeKey)));

        // 3. attempt to create an instance of the type above: fail because no entitlement was assigned
        AnyObjectTO folder = new AnyObjectTO();
        folder.setName("home");
        folder.setRealm(SyncopeConstants.ROOT_REALM);
        folder.setType(anyTypeKey);
        folder.getPlainAttrs().add(attrTO(path.getKey(), "/home"));

        SyncopeClient belliniClient = clientFactory.create("bellini", ADMIN_PWD);
        try {
            belliniClient.getService(AnyObjectService.class).create(folder);
            fail();
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

        UserTO bellini = userService.read("bellini");
        UserPatch patch = new UserPatch();
        patch.setKey(bellini.getKey());
        patch.getRoles().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(role.getKey()).build());
        bellini = updateUser(patch).getEntity();
        assertTrue(bellini.getRoles().contains(role.getKey()));

        // 5. now the instance of the type above can be created successfully
        belliniClient.logout();
        belliniClient.login(new BasicAuthenticationHandler("bellini", ADMIN_PWD));
        belliniClient.getService(AnyObjectService.class).create(folder);
    }

    @Test
    public void issueSYNCOPE434() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // 1. create user with group 'groupForWorkflowApproval' 
        // (users with group groupForWorkflowApproval are defined in workflow as subject to approval)
        UserTO userTO = UserITCase.getUniqueSampleTO("createWithReject@syncope.apache.org");
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());

        // 2. try to authenticate: fail
        try {
            clientFactory.create(userTO.getUsername(), "password123").self();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 3. approve user
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        form = userWorkflowService.claimForm(form.getTaskId());
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // 4. try to authenticate again: success
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(userTO.getUsername(), "password123").self();
        assertNotNull(self);
        assertNotNull(self.getKey());
        assertNotNull(self.getValue());
    }

    @Test
    public void issueSYNCOPE164() throws Exception {
        // 1. create user with db resource
        UserTO user = UserITCase.getUniqueSampleTO("syncope164@syncope.apache.org");
        user.setRealm("/even/two");
        user.setPassword("password123");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user).getEntity();
        assertNotNull(user);

        // 2. unlink the resource from the created user
        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(user.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
        deassociationPatch.getResources().add(RESOURCE_NAME_TESTDB);
        assertNotNull(userService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        // 3. change password on Syncope
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("password234").build());
        user = updateUser(userPatch).getEntity();
        assertNotNull(user);

        // 4. check that the db resource has still the initial password value
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(
                jdbcTemplate, 50, "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("password123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. successfully authenticate with old (on db resource) and new (on internal storage) password values
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(user.getUsername(), "password123").self();
        assertNotNull(self);
        self = clientFactory.create(user.getUsername(), "password234").self();
        assertNotNull(self);
    }

    @Test
    public void issueSYNCOPE706() {
        String username = getUUIDString();
        try {
            userService.read(username);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        try {
            clientFactory.create(username, "anypassword").self();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e.getMessage());
        }
    }
}
