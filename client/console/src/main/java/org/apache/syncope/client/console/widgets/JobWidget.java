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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AbstractSearchResultPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class JobWidget extends AbstractWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private static final int ROWS = 5;

    private final List<JobTO> available;

    private final List<ExecTO> recent;

    public JobWidget(final String id, final PageReference pageRef) {
        super(id);

        JobTO notificationJob = SyncopeConsoleSession.get().getService(NotificationService.class).getJob();

        List<JobTO> taskJobs = SyncopeConsoleSession.get().getService(TaskService.class).listJobs(10);

        List<JobTO> reportJobs = SyncopeConsoleSession.get().getService(ReportService.class).listJobs(10);

        available = new ArrayList<>();
        available.add(notificationJob);
        available.addAll(taskJobs);
        available.addAll(reportJobs);

        add(new AvailableJobsPanel("available", pageRef));

        List<ExecTO> reportExecs = SyncopeConsoleSession.get().
                getService(ReportService.class).listRecentExecutions(10);

        List<ExecTO> taskExecs = SyncopeConsoleSession.get().
                getService(TaskService.class).listRecentExecutions(10);

        recent = new ArrayList<>();
        recent.addAll(reportExecs);
        recent.addAll(taskExecs);

        add(new RecentExecPanel("recent", pageRef));
    }

    protected class AvailableJobsPanel extends AbstractSearchResultPanel<
        JobTO, JobTO, AvailableJobsProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        public AvailableJobsPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<JobTO, JobTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<JobTO> newInstance(final String id) {
                    return new AvailableJobsPanel(id, pageRef);
                }
            }.disableCheckBoxes());

            rows = ROWS;
            initResultTable();
            container.get("paginator").setVisible(false);
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

            columns.add(new PropertyColumn<JobTO, String>(new ResourceModel("reference"), "reference", "reference"));

            columns.add(new BooleanPropertyColumn<JobTO>(new ResourceModel("running"), "running", "running"));

            columns.add(new BooleanPropertyColumn<JobTO>(new ResourceModel("scheduled"), "scheduled", "scheduled"));

            columns.add(new DatePropertyColumn<JobTO>(new ResourceModel("start"), "start", "start"));

            columns.add(new PropertyColumn<JobTO, String>(new ResourceModel("status"), "status", "status"));

            return columns;
        }

    }

    protected final class AvailableJobsProvider extends SearchableDataProvider<JobTO> {

        private static final long serialVersionUID = 3191573490219472572L;

        private final SortableDataProviderComparator<JobTO> comparator;

        private AvailableJobsProvider() {
            super(ROWS);
            setSort("running", SortOrder.DESCENDING);
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

    protected class RecentExecPanel extends AbstractSearchResultPanel<
        ExecTO, ExecTO, RecentExecProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        public RecentExecPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<ExecTO, ExecTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<ExecTO> newInstance(final String id) {
                    return new RecentExecPanel(id, pageRef);
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

            columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("reference"), "reference", "reference"));

            columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("start"), "start", "start"));

            columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("end"), "end", "end"));

            columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("status"), "status", "status"));

            return columns;
        }

    }

    protected final class RecentExecProvider extends SearchableDataProvider<ExecTO> {

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
