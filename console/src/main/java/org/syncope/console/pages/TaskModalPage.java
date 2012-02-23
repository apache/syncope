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
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
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
import org.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.syncope.console.wicket.markup.html.form.ActionLink;
import org.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

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

        final ModalWindow taskExecMessageWin = new ModalWindow(
                "taskExecMessageWin");
        taskExecMessageWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        taskExecMessageWin.setCookieName("task-exec-message-win-modal");
        add(taskExecMessageWin);

        form = new Form("form");
        form.setModel(new CompoundPropertyModel(taskTO));
        add(form);

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
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskExecTO>> cellItem,
                    final String componentId,
                    final IModel<TaskExecTO> model) {

                final TaskExecTO taskExecutionTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        taskExecMessageWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ExecMessageModalPage(
                                                model.getObject().getMessage());
                                    }
                                });
                        taskExecMessageWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Tasks", "read",
                        StringUtils.hasText(model.getObject().getMessage()));

                panel.add(new ActionLink() {

                    private static final long serialVersionUID =
                            -3722207913631435501L;

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
                }, ActionLink.ActionType.DELETE, "Tasks", "delete");

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

        public TaskExecutionsProvider(final TaskTO taskTO) {
            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<TaskExecTO>(this);
        }

        @Override
        public Iterator<TaskExecTO> iterator(final int first,
                final int count) {

            List<TaskExecTO> list = taskTO.getExecutions();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return taskTO.getExecutions().size();
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
    }
}
