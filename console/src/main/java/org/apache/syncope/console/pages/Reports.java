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
import java.util.Set;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.StringUtils;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.JQueryTabbedPanel;
import org.apache.syncope.console.rest.LoggerRestClient;
import org.apache.syncope.console.rest.ReportRestClient;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.to.ReportTO;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditLoggerName;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;

/**
 * Auditing and Reporting.
 */
public class Reports extends BasePage {

    private static final long serialVersionUID = -2071214196989178694L;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    @SpringBean
    private ReportRestClient reportRestClient;

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
        columns.add(new AbstractColumn<ReportTO>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<ReportTO>> cellItem, final String componentId,
                    final IModel<ReportTO> model) {

                final ReportTO reportTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model);

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
                }, ActionLink.ActionType.EDIT, "Reports", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            reportRestClient.startExecution(reportTO.getId());
                            getSession().info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(getPage().get("feedback"));
                        target.add(reportContainer);
                    }
                }, ActionLink.ActionType.EXECUTE, "Reports", "execute");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            reportRestClient.delete(reportTO.getId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.add(reportContainer);
                        target.add(getPage().get("feedback"));
                    }
                }, ActionLink.ActionType.DELETE, "Reports", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable reportTable = new AjaxFallbackDefaultDataTable("reportTable", columns,
                new ReportProvider(), paginatorRows);

        reportContainer.add(reportTable);
        reportContainer.setOutputMarkupId(true);
        
        final AjaxLink reload = new IndicatingAjaxLink("reload") {
            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (target != null) {
                    target.add(reportTable);
                }
            }
        };

        reload.add(new Behavior() {
            
            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {

                if (reportTable.getRowCount() > paginatorRows) {
                    tag.remove("class");
                    tag.put("class", "settingsPosMultiPage");
                } else {
                    tag.remove("class");
                    tag.put("class", "settingsPos");
                }
            }
        });

        reportContainer.add(reload);

        add(reportContainer);

        Form paginatorForm = new Form("paginatorForm");

        MetaDataRoleAuthorizationStrategy.authorize(paginatorForm, RENDER, xmlRolesReader.getAllAllowedRoles("Reports",
                "list"));

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

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

        AjaxLink createLink = new IndicatingAjaxLink("createLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
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

        MetaDataRoleAuthorizationStrategy.authorize(auditContainer, RENDER, xmlRolesReader.getAllAllowedRoles("Audit",
                "list"));

        Form form = new Form("auditForm");
        auditContainer.add(form);

        List<ITab> tabs = new ArrayList<ITab>();

        for (final Category category : Category.values()) {
            tabs.add(new AbstractTab(new Model<String>(StringUtils.capitalize(category.name()))) {

                private static final long serialVersionUID = -5861786415855103549L;

                @Override
                public WebMarkupContainer getPanel(final String panelId) {
                    return new AuditCategoryPanel(panelId, category);
                }
            });
        }

        form.add(new JQueryTabbedPanel("categoriesTabs", tabs));
    }

    private class ReportProvider extends SortableDataProvider<ReportTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private SortableDataProviderComparator<ReportTO> comparator;

        public ReportProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<ReportTO>(this);
        }

        @Override
        public Iterator<ReportTO> iterator(final int first, final int count) {

            List<ReportTO> list = reportRestClient.list((first / paginatorRows) + 1, paginatorRows);

            Collections.sort(list, comparator);

            return list.iterator();
        }

        @Override
        public int size() {
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

    private class AuditsByCategoryModel implements IModel<List<AuditLoggerName>> {

        private static final long serialVersionUID = 605983084097505724L;

        private final Category category;

        private final Result result;

        public AuditsByCategoryModel(final Category category, final Result result) {
            this.category = category;
            this.result = result;
        }

        @Override
        public List<AuditLoggerName> getObject() {
            Map<Category, Set<AuditLoggerName>> audits = loggerRestClient.listAuditsByCategory();

            List<AuditLoggerName> object = new ArrayList<AuditLoggerName>();
            for (Enum<?> subcategory : category.getSubCategoryElements()) {
                AuditLoggerName auditLoggerName = new AuditLoggerName(category, subcategory, result);
                if (audits.containsKey(category) && audits.get(category).contains(auditLoggerName)) {
                    object.add(auditLoggerName);
                }
            }

            return object;
        }

        @Override
        public void setObject(final List<AuditLoggerName> object) {
            for (Enum<?> subcategory : category.getSubCategoryElements()) {
                AuditLoggerName auditLoggerName = new AuditLoggerName(category, subcategory, result);

                if (object.contains(auditLoggerName)) {
                    loggerRestClient.enableAudit(auditLoggerName);
                } else {
                    loggerRestClient.disableAudit(auditLoggerName);
                }
            }
        }

        @Override
        public void detach() {
            // Not needed.
        }
    }

    private class AuditCategoryPanel extends Panel {

        private static final long serialVersionUID = 1076251735476895253L;

        public AuditCategoryPanel(final String id, final Category category) {
            super(id);
            setOutputMarkupId(true);

            final CheckGroup<AuditLoggerName> successGroup = new CheckGroup<AuditLoggerName>("successGroup",
                    new AuditsByCategoryModel(category, Result.success));
            successGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

                private static final long serialVersionUID = -151291731388673682L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // Empty method: here only to let Model.setObject() be invoked.
                }
            });
            add(successGroup);
            MetaDataRoleAuthorizationStrategy.authorize(successGroup, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "enable"));
            MetaDataRoleAuthorizationStrategy.authorize(successGroup, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "disable"));

            final CheckGroupSelector successSelector = new CheckGroupSelector("successSelector", successGroup);
            add(successSelector);
            MetaDataRoleAuthorizationStrategy.authorize(successSelector, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "enable"));
            MetaDataRoleAuthorizationStrategy.authorize(successSelector, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "disable"));

            final CheckGroup<AuditLoggerName> failureGroup = new CheckGroup<AuditLoggerName>("failureGroup",
                    new AuditsByCategoryModel(category, Result.failure));
            failureGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

                private static final long serialVersionUID = -151291731388673682L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // Empty method: here only to let Model.setObject() be invoked.
                }
            });
            add(failureGroup);
            MetaDataRoleAuthorizationStrategy.authorize(failureGroup, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "enable"));
            MetaDataRoleAuthorizationStrategy.authorize(failureGroup, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "disable"));

            final CheckGroupSelector failureSelector = new CheckGroupSelector("failureSelector", failureGroup);
            add(failureSelector);
            MetaDataRoleAuthorizationStrategy.authorize(failureSelector, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "enable"));
            MetaDataRoleAuthorizationStrategy.authorize(failureSelector, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Audit", "disable"));

            ListView<Enum<?>> categoryView =
                    new ListView<Enum<?>>("categoryView", new ArrayList(category.getSubCategoryElements())) {

                        private static final long serialVersionUID = 4949588177564901031L;

                        @Override
                        protected void populateItem(final ListItem<Enum<?>> item) {
                            final Enum<?> subcategory = item.getModelObject();

                            item.add(new Label("subcategory", subcategory.name()));
                        }
                    };
            add(categoryView);

            ListView<Enum<?>> successView =
                    new ListView<Enum<?>>("successView", new ArrayList(category.getSubCategoryElements())) {

                        private static final long serialVersionUID = 4949588177564901031L;

                        @Override
                        protected void populateItem(final ListItem<Enum<?>> item) {
                            final Enum<?> subcategory = item.getModelObject();

                            final Check<AuditLoggerName> successCheck = new Check<AuditLoggerName>("successCheck",
                                    new Model<AuditLoggerName>(
                                    new AuditLoggerName(category, subcategory, Result.success)), successGroup);
                            item.add(successCheck);
                        }
                    };
            successGroup.add(successView);

            ListView<Enum<?>> failureView =
                    new ListView<Enum<?>>("failureView", new ArrayList(category.getSubCategoryElements())) {

                        private static final long serialVersionUID = 4949588177564901031L;

                        @Override
                        protected void populateItem(final ListItem<Enum<?>> item) {
                            final Enum<?> subcategory = item.getModelObject();

                            final Check<AuditLoggerName> failureCheck = new Check<AuditLoggerName>("failureCheck",
                                    new Model<AuditLoggerName>(
                                    new AuditLoggerName(category, subcategory, Result.failure)), failureGroup);
                            item.add(failureCheck);
                        }
                    };
            failureGroup.add(failureView);
        }
    }
}
