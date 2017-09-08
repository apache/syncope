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

import org.apache.syncope.fit.FlowableDetector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserWorkflowService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class UserWorkflowITCase extends AbstractITCase {

    @Autowired
    private DataSource testDataSource;

    @Test
    public void createWithReject() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        UserTO userTO = UserITCase.getUniqueSampleTO("createWithReject@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        // User with group 9 are defined in workflow as subject to approval
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user with group 9
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals("0cbcabd2-4410-4b6b-8f05-a052b451d18f", userTO.getMemberships().get(0).getRightKey());
        assertEquals("createApproval", userTO.getStatus());

        // 2. request if there is any pending task for user just created
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        assertNotNull(form);
        assertNotNull(form.getUsername());
        assertEquals(userTO.getUsername(), form.getUsername());
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());

        // 3. claim task as rossini, with role "User manager" granting entitlement to claim forms but not in group 7,
        // designated for approval in workflow definition: fail
        UserTO rossini = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        if (!rossini.getRoles().contains("User manager")) {
            UserPatch userPatch = new UserPatch();
            userPatch.setKey("1417acbe-cbf6-4277-9372-e75e04f97000");
            userPatch.getRoles().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value("User manager").build());
            rossini = updateUser(userPatch).getEntity();
        }
        assertTrue(rossini.getRoles().contains("User manager"));

        UserWorkflowService userService2 = clientFactory.create("rossini", ADMIN_PWD).
                getService(UserWorkflowService.class);
        try {
            userService2.claimForm(form.getTaskId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Workflow, e.getType());
        }

        // 4. claim task from bellini, with role "User manager" and in group 7
        UserWorkflowService userService3 = clientFactory.create("bellini", ADMIN_PWD).
                getService(UserWorkflowService.class);
        form = userService3.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 5. reject user
        form.getProperty("approveCreate").get().setValue(Boolean.FALSE.toString());
        form.getProperty("rejectReason").get().setValue("I don't like him.");
        userTO = userService3.submitForm(form);
        assertNotNull(userTO);
        assertEquals("rejected", userTO.getStatus());

        // 6. check that rejected user was not propagated to external resource (SYNCOPE-364)
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        Exception exception = null;
        try {
            jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?",
                    new String[] { userTO.getUsername() }, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void createWithApproval() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // read forms *before* any operation
        List<WorkflowFormTO> forms = userWorkflowService.getForms();
        assertNotNull(forms);
        int preForms = forms.size();

        UserTO userTO = UserITCase.getUniqueSampleTO("createWithApproval@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        // User with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f are defined in workflow as subject to approval
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user and verify that no propagation occurred)
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(1, userTO.getMemberships().size());
        assertEquals("0cbcabd2-4410-4b6b-8f05-a052b451d18f", userTO.getMemberships().get(0).getRightKey());
        assertEquals("createApproval", userTO.getStatus());
        assertEquals(Collections.singleton(RESOURCE_NAME_TESTDB), userTO.getResources());

        assertTrue(result.getPropagationStatuses().isEmpty());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        Exception exception = null;
        try {
            jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?",
                    new String[] { userTO.getUsername() }, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);

        // 2. request if there is any pending form for user just created
        forms = userWorkflowService.getForms();
        assertNotNull(forms);
        assertEquals(preForms + 1, forms.size());

        // 3. as admin, request for changes: still pending approval
        String updatedUsername = "changed-" + UUID.randomUUID().toString();
        userTO.setUsername(updatedUsername);
        userWorkflowService.executeTask("default", userTO);

        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getUserTO());
        assertEquals(updatedUsername, form.getUserTO().getUsername());
        assertNull(form.getUserPatch());
        assertNull(form.getOwner());

        // 4. claim task (as admin)
        form = userWorkflowService.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getUserTO());
        assertEquals(updatedUsername, form.getUserTO().getUsername());
        assertNull(form.getUserPatch());
        assertNotNull(form.getOwner());

        // 5. approve user (and verify that propagation occurred)
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals(updatedUsername, userTO.getUsername());
        assertEquals("active", userTO.getStatus());
        assertEquals(Collections.singleton(RESOURCE_NAME_TESTDB), userTO.getResources());

        String username = queryForObject(
                jdbcTemplate, 50, "SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
        assertEquals(userTO.getUsername(), username);

        // 6. update user
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("anotherPassword123").build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void updateApproval() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // read forms *before* any operation
        List<WorkflowFormTO> forms = userWorkflowService.getForms();
        assertNotNull(forms);
        int preForms = forms.size();

        UserTO created = createUser(UserITCase.getUniqueSampleTO("updateApproval@syncope.apache.org")).getEntity();
        assertNotNull(created);
        assertEquals("/", created.getRealm());
        assertEquals(0, created.getMemberships().size());

        UserPatch patch = new UserPatch();
        patch.setKey(created.getKey());
        patch.getMemberships().add(new MembershipPatch.Builder().group("b1f7c12d-ec83-441f-a50e-1691daaedf3b").build());

        SyncopeClient client = clientFactory.create(created.getUsername(), "password123");
        Response response = client.getService(UserSelfService.class).update(patch);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("updateApproval", userService.read(created.getKey()).getStatus());

        forms = userWorkflowService.getForms();
        assertNotNull(forms);
        assertEquals(preForms + 1, forms.size());

        WorkflowFormTO form = userWorkflowService.getFormForUser(created.getKey());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());
        assertNotNull(form.getUserTO());
        assertNotNull(form.getUserPatch());
        assertEquals(patch, form.getUserPatch());

        // as admin, request for more changes: still pending approval
        patch.setRealm(new StringReplacePatchItem.Builder().value("/even/two").build());
        response = userService.update(patch);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("updateApproval", userService.read(created.getKey()).getStatus());

        // the patch is updated in the approval form
        form = userWorkflowService.getFormForUser(created.getKey());
        assertEquals(patch, form.getUserPatch());

        // approve the user
        form = userWorkflowService.claimForm(form.getTaskId());
        form.getProperty("approveUpdate").get().setValue(Boolean.TRUE.toString());
        userWorkflowService.submitForm(form);

        // verify that the approved user bears both original and further changes
        UserTO approved = userService.read(created.getKey());
        assertNotNull(approved);
        assertEquals("/even/two", approved.getRealm());
        assertEquals(1, approved.getMemberships().size());
        assertNotNull(approved.getMembership("b1f7c12d-ec83-441f-a50e-1691daaedf3b").get());
    }

    @Test
    public void issueSYNCOPE15() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        // read forms *before* any operation
        List<WorkflowFormTO> forms = userWorkflowService.getForms();
        assertNotNull(forms);
        int preForms = forms.size();

        UserTO userTO = UserITCase.getUniqueSampleTO("issueSYNCOPE15@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().clear();
        userTO.getMemberships().clear();

        // Users with group 0cbcabd2-4410-4b6b-8f05-a052b451d18f are defined in workflow as subject to approval
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("0cbcabd2-4410-4b6b-8f05-a052b451d18f").build());

        // 1. create user with group 9 (and verify that no propagation occurred)
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertNotEquals(0L, userTO.getKey());
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertEquals(userTO.getCreationDate(), userTO.getLastChangeDate());

        // 2. request if there is any pending form for user just created
        forms = userWorkflowService.getForms();
        assertEquals(preForms + 1, forms.size());

        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        assertNotNull(form);

        // 3. first claim by bellini ....
        UserWorkflowService userService3 = clientFactory.create("bellini", ADMIN_PWD).
                getService(UserWorkflowService.class);
        form = userService3.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 4. second claim task by admin
        form = userWorkflowService.claimForm(form.getTaskId());
        assertNotNull(form);

        // 5. approve user
        form.getProperty("approveCreate").get().setValue(Boolean.TRUE.toString());

        // 6. submit approve
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals(preForms, userWorkflowService.getForms().size());
        assertNull(userWorkflowService.getFormForUser(userTO.getKey()));

        // 7.check that no more forms are still to be processed
        forms = userWorkflowService.getForms();
        assertEquals(preForms, forms.size());
    }

}
