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
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.NotificationTaskModalPage;
import org.apache.syncope.console.pages.Tasks;
import org.apache.syncope.console.pages.Tasks.TasksProvider;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.http.WebResponse;

public class NotificationTasks extends AbstractTasks {

    private static final long serialVersionUID = 4984337552918213290L;

    private int paginatorRows;

    private WebMarkupContainer container;

    private boolean operationResult = false;

    private ModalWindow window;

    private final List<IColumn<AbstractTaskTO, String>> columns;

    private AjaxDataTablePanel<AbstractTaskTO, String> table;

    public NotificationTasks(final String id, final PageReference pageRef) {
        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        add(window = new ModalWindow("taskWin"));

        paginatorRows = prefMan.getPaginatorRows(getWebRequest(), Constants.PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS);

        columns = new ArrayList<IColumn<AbstractTaskTO, String>>();

        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("id", this, null), "id", "id"));
        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("sender", this, null), "sender", "sender"));
        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("recipients", this, null), "recipients", "recipients"));
        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("subject", this, null), "subject", "subject"));
        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("traceLevel", this, null), "traceLevel", "traceLevel"));
        columns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("latestExecStatus", this, null), "latestExecStatus", "latestExecStatus"));

        columns.add(new ActionColumn<AbstractTaskTO, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel getActions(final String componentId, final IModel<AbstractTaskTO> model) {

                final AbstractTaskTO taskTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);

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
                }, ActionLink.ActionType.EDIT, TASKS);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.startExecution(taskTO.getId(), false);
                            getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scce) {
                            error(scce.getMessage());
                        }

                        target.add(getPage().get(Constants.FEEDBACK));
                        target.add(container);
                    }
                }, ActionLink.ActionType.EXECUTE, TASKS);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.delete(taskTO.getId(), NotificationTaskTO.class);
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scce) {
                            error(scce.getMessage());
                        }
                        target.add(container);
                        target.add(getPage().get(Constants.FEEDBACK));
                    }
                }, ActionLink.ActionType.DELETE, TASKS);

                return panel;
            }

            @Override
            public Component getHeader(String componentId) {
                final ActionLinksPanel panel = new ActionLinksPanel(componentId, new Model(), pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        if (target != null) {
                            target.add(table);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, TASKS, "list");

                return panel;
            }
        });

        table = Tasks.updateTaskTable(
                columns,
                new TasksProvider<NotificationTaskTO>(restClient, paginatorRows, getId(), NotificationTaskTO.class),
                container,
                0,
                pageRef,
                restClient);

        container.add(table);

        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(container);
                if (operationResult) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(getPage().get(Constants.FEEDBACK));
                    operationResult = false;
                }
            }
        });

        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName(VIEW_TASK_WIN_COOKIE_NAME);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(),
                        Constants.PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS, String.valueOf(paginatorRows));

                table = Tasks.updateTaskTable(
                        columns,
                        new TasksProvider<NotificationTaskTO>(restClient, paginatorRows, getId(),
                        NotificationTaskTO.class),
                        container,
                        table == null ? 0 : (int) table.getCurrentPage(),
                        pageRef,
                        restClient);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(container);
        }
    }
}
