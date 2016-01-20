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
package org.apache.syncope.client.console.pages;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.panels.XMLWorkflowEditorModalPanel;
import java.io.File;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons.PrimaryModalButton;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

public class Workflow extends BasePage {

    private static final long serialVersionUID = -8781434495150074529L;

    private final WorkflowRestClient wfRestClient = new WorkflowRestClient();

    public Workflow(final PageParameters parameters) {
        super(parameters);

        WebMarkupContainer noActivitiEnabledForUsers = new WebMarkupContainer("noActivitiEnabledForUsers");
        noActivitiEnabledForUsers.setOutputMarkupPlaceholderTag(true);
        add(noActivitiEnabledForUsers);

        final WebMarkupContainer workflowDef = new WebMarkupContainer("workflowDefContainer");
        workflowDef.setOutputMarkupPlaceholderTag(true);

        if (wfRestClient.isActivitiEnabledForUsers()) {
            noActivitiEnabledForUsers.setVisible(false);
        } else {
            workflowDef.setVisible(false);
        }

        BookmarkablePageLink<Void> activitiModeler =
                new BookmarkablePageLink<>("activitiModeler", ActivitiModelerPopupPage.class);
        activitiModeler.setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(activitiModeler, ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        workflowDef.add(activitiModeler);
        // Check if Activiti Modeler directory is found
        boolean activitiModelerEnabled = false;
        try {
            File baseDir = new File(SyncopeConsoleApplication.get().getActivitiModelerDirectory());
            activitiModelerEnabled = baseDir.exists() && baseDir.canRead() && baseDir.isDirectory();
        } catch (Exception e) {
            LOG.error("Could not check for Activiti Modeler directory", e);
        }
        activitiModeler.setEnabled(activitiModelerEnabled);

        final BaseModal<String> xmlEditorModal = new BaseModal<>("xmlEditorModal");
        PrimaryModalButton xmlEditorSubmit = xmlEditorModal.addSumbitButton();
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditorSubmit, ENABLE, StandardEntitlement.WORKFLOW_DEF_UPDATE);
        xmlEditorModal.size(Modal.Size.Large);
        add(xmlEditorModal);

        AjaxLink<Void> xmlEditor = new AjaxLink<Void>("xmlEditor") {

            private static final long serialVersionUID = -1964967067512351526L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(xmlEditorModal.setContent(new XMLWorkflowEditorModalPanel(
                        xmlEditorModal, wfRestClient, Workflow.this.getPageReference())));

                xmlEditorModal.header(new ResourceModel("xmlEditorTitle"));

                xmlEditorModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditor, ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        workflowDef.add(xmlEditor);

        final Image workflowDefDiagram = new Image("workflowDefDiagram", new Model<IResource>()) {

            private static final long serialVersionUID = -8457850449086490660L;

            @Override
            protected IResource getImageResource() {
                return new DynamicImageResource() {

                    private static final long serialVersionUID = 923201517955737928L;

                    @Override
                    protected byte[] getImageData(final IResource.Attributes attributes) {
                        return wfRestClient.isActivitiEnabledForUsers()
                                ? wfRestClient.getDiagram()
                                : new byte[0];
                    }
                };
            }
        };
        workflowDefDiagram.setOutputMarkupId(true);
        workflowDef.add(workflowDefDiagram);

        xmlEditorModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(workflowDefDiagram);
            }
        });

        MetaDataRoleAuthorizationStrategy.authorize(workflowDef, ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        add(workflowDef);
    }

}
