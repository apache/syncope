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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserRequestITCase extends AbstractITCase {

    @BeforeAll
    public static void loadBpmnProcesses() throws IOException {
        assumeFalse(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        WebClient.client(bpmnProcessService).type(MediaType.APPLICATION_XML_TYPE);
        bpmnProcessService.set("directorGroupRequest",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/directorGroupRequest.bpmn20.xml")));
        bpmnProcessService.set("assignPrinterRequest",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/assignPrinterRequest.bpmn20.xml")));
        bpmnProcessService.set("verifyAddedVariables",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/verifyAddedVariables.bpmn20.xml")));
    }

    @BeforeEach
    public void check() {
        assumeFalse(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));
    }

    @Test
    public void twoLevelsApproval() {
        UserTO user = createUser(UserITCase.getUniqueSample("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequest req = userRequestService.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);
        assertEquals("directorGroupRequest", req.getBpmnProcess());
        assertNotNull(req.getExecutionId());
        assertEquals(req.getUsername(), user.getUsername());

        // check that user can see the ongoing request
        SyncopeClient client = clientFactory.create(user.getUsername(), "password123");
        PagedResult<UserRequest> requests = client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, requests.getTotalCount());
        assertEquals("directorGroupRequest", requests.getResult().get(0).getBpmnProcess());

        // 1st approval -> reject
        UserRequestForm form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.FALSE.toString());
        userRequestService.submitForm(form);

        // no more forms, group not assigned
        assertTrue(userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
        assertFalse(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = userRequestService.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // 1st approval -> accept
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // 2nd approval -> reject
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.FALSE.toString());
        user = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        // no more forms, group not assigned
        assertTrue(userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
        assertFalse(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = userRequestService.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // 1st approval -> accept
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // 2nd approval -> accept
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.TRUE.toString());
        user = userRequestService.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        // check that the director group was effectively assigned
        assertTrue(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
        assertTrue(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
    }

    @Test
    public void cancel() {
        PagedResult<UserRequestForm> forms =
                userRequestService.listForms(new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequest req = userRequestService.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // check that form was generated
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        assertEquals(1, userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().size());

        // cancel request
        userRequestService.cancelRequest(req.getExecutionId(), "nothing in particular");

        // check that form was removed
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
    }

    @Test
    public void userSelection() {
        PagedResult<UserRequestForm> forms =
                userRequestService.listForms(new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("userSelection@tirasa.net")).getEntity();
        assertNotNull(user);
        List<RelationshipTO> relationships = userService.read(user.getKey()).getRelationships();
        assertTrue(relationships.isEmpty());

        SyncopeClient client = clientFactory.create(user.getUsername(), "password123");

        // start request as user
        UserRequest req = client.getService(UserRequestService.class).startRequest("assignPrinterRequest", null, null);
        assertNotNull(req);

        // check (as admin) that a new form is available
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        // get (as user) the form, claim and submit
        PagedResult<UserRequestForm> userForms = client.getService(UserRequestService.class).
                listForms(new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, userForms.getTotalCount());

        UserRequestForm form = userForms.getResult().get(0);
        assertEquals("assignPrinterRequest", form.getBpmnProcess());
        form = client.getService(UserRequestService.class).claimForm(form.getTaskId());

        assertFalse(form.getProperty("printer").get().getDropdownValues().isEmpty());
        form.getProperty("printer").ifPresent(printer -> printer.setValue("8559d14d-58c2-46eb-a2d4-a7d35161e8f8"));

        assertFalse(form.getProperty("printMode").get().getEnumValues().isEmpty());
        form.getProperty("printMode").ifPresent(printMode -> printMode.setValue("color"));

        client.getService(UserRequestService.class).submitForm(form);

        userForms = client.getService(UserRequestService.class).listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(0, userForms.getTotalCount());

        // check that user can see the ongoing request
        PagedResult<UserRequest> requests = client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, requests.getTotalCount());
        assertEquals("assignPrinterRequest", requests.getResult().get(0).getBpmnProcess());

        // get (as admin) the new form, claim and submit
        form = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().get(0);
        assertEquals("assignPrinterRequest", form.getBpmnProcess());
        form = userRequestService.claimForm(form.getTaskId());

        assertEquals("8559d14d-58c2-46eb-a2d4-a7d35161e8f8", form.getProperty("printer").get().getValue());

        form.getProperty("approve").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // no more forms available
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());

        // check that relationship was made effective by approval
        relationships = userService.read(user.getKey()).getRelationships();
        assertFalse(relationships.isEmpty());
        assertTrue(relationships.stream().
                anyMatch(relationship -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(relationship.getOtherEndKey())));
    }

    @Test
    public void addVariablesToUserRequestAtStart() {
        PagedResult<UserRequestForm> forms =
                userRequestService.listForms(new UserRequestQuery.Builder().build());
        int preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("addVariables@tirasa.net")).getEntity();
        assertNotNull(user);

        SyncopeClient client = clientFactory.create(user.getUsername(), "password123");

        WorkflowTaskExecInput testInput = new WorkflowTaskExecInput();
        testInput.getVariables().put("providedVariable", "test");

        // start request as user
        UserRequest req = client.getService(UserRequestService.class).
                startRequest("verifyAddedVariables", null, testInput);
        assertNotNull(req);

        // check that a new form is available
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        // get the form and verify the property value
        PagedResult<UserRequestForm> userForms = userRequestService.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, userForms.getTotalCount());

        UserRequestForm form = userForms.getResult().get(0);
        form = userRequestService.claimForm(form.getTaskId());
        assertEquals(form.getProperty("providedVariable").get().getValue(), "test");

        // cancel request
        userRequestService.cancelRequest(req.getExecutionId(), "nothing in particular");

        // no more forms available
        forms = userRequestService.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
    }

    @Test
    public void invalid() throws IOException {
        WebClient.client(bpmnProcessService).type(MediaType.APPLICATION_XML_TYPE);
        try {
            bpmnProcessService.set("invalid",
                    IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/invalidRequest.bpmn20.xml")));
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Workflow, e.getType());
        }
    }
}
