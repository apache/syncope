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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.Application;
import org.apache.wicket.PageReference;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobWidget extends BaseWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private static final Logger LOG = LoggerFactory.getLogger(JobWidget.class);

    private static final int ROWS = 5;

    private static List<JobTO> getAvailable(final SyncopeConsoleSession session) {
        List<JobTO> available = new ArrayList<>();

        if (session.owns(StandardEntitlement.NOTIFICATION_LIST)) {
            JobTO notificationJob = session.getService(NotificationService.class).getJob();
            if (notificationJob != null) {
                available.add(notificationJob);
            }
        }
        if (session.owns(StandardEntitlement.TASK_LIST)) {
            available.addAll(session.getService(TaskService.class).listJobs());
        }
        if (session.owns(StandardEntitlement.REPORT_LIST)) {
            available.addAll(session.getService(ReportService.class).listJobs());
        }

        return available;
    }

    private static List<ExecTO> getRecent(final SyncopeConsoleSession session) {
        List<ExecTO> recent = new ArrayList<>();

        if (session.owns(StandardEntitlement.TASK_LIST)) {
            recent.addAll(session.getService(ReportService.class).listRecentExecutions(10));
        }
        if (session.owns(StandardEntitlement.REPORT_LIST)) {
            recent.addAll(session.getService(TaskService.class).listRecentExecutions(10));
        }

        return recent;
    }

    private final List<JobTO> available;

    private AvailableJobsPanel availableJobsPanel;

    private final List<ExecTO> recent;

    private RecentExecPanel recentExecPanel;

    public JobWidget(final String id, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);

        available = getAvailable(SyncopeConsoleSession.get());
        recent = getRecent(SyncopeConsoleSession.get());

        add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef)));
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
        if (event.getPayload() instanceof WebSocketPushPayload) {
            WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
            if (wsEvent.getMessage() instanceof JobWidgetMessage) {
                List<JobTO> updatedAvailable = ((JobWidgetMessage) wsEvent.getMessage()).getUpdatedAvailable();
                if (!updatedAvailable.equals(available)) {
                    available.clear();
                    available.addAll(updatedAvailable);
                    if (availableJobsPanel != null) {
                        availableJobsPanel.modelChanged();
                        wsEvent.getHandler().add(availableJobsPanel);
                    }
                }

                List<ExecTO> updatedRecent = ((JobWidgetMessage) wsEvent.getMessage()).getUpdatedRecent();
                if (!updatedRecent.equals(recent)) {
                    recent.clear();
                    recent.addAll(updatedRecent);
                    if (recentExecPanel != null) {
                        recentExecPanel.modelChanged();
                        wsEvent.getHandler().add(recentExecPanel);
                    }
                }
            }
        } else if (event.getPayload() instanceof JobActionPanel.JobActionPayload) {
            available.clear();
            available.addAll(getAvailable(SyncopeConsoleSession.get()));
            availableJobsPanel.modelChanged();
            JobActionPanel.JobActionPayload.class.cast(event.getPayload()).getTarget().add(availableJobsPanel);
        }
    }

    private class AvailableJobsPanel extends DirectoryPanel<
        JobTO, JobTO, AvailableJobsProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        AvailableJobsPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<JobTO, JobTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<JobTO> newInstance(final String id, final boolean wizardInModal) {
                    throw new UnsupportedOperationException();
                }
            }.disableCheckBoxes().hidePaginator());

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
        protected Collection<ActionLink.ActionType> getBulkActions() {
            return Collections.<ActionLink.ActionType>emptyList();
        }

        @Override
        protected List<IColumn<JobTO, String>> getColumns() {
            List<IColumn<JobTO, String>> columns = new ArrayList<>();

            columns.add(new PropertyColumn<JobTO, String>(new ResourceModel("refDesc"), "refDesc", "refDesc"));

            columns.add(new AbstractColumn<JobTO, String>(new Model<>(""), "running") {

                private static final long serialVersionUID = -4008579357070833846L;

                @Override
                public void populateItem(
                        final Item<ICellPopulator<JobTO>> cellItem,
                        final String componentId,
                        final IModel<JobTO> rowModel) {

                    JobTO jobTO = rowModel.getObject();
                    JobActionPanel panel = new JobActionPanel(componentId, jobTO, JobWidget.this);
                    MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.ENABLE,
                            String.format("%s,%s",
                                    StandardEntitlement.TASK_EXECUTE,
                                    StandardEntitlement.REPORT_EXECUTE));
                    cellItem.add(panel);
                }

                @Override
                public String getCssClass() {
                    return "col-xs-1";
                }

            });

            columns.add(new BooleanPropertyColumn<JobTO>(new ResourceModel("scheduled"), "scheduled", "scheduled"));

            columns.add(new DatePropertyColumn<JobTO>(new ResourceModel("start"), "start", "start"));

            columns.add(new PropertyColumn<JobTO, String>(new ResourceModel("status"), "status", "status"));

            return columns;
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

    private class RecentExecPanel extends DirectoryPanel<
        ExecTO, ExecTO, RecentExecProvider, BaseRestClient> {

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
        protected Collection<ActionLink.ActionType> getBulkActions() {
            return Collections.<ActionLink.ActionType>emptyList();
        }

        @Override
        protected List<IColumn<ExecTO, String>> getColumns() {
            List<IColumn<ExecTO, String>> columns = new ArrayList<>();

            columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("refDesc"), "refDesc", "refDesc"));

            columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("start"), "start", "start"));

            columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("end"), "end", "end"));

            columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("status"), "status", "status"));

            return columns;
        }

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

    public static final class JobInfoUpdater implements Runnable {

        private final Application application;

        private final SyncopeConsoleSession session;

        private final IKey key;

        public JobInfoUpdater(final ConnectedMessage message) {
            this.application = message.getApplication();
            this.session = SyncopeConsoleSession.get();
            this.key = message.getKey();
        }

        @Override
        public void run() {
            try {
                ThreadContext.setApplication(application);
                ThreadContext.setSession(session);

                WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(application);
                WebSocketPushBroadcaster broadcaster = new WebSocketPushBroadcaster(webSocketSettings.
                        getConnectionRegistry());
                broadcaster.broadcast(
                        new ConnectedMessage(application, session.getId(), key),
                        new JobWidgetMessage(getAvailable(session), getRecent(session)));
            } catch (Throwable t) {
                LOG.error("Unexpected error while checking for updated Job info", t);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    private static class JobWidgetMessage implements IWebSocketPushMessage, Serializable {

        private static final long serialVersionUID = -824793424112532838L;

        private final List<JobTO> updatedAvailable;

        private final List<ExecTO> updatedRecent;

        JobWidgetMessage(final List<JobTO> updatedAvailable, final List<ExecTO> updatedRecent) {
            this.updatedAvailable = updatedAvailable;
            this.updatedRecent = updatedRecent;
        }

        public List<JobTO> getUpdatedAvailable() {
            return updatedAvailable;
        }

        public List<ExecTO> getUpdatedRecent() {
            return updatedRecent;
        }
    }
}
