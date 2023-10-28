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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.widgets.JobActionPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Reports page.
 */
public abstract class ReportDirectoryPanel
        extends DirectoryPanel<ReportTO, ReportTO, DirectoryDataProvider<ReportTO>, ReportRestClient> {

    private static final long serialVersionUID = 4984337552918213290L;

    protected final ReportStartAtTogglePanel startAt;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected ReportRestClient reportRestClient;

    @SpringBean
    protected MIMETypesLoader mimeTypesLoader;

    protected ReportDirectoryPanel(final ReportRestClient restClient, final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, restClient, pageRef, true);

        addNewItemPanelBuilder(new ReportWizardBuilder(
                new ReportTO(), implementationRestClient, reportRestClient, mimeTypesLoader, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.REPORT_CREATE);

        modal.size(Modal.Size.Large);
        initResultTable();

        container.add(new IndicatorAjaxTimerBehavior(Duration.of(10, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = -4661303265651934868L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                container.modelChanged();
                target.add(container);
            }
        });

        startAt = new ReportStartAtTogglePanel(container, pageRef);
        addInnerObject(startAt);
    }

    @Override
    protected List<IColumn<ReportTO, String>> getColumns() {
        List<IColumn<ReportTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("mimeType", this), "mimeType", "mimeType"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("lastExec", this), null, "lastExec"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("nextExec", this), null, "nextExec"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("active", this), "active", "active"));

        columns.add(new AbstractColumn<>(new Model<>(""), "running") {

            private static final long serialVersionUID = 4209532514416998046L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ReportTO>> cellItem,
                    final String componentId,
                    final IModel<ReportTO> rowModel) {

                Component panel;
                try {
                    JobTO jobTO = restClient.getJob(rowModel.getObject().getKey());
                    panel = new JobActionPanel(componentId, jobTO, false, ReportDirectoryPanel.this);
                    MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.ENABLE,
                            String.format("%s,%s",
                                    IdRepoEntitlement.REPORT_EXECUTE,
                                    IdRepoEntitlement.REPORT_UPDATE));
                } catch (Exception e) {
                    LOG.error("Could not get job for report {}", rowModel.getObject().getKey(), e);
                    panel = new Label(componentId, Model.of());
                }
                cellItem.add(panel);
            }

            @Override
            public String getCssClass() {
                return "running-col";
            }
        });

        return columns;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof JobActionPanel.JobActionPayload) {
            container.modelChanged();
            JobActionPanel.JobActionPayload.class.cast(event.getPayload()).getTarget().add(container);
        } else {
            super.onEvent(event);
        }
    }

    @Override
    public ActionsPanel<ReportTO> getActions(final IModel<ReportTO> model) {
        ActionsPanel<ReportTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                viewReportExecs(model.getObject(), target);
            }
        }, ActionLink.ActionType.VIEW_EXECUTIONS, IdRepoEntitlement.REPORT_READ);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                send(ReportDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(restClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.REPORT_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                final ReportTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(ReportDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(clone, target));
            }
        }, ActionLink.ActionType.CLONE, IdRepoEntitlement.REPORT_CREATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                startAt.setExecutionDetail(
                        model.getObject().getKey(), model.getObject().getName(), target);
                startAt.toggle(target, true);
            }
        }, ActionLink.ActionType.EXECUTE, IdRepoEntitlement.REPORT_EXECUTE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                final ReportTO reportTO = model.getObject();
                try {
                    restClient.delete(reportTO.getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", reportTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.REPORT_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        List<ActionType> batches = new ArrayList<>();
        batches.add(ActionType.EXECUTE);
        batches.add(ActionType.DELETE);
        return batches;
    }

    @Override
    protected ReportDataProvider dataProvider() {
        return new ReportDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_REPORT_PAGINATOR_ROWS;
    }

    protected abstract void viewReportExecs(ReportTO reportTO, AjaxRequestTarget target);

    protected class ReportDataProvider extends DirectoryDataProvider<ReportTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<ReportTO> comparator;

        public ReportDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ReportTO> iterator(final long first, final long count) {
            List<ReportTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<ReportTO> model(final ReportTO report) {
            return new CompoundPropertyModel<>(report);
        }
    }
}
