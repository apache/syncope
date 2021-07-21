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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Reportlets page.
 */
public class ReportletDirectoryPanel extends DirectoryPanel<
        ReportletWrapper, ReportletWrapper, DirectoryDataProvider<ReportletWrapper>, ReportRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final BaseModal<ReportTO> baseModal;

    private final String report;

    public ReportletDirectoryPanel(
            final BaseModal<ReportTO> baseModal, final String report, final PageReference pageRef) {
        super(BaseModal.CONTENT_ID, pageRef, false);

        disableCheckBoxes();

        this.baseModal = baseModal;
        this.report = report;
        this.restClient = new ReportRestClient();

        enableUtilityButton();

        this.addNewItemPanelBuilder(
                new ReportletWizardBuilder(report, new ReportletWrapper(true), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.REPORT_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<ReportletWrapper, String>> getColumns() {
        final List<IColumn<ReportletWrapper, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("reportlet", this), "implementationKey", "implementationKey"));

        columns.add(new AbstractColumn<>(
            new StringResourceModel("configuration", this)) {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                final Item<ICellPopulator<ReportletWrapper>> cellItem,
                final String componentId,
                final IModel<ReportletWrapper> rowModel) {

                if (rowModel.getObject().getConf() == null) {
                    cellItem.add(new Label(componentId, ""));
                } else {
                    cellItem.add(new Label(componentId, rowModel.getObject().getConf().getClass().getName()));
                }
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<ReportletWrapper> getActions(final IModel<ReportletWrapper> model) {
        final ActionsPanel<ReportletWrapper> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportletWrapper ignore) {
                ReportletDirectoryPanel.this.getTogglePanel().close(target);
                if (model.getObject().getConf() == null) {
                    SyncopeConsoleSession.get().info(getString("noConf"));
                } else {
                    send(ReportletDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                }
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.REPORT_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportletWrapper ignore) {
                final ReportletConf reportlet = model.getObject().getConf();
                try {
                    final ReportTO actual = ReportRestClient.read(report);
                    actual.getReportlets().remove(model.getObject().getImplementationKey());
                    ReportRestClient.update(actual);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    customActionOnFinishCallback(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", reportlet.getName(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.REPORT_UPDATE);

        return panel;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = new ActionsPanel<>(componentId, null);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (target != null) {
                    customActionOnFinishCallback(target);
                }
            }
        }, ActionLink.ActionType.RELOAD, IdRepoEntitlement.TASK_LIST).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected ReportDataProvider dataProvider() {
        return new ReportDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_REPORTLET_TASKS_PAGINATOR_ROWS;
    }

    protected class ReportDataProvider extends DirectoryDataProvider<ReportletWrapper> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<ReportletWrapper> comparator;

        public ReportDataProvider(final int paginatorRows) {
            super(paginatorRows);

            //Default sorting
            setSort("implementationKey", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        private List<ReportletWrapper> getReportletWrappers(final ReportTO reportTO) {
            return reportTO.getReportlets().stream().map(reportlet -> {
                ImplementationTO impl = ImplementationRestClient.read(IdRepoImplementationType.REPORTLET, reportlet);

                ReportletWrapper wrapper = new ReportletWrapper(false).
                        setImplementationKey(impl.getKey()).
                        setImplementationEngine(impl.getEngine());
                if (impl.getEngine() == ImplementationEngine.JAVA) {
                    try {
                        ReportletConf reportletConf = MAPPER.readValue(impl.getBody(), ReportletConf.class);
                        wrapper.setConf(reportletConf);
                    } catch (Exception e) {
                        LOG.error("During deserialization", e);
                    }
                }

                return wrapper;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public Iterator<ReportletWrapper> iterator(final long first, final long count) {
            final ReportTO actual = ReportRestClient.read(report);

            List<ReportletWrapper> reportlets = getReportletWrappers(actual);

            reportlets.sort(comparator);
            return reportlets.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            final ReportTO actual = ReportRestClient.read(report);
            return getReportletWrappers(actual).size();
        }

        @Override
        public IModel<ReportletWrapper> model(final ReportletWrapper object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
