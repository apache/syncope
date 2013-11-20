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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.util.LoggerEventUtils;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.LoggerCategoryPanel;
import org.apache.syncope.console.pages.panels.SelectedEventsPanel;
import org.apache.syncope.console.rest.LoggerRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.CollectionUtils;

/**
 * Auditing and Reporting.
 */
public class Reports extends BasePage {

    private static final long serialVersionUID = -2071214196989178694L;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    @SpringBean
    private LoggerRestClient loggerRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private WebMarkupContainer reportContainer;

    private WebMarkupContainer auditContainer;

    private int paginatorRows;

    private final ModalWindow window;

    public Reports(final PageParameters parameters) {
        super(parameters);

        window = new ModalWindow("reportWin");
        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName("view-report-win");
        add(window);

        setupReport();
        setupAudit();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupReport() {
        reportContainer = new WebMarkupContainer("reportContainer");
        setWindowClosedCallback(window, reportContainer);

        MetaDataRoleAuthorizationStrategy.authorize(reportContainer, RENDER, xmlRolesReader.getAllAllowedRoles(
                "Reports", "list"));

        paginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_REPORT_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("id"), "id", "id"));
        columns.add(new PropertyColumn(new ResourceModel("name"), "name", "name"));
        columns.add(new DatePropertyColumn(new ResourceModel("lastExec"), "lastExec", "lastExec"));
        columns.add(new DatePropertyColumn(new ResourceModel("nextExec"), "nextExec", "nextExec"));
        columns.add(new DatePropertyColumn(new ResourceModel("startDate"), "startDate", "startDate"));
        columns.add(new DatePropertyColumn(new ResourceModel("endDate"), "endDate", "endDate"));
        columns.add(new PropertyColumn(new ResourceModel("latestExecStatus"), "latestExecStatus", "latestExecStatus"));
        columns.add(new ActionColumn<ReportTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel getActions(final String componentId, final IModel<ReportTO> model) {

                final ReportTO reportTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ReportModalPage(window, reportTO, Reports.this.getPageReference());
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Reports");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            reportRestClient.startExecution(reportTO.getId());
                            getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scce) {
                            error(scce.getMessage());
                        }

                        target.add(feedbackPanel);
                        target.add(reportContainer);
                    }
                }, ActionLink.ActionType.EXECUTE, "Reports");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            reportRestClient.delete(reportTO.getId());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scce) {
                            error(scce.getMessage());
                        }
                        target.add(reportContainer);
                        target.add(feedbackPanel);
                    }
                }, ActionLink.ActionType.DELETE, "Reports");

                return panel;
            }

            @Override
            public Component getHeader(final String componentId) {
                final ActionLinksPanel panel = new ActionLinksPanel(componentId, new Model(), getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        if (target != null) {
                            target.add(reportContainer);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, TASKS, "list");

                return panel;
            }
        });

        final AjaxFallbackDefaultDataTable reportTable =
                new AjaxFallbackDefaultDataTable("reportTable", columns, new ReportProvider(), paginatorRows);

        reportContainer.add(reportTable);
        reportContainer.setOutputMarkupId(true);

        add(reportContainer);

        Form paginatorForm = new Form("paginatorForm");

        MetaDataRoleAuthorizationStrategy.authorize(paginatorForm, RENDER, xmlRolesReader.getAllAllowedRoles("Reports",
                "list"));

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_REPORT_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                reportTable.setItemsPerPage(paginatorRows);

                target.add(reportContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        AjaxLink createLink = new ClearIndicatingAjaxLink("createLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new ReportModalPage(window, new ReportTO(), Reports.this.getPageReference());
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createLink, RENDER, xmlRolesReader.getAllAllowedRoles("Reports",
                "create"));

        add(createLink);
    }

    private void setupAudit() {
        auditContainer = new WebMarkupContainer("auditContainer");
        auditContainer.setOutputMarkupId(true);
        add(auditContainer);

        MetaDataRoleAuthorizationStrategy.authorize(
                auditContainer, RENDER, xmlRolesReader.getAllAllowedRoles("Audit", "list"));

        final Form form = new Form("auditForm");
        auditContainer.add(form);

        final List<String> events = new ArrayList<String>();

        final List<AuditLoggerName> audits = loggerRestClient.listAudits();
        for (AuditLoggerName audit : audits) {
            events.add(LoggerEventUtils.buildEvent(
                    audit.getType(),
                    audit.getCategory(),
                    audit.getSubcategory(),
                    audit.getEvent(),
                    audit.getResult()));
        }

        final ListModel<String> model = new ListModel<String>(new ArrayList<String>(events));

        form.add(new LoggerCategoryPanel(
                "events", loggerRestClient.listEvents(), model, getPageReference(), "Reports") {

            private static final long serialVersionUID = 6113164334533550277L;

            @Override
            protected String[] getListRoles() {
                return new String[] {
                    xmlRolesReader.getAllAllowedRoles("Audit", "list")
                };
            }

            @Override
            protected String[] getChangeRoles() {
                return new String[] {
                    xmlRolesReader.getAllAllowedRoles("Audit", "enable"),
                    xmlRolesReader.getAllAllowedRoles("Audit", "disable")
                };
            }

            @Override
            public void onEventAction(final IEvent<?> event) {
                if (event.getPayload() instanceof SelectedEventsPanel.EventSelectionChanged) {

                    final SelectedEventsPanel.EventSelectionChanged eventSelectionChanged =
                            (SelectedEventsPanel.EventSelectionChanged) event.getPayload();

                    for (String toBeRemoved : eventSelectionChanged.getToBeRemoved()) {
                        if (events.contains(toBeRemoved)) {
                            final Map.Entry<EventCategoryTO, Result> eventCategory =
                                    LoggerEventUtils.parseEventCategory(toBeRemoved);

                            final AuditLoggerName auditLoggerName = new AuditLoggerName(
                                    eventCategory.getKey().getType(),
                                    eventCategory.getKey().getCategory(),
                                    eventCategory.getKey().getSubcategory(),
                                    CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                    ? null : eventCategory.getKey().getEvents().iterator().next(),
                                    eventCategory.getValue());

                            loggerRestClient.disableAudit(auditLoggerName);
                            events.remove(toBeRemoved);
                        }
                    }

                    for (String toBeAdded : eventSelectionChanged.getToBeAdded()) {
                        if (!events.contains(toBeAdded)) {
                            final Map.Entry<EventCategoryTO, Result> eventCategory =
                                    LoggerEventUtils.parseEventCategory(toBeAdded);

                            final AuditLoggerName auditLoggerName = new AuditLoggerName(
                                    eventCategory.getKey().getType(),
                                    eventCategory.getKey().getCategory(),
                                    eventCategory.getKey().getSubcategory(),
                                    CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                    ? null : eventCategory.getKey().getEvents().iterator().next(),
                                    eventCategory.getValue());

                            loggerRestClient.enableAudit(auditLoggerName);
                            events.add(toBeAdded);
                        }
                    }
                }
            }
        });
    }

    private class ReportProvider extends SortableDataProvider<ReportTO, String> {

        private static final long serialVersionUID = -2311716167583335852L;

        private SortableDataProviderComparator<ReportTO> comparator;

        public ReportProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<ReportTO>(this);
        }

        @Override
        public Iterator<ReportTO> iterator(final long first, final long count) {
            final List<ReportTO> list = reportRestClient.list(((int) first / paginatorRows) + 1, paginatorRows);
            Collections.sort(list, comparator);
            return list.iterator();
        }

        @Override
        public long size() {
            return reportRestClient.count();
        }

        @Override
        public IModel<ReportTO> model(final ReportTO configuration) {

            return new AbstractReadOnlyModel<ReportTO>() {

                private static final long serialVersionUID = 4921104837546595602L;

                @Override
                public ReportTO getObject() {
                    return configuration;
                }
            };
        }
    }
}
