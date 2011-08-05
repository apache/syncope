/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import org.syncope.console.pages.panels.PropagationTasks;
import org.syncope.console.pages.panels.SyncTasks;
import org.syncope.console.pages.panels.GenericTasks;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.converters.DateConverter;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.console.SyncopeSession;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.commons.SelectOption;
import org.syncope.console.rest.TaskRestClient;

public class Tasks extends BasePage {

    public static final SelectOption[] CRON_TEMPLATES = new SelectOption[]{
        new SelectOption(
        "Unschedule", "UNSCHEDULE"),
        new SelectOption(
        "Fire at 12pm (noon) every day", "0 0 12 * * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every first day of the month", "0 0 0 1 * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every Last day of the month", "0 0 0 L * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every Monday", "0 0 0 ? * 2")
    };

    public Tasks(final PageParameters parameters) {
        super();

        add(new PropagationTasks("propagation"));
        add(new GenericTasks("sched"));
        add(new SyncTasks("sync"));
    }

    public static class TaskExecutionsProvider
            extends SortableDataProvider<TaskExecTO> {

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private TaskTO taskTO;

        public TaskExecutionsProvider(final TaskTO taskTO) {
            super();

            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", true);
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

    /**
     * Format column's value as date string.
     */
    public static class DatePropertyColumn<T> extends PropertyColumn<T> {

        private SimpleDateFormat formatter;

        public DatePropertyColumn(final IModel<String> displayModel,
                final String sortProperty,
                final String propertyExpression,
                final DateConverter converter) {

            super(displayModel, sortProperty, propertyExpression);

            String language = "en";
            if (SyncopeSession.get().getLocale() != null) {
                language = SyncopeSession.get().getLocale().getLanguage();
            }

            if ("it".equals(language)) {
                formatter = new SimpleDateFormat(Constants.ITALIAN_DATE_FORMAT);
            } else {
                formatter = new SimpleDateFormat(Constants.ENGLISH_DATE_FORMAT);
            }
        }

        @Override
        public void populateItem(final Item<ICellPopulator<T>> item,
                final String componentId, final IModel<T> rowModel) {

            IModel date = (IModel<Date>) createLabelModel(rowModel);

            String convertedDate = "";

            if (date.getObject() != null) {
                convertedDate = formatter.format(date.getObject());
                item.add(new Label(componentId, convertedDate));
            } else {
                item.add(new Label(componentId, convertedDate));
            }
        }
    }

    public static class TasksProvider<T extends SchedTaskTO>
            extends SortableDataProvider<T> {

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
            setSort("id", true);
            comparator = new SortableDataProviderComparator<T>(this);
            this.paginatorRows = paginatorRows;
            this.restClient = restClient;
            this.id = id;
            this.reference = reference;
        }

        @Override
        public Iterator<T> iterator(final int first, final int count) {

            final List<T> tasks = new ArrayList<T>();

            for (T task : (List<T>) restClient.listSchedTasks(
                    reference, (first / paginatorRows) + 1, count)) {

                if (task.getLastExec() == null
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

                    task.setLastExec(
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
        public IModel<SchedTaskTO> model(final SchedTaskTO object) {
            return new CompoundPropertyModel<SchedTaskTO>(object);
        }
    }
}
