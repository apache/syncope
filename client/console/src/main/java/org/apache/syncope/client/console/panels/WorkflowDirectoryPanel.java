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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.ModelerPopupPage;
import org.apache.syncope.client.console.panels.WorkflowDirectoryPanel.WorkflowDefinitionDataProvider;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ImageModalPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IOUtils;

public class WorkflowDirectoryPanel extends DirectoryPanel<
        WorkflowDefinitionTO, WorkflowDefinitionTO, WorkflowDefinitionDataProvider, WorkflowRestClient> {

    private static final long serialVersionUID = 2705668831139984998L;

    private final BaseModal<String> utility;

    private String modelerCtx;

    protected WorkflowDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);

        this.addNewItemPanelBuilder(new AjaxWizardBuilder<WorkflowDefinitionTO>(new WorkflowDefinitionTO(), pageRef) {

            private static final long serialVersionUID = 1633859795677053912L;

            @Override
            protected WizardModel buildModelSteps(
                    final WorkflowDefinitionTO modelObject, final WizardModel wizardModel) {

                return wizardModel;
            }
        }, false);
        final NewWorkflowProcess newWorkflowProcess = new NewWorkflowProcess("newWorkflowProcess", container, pageRef);
        addInnerObject(newWorkflowProcess);
        AjaxLink<Void> newWorkflowProcessLink = new AjaxLink<Void>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                newWorkflowProcess.toggle(target, true);
            }
        };
        ((WebMarkupContainer) get("container:content")).addOrReplace(newWorkflowProcessLink);

        setShowResultPage(true);

        modal.size(Modal.Size.Large);

        utility = new BaseModal<>("outer");
        addOuterObject(utility);
        utility.size(Modal.Size.Large);
        AjaxSubmitLink xmlEditorSubmit = utility.addSubmitButton();
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditorSubmit, RENDER, StandardEntitlement.WORKFLOW_DEF_SET);
        utility.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                utility.show(false);
                utility.close(target);
            }
        });
        initResultTable();

        // Check if Activiti or Flowable Modeler directory is found
        modelerCtx = null;
        try {
            if (SyncopeConsoleApplication.get().getActivitiModelerDirectory() != null) {
                File baseDir = new File(SyncopeConsoleApplication.get().getActivitiModelerDirectory());
                if (baseDir.exists() && baseDir.canRead() && baseDir.isDirectory()) {
                    modelerCtx = Constants.ACTIVITI_MODELER_CONTEXT;
                }
            }
        } catch (Exception e) {
            LOG.error("Could not check for Modeler directory", e);
        }
    }

    @Override
    protected WorkflowDefinitionDataProvider dataProvider() {
        return new WorkflowDefinitionDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_WORKFLOW_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<WorkflowDefinitionTO, String>> getColumns() {
        List<IColumn<WorkflowDefinitionTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<WorkflowDefinitionTO>(new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<WorkflowDefinitionTO, String>(new ResourceModel("name"), "name", "name"));
        columns.add(new BooleanPropertyColumn<WorkflowDefinitionTO>(new ResourceModel("main"), null, "main"));

        columns.add(new ActionColumn<WorkflowDefinitionTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 906457126287899096L;

            @Override
            public ActionLinksPanel<?> getActions(final String componentId, final IModel<WorkflowDefinitionTO> model) {
                final ActionLinksPanel.Builder<WorkflowDefinitionTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<WorkflowDefinitionTO>() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowDefinitionTO ignore) {
                        modal.header(Model.of(model.getObject().getKey()));
                        modal.setContent(new ImageModalPanel<>(
                                modal, restClient.getDiagram(model.getObject().getKey()), pageRef));
                        modal.show(target);
                        target.add(modal);
                    }
                }, ActionLink.ActionType.VIEW, StandardEntitlement.WORKFLOW_DEF_GET);

                panel.add(new ActionLink<WorkflowDefinitionTO>() {

                    private static final long serialVersionUID = -184018732772021627L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowDefinitionTO ignore) {
                        final IModel<String> wfDefinition = new Model<>();
                        try {
                            wfDefinition.setObject(IOUtils.toString(restClient.getDefinition(
                                    MediaType.APPLICATION_XML_TYPE, model.getObject().getKey())));
                        } catch (IOException e) {
                            LOG.error("Could not get workflow definition", e);
                        }

                        utility.header(Model.of(model.getObject().getKey()));
                        utility.setContent(new XMLEditorPanel(utility, wfDefinition, false, pageRef) {

                            private static final long serialVersionUID = -7688359318035249200L;

                            @Override
                            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                                if (StringUtils.isNotBlank(wfDefinition.getObject())) {
                                    try {
                                        restClient.setDefinition(MediaType.APPLICATION_XML_TYPE,
                                                model.getObject().getKey(), wfDefinition.getObject());
                                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));

                                        utility.show(false);
                                        utility.close(target);
                                    } catch (SyncopeClientException e) {
                                        SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                                ? e.getClass().getName() : e.getMessage());
                                    }
                                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                                }
                            }
                        });
                        utility.show(target);
                        target.add(utility);
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.WORKFLOW_DEF_SET);

                panel.add(new ActionLink<WorkflowDefinitionTO>() {

                    private static final long serialVersionUID = -184018732772021627L;

                    @Override
                    public Class<? extends Page> getPageClass() {
                        return ModelerPopupPage.class;
                    }

                    @Override
                    public PageParameters getPageParameters() {
                        PageParameters parameters = new PageParameters();
                        if (modelerCtx != null) {
                            parameters.add(Constants.MODELER_CONTEXT, modelerCtx);
                        }
                        parameters.add(Constants.MODEL_ID_PARAM, model.getObject().getModelId());

                        return parameters;
                    }

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowDefinitionTO ignore) {
                        // do nothing
                    }
                }, ActionLink.ActionType.WORKFLOW_MODELER, StandardEntitlement.WORKFLOW_DEF_SET, modelerCtx != null);

                panel.add(new ActionLink<WorkflowDefinitionTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowDefinitionTO ignore) {
                        try {
                            restClient.deleteDefinition(model.getObject().getKey());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting workflow definition {}", model.getObject().getName(), e);
                            SyncopeConsoleSession.get().error(
                                    StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.WORKFLOW_DEF_DELETE, !model.getObject().isMain());

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<WorkflowDefinitionTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<WorkflowDefinitionTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<WorkflowDefinitionTO>() {

                    private static final long serialVersionUID = -184018732772021627L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowDefinitionTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.WORKFLOW_DEF_LIST).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.emptyList();
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<WorkflowDefinitionTO, WorkflowDefinitionTO, WorkflowRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final PageReference pageRef) {
            super(new WorkflowRestClient(), pageRef);
        }

        @Override
        protected WizardMgtPanel<WorkflowDefinitionTO> newInstance(final String id, final boolean wizardInModal) {
            return new WorkflowDirectoryPanel(id, this);
        }
    }

    protected class WorkflowDefinitionDataProvider extends DirectoryDataProvider<WorkflowDefinitionTO> {

        private static final long serialVersionUID = 1764153405387687592L;

        private final SortableDataProviderComparator<WorkflowDefinitionTO> comparator;

        private final WorkflowRestClient restClient = new WorkflowRestClient();

        public WorkflowDefinitionDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
            setSort("main", SortOrder.DESCENDING);
        }

        @Override
        public Iterator<WorkflowDefinitionTO> iterator(final long first, final long count) {
            List<WorkflowDefinitionTO> result = restClient.getDefinitions();
            Collections.sort(result, comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getDefinitions().size();
        }

        @Override
        public IModel<WorkflowDefinitionTO> model(final WorkflowDefinitionTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
