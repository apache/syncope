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
package org.apache.syncope.client.console.approvals;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.syncope.client.console.panels.SubmitableModalPanel;
import org.apache.syncope.client.console.panels.WizardModalPanel;

public class ApprovalModal extends Panel implements SubmitableModalPanel, WizardModalPanel<WorkflowFormTO> {

    private static final long serialVersionUID = -8847854414429745216L;

    private final UserWorkflowRestClient restClient = new UserWorkflowRestClient();

    private final BaseModal<?> modal;

    private final WorkflowFormTO formTO;

    private final PageReference pageRef;

    public ApprovalModal(final BaseModal<?> modal, final PageReference pageRef, final WorkflowFormTO formTO) {
        super(BaseModal.CONTENT_ID);
        this.modal = modal;
        this.formTO = formTO;
        this.pageRef = pageRef;

        MultilevelPanel mlp = new MultilevelPanel("approval");
        mlp.setFirstLevel(new Approval(pageRef, formTO) {

            private static final long serialVersionUID = -2195387360323687302L;

            @Override
            protected void viewDetails(final AjaxRequestTarget target) {
                mlp.next(getString("approval.details"), new ApprovalDetails(pageRef, formTO), target);
            }
        });
        add(mlp);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        this.restClient.submitForm(formTO);
        this.modal.show(false);
        this.modal.close(target);
        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
    }

    @Override
    public void onError(final AjaxRequestTarget target) {
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    public WorkflowFormTO getItem() {
        return this.formTO;
    }
}
