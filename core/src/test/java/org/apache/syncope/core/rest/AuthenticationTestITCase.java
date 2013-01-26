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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
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

        Response response = roleService.create(authRoleTO);
        authRoleTO = getObject(response, RoleTO.class, roleService);
        assertNotNull(authRoleTO);

        // 1. create a schema (as admin)
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("authTestSchema");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        response = schemaService.create(AttributableType.USER, SchemaService.SchemaType.NORMAL, schemaTO);
        SchemaTO newSchemaTO = getObject(response, SchemaTO.class, entitlementService);
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

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(AttributableType.USER, SchemaService.SchemaType.NORMAL, "authTestSchema");
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        super.setupRestTemplate(userTO.getUsername(), "password123");

        schemaTO = schemaService.read(AttributableType.USER, SchemaService.SchemaType.NORMAL, "authTestSchema");
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        HttpClientErrorException exception = null;
        try {
            schemaService.update(AttributableType.ROLE, SchemaService.SchemaType.NORMAL, schemaTO.getName(), schemaTO);
        } catch (HttpClientErrorException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getLastLoginDate());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
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

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        UserTO readUserTO = userService.read(1L);
        assertNotNull(readUserTO);

        super.setupRestTemplate("user2", "password");

        SyncopeClientException exception = null;
        try {
            userService.read(1L);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e
                    .getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void testUserSearch() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        AttributeCond isNullCond = new AttributeCond(
                AttributeCond.Type.ISNOTNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCondition = NodeCond.getLeafCond(isNullCond);

        List<UserTO> matchedUsers = userService.search(searchCondition);
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(1L));

        super.setupRestTemplate("user2", "password");

        matchedUsers = userService.search(searchCondition);

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
        UserTO userTO = UserTestITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        UserTO readUserTO = userService.read(userTO.getId());

        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());

        // authentications failed ...

        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        Throwable t = null;

        try {
            userService.read(userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            userService.read(userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        readUserTO = userService.read(userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(2), readUserTO.getFailedLogins());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        readUserTO = userService.read(userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());
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

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());

        // authentications failed ...

        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        Throwable t = null;

        try {
            userService.read(userTO.getId());
            fail();
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            userService.read(userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            userService.read(userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());

        // last authentication before suspension
        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        try {
            userService.read(userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());
        assertEquals("suspended", userTO.getStatus());

        // check for authentication

        super.setupRestTemplate(userTO.getUsername(), "password123");

        try {
            userService.read(userTO.getId());
            assertNotNull(userTO);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = userService.reactivate(userTO.getId());

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
    }

    @Test
    public void issueSYNCOPE48() {
        // Parent role, able to create users with role 1
        RoleTO parentRole = new RoleTO();
        parentRole.setName("parentAdminRole" + getUUIDString());
        parentRole.addEntitlement("USER_CREATE");
        parentRole.addEntitlement("ROLE_1");
        parentRole.setParent(1L);

        Response response = roleService.create(parentRole);
        parentRole = getObject(response, RoleTO.class, roleService);
        assertNotNull(parentRole);

        // Child role, with no entitlements
        RoleTO childRole = new RoleTO();
        childRole.setName("childAdminRole");
        childRole.setParent(parentRole.getId());

        response = roleService.create(childRole);
        childRole = getObject(response, RoleTO.class, roleService);
        assertNotNull(childRole);

        // User with child role, created by admin
        UserTO role1Admin = UserTestITCase.getUniqueSampleTO("syncope48admin@apache.org");
        role1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(childRole.getId());
        role1Admin.addMembership(membershipTO);

        role1Admin = userService.create(role1Admin);
        assertNotNull(role1Admin);

        super.setupRestTemplate(role1Admin.getUsername(), "password");

        // User with role 1, created by user with child role created above
        UserTO role1User = UserTestITCase.getUniqueSampleTO("syncope48user@apache.org");
        membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        role1User.addMembership(membershipTO);

        role1User = userService.create(role1User);
        assertNotNull(role1User);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }
}
