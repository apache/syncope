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
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.wrap.EntitlementTO;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.EntitlementService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.misc.security.Encryptor;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class AuthenticationITCase extends AbstractITCase {

    private int getFailedLogins(UserService testUserService, long userId) {
        UserTO readUserTO = testUserService.read(userId);
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        return readUserTO.getFailedLogins();
    }

    private void assertReadFails(UserService userService, long id) {
        try {
            userService.read(id);
            fail("access should not work");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAdminEntitlements() {
        // 1. as anonymous, read all available entitlements
        List<EntitlementTO> allEntitlements = entitlementService.getAllEntitlements();
        assertNotNull(allEntitlements);
        assertFalse(allEntitlements.isEmpty());

        // 2. as admin, read own entitlements
        List<EntitlementTO> adminEntitlements = entitlementService.getOwnEntitlements();

        assertEquals(new HashSet<String>(CollectionWrapper.unwrap(allEntitlements)),
                new HashSet<String>(CollectionWrapper.unwrap(adminEntitlements)));
    }

    @Test
    public void testUserSchemaAuthorization() {
        // 0. create a group that can only read schemas
        GroupTO authGroupTO = new GroupTO();
        authGroupTO.setName("authGroup" + getUUIDString());
        authGroupTO.setParent(8L);
        authGroupTO.getEntitlements().add("SCHEMA_READ");

        authGroupTO = createGroup(authGroupTO);
        assertNotNull(authGroupTO);

        String schemaName = "authTestSchema" + getUUIDString();

        // 1. create a schema (as admin)
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(schemaName);
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(AttrSchemaType.String);

        PlainSchemaTO newPlainSchemaTO = createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        // 2. create an user with the group created above (as admin)
        UserTO userTO = UserITCase.getUniqueSampleTO("auth@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(authGroupTO.getKey());
        AttrTO testAttrTO = new AttrTO();
        testAttrTO.setSchema("testAttribute");
        testAttrTO.getValues().add("a value");
        membershipTO.getPlainAttrs().add(testAttrTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(AttributableType.USER, SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = clientFactory.create(userTO.getUsername(), "password123").getService(
                SchemaService.class);

        schemaTO = schemaService2.read(AttributableType.USER, SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        try {
            schemaService2.update(AttributableType.GROUP, SchemaType.PLAIN, schemaName, schemaTO);
            fail("Schemaupdate as user schould not work");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            // CXF Service will throw this exception
            assertNotNull(e);
        }

        assertEquals(0, getFailedLogins(userService, userTO.getKey()));
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testuserread@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(7L);
        AttrTO testAttrTO = new AttrTO();
        testAttrTO.setSchema("testAttribute");
        testAttrTO.getValues().add("a value");
        membershipTO.getPlainAttrs().add(testAttrTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        UserTO readUserTO = userService2.read(1L);
        assertNotNull(readUserTO);

        UserService userService3 = clientFactory.create("verdi", ADMIN_PWD).getService(UserService.class);

        SyncopeClientException exception = null;
        try {
            userService3.read(1L);
            fail();
        } catch (SyncopeClientException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(ClientExceptionType.UnauthorizedGroup, exception.getType());
    }

    @Test
    public void testUserSearch() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(7L);
        AttrTO testAttrTO = new AttrTO();
        testAttrTO.setSchema("testAttribute");
        testAttrTO.getValues().add("a value");
        membershipTO.getPlainAttrs().add(testAttrTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        PagedResult<UserTO> matchedUsers = userService2.search(
                SyncopeClient.getUserSearchConditionBuilder().isNotNull("loginDate").query());
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getResult().isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.getResult().size());
        for (UserTO user : matchedUsers.getResult()) {
            userIds.add(user.getKey());
        }
        assertTrue(userIds.contains(1L));

        UserService userService3 = clientFactory.create("verdi", "password").getService(UserService.class);

        matchedUsers = userService3.search(
                SyncopeClient.getUserSearchConditionBuilder().isNotNull("loginDate").query());
        assertNotNull(matchedUsers);

        userIds = new HashSet<>(matchedUsers.getResult().size());

        for (UserTO user : matchedUsers.getResult()) {
            userIds.add(user.getKey());
        }
        assertFalse(userIds.contains(1L));
    }

    @Test
    public void checkFailedLogins() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(7L);
        AttrTO testAttrTO = new AttrTO();
        testAttrTO.setSchema("testAttribute");
        testAttrTO.getValues().add("a value");
        membershipTO.getPlainAttrs().add(testAttrTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        long userId = userTO.getKey();

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(
                UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        UserService userService3 = clientFactory.create(userTO.getUsername(), "wrongpwd1").getService(
                UserService.class);
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);

        assertEquals(2, getFailedLogins(userService, userId));

        UserService userService4 = clientFactory.create(userTO.getUsername(), "password123").getService(
                UserService.class);
        assertEquals(0, getFailedLogins(userService4, userId));
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkSuspension@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(7L);
        AttrTO testAttrTO = new AttrTO();
        testAttrTO.setSchema("testAttribute");
        testAttrTO.getValues().add("a value");
        membershipTO.getPlainAttrs().add(testAttrTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        long userId = userTO.getKey();
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        UserService userService3 = clientFactory.create(userTO.getUsername(), "wrongpwd1").
                getService(UserService.class);
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);

        assertEquals(3, getFailedLogins(userService, userId));

        // last authentication before suspension
        assertReadFails(userService3, userId);

        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(3, userTO.getFailedLogins(), 0);
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(UserService.class);
        assertReadFails(userService2, userId);

        StatusMod reactivate = new StatusMod();
        reactivate.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userTO.getKey(), reactivate).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));
    }

    @Test
    public void issueSYNCOPE48() {
        // Parent group, able to create users with group 1
        GroupTO parentGroup = new GroupTO();
        parentGroup.setName("parentAdminGroup" + getUUIDString());
        parentGroup.getEntitlements().add("USER_CREATE");
        parentGroup.getEntitlements().add("GROUP_1");
        parentGroup.setParent(1L);
        parentGroup = createGroup(parentGroup);
        assertNotNull(parentGroup);

        // Child group, with no entitlements
        GroupTO childGroup = new GroupTO();
        childGroup.setName("childAdminGroup");
        childGroup.setParent(parentGroup.getKey());

        childGroup = createGroup(childGroup);
        assertNotNull(childGroup);

        // User with child group, created by admin
        UserTO group1Admin = UserITCase.getUniqueSampleTO("syncope48admin@apache.org");
        group1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(childGroup.getKey());
        group1Admin.getMemberships().add(membershipTO);

        group1Admin = createUser(group1Admin);
        assertNotNull(group1Admin);

        UserService userService2 = clientFactory.create(group1Admin.getUsername(), "password").getService(
                UserService.class);

        // User with group 1, created by user with child group created above
        UserTO group1User = UserITCase.getUniqueSampleTO("syncope48user@apache.org");
        membershipTO = new MembershipTO();
        membershipTO.setGroupId(1L);
        group1User.getMemberships().add(membershipTO);

        Response response = userService2.create(group1User, true);
        assertNotNull(response);
        group1User = response.readEntity(UserTO.class);
        assertNotNull(group1User);
    }

    @Test
    public void issueSYNCOPE434() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers(syncopeService));

        // 1. create user with group 9 (users with group 9 are defined in workflow as subject to approval)
        UserTO userTO = UserITCase.getUniqueSampleTO("createWithReject@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(9L);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());

        // 2. try to authenticate: fail
        EntitlementService myEntitlementService = clientFactory.create(userTO.getUsername(), "password123").
                getService(EntitlementService.class);
        try {
            myEntitlementService.getOwnEntitlements();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 3. approve user
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.getProperties().clear();
        form.getProperties().addAll(props.values());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // 4. try to authenticate again: success
        assertNotNull(myEntitlementService.getOwnEntitlements());
    }

    @Test
    public void issueSYNCOPE164() throws Exception {
        // 1. create user with db resource
        UserTO user = UserITCase.getUniqueSampleTO("syncope164@syncope.apache.org");
        user.setPassword("password1");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user);
        assertNotNull(user);

        // 2. unlink the resource from the created user
        assertNotNull(userService.bulkDeassociation(user.getKey(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_TESTDB, ResourceName.class)).
                readEntity(BulkActionResult.class));

        // 3. change password on Syncope
        UserMod userMod = new UserMod();
        userMod.setKey(user.getKey());
        userMod.setPassword("password2");
        user = updateUser(userMod);
        assertNotNull(user);

        // 4. check that the db resource has still the initial password value
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("password1", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. successfully authenticate with old (on db resource) and new (on internal storage) password values
        user = clientFactory.create(user.getUsername(), "password1").getService(UserSelfService.class).read();
        assertNotNull(user);
        user = clientFactory.create(user.getUsername(), "password2").getService(UserSelfService.class).read();
        assertNotNull(user);
    }
}
