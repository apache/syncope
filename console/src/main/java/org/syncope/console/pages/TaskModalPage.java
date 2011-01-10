/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.syncope.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class TaskModalPage extends BaseModalPage {

    private TextField id;

    private TextField accountId;

    private TextField resource;

    private Label dialogContent;

    @SpringBean
    private TaskRestClient taskRestClient;

    private WebMarkupContainer container;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public TaskModalPage(final BasePage basePage, final ModalWindow window,
            final TaskTO taskTO) {

        dialogContent = new Label("dialogContent", new Model<String>(""));

        add(dialogContent.setOutputMarkupId(true));

        final Form form = new Form("TaskForm");

        form.setModel(new CompoundPropertyModel(taskTO));

        id = new TextField("id");
        id.setEnabled(false);

        form.add(id);

        accountId = new TextField("accountId");
        accountId.setEnabled(false);

        form.add(accountId);

        resource = new TextField("resource");
        resource.setEnabled(false);

        form.add(resource);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model(getString("id")), "id", "id"));

        columns.add(new DatePropertyColumn(new Model(getString("startDate")),
                "startDate", "startDate", null));

        columns.add(new DatePropertyColumn(new Model(getString("endDate")),
                "endDate", "endDate", null));

        columns.add(new PropertyColumn(new Model(getString("status")),
                "status", "status"));

        columns.add(new AbstractColumn<TaskExecutionTO>(
                new Model<String>(getString("message"))) {

            public void populateItem(
                    Item<ICellPopulator<TaskExecutionTO>> cellItem,
                    String componentId, IModel<TaskExecutionTO> model) {

                final TaskExecutionTO taskExecutionTO = model.getObject();

                AjaxLink messageLink = new AjaxLink("link") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        dialogContent.setDefaultModelObject(
                                taskExecutionTO.getMessage());

                        target.addComponent(dialogContent);

                        target.appendJavascript("jQuery('#dialog')"
                                + ".dialog('open')");
                    }
                };

                messageLink.add(new Label("linkTitle",
                        getString("showMessage")));

                LinkPanel panel = new LinkPanel(componentId);
                panel.add(messageLink);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<TaskExecutionTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(
                    Item<ICellPopulator<TaskExecutionTO>> cellItem,
                    String componentId, IModel<TaskExecutionTO> model) {

                final TaskExecutionTO taskExecutionTO = model.getObject();

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        boolean res = false;

                        try {
                            res = taskRestClient.deleteTaskExecution(
                                    taskExecutionTO.getId());
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        if (res) {
                            info(getString("operation_succeded"));
                        }

                        target.addComponent(feedbackPanel);
                        target.addComponent(container);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {
                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };
                DeleteLinkPanel panel = new DeleteLinkPanel(componentId,
                        model);
                panel.add(deleteLink);

                String allowedRoles = null;

                allowedRoles = xmlRolesReader.getAllAllowedRoles("Tasks",
                        "delete");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("executionsTable", columns,
                new TaskExecutionsProvider(taskTO), 10);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        form.add(container);

        add(form);
    }

    class TaskExecutionsProvider extends SortableDataProvider<TaskExecutionTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        private TaskTO taskTO;

        public TaskExecutionsProvider(TaskTO taskTO) {
            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", true);
        }

        @Override
        public Iterator<TaskExecutionTO> iterator(int first, int count) {
            List<TaskExecutionTO> list = getTasksListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getTasksListDB().size();
        }

        @Override
        public IModel<TaskExecutionTO> model(
                final TaskExecutionTO taskExecution) {

            return new AbstractReadOnlyModel<TaskExecutionTO>() {

                @Override
                public TaskExecutionTO getObject() {
                    return taskExecution;
                }
            };
        }

        public List<TaskExecutionTO> getTasksListDB() {
            return taskTO.getExecutions();
        }

        class SortableDataProviderComparator implements
                Comparator<TaskExecutionTO>, Serializable {

            public int compare(final TaskExecutionTO o1,
                    final TaskExecutionTO o2) {
                PropertyModel<Comparable> model1 =
                        new PropertyModel<Comparable>(o1,
                        getSort().getProperty());
                PropertyModel<Comparable> model2 =
                        new PropertyModel<Comparable>(o2,
                        getSort().getProperty());

                int result = 1;

                if (model1.getObject() == null && model2.getObject() == null) {
                    result = 0;
                } else if (model1.getObject() == null) {
                    result = 1;
                } else if (model2.getObject() == null) {
                    result = -1;
                } else {
                    result = ((Comparable) model1.getObject()).compareTo(
                            model2.getObject());
                }

                result = getSort().isAscending() ? result : -result;

                return result;
            }
        }
    }

    /**
     * Format column's value as date string.
     */
    public class DatePropertyColumn<T> extends PropertyColumn<T> {

        private static final long serialVersionUID = 1L;

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
