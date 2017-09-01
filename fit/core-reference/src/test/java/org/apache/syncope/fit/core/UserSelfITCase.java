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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
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
public class UserSelfITCase extends AbstractITCase {

    @Autowired
    private DataSource testDataSource;

    @Test
    public void selfRegistrationAllowed() {
        assertTrue(syncopeService.platform().isSelfRegAllowed());
    }

    @Test
    public void create() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // 1. self-registration as admin: failure
        try {
            userSelfService.create(UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org"), true);
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        // 2. self-registration as anonymous: works
        SyncopeClient anonClient = clientFactory.create();
        UserTO self = anonClient.getService(UserSelfService.class).
                create(UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org"), true).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(self);
        assertEquals("createApproval", self.getStatus());
    }

    @Test
    public void createAndApprove() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // self-create user with membership: goes 'createApproval' with resources and membership but no propagation
        UserTO userTO = UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org");
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("29f96485-729e-4d31-88a1-6fc60e4677f3").build());
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        SyncopeClient anonClient = clientFactory.create();
        userTO = anonClient.getService(UserSelfService.class).
                create(userTO, true).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());
        assertFalse(userTO.getMemberships().isEmpty());
        assertFalse(userTO.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // now approve and verify that propagation has happened
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        form = userWorkflowService.claimForm(form.getTaskId());
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey()));
    }

    @Test
    public void read() {
        UserService userService2 = clientFactory.create("rossini", ADMIN_PWD).getService(UserService.class);

        try {
            userService2.read("1417acbe-cbf6-4277-9372-e75e04f97000");
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create("rossini", ADMIN_PWD).self();
        assertEquals("rossini", self.getValue().getUsername());
    }

    @Test
    public void authenticateByPlainAttribute() {
        UserTO rossini = userService.read("rossini");
        assertNotNull(rossini);
        String userId = rossini.getPlainAttr("userId").get().getValues().get(0);
        assertNotNull(userId);

        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(userId, ADMIN_PWD).self();
        assertEquals(rossini.getUsername(), self.getValue().getUsername());
    }

    @Test
    public void updateWithoutApproval() {
        // 1. create user as admin
        UserTO created = createUser(UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username) - works
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(created.getKey());
        userPatch.setUsername(new StringReplacePatchItem.Builder().value(created.getUsername() + "XX").build());

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(userPatch).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(updated);
        assertEquals(FlowableDetector.isFlowableEnabledForUsers(syncopeService)
                ? "active" : "created", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
    }

    @Test
    public void updateWithApproval() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // 1. create user as admin
        UserTO created = createUser(UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username + memberships + resource) - works but needs approval
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(created.getKey());
        userPatch.setUsername(new StringReplacePatchItem.Builder().value(created.getUsername() + "XX").build());
        userPatch.getMemberships().add(new MembershipPatch.Builder().
                operation(PatchOperation.ADD_REPLACE).
                group("bf825fe1-7320-4a54-bd64-143b5c18ab97").
                build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());
        userPatch.setPassword(new PasswordPatch.Builder().
                value("newPassword123").onSyncope(false).resource(RESOURCE_NAME_TESTDB).build());

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(userPatch).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(updated);
        assertEquals("updateApproval", updated.getStatus());
        assertFalse(updated.getUsername().endsWith("XX"));
        assertTrue(updated.getMemberships().isEmpty());

        // no propagation happened
        assertTrue(updated.getResources().isEmpty());
        try {
            resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), updated.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. approve self-update as admin
        WorkflowFormTO form = userWorkflowService.getFormForUser(updated.getKey());
        form = userWorkflowService.claimForm(form.getTaskId());
        form.getProperty("approveUpdate").get().setValue(Boolean.TRUE.toString());
        updated = userWorkflowService.submitForm(form);
        assertNotNull(updated);
        assertEquals("active", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
        assertEquals(1, updated.getMemberships().size());

        // check that propagation also happened
        assertTrue(updated.getResources().contains(RESOURCE_NAME_TESTDB));
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), updated.getKey()));
    }

    @Test
    public void delete() {
        UserTO created = createUser(UserITCase.getUniqueSampleTO("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO deleted = authClient.getService(UserSelfService.class).delete().readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(deleted);
        assertEquals(FlowableDetector.isFlowableEnabledForUsers(syncopeService)
                ? "deleteApproval" : null, deleted.getStatus());
    }

    @Test
    public void issueSYNCOPE373() {
        UserTO userTO = adminClient.self().getValue();
        assertEquals(ADMIN_UNAME, userTO.getUsername());
    }

    @Test
    public void passwordReset() {
        // 0. ensure that password request DOES require security question
        configurationService.set(attrTO("passwordReset.securityQuestion", "true"));

        // 1. create an user with security question and answer
        UserTO user = UserITCase.getUniqueSampleTO("pwdReset@syncope.apache.org");
        user.setSecurityQuestion("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        user.setSecurityAnswer("Rossi");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(user);

        // verify propagation (including password) on external db
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String pwdOnResource = queryForObject(
                jdbcTemplate, 50, "SELECT password FROM test WHERE id=?", String.class, user.getUsername());
        assertTrue(StringUtils.isNotBlank(pwdOnResource));

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.self().getValue();
        assertNotNull(read);

        // 3. request password reset (as anonymous) providing the expected security answer
        SyncopeClient anonClient = clientFactory.create();
        try {
            anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "WRONG");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSecurityAnswer, e.getType());
        }
        anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "Rossi");

        // 4. get token (normally sent via e-mail, now reading as admin)
        String token = userService.read(read.getKey()).getToken();
        assertNotNull(token);

        // 5. confirm password reset
        try {
            anonClient.getService(UserSelfService.class).confirmPasswordReset("WRONG TOKEN", "newPassword");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword123");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword123");
        read = authClient.self().getValue();
        assertNotNull(read);
        assertNull(read.getToken());

        // 7. verify that password was changed on external resource
        String newPwdOnResource = queryForObject(
                jdbcTemplate, 50, "SELECT password FROM test WHERE id=?", String.class, user.getUsername());
        assertTrue(StringUtils.isNotBlank(newPwdOnResource));
        assertNotEquals(pwdOnResource, newPwdOnResource);
    }

    @Test
    public void passwordResetWithoutSecurityQuestion() {
        // 0. disable security question for password reset
        configurationService.set(attrTO("passwordReset.securityQuestion", "false"));

        // 1. create an user with security question and answer
        UserTO user = UserITCase.getUniqueSampleTO("pwdResetNoSecurityQuestion@syncope.apache.org");
        createUser(user);

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.self().getValue();
        assertNotNull(read);

        // 3. request password reset (as anonymous) with no security answer
        SyncopeClient anonClient = clientFactory.create();
        anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), null);

        // 4. get token (normally sent via e-mail, now reading as admin)
        String token = userService.read(read.getKey()).getToken();
        assertNotNull(token);

        // 5. confirm password reset
        try {
            anonClient.getService(UserSelfService.class).confirmPasswordReset("WRONG TOKEN", "newPassword");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword123");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword123");
        read = authClient.self().getValue();
        assertNotNull(read);
        assertNull(read.getToken());

        // 7. re-enable security question for password reset
        configurationService.set(attrTO("passwordReset.securityQuestion", "true"));
    }

    @Test
    public void mustChangePassword() {
        // PRE: reset vivaldi's password
        UserPatch userPatch = new UserPatch();
        userPatch.setKey("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        userPatch.setPassword(new PasswordPatch.Builder().value("password321").build());
        userService.update(userPatch);

        // 0. access as vivaldi -> succeed
        SyncopeClient vivaldiClient = clientFactory.create("vivaldi", "password321");
        Pair<Map<String, Set<String>>, UserTO> self = vivaldiClient.self();
        assertFalse(self.getRight().isMustChangePassword());

        // 1. update user vivaldi (3) requirig password update
        userPatch = new UserPatch();
        userPatch.setKey("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(true).build());
        UserTO vivaldi = updateUser(userPatch).getEntity();
        assertTrue(vivaldi.isMustChangePassword());

        // 2. attempt to access -> fail
        try {
            vivaldiClient.getService(ResourceService.class).list();
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
            assertEquals("Please change your password first", e.getMessage());
        }

        // 3. change password
        vivaldiClient.getService(UserSelfService.class).changePassword("password123");

        // 4. verify it worked
        self = clientFactory.create("vivaldi", "password123").self();
        assertFalse(self.getRight().isMustChangePassword());
    }

}
