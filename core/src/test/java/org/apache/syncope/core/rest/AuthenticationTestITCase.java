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
package org.apache.syncope.core.rest;

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
import org.apache.syncope.common.mod.StatusMod;

import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AuthenticationTestITCase extends AbstractTest {

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
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole" + getUUIDString());
        authRoleTO.setParent(8L);
        authRoleTO.getEntitlements().add("SCHEMA_READ");

        authRoleTO = createRole(authRoleTO);
        assertNotNull(authRoleTO);

        String schemaName = "authTestSchema" + getUUIDString();

        // 1. create a schema (as admin)
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName(schemaName);
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(AttributeSchemaType.String);

        SchemaTO newSchemaTO = createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        assertEquals(schemaTO, newSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserTO userTO = UserTestITCase.getUniqueSampleTO("auth@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(authRoleTO.getId());
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.getValues().add("a value");
        membershipTO.getAttrs().add(testAttributeTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(AttributableType.USER, SchemaType.NORMAL, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = clientFactory.create(userTO.getUsername(), "password123").getService(
                SchemaService.class);

        schemaTO = schemaService2.read(AttributableType.USER, SchemaType.NORMAL, schemaName);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        try {
            schemaService2.update(AttributableType.ROLE, SchemaType.NORMAL, schemaName, schemaTO);
            fail("Schemaupdate as user schould not work");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            // CXF Service will throw this exception
            assertNotNull(e);
        }

        assertEquals(0, getFailedLogins(userService, userTO.getId()));
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("testuserread@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.getValues().add("a value");
        membershipTO.getAttrs().add(testAttributeTO);
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
        assertEquals(ClientExceptionType.UnauthorizedRole, exception.getType());
    }

    @Test
    public void testUserSearch() throws InvalidSearchConditionException {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.getValues().add("a value");
        membershipTO.getAttrs().add(testAttributeTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCondition = NodeCond.getLeafCond(isNullCond);

        List<UserTO> matchedUsers = userService2.search(searchCondition);
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(1L));

        UserService userService3 = clientFactory.create("verdi", "password").getService(UserService.class);

        matchedUsers = userService3.search(searchCondition);

        assertNotNull(matchedUsers);

        userIds = new HashSet<Long>(matchedUsers.size());

        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertFalse(userIds.contains(1L));
    }

    @Test
    public void checkFailedLogins() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.getValues().add("a value");
        membershipTO.getAttrs().add(testAttributeTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        long userId = userTO.getId();

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(
                UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        UserService userService3 = clientFactory.create(userTO.getUsername(), "wrongpwd1").getService(UserService.class);
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);

        assertEquals(2, getFailedLogins(userService, userId));

        UserService userService4 = clientFactory.create(userTO.getUsername(), "password123").getService(
                UserService.class);
        assertEquals(0, getFailedLogins(userService4, userId));
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("checkSuspension@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.getValues().add("a value");
        membershipTO.getAttrs().add(testAttributeTO);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        long userId = userTO.getId();
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

        userTO = userService.read(userTO.getId());
        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(UserService.class);
        assertReadFails(userService2, userId);

        StatusMod reactivate = new StatusMod();
        reactivate.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userTO.getId(), reactivate).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        userService2 = clientFactory.create(userTO.getUsername(), "password123").getService(UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));
    }

    @Test
    public void issueSYNCOPE48() {
        // Parent role, able to create users with role 1
        RoleTO parentRole = new RoleTO();
        parentRole.setName("parentAdminRole" + getUUIDString());
        parentRole.getEntitlements().add("USER_CREATE");
        parentRole.getEntitlements().add("ROLE_1");
        parentRole.setParent(1L);

        parentRole = createRole(parentRole);
        assertNotNull(parentRole);

        // Child role, with no entitlements
        RoleTO childRole = new RoleTO();
        childRole.setName("childAdminRole");
        childRole.setParent(parentRole.getId());

        childRole = createRole(childRole);
        assertNotNull(childRole);

        // User with child role, created by admin
        UserTO role1Admin = UserTestITCase.getUniqueSampleTO("syncope48admin@apache.org");
        role1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(childRole.getId());
        role1Admin.getMemberships().add(membershipTO);

        role1Admin = createUser(role1Admin);
        assertNotNull(role1Admin);

        UserService userService2 = clientFactory.create(role1Admin.getUsername(), "password").getService(
                UserService.class);

        // User with role 1, created by user with child role created above
        UserTO role1User = UserTestITCase.getUniqueSampleTO("syncope48user@apache.org");
        membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        role1User.getMemberships().add(membershipTO);

        Response response = userService2.create(role1User);
        assertNotNull(response);
        role1User = response.readEntity(UserTO.class);
        assertNotNull(role1User);
    }

    @Test
    public void issueSYNCOPE434() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        // 1. create user with role 9 (users with role 9 are defined in workflow as subject to approval)
        UserTO userTO = UserTestITCase.getUniqueSampleTO("createWithReject@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(9L);
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
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getId());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // 4. try to authenticate again: success
        assertNotNull(myEntitlementService.getOwnEntitlements());
    }
}
