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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AbstractSearchResultPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
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

    private final List<AbstractExecTO> recent;

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

        List<ReportExecTO> reportExecs = SyncopeConsoleSession.get().
                getService(ReportService.class).listRecentExecutions(10);

        List<TaskExecTO> taskExecs = SyncopeConsoleSession.get().
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
            final List<IColumn<JobTO, String>> columns = new ArrayList<>();

            for (Field field : JobTO.class.getDeclaredFields()) {
                if (field != null && !Modifier.isStatic(field.getModifiers())) {
                    final String fieldName = field.getName();
                    if (field.getType().isArray()
                            || Collection.class.isAssignableFrom(field.getType())
                            || Map.class.isAssignableFrom(field.getType())) {

                        columns.add(new PropertyColumn<JobTO, String>(
                                new ResourceModel(field.getName()), field.getName()));
                    } else {
                        columns.add(new PropertyColumn<JobTO, String>(
                                new ResourceModel(field.getName()), field.getName(), field.getName()) {

                            private static final long serialVersionUID = -6902459669035442212L;

                            @Override
                            public String getCssClass() {
                                String css = super.getCssClass();
                                if ("referenceKey".equals(fieldName)) {
                                    css = StringUtils.isBlank(css)
                                            ? "medium_fixedsize"
                                            : css + " medium_fixedsize";
                                }
                                return css;
                            }
                        });
                    }
                }
            }

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
        AbstractExecTO, AbstractExecTO, RecentExecProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        public RecentExecPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<AbstractExecTO, AbstractExecTO, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<AbstractExecTO> newInstance(final String id) {
                    return new RecentExecPanel(id, pageRef);
                }
            }.disableCheckBoxes());

            rows = ROWS;
            initResultTable();
            container.get("paginator").setVisible(false);
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
        protected List<IColumn<AbstractExecTO, String>> getColumns() {
            final List<IColumn<AbstractExecTO, String>> columns = new ArrayList<>();

            for (Field field : AbstractExecTO.class.getDeclaredFields()) {
                if (field != null && !Modifier.isStatic(field.getModifiers())) {
                    final String fieldName = field.getName();
                    if (field.getType().isArray()
                            || Collection.class.isAssignableFrom(field.getType())
                            || Map.class.isAssignableFrom(field.getType())) {

                        columns.add(new PropertyColumn<AbstractExecTO, String>(
                                new ResourceModel(field.getName()), field.getName()));
                    } else {
                        columns.add(new PropertyColumn<AbstractExecTO, String>(
                                new ResourceModel(field.getName()), field.getName(), field.getName()) {

                            private static final long serialVersionUID = -6902459669035442212L;

                            @Override
                            public String getCssClass() {
                                String css = super.getCssClass();
                                if ("key".equals(fieldName)) {
                                    css = StringUtils.isBlank(css)
                                            ? "medium_fixedsize"
                                            : css + " medium_fixedsize";
                                }
                                return css;
                            }
                        });
                    }
                }
            }

            return columns;
        }

    }

    protected final class RecentExecProvider extends SearchableDataProvider<AbstractExecTO> {

        private static final long serialVersionUID = 2835707012690698633L;

        private final SortableDataProviderComparator<AbstractExecTO> comparator;

        private RecentExecProvider() {
            super(ROWS);
            setSort("end", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AbstractExecTO> iterator(final long first, final long count) {
            Collections.sort(recent, comparator);
            return recent.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return recent.size();
        }

        @Override
        public IModel<AbstractExecTO> model(final AbstractExecTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

}
