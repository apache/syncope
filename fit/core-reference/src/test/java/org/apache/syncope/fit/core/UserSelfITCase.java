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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.apache.syncope.fit.FlowableDetector;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class UserSelfITCase extends AbstractITCase {

    @Test
    public void selfRegistrationAllowed() {
        assertTrue(adminClient.platform().isSelfRegAllowed());
    }

    @Test
    public void create() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // 1. self-registration as admin: failure
        try {
            userSelfService.create(UserITCase.getUniqueSample("anonymous@syncope.apache.org"));
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        // 2. self-registration as anonymous: works
        SyncopeClient anonClient = clientFactory.create();
        UserTO self = anonClient.getService(UserSelfService.class).
                create(UserITCase.getUniqueSample("anonymous@syncope.apache.org")).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(self);
        assertEquals("createApproval", self.getStatus());
    }

    @Test
    public void createAndApprove() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // 1. self-create user with membership: goes 'createApproval' with resources and membership but no propagation
        UserCR userCR = UserITCase.getUniqueSample("anonymous@syncope.apache.org");
        userCR.getMemberships().add(
                new MembershipTO.Builder("29f96485-729e-4d31-88a1-6fc60e4677f3").build());
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        SyncopeClient anonClient = clientFactory.create();
        UserTO userTO = anonClient.getService(UserSelfService.class).
                create(userCR).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());
        assertFalse(userTO.getMemberships().isEmpty());
        assertFalse(userTO.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 2. now approve and verify that propagation has happened
        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey()));
    }

    @Test
    public void createAndUnclaim() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // 1. self-create user with membership: goes 'createApproval' with resources and membership but no propagation
        UserCR userCR = UserITCase.getUniqueSample("anonymous@syncope.apache.org");
        userCR.getMemberships().add(
                new MembershipTO.Builder("29f96485-729e-4d31-88a1-6fc60e4677f3").build());
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        SyncopeClient anonClient = clientFactory.create();
        UserTO userTO = anonClient.getService(UserSelfService.class).
                create(userCR).
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

        // 2. unclaim and verify that propagation has NOT happened
        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        form = userRequestService.unclaimForm(form.getTaskId());
        assertNull(form.getAssignee());
        assertNotNull(userTO);
        assertNotEquals("active", userTO.getStatus());
        try {
            resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        // 3. approve and verify that propagation has happened
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey()));
    }

    @Test
    public void read() {
        UserTO user = createUser(UserITCase.getUniqueSample("selfread@syncope.apache.org")).getEntity();
        UserService us2 = clientFactory.create(user.getUsername(), "password123").getService(UserService.class);
        try {
            us2.read(user.getKey());
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                clientFactory.create(user.getUsername(), "password123").self();
        assertEquals(user.getUsername(), self.getRight().getUsername());
    }

    @Test
    public void authenticateByPlainAttribute() {
        UserTO rossini = userService.read("rossini");
        assertNotNull(rossini);
        String userId = rossini.getPlainAttr("userId").get().getValues().get(0);
        assertNotNull(userId);

        Triple<Map<String, Set<String>>, List<String>, UserTO> self = clientFactory.create(userId, ADMIN_PWD).self();
        assertEquals(rossini.getUsername(), self.getRight().getUsername());
    }

    @Test
    public void updateWithoutApproval() {
        // 1. create user as admin
        UserTO created = createUser(UserITCase.getUniqueSample("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username) - works
        UserUR userUR = new UserUR();
        userUR.setKey(created.getKey());
        userUR.setUsername(new StringReplacePatchItem.Builder().value(created.getUsername() + "XX").build());

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(userUR).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                }).getEntity();
        assertNotNull(updated);
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform())
                ? "active" : "created", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
    }

    @Test
    public void updateWithApproval() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // 1. create user as admin
        UserTO created = createUser(UserITCase.getUniqueSample("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username + memberships + resource) - works but needs approval
        UserUR userUR = new UserUR();
        userUR.setKey(created.getKey());
        userUR.setUsername(new StringReplacePatchItem.Builder().value(created.getUsername() + "XX").build());
        userUR.getMemberships().add(new MembershipUR.Builder("bf825fe1-7320-4a54-bd64-143b5c18ab97").
                operation(PatchOperation.ADD_REPLACE).
                build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());
        userUR.setPassword(new PasswordPatch.Builder().
                value("newPassword123").onSyncope(false).resource(RESOURCE_NAME_TESTDB).build());

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(userUR).
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
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. approve self-update as admin
        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(updated.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("approveUpdate").get().setValue(Boolean.TRUE.toString());
        updated = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
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
        UserTO created = createUser(UserITCase.getUniqueSample("anonymous@syncope.apache.org")).getEntity();
        assertNotNull(created);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO deleted = authClient.getService(UserSelfService.class).delete().readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(deleted);
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform())
                ? "deleteApproval" : null, deleted.getStatus());
    }

    @Test
    public void passwordReset() {
        // 0. ensure that password request DOES require security question
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "passwordReset.securityQuestion", true);

        // 1. create an user with security question and answer
        UserCR user = UserITCase.getUniqueSample("pwdReset@syncope.apache.org");
        user.setSecurityQuestion("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        user.setSecurityAnswer("Rossi");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(user);

        // verify propagation (including password) on external db
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String pwdOnResource = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT password FROM test WHERE id=?", String.class, user.getUsername());
        assertTrue(StringUtils.isNotBlank(pwdOnResource));

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.self().getRight();
        assertNotNull(read);

        // 3. request password reset (as anonymous) providing the expected security answer
        SyncopeClient anonClient = clientFactory.create();
        try {
            anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "WRONG");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSecurityAnswer, e.getType());
        }
        anonClient.getService(UserSelfService.class).requestPasswordReset(user.getUsername(), "Rossi");

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        // 4. get token (normally sent via e-mail, now reading as admin)
        String token = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                () -> userService.read(read.getKey()).getToken(),
                StringUtils::isNotBlank);

        // 5. confirm password reset
        try {
            anonClient.getService(UserSelfService.class).confirmPasswordReset("WRONG TOKEN", "newPassword");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword123");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword123");
        assertNull(authClient.self().getRight().getToken());

        // 7. verify that password was changed on external resource
        String newPwdOnResource = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT password FROM test WHERE id=?", String.class, user.getUsername());
        assertTrue(StringUtils.isNotBlank(newPwdOnResource));
        assertNotEquals(pwdOnResource, newPwdOnResource);
    }

    @Test
    public void passwordResetWithoutSecurityQuestion() {
        // 0. disable security question for password reset
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "passwordReset.securityQuestion", false);

        // 1. create an user with security question and answer
        UserCR user = UserITCase.getUniqueSample("pwdResetNoSecurityQuestion@syncope.apache.org");
        createUser(user);

        // 2. verify that new user is able to authenticate
        SyncopeClient authClient = clientFactory.create(user.getUsername(), "password123");
        UserTO read = authClient.self().getRight();
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
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("WRONG TOKEN"));
        }
        anonClient.getService(UserSelfService.class).confirmPasswordReset(token, "newPassword123");

        // 6. verify that password was reset and token removed
        authClient = clientFactory.create(user.getUsername(), "newPassword123");
        read = authClient.self().getRight();
        assertNotNull(read);
        assertNull(read.getToken());

        // 7. re-enable security question for password reset
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "passwordReset.securityQuestion", true);
    }

    @Test
    public void mustChangePassword() {
        // PRE: reset vivaldi's password
        UserUR userUR = new UserUR();
        userUR.setKey("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        userUR.setPassword(new PasswordPatch.Builder().value("password321").build());
        userService.update(userUR);

        // 0. access as vivaldi -> succeed
        SyncopeClient vivaldiClient = clientFactory.create("vivaldi", "password321");
        Response response = vivaldiClient.getService(AccessTokenService.class).refresh();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // 1. update user vivaldi requiring password update
        userUR = new UserUR();
        userUR.setKey("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        userUR.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(true).build());
        UserTO vivaldi = updateUser(userUR).getEntity();
        assertTrue(vivaldi.isMustChangePassword());

        // 2. attempt to access -> fail
        try {
            vivaldiClient.self();
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
            assertEquals("Please change your password first", e.getMessage());
        }

        // 3. change password
        vivaldiClient.getService(UserSelfService.class).mustChangePassword("password123");

        // 4. verify it worked
        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                clientFactory.create("vivaldi", "password123").self();
        assertFalse(self.getRight().isMustChangePassword());
    }

    @Test
    public void createWithReject() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        UserCR userCR = UserITCase.getUniqueSample("createWithReject@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        // User with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f are defined in workflow as subject to approval
        userCR.getMemberships().add(new MembershipTO.Builder("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals("0cbcabd2-4410-4b6b-8f05-a052b451d18f", userTO.getMemberships().get(0).getGroupKey());
        assertEquals("createApproval", userTO.getStatus());

        // 2. request if there is any pending task for user just created
        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        assertNotNull(form);
        assertNotNull(form.getUsername());
        assertEquals(userTO.getUsername(), form.getUsername());
        assertNotNull(form.getTaskId());
        assertNull(form.getAssignee());

        // 3. claim task as rossini, with role "User manager" granting entitlement to claim forms but not in
        // groupForWorkflowApproval, designated for approval in workflow definition: fail
        UserTO rossini = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        if (!rossini.getRoles().contains("User manager")) {
            UserUR userUR = new UserUR();
            userUR.setKey("1417acbe-cbf6-4277-9372-e75e04f97000");
            userUR.getRoles().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value("User manager").build());
            rossini = updateUser(userUR).getEntity();
        }
        assertTrue(rossini.getRoles().contains("User manager"));

        UserRequestService userService2 = clientFactory.create("rossini", ADMIN_PWD).
                getService(UserRequestService.class);
        try {
            userService2.claimForm(form.getTaskId());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Workflow, e.getType());
        }

        // 4. claim task from bellini, with role "User manager" and in groupForWorkflowApproval
        UserRequestService userService3 = clientFactory.create("bellini", ADMIN_PWD).
                getService(UserRequestService.class);
        assertEquals(1, userService3.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getTotalCount());
        form = userService3.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getAssignee());

        // 5. reject user
        form.getProperty("approveCreate").get().setValue(Boolean.FALSE.toString());
        form.getProperty("rejectReason").get().setValue("I don't like him.");
        userTO = userService3.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("rejected", userTO.getStatus());

        // 6. check that rejected user was not propagated to external resource (SYNCOPE-364)
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        Exception exception = null;
        try {
            jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", Integer.class, userTO.getUsername());
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void createWithApproval() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // read forms *before* any operation
        PagedResult<UserRequestForm> forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserCR userCR = UserITCase.getUniqueSample("createWithApproval@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        // User with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f are defined in workflow as subject to approval
        userCR.getMemberships().add(
                new MembershipTO.Builder("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user and verify that no propagation occurred)
        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        UserTO userTO = result.getEntity();
        assertEquals(1, userTO.getMemberships().size());
        assertEquals("0cbcabd2-4410-4b6b-8f05-a052b451d18f", userTO.getMemberships().get(0).getGroupKey());
        assertEquals("createApproval", userTO.getStatus());
        assertEquals(Set.of(RESOURCE_NAME_TESTDB), userTO.getResources());

        assertTrue(result.getPropagationStatuses().isEmpty());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        Exception exception = null;
        try {
            jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", Integer.class, userTO.getUsername());
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);

        // 2. request if there is any pending form for user just created
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        // 3. as admin, update user: still pending approval
        String updatedUsername = "changed-" + UUID.randomUUID().toString();
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setUsername(new StringReplacePatchItem.Builder().value(updatedUsername).build());
        updateUser(userUR);

        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getUserTO());
        assertEquals(updatedUsername, form.getUserTO().getUsername());
        assertNull(form.getUserUR());
        assertNull(form.getAssignee());

        // 4. claim task (as admin)
        form = userRequestService.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getUserTO());
        assertEquals(updatedUsername, form.getUserTO().getUsername());
        assertNull(form.getUserUR());
        assertNotNull(form.getAssignee());

        // 5. approve user (and verify that propagation occurred)
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals(updatedUsername, userTO.getUsername());
        assertEquals("active", userTO.getStatus());
        assertEquals(Set.of(RESOURCE_NAME_TESTDB), userTO.getResources());

        String username = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
        assertEquals(userTO.getUsername(), username);

        // 6. update user
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("anotherPassword123").build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void updateApproval() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // read forms *before* any operation
        PagedResult<UserRequestForm> forms = userRequestService.listForms(
                new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserTO created = createUser(UserITCase.getUniqueSample("updateApproval@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertEquals("/", created.getRealm());
        assertEquals(0, created.getMemberships().size());

        UserUR req = new UserUR();
        req.setKey(created.getKey());
        req.getMemberships().add(new MembershipUR.Builder("b1f7c12d-ec83-441f-a50e-1691daaedf3b").build());

        SyncopeClient client = clientFactory.create(created.getUsername(), "password123");
        Response response = client.getService(UserSelfService.class).update(req);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("updateApproval", userService.read(created.getKey()).getStatus());

        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(created.getKey()).build()).getResult().get(0);
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getAssignee());
        assertNotNull(form.getUserTO());
        assertNotNull(form.getUserUR());
        assertEquals(req, form.getUserUR());

        // as admin, update user: still pending approval
        UserUR adminPatch = new UserUR();
        adminPatch.setKey(created.getKey());
        adminPatch.setRealm(new StringReplacePatchItem.Builder().value("/even/two").build());

        UserTO updated = updateUser(adminPatch).getEntity();
        assertEquals("updateApproval", updated.getStatus());
        assertEquals("/even/two", updated.getRealm());
        assertEquals(0, updated.getMemberships().size());

        // the patch is not updated in the approval form
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(created.getKey()).build()).getResult().get(0);
        assertEquals(req, form.getUserUR());

        // approve the user
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("approveUpdate").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // verify that the approved user bears both original and further changes
        UserTO approved = userService.read(created.getKey());
        assertEquals("active", approved.getStatus());
        assertEquals("/even/two", approved.getRealm());
        assertEquals(1, approved.getMemberships().size());
        assertTrue(approved.getMembership("b1f7c12d-ec83-441f-a50e-1691daaedf3b").isPresent());
    }

    @Test
    public void availableTasks() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        UserTO user = createUser(UserITCase.getUniqueSample("availableTasks@apache.org")).getEntity();
        assertEquals("active", user.getStatus());

        List<WorkflowTask> tasks = userWorkflowTaskService.getAvailableTasks(user.getKey());
        assertNotNull(tasks);
        assertTrue(tasks.stream().anyMatch(task -> "update".equals(task.getName())));
        assertTrue(tasks.stream().anyMatch(task -> "suspend".equals(task.getName())));
        assertTrue(tasks.stream().anyMatch(task -> "delete".equals(task.getName())));
    }

    @Test
    public void issueSYNCOPE15() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        // read forms *before* any operation
        PagedResult<UserRequestForm> forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserCR userCR = UserITCase.getUniqueSample("issueSYNCOPE15@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getVirAttrs().clear();
        userCR.getMemberships().clear();

        // Users with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f are defined in workflow as subject to approval
        userCR.getMemberships().add(new MembershipTO.Builder("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user with group 9 (and verify that no propagation occurred)
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertNotEquals(0L, userTO.getKey());
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertEquals(userTO.getCreationDate(), userTO.getLastChangeDate());

        // 2. request if there is any pending form for user just created
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().get(0);
        assertNotNull(form);

        // 3. first claim by bellini ....
        UserRequestService userService3 = clientFactory.create("bellini", ADMIN_PWD).
                getService(UserRequestService.class);
        form = userService3.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getAssignee());

        // 4. second claim task by admin
        form = userRequestService.claimForm(form.getTaskId());
        assertNotNull(form);

        // 5. approve user
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());

        // 6. submit approve
        userRequestService.submitForm(form);
        assertEquals(preForms,
                userRequestService.listForms(new UserRequestQuery.Builder().build()).getTotalCount());
        assertTrue(userRequestService.listForms(
                new UserRequestQuery.Builder().user(userTO.getKey()).build()).getResult().isEmpty());

        // 7.check that no more forms are still to be processed
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());
    }

    @Test
    public void issueSYNCOPE373() {
        UserTO userTO = adminClient.self().getRight();
        assertEquals(ADMIN_UNAME, userTO.getUsername());
    }
}
