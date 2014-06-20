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

import static org.apache.syncope.console.pages.panels.AbstractTasks.TASKS;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.to.PushTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.PushTaskModalPage;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class PushTasksPanel extends AbstractSyncTasksPanel<PushTaskTO> {

    private static final long serialVersionUID = -2492299671757861889L;

    public PushTasksPanel(final String id, final PageReference pageRef) {
        super(id, pageRef, PushTaskTO.class);
        initTasksTable();
    }

    @Override
    protected List<IColumn<AbstractTaskTO, String>> getColumns() {
        final List<IColumn<AbstractTaskTO, String>> pushTasksColumns = new ArrayList<IColumn<AbstractTaskTO, String>>();

        pushTasksColumns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("id", this, null), "id", "id"));
        pushTasksColumns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("name", this, null), "name", "name"));
        pushTasksColumns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("description", this, null), "description", "description"));
        pushTasksColumns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("resourceName", this, null), "resource", "resource"));
        pushTasksColumns.add(new DatePropertyColumn<AbstractTaskTO>(
                new StringResourceModel("lastExec", this, null), "lastExec", "lastExec"));
        pushTasksColumns.add(new DatePropertyColumn<AbstractTaskTO>(
                new StringResourceModel("nextExec", this, null), "nextExec", "nextExec"));
        pushTasksColumns.add(new PropertyColumn<AbstractTaskTO, String>(
                new StringResourceModel("latestExecStatus", this, null), "latestExecStatus", "latestExecStatus"));

        pushTasksColumns.add(
                new ActionColumn<AbstractTaskTO, String>(new StringResourceModel("actions", this, null, "")) {

                    private static final long serialVersionUID = 2054811145491901166L;

                    @Override
                    public ActionLinksPanel getActions(final String componentId, final IModel<AbstractTaskTO> model) {

                        final PushTaskTO taskTO = (PushTaskTO) model.getObject();

                        final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);

                        panel.add(new ActionLink() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {

                                window.setPageCreator(new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID = -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new PushTaskModalPage(window, taskTO, pageRef);
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

                                target.add(container);
                                ((NotificationPanel) getPage().get(Constants.FEEDBACK)).refresh(target);
                            }
                        }, ActionLink.ActionType.EXECUTE, TASKS);

                        panel.add(new ActionLink() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                try {
                                    restClient.startExecution(taskTO.getId(), true);
                                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                                } catch (SyncopeClientException scce) {
                                    error(scce.getMessage());
                                }

                                target.add(container);
                                ((NotificationPanel) getPage().get(Constants.FEEDBACK)).refresh(target);
                            }
                        }, ActionLink.ActionType.DRYRUN, TASKS);

                        panel.add(new ActionLink() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                try {
                                    restClient.delete(taskTO.getId(), SyncTaskTO.class);
                                    info(getString(Constants.OPERATION_SUCCEEDED));
                                } catch (SyncopeClientException scce) {
                                    error(scce.getMessage());
                                }
                                target.add(container);
                                ((NotificationPanel) getPage().get(Constants.FEEDBACK)).refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, TASKS);

                        return panel;
                    }

                    @Override
                    public Component getHeader(final String componentId) {
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

        return pushTasksColumns;

    }
}
