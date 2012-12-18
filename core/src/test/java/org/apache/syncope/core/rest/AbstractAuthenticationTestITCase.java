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
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.exceptions.InvalidSearchConditionException;
import org.apache.syncope.exceptions.UnauthorizedRoleException;
import org.apache.syncope.mod.StatusMod;
import org.apache.syncope.mod.StatusMod.Status;
import org.apache.syncope.propagation.PropagationException;
import org.apache.syncope.search.AttributeCond;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.services.AuthenticationService;
import org.apache.syncope.services.RoleService;
import org.apache.syncope.services.UserService;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.to.EntitlementTO;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.to.SchemaTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.SchemaType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.workflow.WorkflowException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public abstract class AbstractAuthenticationTestITCase extends AbstractTest {

    protected AuthenticationService as;
    protected RoleService rs;
    protected UserService us;

    @Test
    public void testAdminEntitlements() {
        // 1. as anonymous, read all available entitlements
        AuthenticationService as1 = createServiceInstance(AuthenticationService.class, null, null, as);
        Set<EntitlementTO> allEntitlements = as1.getAllEntitlements();

        assertNotNull(allEntitlements);
        assertFalse(allEntitlements.isEmpty());

        // 2. as admin, read own entitlements
        Set<EntitlementTO> adminEntitlements = as.getMyEntitlements();

        assertEquals(allEntitlements, adminEntitlements);
    }

    @Test
    public void testUserSchemaAuthorization() throws UnauthorizedRoleException, PropagationException,
            WorkflowException, NotFoundException {
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole" + UUID.randomUUID().toString().substring(0,8));
        authRoleTO.setParent(8L);
        authRoleTO.addEntitlement("SCHEMA_READ");

        Response r = rs.create(authRoleTO);
        authRoleTO = resolve(RoleTO.class, r, rs);

        // 1. create a schema (as admin)
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("authTestSchema");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = null;
        try {
            WebClient wc = super.createWebClient("schema/user");
            newSchemaTO = wc.path(schemaTO.getName()).get(SchemaTO.class);
        } catch (Exception e) {
            newSchemaTO = restTemplate.postForObject(BASE_URL + "schema/user/create", schemaTO,
                    SchemaTO.class);
        }

        assertEquals(schemaTO, newSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserTO userTO = AbstractUserTestITCase.getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(authRoleTO.getId());
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        r = us.create(userTO);
        userTO = resolve(UserTO.class, r, us);

        // 3. read the schema created above (as admin) - success
        schemaTO = restTemplate.getForObject(BASE_URL + "schema/user/read/authTestSchema.json",
                SchemaTO.class);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        super.setupRestTemplate(userTO.getUsername(), "password123");

        schemaTO = restTemplate.getForObject(BASE_URL + "schema/user/read/authTestSchema.json",
                SchemaTO.class);
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

        userTO = us.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getLastLoginDate());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
    }

    @Test
    public void testUserRead() throws UnauthorizedRoleException, PropagationException, WorkflowException,
            NotFoundException {
        UserTO userTO = AbstractUserTestITCase.getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        Response r = us.create(userTO);
        userTO = resolve(UserTO.class, r, us);

        super.setupRestTemplate(userTO.getUsername(), "password123");
        UserService us1 = createServiceInstance(UserService.class, userTO.getUsername(), "password123", us);

        UserTO readUserTO = us1.read(1L);
        assertNotNull(readUserTO);

        super.setupRestTemplate("user2", "password");
        UserService us2 = createServiceInstance(UserService.class, "user2", "password", us);

        try {
            us2.read(1L);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.UnauthorizedRole));
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void testUserSearch() throws UnauthorizedRoleException, PropagationException, WorkflowException,
            NotFoundException, InvalidSearchConditionException {
        UserTO userTO = AbstractUserTestITCase.getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        Response r = us.create(userTO);
        userTO = resolve(UserTO.class, r, us);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");
        UserService us1 = createServiceInstance(UserService.class, userTO.getUsername(), "password123", us);

        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCondition = NodeCond.getLeafCond(isNullCond);

        List<UserTO> matchedUsers = us1.search(searchCondition);
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(1L));

        super.setupRestTemplate("user2", "password");
        UserService us2 = createServiceInstance(UserService.class, "user2", "password", us);

        matchedUsers = us2.search(searchCondition);

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
    public void checkFailedLogins() throws UnauthorizedRoleException, PropagationException,
            WorkflowException, NotFoundException {
        UserTO userTO = AbstractUserTestITCase.getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        Response r = us.create(userTO);
        userTO = resolve(UserTO.class, r, us);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");
        UserService us1 = createServiceInstance(UserService.class, userTO.getUsername(), "password123", us);

        UserTO readUserTO = us1.read(userTO.getId());

        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());

        // authentications failed ...
        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");
        UserService us2 = createServiceInstance(UserService.class, userTO.getUsername(), "wrongpwd1", us);

        try {
            readUserTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
        // Try twice
        try {
            readUserTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        readUserTO = us.read(userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(2), readUserTO.getFailedLogins());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        readUserTO = us1.read(userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), readUserTO.getFailedLogins());
    }

    @Test
    public void checkUserSuspension() throws UnauthorizedRoleException, WorkflowException,
            PropagationException, NotFoundException {
        UserTO userTO = AbstractUserTestITCase.getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        Response r = us.create(userTO);
        userTO = resolve(UserTO.class, r, us);
        assertNotNull(userTO);

        super.setupRestTemplate(userTO.getUsername(), "password123");
        UserService us1 = createServiceInstance(UserService.class, userTO.getUsername(), "password123", us);

        userTO = us1.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());

        // authentications failed ...

        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");
        UserService us2 = createServiceInstance(UserService.class, userTO.getUsername(), "wrongpwd1", us);

        try {
            userTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        try {
            userTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
        try {
            userTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = us.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());

        // last authentication before suspension
        super.setupRestTemplate(userTO.getUsername(), "wrongpwd1");

        try {
            userTO = us2.read(userTO.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        userTO = us.read(userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(Integer.valueOf(3), userTO.getFailedLogins());
        assertEquals("suspended", userTO.getStatus());

        // check for authentication
        super.setupRestTemplate(userTO.getUsername(), "password123");

        try {
            userTO = us1.read(userTO.getId());
            fail("User should be suspended");
        } catch (Exception e) {
            assertNotNull(e);
        }

        // reset admin credentials for restTemplate
        super.resetRestTemplate();

        StatusMod status = new StatusMod(userTO.getId(), Status.REACTIVATE);
        userTO = us.setStatus(userTO.getId(), status);
        //userTO = restTemplate.getForObject(BASE_URL + "user/reactivate/" + userTO.getId(), UserTO.class);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        super.setupRestTemplate(userTO.getUsername(), "password123");

        userTO = us1.read(userTO.getId());

        assertNotNull(userTO);
        assertEquals(Integer.valueOf(0), userTO.getFailedLogins());
    }

    @Test
    public void issueSYNCOPE48() throws UnauthorizedRoleException, WorkflowException, PropagationException,
            NotFoundException {
        // Parent role, able to create users with role 1
        RoleTO parentRole = new RoleTO();
        parentRole.setName("parentAdminRole" + UUID.randomUUID().toString().substring(0,8));
        parentRole.addEntitlement("USER_CREATE");
        parentRole.addEntitlement("ROLE_1");
        parentRole.setParent(1L);

        Response r = rs.create(parentRole);
        parentRole = resolve(RoleTO.class, r, rs);
        assertNotNull(parentRole);

        // Child role, with no entitlements
        RoleTO childRole = new RoleTO();
        childRole.setName("childAdminRole" + UUID.randomUUID().toString().substring(0,8));
        childRole.setParent(parentRole.getId());

        r = rs.create(childRole);
        childRole = resolve(RoleTO.class, r, rs);
        assertNotNull(childRole);

        // User with child role, created by admin
        UserTO role1Admin = AbstractUserTestITCase.getSampleTO();
        role1Admin.setPassword("password");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(childRole.getId());
        role1Admin.addMembership(membershipTO);

        r = us.create(role1Admin);
        role1Admin = resolve(UserTO.class, r, us);
        assertNotNull(role1Admin);

        super.setupRestTemplate(role1Admin.getUsername(), "password");
        UserService us1 = createServiceInstance(UserService.class, role1Admin.getUsername(), "password", us);

        // User with role 1, created by user with child role created above
        UserTO role1User = AbstractUserTestITCase.getSampleTO();
        membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        role1User.addMembership(membershipTO);

        r = us1.create(role1User);
        role1User = resolve(UserTO.class, r, us1);
        assertNotNull(role1User);

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

}
