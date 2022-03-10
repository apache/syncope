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
package org.apache.syncope.client.console.widgets;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.ajax.markup.html.IndicatorAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.Action;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionPanel;
import org.apache.syncope.client.console.widgets.reconciliation.Any;
import org.apache.syncope.client.console.widgets.reconciliation.Anys;
import org.apache.syncope.client.console.widgets.reconciliation.Misaligned;
import org.apache.syncope.client.console.widgets.reconciliation.Missing;
import org.apache.syncope.client.console.widgets.reconciliation.ReconciliationReport;
import org.apache.syncope.client.console.widgets.reconciliation.ReconciliationReportParser;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconciliationWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationWidget.class);

    private static final int ROWS = 10;

    private final String reconciliationReportKey;

    private final BaseModal<Any> detailsModal = new BaseModal<>("detailsModal");

    private final PageReference pageRef;

    private final ReportRestClient restClient = new ReportRestClient();

    private final WebMarkupContainer container;

    private final WebMarkupContainer overlay;

    private boolean checkReconciliationJob = false;

    public ReconciliationWidget(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
        setOutputMarkupId(true);
        add(detailsModal);

        container = new WebMarkupContainer("reconciliationContainer");
        container.add(new IndicatorAjaxTimerBehavior(java.time.Duration.of(10, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                if (isCheckReconciliationJob()) {
                    try {
                        ReportRestClient.listJobs().stream().
                                filter(jobTO -> SyncopeWebApplication.get().
                                getReconciliationReportKey().equals(jobTO.getRefKey())).
                                findFirst().ifPresent(reportJobTO -> {
                                    if (!reportJobTO.isRunning()) {
                                        LOG.debug("Report {} is not running",
                                                SyncopeWebApplication.get().getReconciliationReportKey());

                                        overlay.setVisible(false);

                                        container.addOrReplace(buildExecFragment());

                                        target.add(ReconciliationWidget.this);
                                        setCheckReconciliationJob(false);
                                    }
                                });
                    } catch (Throwable t) {
                        LOG.error("Unexpected error while checking for updated reconciliation job info", t);
                    }
                }
            }
        });
        add(container);

        overlay = new WebMarkupContainer("overlay");
        overlay.setOutputMarkupPlaceholderTag(true);
        overlay.setVisible(false);
        container.add(overlay);

        this.reconciliationReportKey = SyncopeWebApplication.get().getReconciliationReportKey();

        ReportTO reconciliationReport = null;
        if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.REPORT_READ)) {
            try {
                reconciliationReport = ReportRestClient.read(reconciliationReportKey);
            } catch (Exception e) {
                LOG.error("Could not fetch the expected reconciliation report with key {}, aborting",
                        reconciliationReportKey, e);
            }
        }

        Fragment reportResult = reconciliationReport == null || reconciliationReport.getExecutions().isEmpty()
                ? new Fragment("reportResult", "noExecFragment", this)
                : buildExecFragment();
        reportResult.setOutputMarkupId(true);
        container.add(reportResult);

        IndicatorAjaxLink<Void> refresh = new IndicatorAjaxLink<>("refresh") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    restClient.startExecution(reconciliationReportKey, null);

                    overlay.setVisible(true);
                    target.add(ReconciliationWidget.this);

                    setCheckReconciliationJob(true);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While starting reconciliation report", e);
                    SyncopeConsoleSession.get().error("Could not start reconciliation report");
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(refresh, Component.RENDER, IdRepoEntitlement.REPORT_EXECUTE);
        container.add(refresh);
    }

    private boolean isCheckReconciliationJob() {
        return checkReconciliationJob;
    }

    private void setCheckReconciliationJob(final boolean checkReconciliationJob) {
        this.checkReconciliationJob = checkReconciliationJob;
    }

    private Fragment buildExecFragment() {
        Fragment execFragment = new Fragment("reportResult", "execFragment", this);
        execFragment.setOutputMarkupId(true);

        Pair<List<ProgressBean>, ReconciliationReport> execResult;
        try {
            execResult = parseReconciliationReportExec();
        } catch (Exception e) {
            LOG.error("Could not parse the reconciliation report result", e);
            execResult = Pair.of(List.of(), new ReconciliationReport(OffsetDateTime.now()));
        }
        final List<ProgressBean> progressBeans = execResult.getLeft();
        final ReconciliationReport report = execResult.getRight();

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(new ResourceModel("summary")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ProgressesPanel(panelId, report.getRun(), progressBeans);
            }
        });
        tabs.add(new AbstractTab(Model.of(AnyTypeKind.USER.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new AnysReconciliationPanel(panelId, report.getUsers(), pageRef);
            }
        });
        tabs.add(new AbstractTab(Model.of(AnyTypeKind.GROUP.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new AnysReconciliationPanel(panelId, report.getGroups(), pageRef);
            }
        });
        for (final Anys anys : report.getAnyObjects()) {
            tabs.add(new AbstractTab(Model.of(anys.getAnyType())) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AnysReconciliationPanel(panelId, anys, pageRef);
                }
            });
        }

        execFragment.add(new AjaxBootstrapTabbedPanel<>("execResult", tabs));

        return execFragment;
    }

    private Pair<List<ProgressBean>, ReconciliationReport> parseReconciliationReportExec() throws IOException {
        List<ProgressBean> beans = List.of();
        ReconciliationReport report = null;

        Optional<ExecTO> exec = Optional.empty();
        if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.REPORT_LIST)) {
            exec = restClient.listRecentExecutions(ROWS).stream().
                    filter(e -> reconciliationReportKey.equals(e.getRefKey())).findFirst();
        }
        if (exec.isEmpty()) {
            LOG.error("Could not find the last execution of reconciliation report");
        } else {
            Object entity = ReportRestClient.exportExecutionResult(
                    exec.get().getKey(), ReportExecExportFormat.XML).getEntity();
            if (entity instanceof InputStream) {
                try {
                    report = ReconciliationReportParser.parse(exec.get().getEnd(), (InputStream) entity);

                    beans = new ArrayList<>();

                    ProgressBean progressBean = new ProgressBean();
                    progressBean.setText(getString("users"));
                    progressBean.setTotal(report.getUsers().getTotal());
                    progressBean.setFraction(report.getUsers().getTotal() - report.getUsers().getAnys().size());
                    progressBean.setCssClass("bg-yellow");
                    beans.add(progressBean);

                    progressBean = new ProgressBean();
                    progressBean.setText(getString("groups"));
                    progressBean.setTotal(report.getGroups().getTotal());
                    progressBean.setFraction(report.getGroups().getTotal() - report.getGroups().getAnys().size());
                    progressBean.setCssClass("bg-red");
                    beans.add(progressBean);

                    int i = 0;
                    for (Anys anys : report.getAnyObjects()) {
                        progressBean = new ProgressBean();
                        progressBean.setText(anys.getAnyType());
                        progressBean.setTotal(anys.getTotal());
                        progressBean.setFraction(anys.getTotal() - anys.getAnys().size());
                        progressBean.setCssClass("bg-" + (i % 2 == 0 ? "success" : "info"));
                        beans.add(progressBean);

                        i++;
                    }
                } catch (Exception e) {
                    LOG.error("Could not parse the last execution available of reconciliation report", e);
                }
            }
        }

        return Pair.of(beans, Optional.ofNullable(report).
                orElseGet(() -> new ReconciliationReport(OffsetDateTime.now())));
    }

    private class AnysReconciliationPanel extends DirectoryPanel<Any, Any, AnysReconciliationProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        private final Anys anys;

        AnysReconciliationPanel(final String id, final Anys anys, final PageReference pageRef) {
            super(id, new Builder<Any, Any, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<Any> newInstance(final String id, final boolean wizardInModal) {
                    throw new UnsupportedOperationException();
                }
            }.disableCheckBoxes().hidePaginator());

            this.anys = anys;
            this.rows = ROWS;
            initResultTable();
        }

        @Override
        protected AnysReconciliationProvider dataProvider() {
            return new AnysReconciliationProvider(anys);
        }

        @Override
        protected String paginatorRowsKey() {
            return StringUtils.EMPTY;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return List.of();
        }

        @Override
        protected List<IColumn<Any, String>> getColumns() {
            List<IColumn<Any, String>> columns = new ArrayList<>();

            columns.add(new AbstractColumn<>(new ResourceModel("reference"), Constants.KEY_FIELD_NAME) {

                private static final long serialVersionUID = -1822504503325964706L;

                @Override
                public void populateItem(
                        final Item<ICellPopulator<Any>> cellItem,
                        final String componentId,
                        final IModel<Any> rowModel) {

                    cellItem.add(new Label(componentId,
                            rowModel.getObject().getKey()
                            + (StringUtils.isBlank(rowModel.getObject().getName())
                            ? StringUtils.EMPTY
                            : ' ' + rowModel.getObject().getName())));
                }
            });

            final Set<String> resources = new HashSet<>();
            anys.getAnys().forEach(any -> {
                resources.addAll(any.getMissing().stream().
                        map(Missing::getResource).collect(Collectors.toList()));
                resources.addAll(any.getMisaligned().stream().
                        map(Misaligned::getResource).collect(Collectors.toList()));
            });
            for (final String resource : resources) {
                columns.add(new AbstractColumn<>(Model.of(resource)) {

                    private static final long serialVersionUID = -1822504503325964706L;

                    @Override
                    public void populateItem(
                            final Item<ICellPopulator<Any>> cellItem,
                            final String componentId,
                            final IModel<Any> rowModel) {

                        Any any = rowModel.getObject();

                        Optional<Missing> missing = any.getMissing().stream().
                                filter(object -> resource.equals(object.getResource())).findFirst();
                        List<Misaligned> misaligned = any.getMisaligned().stream().
                                filter(object -> resource.equals(object.getResource())).collect(Collectors.toList());

                        Component content;
                        if (missing.isEmpty()) {
                            if (misaligned.isEmpty()) {
                                content = new Label(componentId, StringUtils.EMPTY);
                            } else {
                                Action<Any> action = new Action<>(new ActionLink<>() {

                                    private static final long serialVersionUID = -3722207913631435501L;

                                    @Override
                                    public void onClick(final AjaxRequestTarget target, final Any ignore) {
                                        modal.header(Model.of(
                                                rowModel.getObject().getType()
                                                + ' ' + rowModel.getObject().getKey()
                                                + ' ' + rowModel.getObject().getName()));
                                        modal.setContent(new ReconDetailsModalPanel(
                                                modal,
                                                resource,
                                                misaligned,
                                                ReconciliationWidget.this.pageRef));
                                        modal.show(true);
                                        target.add(modal);
                                    }
                                }, ActionLink.ActionType.VIEW);
                                action.hideLabel();
                                content = new ActionPanel<>(componentId, rowModel, action);
                            }
                        } else {
                            Action<Any> action = new Action<>(null, ActionLink.ActionType.NOT_FOUND);
                            action.hideLabel();
                            content = new ActionPanel<>(componentId, rowModel, action);
                        }
                        cellItem.add(content);
                        cellItem.add(new AttributeModifier("class", "text-center"));
                    }
                });
            }

            return columns;
        }
    }

    protected static final class AnysReconciliationProvider extends DirectoryDataProvider<Any> {

        private static final long serialVersionUID = -1500081449932597854L;

        private final Anys anys;

        private final SortableDataProviderComparator<Any> comparator;

        private AnysReconciliationProvider(final Anys anys) {
            super(ROWS);
            this.anys = anys;
            setSort(Constants.KEY_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<Any> iterator(final long first, final long count) {
            anys.getAnys().sort(comparator);
            return anys.getAnys().subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return anys.getAnys().size();
        }

        @Override
        public IModel<Any> model(final Any object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
