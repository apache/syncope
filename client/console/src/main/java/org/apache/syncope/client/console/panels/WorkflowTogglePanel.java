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
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.File;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.ActivitiModelerPopupPage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.io.IOUtils;

public class WorkflowTogglePanel extends TogglePanel<String> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final WebMarkupContainer container;

    protected final BaseModal<String> modal;

    public WorkflowTogglePanel(final String id, final PageReference pageRef, final Image workflowDefDiagram) {
        super(id, pageRef);
        modal = new BaseModal<>("outer");
        addOuterObject(modal);
        modal.size(Modal.Size.Large);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        BookmarkablePageLink<Void> activitiModeler = new BookmarkablePageLink<>("activitiModeler",
                ActivitiModelerPopupPage.class);
        activitiModeler.setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(activitiModeler, ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        container.add(activitiModeler);
        // Check if Activiti Modeler directory is found
        boolean activitiModelerEnabled = false;
        try {
            File baseDir = new File(SyncopeConsoleApplication.get().getActivitiModelerDirectory());
            activitiModelerEnabled = baseDir.exists() && baseDir.canRead() && baseDir.isDirectory();
        } catch (Exception e) {
            LOG.error("Could not check for Activiti Modeler directory", e);
        }
        activitiModeler.setEnabled(activitiModelerEnabled);

        AjaxSubmitLink xmlEditorSubmit = modal.addSubmitButton();
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditorSubmit, RENDER, StandardEntitlement.WORKFLOW_DEF_UPDATE);
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
                modal.close(target);
                target.add(workflowDefDiagram);
            }
        });

        AjaxLink<Void> xmlEditor = new AjaxLink<Void>("xmlEditor") {

            private static final long serialVersionUID = -1964967067512351526L;

            private final WorkflowRestClient restClient = new WorkflowRestClient();

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IModel<String> wfDefinition = new Model<>();
                try {
                    wfDefinition.setObject(IOUtils.toString(restClient.getDefinition(MediaType.APPLICATION_XML_TYPE)));
                } catch (IOException e) {
                    LOG.error("Could not get workflow definition", e);
                }

                target.add(modal.setContent(new XMLEditorPanel(modal, wfDefinition, false, pageRef) {

                    private static final long serialVersionUID = 5488080606102212554L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        if (StringUtils.isNotBlank(wfDefinition.getObject())) {
                            try {
                                restClient.updateDefinition(MediaType.APPLICATION_XML_TYPE, wfDefinition.getObject());
                                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));

                                modal.show(false);
                                modal.close(target);
                            } catch (SyncopeClientException e) {
                                SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                        getName() : e.
                                                getMessage());
                            }
                            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    }
                }));

                modal.header(new ResourceModel("xmlEditorTitle"));
                modal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditor, RENDER, StandardEntitlement.WORKFLOW_DEF_READ);
        container.add(xmlEditor);
    }

}
