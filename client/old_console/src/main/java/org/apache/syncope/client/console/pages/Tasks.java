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
package org.apache.syncope.client.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.NotificationTasks;
import org.apache.syncope.client.console.panels.PropagationTasks;
import org.apache.syncope.client.console.panels.PushTasksPanel;
import org.apache.syncope.client.console.panels.SchedTasks;
import org.apache.syncope.client.console.panels.SyncTasksPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public class Tasks extends BasePage {

    private static final long serialVersionUID = 5289215853622289061L;

    public Tasks() {
        super();

        add(new PropagationTasks("propagation", getPageReference()));
        add(new NotificationTasks("notification", getPageReference()));
        add(new SchedTasks("sched", getPageReference()));
        add(new SyncTasksPanel("sync", getPageReference()));
        add(new PushTasksPanel("push", getPageReference()));

        getPageReference();
    }

    @Override
    public void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {

        super.setWindowClosedCallback(window, container);
    }

    public static class TaskExecutionsProvider extends SortableDataProvider<TaskExecTO, String> {

        private static final long serialVersionUID = -5401263348984206145L;

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private AbstractTaskTO taskTO;

        public TaskExecutionsProvider(final AbstractTaskTO taskTO) {
            super();

            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<TaskExecTO>(this);
        }

        @Override
        public Iterator<TaskExecTO> iterator(final long first, final long count) {

            List<TaskExecTO> list = getTaskDB();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return getTaskDB().size();
        }

        @Override
        public IModel<TaskExecTO> model(final TaskExecTO taskExecution) {

            return new AbstractReadOnlyModel<TaskExecTO>() {

                private static final long serialVersionUID = 7485475149862342421L;

                @Override
                public TaskExecTO getObject() {
                    return taskExecution;
                }
            };
        }

        public List<TaskExecTO> getTaskDB() {
            return taskTO.getExecutions();
        }
    }

    public static class TasksProvider<T extends AbstractTaskTO> extends SortableDataProvider<T, String> {

        private static final long serialVersionUID = -20112718133295756L;

        private SortableDataProviderComparator<T> comparator;

        private TaskRestClient restClient;

        private int paginatorRows;

        private String id;

        private Class<T> reference;

        public TasksProvider(
                final TaskRestClient restClient, final int paginatorRows, final String id, final Class<T> reference) {

            super();

            //Default sorting
            setSort("key", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<>(this);
            this.paginatorRows = paginatorRows;
            this.restClient = restClient;
            this.id = id;
            this.reference = reference;
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            final List<T> tasks = new ArrayList<>();

            final int page = ((int) first / paginatorRows);

            for (T task : restClient.list(reference, (page < 0 ? 0 : page) + 1, paginatorRows, getSort())) {
                if (task instanceof SchedTaskTO && ((SchedTaskTO) task).getLastExec() == null
                        && task.getExecutions() != null && !task.getExecutions().isEmpty()) {

                    Collections.sort(task.getExecutions(), new Comparator<TaskExecTO>() {

                        @Override
                        public int compare(final TaskExecTO left, final TaskExecTO right) {
                            return left.getStartDate().compareTo(right.getStartDate());
                        }
                    });

                    ((SchedTaskTO) task).setLastExec(task.getExecutions().get(task.getExecutions().size() - 1).
                            getStartDate());
                }
                tasks.add(task);
            }

            Collections.sort(tasks, comparator);
            return tasks.iterator();
        }

        @Override
        public long size() {
            return restClient.count(id);
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    /**
     * Update task table.
     *
     * @param columns columns.
     * @param dataProvider data provider.
     * @param container container.
     * @param currentPage current page index.
     * @param pageRef page reference
     * @param restClient syncope base rest client
     * @return data table.
     */
    public static AjaxDataTablePanel<AbstractTaskTO, String> updateTaskTable(
            final List<IColumn<AbstractTaskTO, String>> columns,
            final TasksProvider<? extends AbstractTaskTO> dataProvider,
            final WebMarkupContainer container,
            final int currentPage,
            final PageReference pageRef,
            final BaseRestClient restClient) {

        @SuppressWarnings("unchecked")
        final AjaxDataTablePanel<AbstractTaskTO, String> table = new AjaxDataTablePanel<>(
                "datatable",
                columns,
                (ISortableDataProvider<AbstractTaskTO, String>) dataProvider,
                dataProvider.paginatorRows,
                Arrays.asList(new ActionLink.ActionType[] {
                    ActionLink.ActionType.DELETE, ActionLink.ActionType.DRYRUN, ActionLink.ActionType.EXECUTE }),
                restClient,
                "key",
                TASKS,
                pageRef);

        table.setCurrentPage(currentPage);
        table.setOutputMarkupId(true);

        container.addOrReplace(table);

        return table;
    }

    /**
     * IndicatorMarkupId behaviour is embedded in Tasks.html
     */
    @Override

    public String getAjaxIndicatorMarkupId() {
        return "";
    }
}
