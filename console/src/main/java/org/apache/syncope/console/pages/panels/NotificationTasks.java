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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.NotificationTaskModalPage;
import org.apache.syncope.console.pages.Tasks;
import org.apache.syncope.console.pages.Tasks.TasksProvider;
import org.apache.syncope.console.rest.TaskRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.to.NotificationTaskTO;
import org.apache.syncope.to.TaskTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class NotificationTasks extends Panel {

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    private static final long serialVersionUID = 4984337552918213290L;

    @SpringBean
    private TaskRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private int paginatorRows;

    private WebMarkupContainer container;

    private boolean operationResult = false;

    private ModalWindow window;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    private final List<IColumn<TaskTO>> columns;

    private AjaxFallbackDefaultDataTable<TaskTO> table;

    public NotificationTasks(String id) {
        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        add(window = new ModalWindow("taskWin"));

        paginatorRows = prefMan.getPaginatorRows(getWebRequest(), Constants.PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS);

        columns = new ArrayList<IColumn<TaskTO>>();

        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("id"), "id", "id"));
        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("sender"), "sender", "sender"));
        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("recipients"), "recipients", "recipients"));
        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("subject"), "subject", "subject"));
        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("traceLevel"), "traceLevel", "traceLevel"));
        columns.add(new PropertyColumn<TaskTO>(new ResourceModel("latestExecStatus"), "latestExecStatus", "latestExecStatus"));

        columns.add(new AbstractColumn<TaskTO>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<TaskTO>> cellItem, final String componentId,
                    final IModel<TaskTO> model) {

                final TaskTO taskTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new NotificationTaskModalPage(taskTO);
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Tasks", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.startExecution(taskTO.getId(), false);
                            getSession().info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(getPage().get("feedback"));
                        target.add(container);
                    }
                }, ActionLink.ActionType.EXECUTE, "Tasks", "execute");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.delete(taskTO.getId(), NotificationTaskTO.class);
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.add(container);
                        target.add(getPage().get("feedback"));
                    }
                }, ActionLink.ActionType.DELETE, "Tasks", "delete");

                cellItem.add(panel);
            }
        });

        table = Tasks.updateTaskTable(
                columns,
                new TasksProvider<NotificationTaskTO>(restClient, paginatorRows, getId(), NotificationTaskTO.class),
                container,
                0);

        container.add(table);

        final AjaxLink reload = new IndicatingAjaxLink("reload") {
            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (target != null) {
                    target.add(table);
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

        container.add(reload);
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(container);
                if (operationResult) {
                    info(getString("operation_succeded"));
                    target.add(getPage().get("feedback"));
                    operationResult = false;
                }
            }
        });

        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName("view-task-win");

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(),
                        Constants.PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS, String.valueOf(paginatorRows));

                table = Tasks.updateTaskTable(
                        columns,
                        new TasksProvider<NotificationTaskTO>(restClient, paginatorRows, getId(), NotificationTaskTO.class),
                        container,
                        table == null ? 0 : table.getCurrentPage());

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }
}
