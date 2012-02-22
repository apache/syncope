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
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.pages.panels.NotificationTasks;
import org.syncope.console.pages.panels.SchedTasks;
import org.syncope.console.pages.panels.PropagationTasks;
import org.syncope.console.pages.panels.SyncTasks;
import org.syncope.console.rest.TaskRestClient;

public class Tasks extends BasePage {

    private static final long serialVersionUID = 5289215853622289061L;

    public Tasks(final PageParameters parameters) {
        super();

        add(new PropagationTasks("propagation"));
        add(new NotificationTasks("notification"));
        add(new SchedTasks("sched", getPageReference()));
        add(new SyncTasks("sync", getPageReference()));
    }

    @Override
    public void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        super.setWindowClosedCallback(window, container);
    }

    public static class TaskExecutionsProvider
            extends SortableDataProvider<TaskExecTO> {

        private static final long serialVersionUID = -5401263348984206145L;

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private TaskTO taskTO;

        public TaskExecutionsProvider(final TaskTO taskTO) {
            super();

            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<TaskExecTO>(this);
        }

        @Override
        public Iterator<TaskExecTO> iterator(final int first,
                final int count) {

            List<TaskExecTO> list = getTaskDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getTaskDB().size();
        }

        @Override
        public IModel<TaskExecTO> model(
                final TaskExecTO taskExecution) {

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

    public static class TasksProvider<T extends TaskTO>
            extends SortableDataProvider<T> {

        private static final long serialVersionUID = -20112718133295756L;

        private SortableDataProviderComparator<T> comparator;

        private TaskRestClient restClient;

        private int paginatorRows;

        private String id;

        private Class<T> reference;

        public TasksProvider(
                final TaskRestClient restClient,
                final int paginatorRows,
                final String id,
                final Class<T> reference) {

            super();

            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<T>(this);
            this.paginatorRows = paginatorRows;
            this.restClient = restClient;
            this.id = id;
            this.reference = reference;
        }

        @Override
        public Iterator<T> iterator(final int first, final int count) {
            final List<T> tasks = new ArrayList<T>();

            for (T task : (List<T>) restClient.listTasks(
                    reference, (first / paginatorRows) + 1, paginatorRows)) {

                if (task instanceof SchedTaskTO
                        && ((SchedTaskTO) task).getLastExec() == null
                        && task.getExecutions() != null
                        && !task.getExecutions().isEmpty()) {

                    Collections.sort(task.getExecutions(),
                            new Comparator<TaskExecTO>() {

                                @Override
                                public int compare(
                                        final TaskExecTO left,
                                        final TaskExecTO right) {

                                    return left.getStartDate().
                                            compareTo(right.getStartDate());
                                }
                            });

                    ((SchedTaskTO) task).setLastExec(
                            task.getExecutions().get(
                            task.getExecutions().size() - 1).getStartDate());
                }
                tasks.add(task);
            }

            Collections.sort(tasks, comparator);
            return tasks.iterator();
        }

        @Override
        public int size() {
            return restClient.count(id);
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<T>(object);
        }
    }
}
