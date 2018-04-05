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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class UserSelfTestITCase extends AbstractTest {

    @Test
    public void selfRegistrationAllowed() {
        assertTrue(clientFactory.createAnonymous().isSelfRegAllowed());
    }

    @Test
    public void create() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        // 1. self-registration as admin: failure
        try {
            userSelfService.create(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"), true);
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        // 2. self-registration as anonymous: works
        SyncopeClient anonClient = clientFactory.createAnonymous();
        UserTO self = anonClient.getService(UserSelfService.class).
                create(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"), true).
                readEntity(UserTO.class);
        assertNotNull(self);
        assertEquals("createApproval", self.getStatus());
    }

    @Test
    public void createAndApprove() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        // self-create user with membership: goes 'createApproval' with resources and membership but no propagation
        UserTO userTO = UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org");
        MembershipTO membership = new MembershipTO();
        membership.setRoleId(3L);
        userTO.getMemberships().add(membership);
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        SyncopeClient anonClient = clientFactory.createAnonymous();
        userTO = anonClient.getService(UserSelfService.class).
                create(userTO, true).
                readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());
        assertFalse(userTO.getMemberships().isEmpty());
        assertFalse(userTO.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, SubjectType.USER, userTO.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // now approve and verify that propagation has happened
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getId());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, SubjectType.USER, userTO.getId()));
    }

    @Test
    public void read() {
        UserService userService2 = clientFactory.create("rossini", ADMIN_PWD).getService(UserService.class);

        try {
            userService2.read(1L);
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        UserSelfService userSelfService2 = clientFactory.create("rossini", ADMIN_PWD).getService(UserSelfService.class);
        UserTO userTO = userSelfService2.read();
        assertEquals("rossini", userTO.getUsername());
    }

    @Test
    public void updateWithoutApproval() {
        // 1. create user as admin
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username) - works
        UserMod userMod = new UserMod();
        userMod.setUsername(created.getUsername() + "XX");

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(created.getId(), userMod).
                readEntity(UserTO.class);
        assertNotNull(updated);
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers() ? "active" : "created", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
    }

    @Test
    public void updateWithApproval() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        // 1. create user as admin
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username + memberships + resource) - works but needs approval
        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(7L);
        AttributeMod testAttributeMod = new AttributeMod();
        testAttributeMod.setSchema("testAttribute");
        testAttributeMod.getValuesToBeAdded().add("a value");
        membershipMod.getAttrsToUpdate().add(testAttributeMod);

        UserMod userMod = new UserMod();
        userMod.setUsername(created.getUsername() + "XX");
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getResourcesToAdd().add(RESOURCE_NAME_TESTDB);
        userMod.setPassword("newPassword123");
        StatusMod statusMod = new StatusMod();
        statusMod.setOnSyncope(false);
        statusMod.getResourceNames().add(RESOURCE_NAME_TESTDB);
        userMod.setPwdPropRequest(statusMod);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(created.getId(), userMod).
                readEntity(UserTO.class);
        assertNotNull(updated);
        assertEquals("updateApproval", updated.getStatus());
        assertFalse(updated.getUsername().endsWith("XX"));
        assertTrue(updated.getMemberships().isEmpty());

        // no propagation happened
        assertTrue(updated.getResources().isEmpty());
        try {
            resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, SubjectType.USER, updated.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. approve self-update as admin
        WorkflowFormTO form = userWorkflowService.getFormForUser(updated.getId());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        updated = userWorkflowService.submitForm(form);
        assertNotNull(updated);
        assertEquals("active", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
        assertEquals(1, updated.getMemberships().size());

        // check that propagation also happened
        assertTrue(updated.getResources().contains(RESOURCE_NAME_TESTDB));
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, SubjectType.USER, updated.getId()));
    }

    @Test
    public void delete() {
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO deleted = authClient.getService(UserSelfService.class).delete().readEntity(UserTO.class);
        assertNotNull(deleted);
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers() ? "deleteApproval" : null, deleted.getStatus());
    }

    @Test
    public void issueSYNCOPE373() {
        UserTO userTO = userSelfService.read();
        assertEquals(ADMIN_UNAME, userTO.getUsername());
    }

    @Test
    public void noContent() throws IOException {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        SyncopeClient anonClient = clientFactory.createAnonymous();
        UserSelfService noContentService = anonClient.prefer(UserSelfService.class, Preference.RETURN_NO_CONTENT);

        UserTO user = UserTestITCase.getUniqueSampleTO("nocontent-anonymous@syncope.apache.org");

        Response response = noContentService.create(user, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));
    }

    @Test
    public void passwordReset() {
        // 1. create an user with security question and answer
        UserTO user = UserTestITCase.getUniqueSampleTO("pwdReset@syncope.apache.org");
        user.setSecurityQuestion(1L);
        user.setSecurityAnswer("Rossi");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(user);

        // verify propagation (including password) on external db
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String pwdOnResource = jdbcTemplate.queryForObject("SELECT password FROM test WHERE id=?", String.class,
                user.getUsername());
        assertTrue(StringUtils.isNotBlank(pwdOnResource));

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.getService(UserSelfService.class).read();
        assertNotNull(read);

        // SYNCOPE-1293:get users with token not null before requesting password reset
        PagedResult<UserTO> before = userService.search("token!=$null");

        // 3. request password reset (as anonymous) providing the expected security answer
        SyncopeClient anonClient = clientFactory.createAnonymous();
        try {
            anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "WRONG");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSecurityAnswer, e.getType());
        }
        anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "Rossi");

        // SYNCOPE-1293:get users with token not null before requesting password reset
        PagedResult<UserTO> after = userService.search("token!=$null");
        assertEquals(before.getTotalCount() + 1, after.getTotalCount());

        // 4. get token (normally sent via e-mail, now reading as admin)
        String token = userService.read(read.getId()).getToken();
        assertNotNull(token);

        // 5. confirm password reset
        try {
            anonClient.getService(UserSelfService.class).confirmPasswordReset("WRONG TOKEN", "newPassword");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword");
        read = authClient.getService(UserSelfService.class).read();
        assertNotNull(read);
        assertNull(read.getToken());

        // 7. verify that password was changed on external resource
        String newPwdOnResource = jdbcTemplate.queryForObject("SELECT password FROM test WHERE id=?", String.class,
                user.getUsername());
        assertTrue(StringUtils.isNotBlank(newPwdOnResource));
        assertNotEquals(pwdOnResource, newPwdOnResource);
    }

    @Test
    public void passwordResetWithoutSecurityQuestion() {
        // 0. disable security question for password reset
        configurationService.set("passwordReset.securityQuestion",
                attributeTO("passwordReset.securityQuestion", "false"));

        // 1. create an user with security question and answer
        UserTO user = UserTestITCase.getUniqueSampleTO("pwdResetNoSecurityQuestion@syncope.apache.org");
        createUser(user);

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.getService(UserSelfService.class).read();
        assertNotNull(read);

        // 3. request password reset (as anonymous) with no security answer
        SyncopeClient anonClient = clientFactory.createAnonymous();
        anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), null);

        // 4. get token (normally sent via e-mail, now reading as admin)
        String token = userService.read(read.getId()).getToken();
        assertNotNull(token);

        // 5. confirm password reset
        try {
            anonClient.getService(UserSelfService.class).confirmPasswordReset("WRONG TOKEN", "newPassword");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword");
        read = authClient.getService(UserSelfService.class).read();
        assertNotNull(read);
        assertNull(read.getToken());

        // 7. re-enable security question for password reset
        configurationService.set("passwordReset.securityQuestion",
                attributeTO("passwordReset.securityQuestion", "true"));
    }

}
