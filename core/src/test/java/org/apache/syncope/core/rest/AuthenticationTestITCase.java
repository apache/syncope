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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.types.SchemaType;
import org.apache.syncope.types.SyncopeClientExceptionType;

public class AuthenticationTestITCase extends AbstractTest {

    @Test
    public void testAdminEntitlements() {
        // 1. as anonymous, read all available entitlements
        Set<String> allEntitlements = new HashSet<String>(Arrays.asList(anonymousRestTemplate().getForObject(
                BASE_URL + "auth/allentitlements.json", String[].class)));
        assertNotNull(allEntitlements);
        assertFalse(allEntitlements.isEmpty());

        // 2. as admin, read own entitlements
        super.resetRestTemplate();

        Set<String> adminEntitlements = new HashSet<String>(Arrays.asList(restTemplate.getForObject(BASE_URL
                + "auth/entitlements.json", String[].class)));

        assertEquals(allEntitlements, adminEntitlements);
    }

    @Test
    public void testUserSchemaAuthorization() {
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole");
        authRoleTO.setParent(8L);
        authRoleTO.addEntitlement("SCHEMA_READ");

        authRoleTO = restTemplate.postForObject(BASE_URL + "role/create", authRoleTO, RoleTO.class);
        assertNotNull(authRoleTO);

        // 1. create a schema (as admin)
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("authTestSchema");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserTO userTO = UserTestITCase.getSampleTO("auth@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(authRoleTO.getId());
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = restTemplate.getForObject(BASE_URL + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        super.setupRestTemplate(userTO.getUsername(), "password123");

        schemaTO = restTemplate.getForObject(BASE_URL + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        HttpClientErrorException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "schema/role/update", schemaTO, SchemaTO.class);
        } catch (HttpClientErrorException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getLastLoginDate());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserTestITCase.getSampleTO("testuserread@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        UserTO readUserTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
        assertNotNull(readUserTO);

        super.setupRestTemplate("user2", "password");

        SyncopeClientException exception = null;
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void testUserSearch() {
        UserTO userTO = UserTestITCase.getSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCondition = NodeCond.getLeafCond(isNullCond);

        List<UserTO> matchedUsers = Arrays.asList(restTemplate.postForObject(BASE_URL + "user/search", searchCondition,
                UserTO[].class));
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(1L));

        super.setupRestTemplate("user2", "password");

        matchedUsers =
                Arrays.asList(restTemplate.postForObject(BASE_URL + "user/search", searchCondition, UserTO[].class));

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
        UserTO userTO = UserTestITCase.getSampleTO("checkFailedLogin@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        UserTO readUserTO =
                restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());

        // authentications failed ...

        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        Throwable t = null;

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        readUserTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(2), readUserTO.getFailedLogins());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        readUserTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserTestITCase.getSampleTO("checkSuspension@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());

        // authentications failed ...

        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        Throwable t = null;

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());

        // last authentication before suspension
        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());
        assertEquals("suspended", userTO.getStatus());

        // check for authentication

        super.setupRestTemplate(userTO.getUsername(), "password123");

        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());
            assertNotNull(userTO);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = restTemplate.getForObject(BASE_URL + "user/reactivate/" + userTO.getId(), UserTO.class);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
    }

    @Test
    public void issueSYNCOPE48() {
        // Parent role, able to create users with role 1
        RoleTO parentRole = new RoleTO();
        parentRole.setName("parentAdminRole");
        parentRole.addEntitlement("USER_CREATE");
        parentRole.addEntitlement("ROLE_1");
        parentRole.setParent(1L);

        parentRole = restTemplate.postForObject(BASE_URL + "role/create", parentRole, RoleTO.class);
        assertNotNull(parentRole);

        // Child role, with no entitlements
        RoleTO childRole = new RoleTO();
        childRole.setName("childAdminRole");
        childRole.setParent(parentRole.getId());

        childRole = restTemplate.postForObject(BASE_URL + "role/create", childRole, RoleTO.class);
        assertNotNull(childRole);

        // User with child role, created by admin
        UserTO role1Admin = UserTestITCase.getSampleTO("syncope48admin@apache.org");
        role1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(childRole.getId());
        role1Admin.addMembership(membershipTO);

        role1Admin = restTemplate.postForObject(BASE_URL + "user/create", role1Admin, UserTO.class);
        assertNotNull(role1Admin);

        super.setupRestTemplate(role1Admin.getUsername(), "password");

        // User with role 1, created by user with child role created above
        UserTO role1User = UserTestITCase.getSampleTO("syncope48user@apache.org");
        membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        role1User.addMembership(membershipTO);

        role1User = restTemplate.postForObject(BASE_URL + "user/create", role1User, UserTO.class);
        assertNotNull(role1User);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }
}
