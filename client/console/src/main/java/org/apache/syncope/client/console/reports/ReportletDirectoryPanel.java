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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.reports.ReportletDirectoryPanel.ReportletWrapper;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.ReportTO;
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

        enableExitButton();

        this.addNewItemPanelBuilder(new ReportletWizardBuilder(report, new ReportletWrapper(), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.REPORT_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<ReportletWrapper, String>> getColumns() {
        final List<IColumn<ReportletWrapper, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("reportlet", this), "name", "name"));

        columns.add(new AbstractColumn<ReportletWrapper, String>(
                new StringResourceModel("configuration", this)) {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ReportletWrapper>> cellItem,
                    final String componentId,
                    final IModel<ReportletWrapper> rowModel) {

                cellItem.add(new Label(componentId, rowModel.getObject().getConf().getClass().getName()));
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<ReportletWrapper> getActions(final IModel<ReportletWrapper> model) {
        final ActionsPanel<ReportletWrapper> panel = super.getActions(model);

        panel.add(new ActionLink<ReportletWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportletWrapper ignore) {
                ReportletDirectoryPanel.this.getTogglePanel().close(target);
                AbstractReportletConf clone = SerializationUtils.clone(model.getObject().getConf());
                clone.setName(null);

                send(ReportletDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new ReportletWrapper().setConf(clone),
                                target));
            }
        }, ActionLink.ActionType.CLONE, StandardEntitlement.REPORT_CREATE);
        panel.add(new ActionLink<ReportletWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportletWrapper ignore) {
                ReportletDirectoryPanel.this.getTogglePanel().close(target);
                send(ReportletDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.REPORT_UPDATE);
        panel.add(new ActionLink<ReportletWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ReportletWrapper ignore) {
                final ReportletConf reportlet = model.getObject().getConf();
                try {
                    final ReportTO actual = restClient.read(report);
                    actual.getReportletConfs().removeAll(actual.getReportletConfs().stream().
                            filter(conf -> conf.getName().equals(reportlet.getName())).collect(Collectors.toList()));
                    restClient.update(actual);
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    customActionOnFinishCallback(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", reportlet.getName(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.REPORT_DELETE, true);

        return panel;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = new ActionsPanel<>(componentId, null);

        panel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (target != null) {
                    customActionOnFinishCallback(target);
                }
            }
        }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.DELETE);
        return bulkActions;
    }

    @Override
    protected ReportDataProvider dataProvider() {
        return new ReportDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_REPORTLET_TASKS_PAGINATOR_ROWS;
    }

    protected class ReportDataProvider extends DirectoryDataProvider<ReportletWrapper> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<ReportletWrapper> comparator;

        public ReportDataProvider(final int paginatorRows) {
            super(paginatorRows);

            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ReportletWrapper> iterator(final long first, final long count) {
            final ReportTO actual = restClient.read(report);

            final List<ReportletWrapper> reportlets = actual.getReportletConfs().stream().
                    map(conf -> new ReportletWrapper(conf.getName()).setName(conf.getName()).setConf(conf)).
                    collect(Collectors.toList());

            Collections.sort(reportlets, comparator);
            return reportlets.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            final ReportTO actual = restClient.read(report);
            return actual.getReportletConfs().size();
        }

        @Override
        public IModel<ReportletWrapper> model(final ReportletWrapper object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public static class ReportletWrapper implements Serializable {

        private static final long serialVersionUID = 2472755929742424558L;

        private String oldname;

        private String name;

        private AbstractReportletConf conf;

        private final Map<String, Pair<AbstractFiqlSearchConditionBuilder, List<SearchClause>>> scondWrapper;

        public ReportletWrapper() {
            this(null);
        }

        public ReportletWrapper(final String name) {
            this.oldname = name;
            this.scondWrapper = new HashMap<>();
        }

        public boolean isNew() {
            return oldname == null;
        }

        public String getOldName() {
            return this.oldname;
        }

        public String getName() {
            return this.name;
        }

        public ReportletWrapper setName(final String name) {
            this.name = name;
            return this;
        }

        public AbstractReportletConf getConf() {
            return conf;
        }

        public ReportletWrapper setConf(final AbstractReportletConf conf) {
            this.conf = conf;
            return this;
        }

        public Map<String, Pair<AbstractFiqlSearchConditionBuilder, List<SearchClause>>> getSCondWrapper() {
            return scondWrapper;
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent && modal != null) {
            final AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
