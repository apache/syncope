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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.rest.TaskRestClient;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
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

        final ModalWindow taskExecMessageWin = new ModalWindow("taskExecMessageWin");
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

        final AjaxTextFieldPanel id = new AjaxTextFieldPanel("id", getString("id"), new PropertyModel<String>(taskTO,
                "id"));

        id.setEnabled(false);
        profile.add(id);
        
        final List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("id"), "id", "id"));

        columns.add(new DatePropertyColumn(new ResourceModel("startDate"), "startDate", "startDate"));

        columns.add(new DatePropertyColumn(new ResourceModel("endDate"), "endDate", "endDate"));

        columns.add(new PropertyColumn(new ResourceModel("status"), "status", "status"));

        columns.add(new AbstractColumn<TaskExecTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<TaskExecTO>> cellItem, final String componentId,
                    final IModel<TaskExecTO> model) {

                final TaskExecTO taskExecutionTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        taskExecMessageWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ExecMessageModalPage(model.getObject().getMessage());
                            }
                        });
                        taskExecMessageWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Tasks", "read", StringUtils.hasText(model.getObject().getMessage()));

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            taskRestClient.deleteExecution(taskExecutionTO.getId());

                            taskTO.removeExecution(taskExecutionTO);

                            info(getString("operation_succeeded"));
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

        final int paginatorRows = 10;
        final AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("executionsTable", columns,
                new TaskExecutionsProvider(getCurrentTaskExecution(taskTO)), paginatorRows);

        executions.add(table);

        final AjaxLink reload = new IndicatingAjaxLink("reload") {
            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (target != null) {
                    final AjaxFallbackDefaultDataTable currentTable = 
                            new AjaxFallbackDefaultDataTable("executionsTable", columns,
                            new TaskExecutionsProvider(getCurrentTaskExecution(taskTO)), paginatorRows);
                    currentTable.setOutputMarkupId(true);
                    target.add(currentTable);
                    executions.addOrReplace(currentTable);   
                }
            }
        };
        
        reload.add(new Behavior() {
            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {

                if (table.getRowCount() > paginatorRows) {
                    tag.remove("class");
                    tag.put("class", "settingsPosMultiPage");
                } else {
                    tag.remove("class");
                    tag.put("class", "settingsPos");
                }
            }
        });

        executions.addOrReplace(reload);
    }

    protected static class TaskExecutionsProvider extends SortableDataProvider<TaskExecTO, String> {

        private static final long serialVersionUID = 8943636537120648961L;

        private SortableDataProviderComparator<TaskExecTO> comparator;

        private TaskTO taskTO;

        public TaskExecutionsProvider(final TaskTO taskTO) {
            //Default sorting
            this.taskTO = taskTO;
            setSort("startDate", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<TaskExecTO>(this);
        }

        @Override
        public Iterator<TaskExecTO> iterator(final long first, final long count) {

            List<TaskExecTO> list = taskTO.getExecutions();

            Collections.sort(list, comparator);

            return list.subList((int)first, (int)first + (int)count).iterator();
        }

        @Override
        public long size() {
            return taskTO.getExecutions().size();
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
    }

    private TaskTO getCurrentTaskExecution(final TaskTO taskTO) {
        final TaskTO currentTask = taskTO.getId() == 0
                ? taskTO
                : taskTO instanceof PropagationTaskTO
                ? taskRestClient.readPropagationTask(taskTO.getId())
                : taskTO instanceof NotificationTaskTO
                ? taskRestClient.readNotificationTask(taskTO.getId())
                : taskTO instanceof SyncTaskTO
                ? taskRestClient.readSchedTask(SyncTaskTO.class, taskTO.getId())
                : taskRestClient.readSchedTask(SchedTaskTO.class, taskTO.getId());

        taskTO.setExecutions(currentTask.getExecutions());
        return taskTO;
    }
}
