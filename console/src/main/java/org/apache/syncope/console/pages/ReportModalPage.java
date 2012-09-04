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
import org.apache.syncope.client.report.ReportletConf;
import org.apache.syncope.client.to.ReportExecTO;
import org.apache.syncope.client.to.ReportTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.types.ReportExecStatus;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.springframework.util.StringUtils;
import org.apache.syncope.console.commons.DateFormatROModel;
import org.apache.syncope.console.commons.HttpResourceStream;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.markup.html.CrontabContainer;
import org.apache.syncope.console.rest.ReportRestClient;
import org.apache.syncope.console.wicket.ajax.form.AbstractAjaxDownloadBehavior;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.SingleColumnPalette;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;

public class ReportModalPage extends BaseModalPage {

    private static final long serialVersionUID = -5747628615211127644L;

    private static final String ADD_BUTTON_ID = "addButton";

    private static final String EDIT_BUTTON_ID = "editButton";

    private static final String REMOVE_BUTTON_ID = "removeButton";

    private static final int EXEC_EXPORT_WIN_HEIGHT = 100;

    private static final int EXEC_EXPORT_WIN_WIDTH = 400;

    private static final int REPORTLET_CONF_WIN_HEIGHT = 500;

    private static final int REPORTLET_CONF_WIN_WIDTH = 800;

    @SpringBean
    private ReportRestClient restClient;

    @SpringBean(name = "baseURL")
    protected String baseURL;

    private final ReportTO reportTO;

    private Form<ReportTO> form;

    private String exportFormat;

    private long exportExecId;

    private ReportletConf modalReportletConf;

    private String modalReportletConfOldName;

    public ReportModalPage(final ModalWindow window, final ReportTO reportTO, final PageReference callerPageRef) {
        this.reportTO = reportTO;

        form = new Form<ReportTO>("form");
        form.setModel(new CompoundPropertyModel(reportTO));
        add(form);

        setupProfile();
        setupExecutions();

        final CrontabContainer crontab = new CrontabContainer("crontab", new PropertyModel<String>(reportTO,
                "cronExpression"), reportTO.getCronExpression());
        form.add(crontab);

        final IndicatingAjaxButton submit = new IndicatingAjaxButton("apply", new ResourceModel("apply")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                ReportTO reportTO = (ReportTO) form.getModelObject();
                reportTO.setCronExpression(StringUtils.hasText(reportTO.getCronExpression())
                        ? crontab.getCronExpression()
                        : null);

                try {
                    if (reportTO.getId() > 0) {
                        restClient.update(reportTO);
                    } else {
                        restClient.create(reportTO);
                    }

                    ((BasePage) callerPageRef.getPage()).setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    LOG.error("While creating or updating report", e);
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        if (reportTO.getId() > 0) {
            MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, xmlRolesReader.getAllAllowedRoles("Reports",
                    "update"));
        } else {
            MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, xmlRolesReader.getAllAllowedRoles("Reports",
                    "create"));
        }

        form.add(submit);
    }

