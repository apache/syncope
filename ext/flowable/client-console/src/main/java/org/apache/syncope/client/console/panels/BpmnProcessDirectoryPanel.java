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
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.ModelerPopupPage;
import org.apache.syncope.client.console.panels.BpmnProcessDirectoryPanel.BpmProcessDataProvider;
import org.apache.syncope.client.console.rest.BpmnProcessRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ImageModalPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IOUtils;

public class BpmnProcessDirectoryPanel extends DirectoryPanel<
        BpmnProcess, BpmnProcess, BpmProcessDataProvider, BpmnProcessRestClient> {

    private static final long serialVersionUID = 2705668831139984998L;

    private static final String PREF_WORKFLOW_PAGINATOR_ROWS = "workflow.paginator.rows";

    private static final String FLOWABLE_MODELER_CTX = "flowable-modeler";

    private final BaseModal<String> utility;

    protected BpmnProcessDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);

        this.addNewItemPanelBuilder(new AjaxWizardBuilder<BpmnProcess>(new BpmnProcess(), pageRef) {

            private static final long serialVersionUID = 1633859795677053912L;

            @Override
            protected WizardModel buildModelSteps(final BpmnProcess modelObject, final WizardModel wizardModel) {
                return wizardModel;
            }

            @Override
            protected long getMaxWaitTimeInSeconds() {
                return SyncopeWebApplication.get().getMaxWaitTimeInSeconds();
            }

            @Override
            protected void sendError(final Exception exception) {
                SyncopeConsoleSession.get().onException(exception);
            }

            @Override
            protected void sendWarning(final String message) {
                SyncopeConsoleSession.get().warn(message);
            }

            @Override
            protected Future<Pair<Serializable, Serializable>> execute(
                    final Callable<Pair<Serializable, Serializable>> future) {
                return SyncopeConsoleSession.get().execute(future);
            }
        }, false);
        NewBpmnProcess newBpmnProcess = new NewBpmnProcess("newBpmnProcess", container, pageRef);
        addInnerObject(newBpmnProcess);
        AjaxLink<Void> newBpmnProcessLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                newBpmnProcess.toggle(target, true);
            }
        };
        ((WebMarkupContainer) get("container:content")).addOrReplace(newBpmnProcessLink);

        setShowResultPanel(true);

        modal.size(Modal.Size.Large);

        utility = new BaseModal<>("outer");
        addOuterObject(utility);
        utility.size(Modal.Size.Large);
        AjaxSubmitLink xmlEditorSubmit = utility.addSubmitButton();
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditorSubmit, RENDER, FlowableEntitlement.BPMN_PROCESS_SET);
        utility.setWindowClosedCallback(target -> {
            utility.show(false);
            utility.close(target);
        });
        initResultTable();
    }

    @Override
    protected BpmProcessDataProvider dataProvider() {
        return new BpmProcessDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_WORKFLOW_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<BpmnProcess, String>> getColumns() {
        List<IColumn<BpmnProcess, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(new ResourceModel("key"), "key"));
        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));

        return columns;
    }

    @Override
    public ActionsPanel<BpmnProcess> getActions(final IModel<BpmnProcess> model) {
        final ActionsPanel<BpmnProcess> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -184018732772021627L;

            @Override
            public void onClick(final AjaxRequestTarget target, final BpmnProcess ignore) {
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
                    public void onSubmit(final AjaxRequestTarget target) {
                        if (StringUtils.isNotBlank(wfDefinition.getObject())) {
                            try {
                                restClient.setDefinition(MediaType.APPLICATION_XML_TYPE,
                                        model.getObject().getKey(), wfDefinition.getObject());
                                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));

                                target.add(container);
                                utility.show(false);
                                utility.close(target);
                            } catch (SyncopeClientException e) {
                                SyncopeConsoleSession.get().onException(e);
                            }
                            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    }
                });
                utility.show(target);
                target.add(utility);
            }
        }, ActionLink.ActionType.EDIT, FlowableEntitlement.BPMN_PROCESS_SET);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            public void onClick(final AjaxRequestTarget target, final BpmnProcess ignore) {
                modal.header(Model.of(model.getObject().getKey()));
                modal.setContent(new ImageModalPanel<>(
                        modal, restClient.getDiagram(model.getObject().getKey()), pageRef));
                modal.show(target);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW, FlowableEntitlement.BPMN_PROCESS_GET);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -184018732772021627L;

            @Override
            public Class<? extends Page> getPageClass() {
                return ModelerPopupPage.class;
            }

            @Override
            public PageParameters getPageParameters() {
                PageParameters parameters = new PageParameters();
                parameters.add(Constants.MODELER_CONTEXT, FLOWABLE_MODELER_CTX);
                parameters.add(Constants.MODEL_ID_PARAM, model.getObject().getModelId());

                return parameters;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final BpmnProcess ignore) {
                // do nothing
            }
        }, ActionLink.ActionType.EXTERNAL_EDITOR, FlowableEntitlement.BPMN_PROCESS_SET);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected boolean statusCondition(final BpmnProcess modelObject) {
                return !modelObject.isUserWorkflow();
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final BpmnProcess ignore) {
                try {
                    restClient.deleteDefinition(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting BPMN definition {}", model.getObject().getName(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, FlowableEntitlement.BPMN_PROCESS_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class BpmProcessDataProvider extends DirectoryDataProvider<BpmnProcess> {

        private static final long serialVersionUID = 1764153405387687592L;

        private final SortableDataProviderComparator<BpmnProcess> comparator;

        public BpmProcessDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
            setSort("key", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<BpmnProcess> iterator(final long first, final long count) {
            List<BpmnProcess> result = restClient.getDefinitions();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getDefinitions().size();
        }

        @Override
        public IModel<BpmnProcess> model(final BpmnProcess object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<BpmnProcess, BpmnProcess, BpmnProcessRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final BpmnProcessRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
        }

        @Override
        protected WizardMgtPanel<BpmnProcess> newInstance(final String id, final boolean wizardInModal) {
            return new BpmnProcessDirectoryPanel(id, this);
        }
    }
}
