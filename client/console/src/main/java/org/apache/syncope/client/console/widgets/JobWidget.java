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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ExecMessageModal;
import org.apache.syncope.client.console.reports.ReportWizardBuilder;
import org.apache.syncope.client.console.reports.ReportletDirectoryPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.tasks.SchedTaskWizardBuilder;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.time.Duration;

public class JobWidget extends BaseWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private static final int ROWS = 5;

    private final ActionLinksTogglePanel<JobTO> actionTogglePanel;

    private final BaseModal<Serializable> modal = new BaseModal<Serializable>("modal") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(false);
        }
    };

    private final BaseModal<Serializable> detailModal = new BaseModal<Serializable>("detailModal") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(true);
        }
    };

    private final BaseModal<ReportTO> reportModal = new BaseModal<ReportTO>("reportModal") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(false);
        }
    };

    private final NotificationRestClient notificationRestClient = new NotificationRestClient();

    private final TaskRestClient taskRestClient = new TaskRestClient();

    private final ReportRestClient reportRestClient = new ReportRestClient();

    private final WebMarkupContainer container;

    private final List<JobTO> available;

    private AvailableJobsPanel availableJobsPanel;

    private final List<ExecTO> recent;

    private RecentExecPanel recentExecPanel;

    public JobWidget(final String id, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);
        add(modal);
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
            }
        });

        add(detailModal);
        detailModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                detailModal.show(false);
            }
        });

        add(reportModal);
        reportModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                reportModal.show(false);
            }
        });

        reportModal.size(Modal.Size.Large);

        available = getUpdatedAvailable();
        recent = getUpdatedRecent();

        container = new WebMarkupContainer("jobContainer");
        container.add(new IndicatorAjaxTimerBehavior(Duration.seconds(10)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                List<JobTO> updatedAvailable = getUpdatedAvailable();
                if (!updatedAvailable.equals(available)) {
                    available.clear();
                    available.addAll(updatedAvailable);
                    if (availableJobsPanel != null) {
                        availableJobsPanel.modelChanged();
                        target.add(availableJobsPanel);
                    }
                }

                List<ExecTO> updatedRecent = getUpdatedRecent();
                if (!updatedRecent.equals(recent)) {
                    recent.clear();
                    recent.addAll(updatedRecent);
                    if (recentExecPanel != null) {
                        recentExecPanel.modelChanged();
                        target.add(recentExecPanel);
                    }
                }
            }
        });
        add(container);

        container.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef)));

        actionTogglePanel = new ActionLinksTogglePanel<>("actionTogglePanel", pageRef);
        add(actionTogglePanel);
    }

    private List<JobTO> getUpdatedAvailable() {
        List<JobTO> updatedAvailable = new ArrayList<>();

        if (SyncopeConsoleSession.get().owns(StandardEntitlement.NOTIFICATION_LIST)) {
            JobTO notificationJob = notificationRestClient.getJob();
            if (notificationJob != null) {
                updatedAvailable.add(notificationJob);
            }
        }
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.TASK_LIST)) {
            updatedAvailable.addAll(taskRestClient.listJobs());
        }
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.REPORT_LIST)) {
            updatedAvailable.addAll(reportRestClient.listJobs());
        }

        return updatedAvailable;
    }

    private List<ExecTO> getUpdatedRecent() {
        List<ExecTO> updatedRecent = new ArrayList<>();

        if (SyncopeConsoleSession.get().owns(StandardEntitlement.TASK_LIST)) {
            updatedRecent.addAll(taskRestClient.listRecentExecutions(10));
        }
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.REPORT_LIST)) {
            updatedRecent.addAll(reportRestClient.listRecentExecutions(10));
        }

        return updatedRecent;
    }

    private List<ITab> buildTabList(final PageReference pageRef) {
        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new ResourceModel("available")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                availableJobsPanel = new AvailableJobsPanel(panelId, pageRef);
                availableJobsPanel.setOutputMarkupId(true);
                return availableJobsPanel;
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("recent")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                recentExecPanel = new RecentExecPanel(panelId, pageRef);
                recentExecPanel.setOutputMarkupId(true);
                return recentExecPanel;
            }
        });

        return tabs;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof JobActionPanel.JobActionPayload) {
            available.clear();
            available.addAll(getUpdatedAvailable());
            availableJobsPanel.modelChanged();
            JobActionPanel.JobActionPayload.class.cast(event.getPayload()).getTarget().add(availableJobsPanel);
        }
    }

    private class AvailableJobsPanel extends DirectoryPanel<JobTO, JobTO, AvailableJobsProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        private final BaseModal<ReportTO> reportModal;

        private final BaseModal<Serializable> jobModal;

        AvailableJobsPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<JobTO, JobTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<JobTO> newInstance(final String id, final boolean wizardInModal) {
                    throw new UnsupportedOperationException();
                }
            }.disableCheckBoxes().hidePaginator());

            super.setTogglePanel(actionTogglePanel);

            this.reportModal = JobWidget.this.reportModal;
            setWindowClosedReloadCallback(reportModal);

            this.jobModal = JobWidget.this.modal;
            setWindowClosedReloadCallback(jobModal);

            rows = ROWS;
            initResultTable();
        }

        @Override
        protected AvailableJobsProvider dataProvider() {
            return new AvailableJobsProvider();
        }

        @Override
        protected String paginatorRowsKey() {
            return StringUtils.EMPTY;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return Collections.<ActionLink.ActionType>emptyList();
        }

        @Override
        protected List<IColumn<JobTO, String>> getColumns() {
            List<IColumn<JobTO, String>> columns = new ArrayList<>();

            columns.add(new PropertyColumn<>(new ResourceModel("refDesc"), "refDesc", "refDesc"));

            columns.add(new BooleanPropertyColumn<>(new ResourceModel("scheduled"), "scheduled", "scheduled"));

            columns.add(new DatePropertyColumn<>(new ResourceModel("start"), "start", "start"));

            columns.add(new AbstractColumn<JobTO, String>(new Model<>(""), "running") {

                private static final long serialVersionUID = -4008579357070833846L;

                @Override
                public void populateItem(
                        final Item<ICellPopulator<JobTO>> cellItem,
                        final String componentId,
                        final IModel<JobTO> rowModel) {

                    JobTO jobTO = rowModel.getObject();
                    JobActionPanel panel = new JobActionPanel(componentId, jobTO, true, JobWidget.this, pageRef);
                    MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.ENABLE,
                            String.format("%s,%s,%s,%s",
                                    StandardEntitlement.TASK_EXECUTE,
                                    StandardEntitlement.REPORT_EXECUTE,
                                    StandardEntitlement.TASK_UPDATE,
                                    StandardEntitlement.REPORT_UPDATE));
                    cellItem.add(panel);
                }

                @Override
                public String getCssClass() {
                    return "col-xs-1";
                }
            });

            return columns;
        }

        @Override
        protected ActionsPanel<JobTO> getActions(final IModel<JobTO> model) {
            final ActionsPanel<JobTO> panel = super.getActions(model);

            final JobTO jobTO = model.getObject();

            panel.add(new ActionLink<JobTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final JobTO ignore) {
                    switch (jobTO.getType()) {
                        case NOTIFICATION:
                            break;

                        case REPORT:
                            ReportTO reportTO = reportRestClient.read(jobTO.getRefKey());

                            ReportWizardBuilder rwb = new ReportWizardBuilder(reportTO, pageRef);
                            rwb.setEventSink(AvailableJobsPanel.this);

                            target.add(jobModal.setContent(rwb.build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT)));

                            jobModal.header(new StringResourceModel(
                                    "any.edit",
                                    AvailableJobsPanel.this,
                                    new Model<>(reportTO)));

                            jobModal.show(true);
                            break;

                        case TASK:
                            ProvisioningTaskTO schedTaskTO;
                            try {
                                schedTaskTO = taskRestClient.readTask(TaskType.PULL, jobTO.getRefKey());
                            } catch (Exception e) {
                                LOG.debug("Failed to read {} as {}, attempting {}",
                                        jobTO.getRefKey(), TaskType.PULL, TaskType.PUSH, e);
                                schedTaskTO = taskRestClient.readTask(TaskType.PUSH, jobTO.getRefKey());
                            }

                            SchedTaskWizardBuilder<ProvisioningTaskTO> swb =
                                    new SchedTaskWizardBuilder<>(schedTaskTO instanceof PullTaskTO
                                            ? TaskType.PULL : TaskType.PUSH, schedTaskTO, pageRef);
                            swb.setEventSink(AvailableJobsPanel.this);

                            target.add(jobModal.setContent(swb.build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT)));

                            jobModal.header(new StringResourceModel(
                                    "any.edit",
                                    AvailableJobsPanel.this,
                                    new Model<>(schedTaskTO)));

                            jobModal.show(true);
                            break;

                        default:
                            break;
                    }
                }

                @Override
                protected boolean statusCondition(final JobTO modelObject) {
                    return !(null != jobTO.getType() && JobType.NOTIFICATION.equals(jobTO.getType()));
                }
            }, ActionType.EDIT, StandardEntitlement.TASK_UPDATE);

            panel.add(new ActionLink<JobTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final JobTO ignore) {

                    if (null != jobTO.getType()) {
                        switch (jobTO.getType()) {

                            case NOTIFICATION:
                                break;

                            case REPORT:

                                final ReportTO reportTO = reportRestClient.read(jobTO.getRefKey());

                                target.add(AvailableJobsPanel.this.reportModal.setContent(
                                        new ReportletDirectoryPanel(reportModal, jobTO.getRefKey(), pageRef)));

                                MetaDataRoleAuthorizationStrategy.authorize(
                                        reportModal.getForm(),
                                        ENABLE, StandardEntitlement.REPORT_UPDATE);

                                reportModal.header(new StringResourceModel(
                                        "reportlet.conf", AvailableJobsPanel.this, new Model<>(reportTO)));

                                reportModal.show(true);

                                break;

                            case TASK:
                                break;

                            default:
                                break;
                        }
                    }
                }

                @Override
                protected boolean statusCondition(final JobTO modelObject) {
                    return !(null != jobTO.getType() && (JobType.TASK.equals(jobTO.getType())
                            || JobType.NOTIFICATION.equals(jobTO.getType())));
                }

            }, ActionType.COMPOSE, StandardEntitlement.TASK_UPDATE);

            panel.add(new ActionLink<JobTO>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final JobTO ignore) {
                    try {
                        if (null != jobTO.getType()) {
                            switch (jobTO.getType()) {

                                case NOTIFICATION:
                                    break;

                                case REPORT:
                                    reportRestClient.actionJob(jobTO.getRefKey(), JobAction.DELETE);
                                    break;

                                case TASK:
                                    taskRestClient.actionJob(jobTO.getRefKey(), JobAction.DELETE);
                                    break;

                                default:
                                    break;
                            }
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        }
                    } catch (SyncopeClientException e) {
                        LOG.error("While deleting object {}", jobTO.getRefKey(), e);
                        SyncopeConsoleSession.get().onException(e);
                    }
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }

                @Override
                protected boolean statusCondition(final JobTO modelObject) {
                    return (null != jobTO.getType()
                            && !JobType.NOTIFICATION.equals(jobTO.getType())
                            && (jobTO.isScheduled() && !jobTO.isRunning()));
                }
            }, ActionLink.ActionType.DELETE, StandardEntitlement.TASK_DELETE, true);

            return panel;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onEvent(final IEvent<?> event) {
            if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
                Optional<AjaxRequestTarget> target = ((AjaxWizard.NewItemEvent<?>) event.getPayload()).getTarget();

                if (target.isPresent()
                        && event.getPayload() instanceof AjaxWizard.NewItemCancelEvent
                        || event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {

                    jobModal.close(target.get());
                }
            }

            super.onEvent(event);
        }
    }

    protected final class AvailableJobsProvider extends DirectoryDataProvider<JobTO> {

        private static final long serialVersionUID = 3191573490219472572L;

        private final SortableDataProviderComparator<JobTO> comparator;

        private AvailableJobsProvider() {
            super(ROWS);
            setSort("type", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<JobTO> iterator(final long first, final long count) {
            Collections.sort(available, comparator);
            return available.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return available.size();
        }

        @Override
        public IModel<JobTO> model(final JobTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    private class RecentExecPanel
            extends DirectoryPanel<ExecTO, ExecTO, RecentExecPanel.RecentExecProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        RecentExecPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<ExecTO, ExecTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<ExecTO> newInstance(final String id, final boolean wizardInModal) {
                    throw new UnsupportedOperationException();
                }
            }.disableCheckBoxes().hidePaginator());

            rows = ROWS;
            initResultTable();
        }

        @Override
        protected RecentExecProvider dataProvider() {
            return new RecentExecProvider();
        }

        @Override
        protected String paginatorRowsKey() {
            return StringUtils.EMPTY;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return Collections.<ActionLink.ActionType>emptyList();
        }

        @Override
        protected List<IColumn<ExecTO, String>> getColumns() {
            List<IColumn<ExecTO, String>> columns = new ArrayList<>();

            columns.add(new PropertyColumn<>(new ResourceModel("refDesc"), "refDesc", "refDesc"));

            columns.add(new DatePropertyColumn<>(new ResourceModel("start"), "start", "start"));

            columns.add(new DatePropertyColumn<>(new ResourceModel("end"), "end", "end"));

            columns.add(new PropertyColumn<>(new ResourceModel("status"), "status", "status"));

            return columns;
        }

        @Override
        public ActionsPanel<ExecTO> getActions(final IModel<ExecTO> model) {
            final ActionsPanel<ExecTO> panel = super.getActions(model);

            panel.add(new ActionLink<ExecTO>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    detailModal.header(new StringResourceModel("execution.view", JobWidget.this, model));
                    detailModal.setContent(new ExecMessageModal(model.getObject().getMessage()));
                    detailModal.show(true);
                    target.add(detailModal);
                }
            }, ActionLink.ActionType.VIEW, StandardEntitlement.TASK_READ);
            return panel;
        }

        protected final class RecentExecProvider extends DirectoryDataProvider<ExecTO> {

            private static final long serialVersionUID = 2835707012690698633L;

            private final SortableDataProviderComparator<ExecTO> comparator;

            private RecentExecProvider() {
                super(ROWS);
                setSort("end", SortOrder.DESCENDING);
                comparator = new SortableDataProviderComparator<>(this);
            }

            @Override
            public Iterator<ExecTO> iterator(final long first, final long count) {
                Collections.sort(recent, comparator);
                return recent.subList((int) first, (int) first + (int) count).iterator();
            }

            @Override
            public long size() {
                return recent.size();
            }

            @Override
            public IModel<ExecTO> model(final ExecTO object) {
                return new CompoundPropertyModel<>(object);
            }
        }
    }
}
