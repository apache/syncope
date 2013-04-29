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

import java.net.Authenticator;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.cxf.transport.http.CXFAuthenticator;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.rest.jaxrs.CXFPatchedAuthenticator;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@FixMethodOrder(MethodSorters.JVM)
public class AuthenticationTestITCase extends AbstractTest {

    @Test
    public void testAdminEntitlements() {
        // 1. as anonymous, read all available entitlements
        Set<EntitlementTO> allEntitlements = entitlementService.getAllEntitlements();
        assertNotNull(allEntitlements);
        assertFalse(allEntitlements.isEmpty());

        // 2. as admin, read own entitlements
        super.resetRestTemplate();

        Set<EntitlementTO> adminEntitlements = entitlementService.getMyEntitlements();

        assertEquals(allEntitlements, adminEntitlements);
    }

    @Test
    public void testUserSchemaAuthorization() {
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole" + getUUIDString());
        authRoleTO.setParent(8L);
        authRoleTO.addEntitlement("SCHEMA_READ");

        authRoleTO = createRole(roleService, authRoleTO);
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
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(AttributableType.USER, SchemaType.NORMAL, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = setupCredentials(schemaService, SchemaService.class,
                userTO.getUsername(), "password123");

        schemaTO = schemaService2.read(AttributableType.USER, SchemaType.NORMAL, schemaName);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        try {
            schemaService2.update(AttributableType.ROLE, SchemaType.NORMAL, schemaName, schemaTO);
            fail("Schemaupdate as user schould not work");
        } catch (HttpClientErrorException e) {
            assertNotNull(e);
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        } catch (AccessControlException e) {
            // CXF Service will throw this exception
            assertNotNull(e);
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
        assertEquals(0, getFailedLogins(userService, userTO.getId()));
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("testuserread@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "password123");

        UserTO readUserTO = userService2.read(1L);
        assertNotNull(readUserTO);

        UserService userService3 = setupCredentials(userService, UserService.class, "verdi", ADMIN_PWD);

        SyncopeClientException exception = null;
        try {
            userService3.read(1L);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void testUserSearch() throws InvalidSearchConditionException {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "password123");

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

        UserService userService3 = setupCredentials(userService, UserService.class, "verdi", "password");

        matchedUsers = userService3.search(searchCondition);

        assertNotNull(matchedUsers);

        userIds = new HashSet<Long>(matchedUsers.size());

        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertFalse(userIds.contains(1L));

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void checkFailedLogins() {
        // Workaround for CXF issue.. remove after upgrade to 2.7.3
        CXFAuthenticator.addAuthenticator();
        Authenticator.setDefault(new CXFPatchedAuthenticator());
        UserTO userTO = UserTestITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        long userId = userTO.getId();

        UserService userService2 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "password123");
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        UserService userService3 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "wrongpwd1");
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
        assertEquals(2, getFailedLogins(userService, userId));

        UserService userService4 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "password123");
        assertEquals(0, getFailedLogins(userService4, userId));
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("checkSuspension@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = createUser(userTO);
        long userId = userTO.getId();
        assertNotNull(userTO);

        UserService userService2 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "password123");
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        UserService userService3 = setupCredentials(userService, UserService.class,
                userTO.getUsername(), "wrongpwd1");
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);
        assertReadFails(userService3, userId);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
        assertEquals(3, getFailedLogins(userService, userId));

        // last authentication before suspension
        // TODO remove after CXF migration is complete
        userService3 = setupCredentials(userService, UserService.class, userTO.getUsername(), "wrongpwd1");
        assertReadFails(userService3, userId);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        // TODO remove after CXF migration is complete
        userService2 = setupCredentials(userService, UserService.class, userTO.getUsername(), "password123");
        assertReadFails(userService2, userId);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
        userTO = userService.reactivate(userTO.getId());
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // TODO remove after CXF migration is complete
        userService2 = setupCredentials(userService, UserService.class, userTO.getUsername(), "password123");
        assertEquals(0, getFailedLogins(userService2, userId));
    }

    @Test
    public void issueSYNCOPE48() {
        // Parent role, able to create users with role 1
        RoleTO parentRole = new RoleTO();
        parentRole.setName("parentAdminRole" + getUUIDString());
        parentRole.addEntitlement("USER_CREATE");
        parentRole.addEntitlement("ROLE_1");
        parentRole.setParent(1L);

        parentRole =  createRole(roleService, parentRole);
        assertNotNull(parentRole);

        // Child role, with no entitlements
        RoleTO childRole = new RoleTO();
        childRole.setName("childAdminRole");
        childRole.setParent(parentRole.getId());

        childRole = createRole(roleService, childRole);
        assertNotNull(childRole);

        // User with child role, created by admin
        UserTO role1Admin = UserTestITCase.getUniqueSampleTO("syncope48admin@apache.org");
        role1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(childRole.getId());
        role1Admin.addMembership(membershipTO);

        role1Admin = createUser(role1Admin);
        assertNotNull(role1Admin);

        UserService userService2 = setupCredentials(userService, UserService.class,
                role1Admin.getUsername(), "password");

        // User with role 1, created by user with child role created above
        UserTO role1User = UserTestITCase.getUniqueSampleTO("syncope48user@apache.org");
        membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        role1User.addMembership(membershipTO);

        Response response = userService2.create(role1User);
        assertNotNull(response);
        role1User = response.readEntity(UserTO.class);
        assertNotNull(role1User);
    }

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
}
