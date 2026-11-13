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
package org.apache.syncope.client.enduser.panels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import org.apache.syncope.client.enduser.AbstractFlowableEnduserTest;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.common.lib.form.FormProperty;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRequestDetailsTest extends AbstractFlowableEnduserTest {

    private WebMarkupContainer container;

    private NotificationPanel notificationPanel;

    private UserRequest userRequest;

    @BeforeEach
    void setup() {
        container = new WebMarkupContainer("container");
        notificationPanel = new NotificationPanel(Constants.FEEDBACK);

        userRequest = new UserRequest();
        userRequest.setExecutionId("execution");
        userRequest.setActivityId("activity");
        userRequest.setStartTime(Date.from(OffsetDateTime.parse("2024-01-01T10:15:30Z").toInstant()));
        userRequest.setTaskId("task");
    }

    private UserRequestDetails details() {
        return new UserRequestDetails("details", userRequest, container, notificationPanel);
    }

    private UserRequestForm form() {
        FormProperty property = new FormProperty();
        property.setId("property");
        property.setName("Property");
        property.setType(FormPropertyType.String);
        property.setReadable(true);
        property.setWritable(true);

        UserRequestForm form = new UserRequestForm();
        form.setTaskId("task");
        form.setBpmnProcess("bpmnProcess");
        form.setUserTO(new UserTO());
        form.getProperties().add(property);
        return form;
    }

    @Test
    void requestDetailsShownWithoutForm() {
        userRequest.setHasForm(false);

        TESTER.startComponentInPage(details());

        TESTER.assertLabel("details:fragContainer:executionId", "execution");
        TESTER.assertLabel("details:fragContainer:activityId", "activity");
    }

    @Test
    void formShownAndSubmitted() {
        userRequest.setHasForm(true);
        UserRequestForm form = form();
        when(userRequestRestClient.getForm("task")).thenReturn(Optional.of(form));
        when(userRequestRestClient.submitForm(any(UserRequestForm.class))).thenReturn(new ProvisioningResult<>());

        TESTER.startComponentInPage(details());

        TESTER.assertComponent("details:fragContainer:userRequestWrapForm", Form.class);

        FormTester formTester = TESTER.newFormTester("details:fragContainer:userRequestWrapForm");
        formTester.submit("submit");

        verify(userRequestRestClient).claimForm("task");
        verify(userRequestRestClient).submitForm(form);
    }

    @Test
    void requestCancelled() {
        userRequest.setHasForm(false);

        TESTER.startComponentInPage(details());
        TESTER.clickLink("details:delete");

        verify(userRequestRestClient).cancelRequest("execution", null);
    }

    @Test
    void submissionWithFailedPropagationIsNotified() {
        userRequest.setHasForm(true);
        UserRequestForm form = form();
        when(userRequestRestClient.getForm("task")).thenReturn(Optional.of(form));

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        PropagationStatus status = new PropagationStatus();
        status.setStatus(ExecStatus.FAILURE);
        result.getPropagationStatuses().add(status);
        when(userRequestRestClient.submitForm(any(UserRequestForm.class))).thenReturn(result);

        TESTER.startComponentInPage(details());
        TESTER.newFormTester("details:fragContainer:userRequestWrapForm").submit("submit");

        assertFalse(TESTER.getSession().getFeedbackMessages().isEmpty());
    }
}
