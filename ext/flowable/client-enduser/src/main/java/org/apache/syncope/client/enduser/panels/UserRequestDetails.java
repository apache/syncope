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

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.rest.UserRequestRestClient;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.client.ui.commons.panels.SyncopeFormPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRequestDetails extends Panel {

    private static final long serialVersionUID = -2447602429647965090L;

    protected static final Logger LOG = LoggerFactory.getLogger(UserRequestDetails.class);

    protected static final String USER_REQUEST_ERROR = "user_request_error";

    @SpringBean
    protected UserRequestRestClient userRequestRestClient;

    public UserRequestDetails(
            final String id,
            final UserRequest userRequest,
            final WebMarkupContainer container,
            final NotificationPanel notificationPanel) {

        super(id);

        UserRequestForm formTO = userRequest.getHasForm()
                ? userRequestRestClient.getForm(
                        SyncopeEnduserSession.get().getSelfTO().getUsername(),
                        userRequest.getTaskId()).orElse(null)
                : null;

        if (formTO == null || formTO.getProperties() == null || formTO.getProperties().isEmpty()) {
            add(new Fragment("fragContainer", "formDetails", UserRequestDetails.this)
                    .add(new Label("executionId", userRequest.getExecutionId()))
                    .add(new Label("startTime", userRequest.getStartTime())));
        } else {
            Form<Void> form = new Form<>("userRequestWrapForm");

            form.add(new SyncopeFormPanel<>("userRequestFormPanel", formTO));

            form.add(new AjaxButton("submit") {

                private static final long serialVersionUID = 4284361595033427185L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    try {
                        userRequestRestClient.claimForm(formTO.getTaskId());
                        ProvisioningResult<UserTO> result = userRequestRestClient.submitForm(formTO);

                        if (result.getPropagationStatuses().stream().
                                anyMatch(p -> ExecStatus.FAILURE == p.getStatus()
                                || ExecStatus.NOT_ATTEMPTED == p.getStatus())) {

                            SyncopeEnduserSession.get().error(getString(USER_REQUEST_ERROR));
                            notificationPanel.refresh(target);
                        }

                        target.add(container);
                    } catch (SyncopeClientException sce) {
                        LOG.error("Unable to submit user request form for BPMN process [{}]",
                                formTO.getBpmnProcess(), sce);
                        SyncopeEnduserSession.get().error(getString(USER_REQUEST_ERROR));
                        notificationPanel.refresh(target);
                    }
                }

            }.setOutputMarkupId(true));

            add(new Fragment("fragContainer", "formProperties", UserRequestDetails.this).add(form));
        }

        add(new AjaxLink<Void>("delete") {

            private static final long serialVersionUID = 3669569969172391336L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userRequestRestClient.cancelRequest(userRequest.getExecutionId(), null);
                target.add(container);
            }
        });
    }
}
