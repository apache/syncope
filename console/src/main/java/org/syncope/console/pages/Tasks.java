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

import java.text.SimpleDateFormat;
import java.util.Collections;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.converters.DateConverter;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.commons.SelectOption;

public class Tasks extends BasePage {

    public final static SelectOption[] cronTemplates = new SelectOption[]{
        new SelectOption(
        "Unschedule", "UNSCHEDULE"),
        new SelectOption(
        "Fire at 12pm (noon) every day", "0 0 12 * * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every first day of the month", "0 0 0 1 * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every Last day of the month", "0 0 0 L * ?"),
        new SelectOption(
        "Fire at 12am (midnight) every Monday", "0 0 0 ? ? 2")
    };

    public Tasks(final PageParameters parameters) {
        add(new PropagationTasks("propagation"));
        add(new GenericTasks("sched"));
        add(new SyncTasks("sync"));
    }

    public class TaskExecutionsProvider
            extends SortableDataProvider<TaskExecTO> {

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private TaskTO taskTO;

        public TaskExecutionsProvider(TaskTO taskTO) {
            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", true);
            comparator =
                    new SortableDataProviderComparator<TaskExecTO>(this);
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
    public class DatePropertyColumn<T> extends PropertyColumn<T> {

        private SimpleDateFormat formatter;

        public DatePropertyColumn(
                IModel<String> displayModel, String sortProperty,
                String propertyExpression, DateConverter converter) {
            super(displayModel, sortProperty, propertyExpression);

            String language = "en";
            if (getSession().getLocale() != null) {
                language = getSession().getLocale().getLanguage();
            }

            if ("it".equals(language)) {
                formatter = new SimpleDateFormat(Constants.ITALIAN_DATE_FORMAT);
            } else {
                formatter = new SimpleDateFormat(Constants.ENGLISH_DATE_FORMAT);
            }
        }

        @Override
        public void populateItem(
                Item<ICellPopulator<T>> item, String componentId,
                IModel<T> rowModel) {
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
}
