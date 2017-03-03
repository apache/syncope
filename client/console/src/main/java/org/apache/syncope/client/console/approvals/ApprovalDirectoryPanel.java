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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.approvals.ApprovalDirectoryPanel.ApprovalProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class ApprovalDirectoryPanel
        extends DirectoryPanel<WorkflowFormTO, WorkflowFormTO, ApprovalProvider, UserWorkflowRestClient> {

    private static final long serialVersionUID = -7122136682275797903L;

    public ApprovalDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, false);
        disableCheckBoxes();

        setFooterVisibility(true);
        modal.addSubmitButton();
        modal.size(Modal.Size.Large);

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                ((BasePage) pageReference.getPage()).getApprovalsWidget().refreshLatestAlerts(target);
                modal.show(false);
            }
        });

        restClient = new UserWorkflowRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.WORKFLOW_FORM_SUBMIT);
    }

    @Override
    protected List<IColumn<WorkflowFormTO, String>> getColumns() {

        List<IColumn<WorkflowFormTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("taskId"), "taskId", "taskId"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("username"), "username", "username"));
        columns.add(new DatePropertyColumn<WorkflowFormTO>(
                new ResourceModel("createTime"), "createTime", "createTime"));
        columns.add(new DatePropertyColumn<WorkflowFormTO>(
                new ResourceModel("dueDate"), "dueDate", "dueDate"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(new ResourceModel("owner"), "owner", "owner"));
        columns.add(new ActionColumn<WorkflowFormTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863133L;

            @Override
            public ActionLinksPanel<WorkflowFormTO> getActions(
                    final String componentId, final IModel<WorkflowFormTO> model) {
                final ActionLinksPanel.Builder<WorkflowFormTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<WorkflowFormTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                        try {
                            restClient.claimForm(model.getObject().getTaskId());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scee) {
                            SyncopeConsoleSession.get().error(getString(Constants.ERROR) + ": " + scee.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        target.add(container);
                    }
                }, ActionLink.ActionType.CLAIM, StandardEntitlement.WORKFLOW_FORM_CLAIM);

                panel.add(new ActionLink<WorkflowFormTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                        final IModel<WorkflowFormTO> formModel = new CompoundPropertyModel<>(model.getObject());
                        modal.setFormModel(formModel);

                        target.add(modal.setContent(new ApprovalModal(modal, pageRef, model.getObject())));
                        modal.header(new Model<>(getString("approval.edit", new Model<>(model.getObject()))));

                        modal.show(true);
                    }

                    @Override
                    protected boolean statusCondition(final WorkflowFormTO modelObject) {
                        return SyncopeConsoleSession.get().getSelfTO().getUsername().
                                equals(model.getObject().getOwner());
                    }

                }, ActionLink.ActionType.EDIT, StandardEntitlement.WORKFLOW_FORM_READ);

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<WorkflowFormTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<WorkflowFormTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<WorkflowFormTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.WORKFLOW_FORM_LIST).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected ApprovalProvider dataProvider() {
        return new ApprovalProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_WORKFLOW_FORM_PAGINATOR_ROWS;
    }

    public static class ApprovalProvider extends DirectoryDataProvider<WorkflowFormTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private final SortableDataProviderComparator<WorkflowFormTO> comparator;

        private final UserWorkflowRestClient restClient = new UserWorkflowRestClient();

        public ApprovalProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("createTime", SortOrder.ASCENDING);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<WorkflowFormTO> iterator(final long first, final long count) {
            final List<WorkflowFormTO> list = restClient.getForms();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getForms().size();
        }

        @Override
        public IModel<WorkflowFormTO> model(final WorkflowFormTO configuration) {
            return new AbstractReadOnlyModel<WorkflowFormTO>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public WorkflowFormTO getObject() {
                    return configuration;
                }
            };
        }
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }
}
