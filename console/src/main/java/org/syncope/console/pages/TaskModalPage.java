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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
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
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.StringUtils;
import org.syncope.client.to.NotificationTaskTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public abstract class TaskModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4110576026663173545L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    protected WebMarkupContainer profile;

    protected WebMarkupContainer executions;

    protected Form form;

    public TaskModalPage(final TaskTO taskTO) {
        final TaskTO actual = taskTO.getId() == 0
                ? taskTO
                : taskTO instanceof PropagationTaskTO
                ? taskRestClient.readPropagationTask(taskTO.getId())
                : taskTO instanceof NotificationTaskTO
                ? taskRestClient.readNotificationTask(taskTO.getId())
                : taskTO instanceof SyncTaskTO
                ? taskRestClient.readSchedTask(
                SyncTaskTO.class, taskTO.getId())
                : taskRestClient.readSchedTask(
                SchedTaskTO.class, taskTO.getId());

        taskTO.setExecutions(actual.getExecutions());

        final Label dialogContent =
                new Label("dialogContent", new Model<String>(""));

        add(dialogContent.setOutputMarkupId(true));

        form = new Form("form");
        add(form);

        form.setModel(new CompoundPropertyModel(taskTO));

        profile = new WebMarkupContainer("profile");
        profile.setOutputMarkupId(true);
        form.add(profile);

        executions = new WebMarkupContainer("executions");
        executions.setOutputMarkupId(true);
        form.add(executions);

        final Label idLabel = new Label("idLabel", new ResourceModel("id"));
        profile.add(idLabel);

        final AjaxTextFieldPanel id = new AjaxTextFieldPanel(
                "id", getString("id"),
                new PropertyModel<String>(taskTO, "id"), false);

        id.setEnabled(false);
        profile.add(id);

        final List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("id"), "id", "id"));

        columns.add(new DatePropertyColumn(
                new ResourceModel("startDate"), "startDate", "startDate"));

        columns.add(new DatePropertyColumn(
                new ResourceModel("endDate"), "endDate", "endDate"));

        columns.add(new PropertyColumn(
                new ResourceModel("status"), "status", "status"));

        columns.add(new AbstractColumn<TaskExecTO>(
                new ResourceModel("message")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskExecTO>> cellItem,
                    final String componentId,
                    final IModel<TaskExecTO> model) {

                AjaxLink messageLink = new IndicatingAjaxLink("link") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        dialogContent.setDefaultModelObject(
                                model.getObject().getMessage());

                        target.add(dialogContent);
                        target.appendJavaScript(
                                "jQuery('#dialog').dialog('open')");
                    }
                };

                messageLink.add(new Label("linkTitle",
                        getString("showMessage")));

                LinkPanel panel = new LinkPanel(componentId);
                panel.add(messageLink);

                cellItem.add(panel);

                if (!StringUtils.hasText(model.getObject().getMessage())) {
                    messageLink.setEnabled(false);
                }
            }
        });

        columns.add(new AbstractColumn<TaskExecTO>(
                new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskExecTO>> cellItem,
                    final String componentId,
                    final IModel<TaskExecTO> model) {

                final TaskExecTO taskExecutionTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            taskRestClient.deleteExecution(
                                    taskExecutionTO.getId());

                            taskTO.removeExecution(taskExecutionTO);

                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(feedbackPanel);
                        target.add(executions);
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Tasks", "delete"));

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("executionsTable", columns,
                new TaskExecutionsProvider(taskTO), 10);

        executions.add(table);
    }

    protected class TaskExecutionsProvider
            extends SortableDataProvider<TaskExecTO> {

        private static final long serialVersionUID = 8943636537120648961L;

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private TaskTO taskTO;

        public TaskExecutionsProvider(TaskTO taskTO) {
            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", SortOrder.ASCENDING);
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

                private static final long serialVersionUID =
                        7485475149862342421L;

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

    protected String getCronField(
            final FormComponent formComponent, final int field) {
        String cronField = null;

        if (formComponent != null) {
            cronField = getCronField(formComponent.getInput(), field);
        }

        return cronField;
    }

    protected String getCronField(
            final String cron, final int field) {
        String cronField = null;

        if (cron != null && !cron.isEmpty() && !"UNSCHEDULE".equals(cron)) {
            cronField = cron.split(" ")[field].trim();
        }

        return cronField;
    }

    protected String getCron(
            final FormComponent seconds,
            final FormComponent minutes,
            final FormComponent hours,
            final FormComponent daysOfMonth,
            final FormComponent months,
            final FormComponent daysOfWeek) {

        final StringBuilder cron = new StringBuilder();

        if (seconds != null && seconds.getInput() != null
                && minutes != null && minutes.getInput() != null
                && hours != null && hours.getInput() != null
                && daysOfMonth != null && daysOfMonth.getInput() != null
                && months != null && months.getInput() != null
                && daysOfWeek != null && daysOfWeek.getInput() != null) {

            cron.append(seconds.getInput().trim()).
                    append(" ").
                    append(minutes.getInput().trim()).
                    append(" ").
                    append(hours.getInput().trim()).
                    append(" ").
                    append(daysOfMonth.getInput().trim()).
                    append(" ").
                    append(months.getInput().trim()).
                    append(" ").
                    append(daysOfWeek.getInput().trim());
        }

        return cron.toString();
    }
}
