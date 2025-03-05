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

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserRequestITCase extends AbstractITCase {

    @BeforeAll
    public static void loadBpmnProcesses() throws IOException {
        assumeFalse(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(IS_FLOWABLE_ENABLED);

        WebClient.client(BPMN_PROCESS_SERVICE).type(MediaType.APPLICATION_XML_TYPE);
        BPMN_PROCESS_SERVICE.set("directorGroupRequest",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/directorGroupRequest.bpmn20.xml")));
        BPMN_PROCESS_SERVICE.set("assignPrinterRequest",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/assignPrinterRequest.bpmn20.xml")));
        BPMN_PROCESS_SERVICE.set("verifyAddedVariables",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/verifyAddedVariables.bpmn20.xml")));

        WebClient.getConfig(WebClient.client(USER_REQUEST_SERVICE)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.FALSE);
    }

    @AfterAll
    public static void reset() {
        WebClient.getConfig(WebClient.client(USER_REQUEST_SERVICE)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
    }

    @BeforeEach
    public void check() {
        assumeFalse(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(IS_FLOWABLE_ENABLED);
    }

    @Test
    public void twoLevelsApproval() {
        UserTO user = createUser(UserITCase.getUniqueSample("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequest req = USER_REQUEST_SERVICE.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);
        assertEquals("directorGroupRequest", req.getBpmnProcess());
        assertNotNull(req.getExecutionId());
        assertEquals(req.getUsername(), user.getUsername());

        // check that user can see the ongoing request
        SyncopeClient client = CLIENT_FACTORY.create(user.getUsername(), "password123");
        PagedResult<UserRequest> requests = client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, requests.getTotalCount());
        assertEquals("directorGroupRequest", requests.getResult().getFirst().getBpmnProcess());

        // 1st approval -> reject
        UserRequestForm form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.FALSE.toString());
        USER_REQUEST_SERVICE.submitForm(form);

        // no more forms, group not assigned
        assertTrue(USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
        assertFalse(USER_SERVICE.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = USER_REQUEST_SERVICE.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // 1st approval -> accept
        form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        USER_REQUEST_SERVICE.submitForm(form);

        // 2nd approval -> reject
        form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.FALSE.toString());
        user = USER_REQUEST_SERVICE.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        // no more forms, group not assigned
        assertTrue(USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
        assertFalse(USER_SERVICE.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = USER_REQUEST_SERVICE.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // 1st approval -> accept
        form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        USER_REQUEST_SERVICE.submitForm(form);

        // 2nd approval -> accept
        form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.TRUE.toString());
        user = USER_REQUEST_SERVICE.submitForm(form).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        // check that the director group was effectively assigned
        assertTrue(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
        assertTrue(USER_SERVICE.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
    }

    @Test
    public void cancel() {
        PagedResult<UserRequestForm> forms =
                USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        long preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequest req = USER_REQUEST_SERVICE.startRequest("directorGroupRequest", user.getKey(), null);
        assertNotNull(req);

        // check that form was generated
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        assertEquals(1, USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().size());

        // cancel request
        USER_REQUEST_SERVICE.cancelRequest(req.getExecutionId(), "nothing in particular");

        // check that form was removed
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
    }

    @Test
    public void userSelection() {
        PagedResult<UserRequestForm> forms =
                USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        long preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("userSelection@tirasa.net")).getEntity();
        assertNotNull(user);
        List<RelationshipTO> relationships = USER_SERVICE.read(user.getKey()).getRelationships();
        assertTrue(relationships.isEmpty());

        SyncopeClient client = CLIENT_FACTORY.create(user.getUsername(), "password123");

        // start request as user
        UserRequestService service = client.getService(UserRequestService.class);
        WebClient.getConfig(WebClient.client(service)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.FALSE);

        UserRequest req = service.startRequest("assignPrinterRequest", null, null);
        assertNotNull(req);
        WebClient.getConfig(WebClient.client(service)).
                getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);

        // check (as admin) that a new form is available
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        // get (as user) the form, claim and submit
        PagedResult<UserRequestForm> userForms = service.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, userForms.getTotalCount());

        UserRequestForm form = userForms.getResult().getFirst();
        assertEquals("assignPrinterRequest", form.getBpmnProcess());
        form = service.claimForm(form.getTaskId());

        assertFalse(form.getProperty("printer").get().getDropdownValues().isEmpty());
        form.getProperty("printer").ifPresent(printer -> printer.setValue("8559d14d-58c2-46eb-a2d4-a7d35161e8f8"));

        assertFalse(form.getProperty("printMode").get().getEnumValues().isEmpty());
        form.getProperty("printMode").ifPresent(printMode -> printMode.setValue("color"));

        service.submitForm(form);

        userForms = service.listForms(new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(0, userForms.getTotalCount());

        // check that user can see the ongoing request
        PagedResult<UserRequest> requests = service.listRequests(
                new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, requests.getTotalCount());
        assertEquals("assignPrinterRequest", requests.getResult().getFirst().getBpmnProcess());

        // get (as admin) the new form, claim and submit
        form = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().getFirst();
        assertEquals("assignPrinterRequest", form.getBpmnProcess());
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());

        assertEquals("8559d14d-58c2-46eb-a2d4-a7d35161e8f8", form.getProperty("printer").get().getValue());

        form.getProperty("approve").get().setValue(Boolean.TRUE.toString());
        USER_REQUEST_SERVICE.submitForm(form);

        // no more forms available
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(service.listRequests(
                new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());

        // check that relationship was made effective by approval
        relationships = USER_SERVICE.read(user.getKey()).getRelationships();
        assertFalse(relationships.isEmpty());
        assertTrue(relationships.stream().
                anyMatch(relationship -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(relationship.getOtherEndKey())));
    }

    @Test
    public void addVariablesToUserRequestAtStart() {
        PagedResult<UserRequestForm> forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        long preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSample("addVariables@tirasa.net")).getEntity();
        assertNotNull(user);

        SyncopeClient client = CLIENT_FACTORY.create(user.getUsername(), "password123");

        WorkflowTaskExecInput testInput = new WorkflowTaskExecInput();
        testInput.getVariables().put("providedVariable", "test");

        // start request as user
        UserRequest req = client.getService(UserRequestService.class).
                startRequest("verifyAddedVariables", null, testInput);
        assertNotNull(req);

        // check that a new form is available
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms + 1, forms.getTotalCount());

        // get the form and verify the property value
        PagedResult<UserRequestForm> userForms = USER_REQUEST_SERVICE.listForms(
                new UserRequestQuery.Builder().user(user.getKey()).build());
        assertEquals(1, userForms.getTotalCount());

        UserRequestForm form = userForms.getResult().getFirst();
        form = USER_REQUEST_SERVICE.claimForm(form.getTaskId());
        assertEquals(form.getProperty("providedVariable").get().getValue(), "test");

        // cancel request
        USER_REQUEST_SERVICE.cancelRequest(req.getExecutionId(), "nothing in particular");

        // no more forms available
        forms = USER_REQUEST_SERVICE.listForms(new UserRequestQuery.Builder().build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(client.getService(UserRequestService.class).
                listRequests(new UserRequestQuery.Builder().user(user.getKey()).build()).getResult().isEmpty());
    }

    @Test
    public void invalid() throws IOException {
        WebClient.client(BPMN_PROCESS_SERVICE).type(MediaType.APPLICATION_XML_TYPE);
        try {
            BPMN_PROCESS_SERVICE.set("invalid",
                    IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/invalidRequest.bpmn20.xml")));
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Workflow, e.getType());
        }
    }
}
