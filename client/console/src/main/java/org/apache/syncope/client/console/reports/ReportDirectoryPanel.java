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
package org.apache.syncope.client.console.reports;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Reports page.
 */
public abstract class ReportDirectoryPanel
        extends DirectoryPanel<ReportTO, ReportTO, DirectoryDataProvider<ReportTO>, ReportRestClient> {

    private static final long serialVersionUID = 4984337552918213290L;

    private final ReportStartAtTogglePanel startAt;

    protected ReportDirectoryPanel(final MultilevelPanel multiLevelPanelRef, final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, true);
        this.restClient = new ReportRestClient();

        this.addNewItemPanelBuilder(new ReportWizardBuilder(new ReportTO(), pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.REPORT_CREATE);

        modal.size(Modal.Size.Large);
        initResultTable();

        startAt = new ReportStartAtTogglePanel(container, pageRef);
        addInnerObject(startAt);
    }

    @Override
    protected List<IColumn<ReportTO, String>> getColumns() {
        final List<IColumn<ReportTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<ReportTO>(
                new StringResourceModel("key", this), "key", "key"));

        columns.add(new PropertyColumn<ReportTO, String>(new StringResourceModel(
                "name", this), "name", "name"));

        columns.add(new DatePropertyColumn<ReportTO>(
                new StringResourceModel("lastExec", this), "lastExec", "lastExec"));

        columns.add(new DatePropertyColumn<ReportTO>(
                new StringResourceModel("nextExec", this), "nextExec", "nextExec"));

        columns.add(new DatePropertyColumn<ReportTO>(
                new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<ReportTO>(
                new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<ReportTO, String>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        columns.add(new BooleanPropertyColumn<ReportTO>(
                new StringResourceModel("active", this), "active", "active"));

        columns.add(new ActionColumn<ReportTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel<ReportTO> getActions(final String componentId, final IModel<ReportTO> model) {

                final ActionLinksPanel<ReportTO> panel = ActionLinksPanel.<ReportTO>builder().
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                target.add(modal.setContent(new ReportletDirectoryPanel(
                                        modal, model.getObject().getKey(), pageRef)));

                                modal.header(new StringResourceModel(
                                        "reportlet.conf", ReportDirectoryPanel.this, Model.of(model.getObject())));

                                MetaDataRoleAuthorizationStrategy.authorize(
                                        modal.getForm(), ENABLE, StandardEntitlement.RESOURCE_UPDATE);

                                modal.show(true);
                            }
                        }, ActionLink.ActionType.COMPOSE, StandardEntitlement.REPORT_UPDATE).
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                final ReportTO clone = SerializationUtils.clone(model.getObject());
                                clone.setKey(null);
                                send(ReportDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(clone, target));
                            }
                        }, ActionLink.ActionType.CLONE, StandardEntitlement.REPORT_CREATE).
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                send(ReportDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(
                                                restClient.read(model.getObject().getKey()), target));
                            }
                        }, ActionLink.ActionType.EDIT, StandardEntitlement.REPORT_UPDATE).
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                viewTask(model.getObject(), target);
                            }
                        }, ActionLink.ActionType.VIEW, StandardEntitlement.REPORT_READ).
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                startAt.setExecutionDetail(
                                        model.getObject().getKey(), model.getObject().getName(), target);
                                startAt.toggle(target, true);
                            }
                        }, ActionLink.ActionType.EXECUTE, StandardEntitlement.REPORT_EXECUTE).
                        add(new ActionLink<ReportTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                                final ReportTO reportTO = model.getObject();
                                try {
                                    restClient.delete(reportTO.getKey());
                                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (SyncopeClientException e) {
                                    LOG.error("While deleting {}", reportTO.getKey(), e);
                                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                }
                                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.REPORT_DELETE).build(componentId);

                return panel;
            }

            @Override
            public ActionLinksPanel<ReportTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<ReportTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<ReportTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.EXECUTE);
        bulkActions.add(ActionType.DELETE);
        return bulkActions;
    }

    @Override
    protected ReportDataProvider dataProvider() {
        return new ReportDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_REPORT_TASKS_PAGINATOR_ROWS;
    }

    protected abstract void viewTask(final ReportTO reportTO, final AjaxRequestTarget target);

    protected class ReportDataProvider extends DirectoryDataProvider<ReportTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<ReportTO> comparator;

        public ReportDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ReportTO> iterator(final long first, final long count) {
            List<ReportTO> list = restClient.list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<ReportTO> model(final ReportTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