    private void setupProfile() {
        final WebMarkupContainer profile = new WebMarkupContainer("profile");
        profile.setOutputMarkupId(true);
        form.add(profile);

        final ModalWindow reportletConfWin = new ModalWindow("reportletConfWin");
        reportletConfWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        reportletConfWin.setCookieName("reportlet-conf-win-modal");
        reportletConfWin.setInitialHeight(REPORTLET_CONF_WIN_HEIGHT);
        reportletConfWin.setInitialWidth(REPORTLET_CONF_WIN_WIDTH);
        reportletConfWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                int foundIdx = -1;
                if (modalReportletConfOldName != null) {
                    for (int i = 0; i < reportTO.getReportletConfs().size() && foundIdx == -1; i++) {
                        if (reportTO.getReportletConfs().get(i).getName().equals(modalReportletConfOldName)) {
                            foundIdx = i;
                        }
                    }
                }
                if (modalReportletConf != null) {
                    if (foundIdx == -1) {
                        reportTO.addReportletConf(modalReportletConf);
                    } else {
                        reportTO.getReportletConfs().set(foundIdx, modalReportletConf);
                    }
                }

                target.add(profile);
            }
        });
        add(reportletConfWin);

        final Label idLabel = new Label("idLabel", new ResourceModel("id"));
        profile.add(idLabel);

        final AjaxTextFieldPanel id = new AjaxTextFieldPanel("id", getString("id"), new PropertyModel<String>(reportTO,
                "id"));
        id.setEnabled(false);
        profile.add(id);

        final Label nameLabel = new Label("nameLabel", new ResourceModel("name"));
        profile.add(nameLabel);

        final AjaxTextFieldPanel name = new AjaxTextFieldPanel("name", getString("name"), new PropertyModel<String>(
                reportTO, "name"));
        profile.add(name);

        final AjaxTextFieldPanel lastExec = new AjaxTextFieldPanel("lastExec", getString("lastExec"),
                new DateFormatROModel(new PropertyModel<String>(reportTO, "lastExec")));
        lastExec.setEnabled(false);
        profile.add(lastExec);

        final AjaxTextFieldPanel nextExec = new AjaxTextFieldPanel("nextExec", getString("nextExec"),
                new DateFormatROModel(new PropertyModel<String>(reportTO, "nextExec")));
        nextExec.setEnabled(false);
        profile.add(nextExec);

        final SingleColumnPalette<ReportletConf> reportlets = new SingleColumnPalette<ReportletConf>("reportlets",
                new PropertyModel<List<? extends ReportletConf>>(reportTO, "reportletConfs"),
                new IChoiceRenderer<ReportletConf>() {

                    private static final long serialVersionUID = 1048000918946220007L;

                    @Override
                    public Object getDisplayValue(final ReportletConf object) {
                        return object.getName();
                    }

                    @Override
                    public String getIdValue(final ReportletConf object, int index) {

                        return object.getName();
                    }
                }, 5, true);
        reportlets.setOutputMarkupId(true);
        reportlets.addRecordBehavior(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        reportlets.addRecordBehavior(new AjaxFormComponentUpdatingBehavior("onselect") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (target.getLastFocusedElementId() != null
                        && target.getLastFocusedElementId().startsWith(EDIT_BUTTON_ID)
                        && reportlets.getSelectedItem() != null) {

                    reportletConfWin.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            modalReportletConfOldName = reportlets.getSelectedItem().getName();
                            modalReportletConf = null;
                            return new ReportletConfModalPage(reportlets.getSelectedItem(), reportletConfWin,
                                    ReportModalPage.this.getPageReference());
                        }
                    });
                    reportletConfWin.show(target);
                }
            }
        });
        reportlets.setAddLink(new AjaxLink(ADD_BUTTON_ID) {

            private static final long serialVersionUID = -7978723352517770644L;
            
            @Override
            public void onClick(final AjaxRequestTarget target) {
                reportletConfWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        modalReportletConfOldName = null;
                        modalReportletConf = null;
                        return new ReportletConfModalPage(null, reportletConfWin,
                                ReportModalPage.this.getPageReference());
                    }
                });
                reportletConfWin.show(target);
            }
        });
        reportlets.setEditLink(new AjaxLink(EDIT_BUTTON_ID) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.focusComponent(this);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    private static final long serialVersionUID = -7927968187160354605L;

                    @Override
                    public CharSequence preDecorateScript(final CharSequence script) {
                        return script + reportlets.getEditOnClickJS();
                    }
                };
            }
        });
        reportlets.setRemoveLink(new AjaxLink(REMOVE_BUTTON_ID) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    private static final long serialVersionUID = -7927968187160354605L;

                    @Override
                    public CharSequence preDecorateScript(final CharSequence script) {
                        return "if (confirm('" + getString("confirmDelete") + "')) {" + script
                                + reportlets.getRemoveOnClickJS() + "}";
                    }
                };
            }
        });
        profile.add(reportlets);
    }

    private void setupExecutions() {
        final WebMarkupContainer executions = new WebMarkupContainer("executions");
        executions.setOutputMarkupId(true);
        form.add(executions);

        final ModalWindow reportExecMessageWin = new ModalWindow("reportExecMessageWin");
        reportExecMessageWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        reportExecMessageWin.setCookieName("report-exec-message-win-modal");
        add(reportExecMessageWin);

        final ModalWindow reportExecExportWin = new ModalWindow("reportExecExportWin");
        reportExecExportWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        reportExecExportWin.setCookieName("report-exec-export-win-modal");
        reportExecExportWin.setInitialHeight(EXEC_EXPORT_WIN_HEIGHT);
        reportExecExportWin.setInitialWidth(EXEC_EXPORT_WIN_WIDTH);
        reportExecExportWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                AjaxExportDownloadBehavior behavior = new AjaxExportDownloadBehavior(ReportModalPage.this.exportFormat,
                        ReportModalPage.this.exportExecId);
                executions.add(behavior);
                behavior.initiate(target);
            }
        });
        add(reportExecExportWin);

        final List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("id"), "id", "id"));
        columns.add(new DatePropertyColumn(new ResourceModel("startDate"), "startDate", "startDate"));
        columns.add(new DatePropertyColumn(new ResourceModel("endDate"), "endDate", "endDate"));
        columns.add(new PropertyColumn(new ResourceModel("status"), "status", "status"));
        columns.add(new AbstractColumn<ReportExecTO>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<ReportExecTO>> cellItem, final String componentId,
                    final IModel<ReportExecTO> model) {

                final ReportExecTO taskExecutionTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        reportExecMessageWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ExecMessageModalPage(model.getObject().getMessage());
                            }
                        });
                        reportExecMessageWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Reports", "read", StringUtils.hasText(model.getObject().getMessage()));

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        reportExecExportWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                ReportModalPage.this.exportExecId = model.getObject().getId();
                                return new ReportExecResultDownloadModalPage(reportExecExportWin, ReportModalPage.this.
                                        getPageReference());
                            }
                        });
                        reportExecExportWin.show(target);
                    }
                }, ActionLink.ActionType.EXPORT, "Reports", "read", ReportExecStatus.SUCCESS.name().equals(
                        model.getObject().getStatus()));

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.deleteExecution(taskExecutionTO.getId());

                            reportTO.removeExecution(taskExecutionTO);

                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(feedbackPanel);
                        target.add(executions);
                    }
                }, ActionLink.ActionType.DELETE, "Reports", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("executionsTable", columns,
                new ReportExecutionsProvider(reportTO), 10);
        executions.add(table);
    }

    public void setExportFormat(final String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setModalReportletConf(final ReportletConf modalReportletConf) {
        this.modalReportletConf = modalReportletConf;
    }

    private static class ReportExecutionsProvider extends SortableDataProvider<ReportExecTO> {

        private static final long serialVersionUID = 2118096121691420539L;

        private SortableDataProviderComparator<ReportExecTO> comparator;

        private ReportTO reportTO;

        public ReportExecutionsProvider(final ReportTO reportTO) {
            this.reportTO = reportTO;
            setSort("startDate", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<ReportExecTO>(this);
        }

        @Override
        public Iterator<ReportExecTO> iterator(final int first, final int count) {

            List<ReportExecTO> list = reportTO.getExecutions();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return reportTO.getExecutions().size();
        }

        @Override
        public IModel<ReportExecTO> model(final ReportExecTO taskExecution) {

            return new AbstractReadOnlyModel<ReportExecTO>() {

                private static final long serialVersionUID = 7485475149862342421L;

                @Override
                public ReportExecTO getObject() {
                    return taskExecution;
                }
            };
        }
    }

    private class AjaxExportDownloadBehavior extends AbstractAjaxDownloadBehavior {

        private static final long serialVersionUID = 3109256773218160485L;

        private final String exportFormat;

        private final long exportExecId;

        private String url;

        private HttpResourceStream stream;

        public AjaxExportDownloadBehavior(final String exportFormat, final long exportExecId) {
            this.exportFormat = exportFormat;
            this.exportExecId = exportExecId;
        }

        private void createResourceStream() {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(baseURL).append("report/execution/export/").append(exportExecId);
            if (exportFormat != null) {
                urlBuilder.append("?fmt=").append(exportFormat);
            }

            if (this.url != null && this.url.equals(urlBuilder.toString())) {
                return;
            }
            this.url = urlBuilder.toString();

            try {
                stream = new HttpResourceStream(this.url);
            } catch (Exception e) {
                LOG.error("While contacting target URL", e);
            }
        }

        @Override
        protected String getFileName() {
            createResourceStream();
            return stream == null
                    ? null
                    : stream.getFilename();
        }

        @Override
        protected IResourceStream getResourceStream() {
            createResourceStream();
            return stream;
        }
    }
}
