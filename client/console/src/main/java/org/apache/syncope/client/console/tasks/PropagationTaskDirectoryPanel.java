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
package org.apache.syncope.client.console.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.TaskDataProvider;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Tasks page.
 */
public abstract class PropagationTaskDirectoryPanel
        extends TaskDirectoryPanel<PropagationTaskTO> implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final String resource;

    protected PropagationTaskDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String resource,
            final PageReference pageRef) {
        super(baseModal, multiLevelPanelRef, pageRef);
        this.resource = resource;
        initResultTable();
    }

    @Override
    protected List<IColumn<PropagationTaskTO, String>> getColumns() {
        final List<IColumn<PropagationTaskTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<PropagationTaskTO>(
                new StringResourceModel("key", this, null), "key", "key"));

        columns.add(new PropertyColumn<PropagationTaskTO, String>(new StringResourceModel(
                "operation", this, null), "operation", "operation"));

        columns.add(new PropertyColumn<PropagationTaskTO, String>(
                new StringResourceModel("anyTypeKind", this, null), "anyTypeKind", "anyTypeKind"));

        columns.add(new PropertyColumn<PropagationTaskTO, String>(
                new StringResourceModel("anyKey", this, null), "anyKey", "anyKey"));

        columns.add(new PropertyColumn<PropagationTaskTO, String>(
                new StringResourceModel("connObjectKey", this, null), "connObjectKey", "connObjectKey"));

        columns.add(new DatePropertyColumn<PropagationTaskTO>(
                new StringResourceModel("start", this, null), "start", "start"));

        columns.add(new DatePropertyColumn<PropagationTaskTO>(
                new StringResourceModel("end", this, null), "end", "end"));

        columns.add(new PropertyColumn<PropagationTaskTO, String>(
                new StringResourceModel("latestExecStatus", this, null), "latestExecStatus", "latestExecStatus"));

        columns.add(new ActionColumn<PropagationTaskTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel<PropagationTaskTO> getActions(
                    final String componentId, final IModel<PropagationTaskTO> model) {

                final PropagationTaskTO taskTO = model.getObject();

                final ActionLinksPanel<PropagationTaskTO> panel = ActionLinksPanel.<PropagationTaskTO>builder().
                        add(new ActionLink<PropagationTaskTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                                viewTask(taskTO, target);
                            }
                        }, ActionLink.ActionType.VIEW, StandardEntitlement.TASK_READ).
                        add(new ActionLink<PropagationTaskTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                                try {
                                    restClient.startExecution(taskTO.getKey(), new Date());
                                    info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (SyncopeClientException e) {
                                    error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                    LOG.error("While running {}", taskTO.getKey(), e);
                                }
                                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.EXECUTE, StandardEntitlement.TASK_EXECUTE).
                        add(new ActionLink<PropagationTaskTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                                try {
                                    restClient.delete(taskTO.getKey(), PropagationTaskTO.class);
                                    info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (SyncopeClientException e) {
                                    LOG.error("While deleting {}", taskTO.getKey(), e);
                                    error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                }
                                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.TASK_DELETE).build(componentId);

                return panel;
            }

            @Override
            public ActionLinksPanel<PropagationTaskTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<PropagationTaskTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<PropagationTaskTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final PropagationTaskTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.DELETE);
        bulkActions.add(ActionType.EXECUTE);
        return bulkActions;
    }

    @Override
    protected PropagationTasksProvider dataProvider() {
        return new PropagationTasksProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_PROPAGATION_TASKS_PAGINATOR_ROWS;
    }

    protected class PropagationTasksProvider extends TaskDataProvider<PropagationTaskTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        public PropagationTasksProvider(final int paginatorRows) {
            super(paginatorRows, TaskType.PROPAGATION, restClient);
        }

        @Override
        public Iterator<PropagationTaskTO> iterator(final long first, final long count) {
            final int page = ((int) first / paginatorRows);

            final List<PropagationTaskTO> tasks = restClient.listPropagationTasks(
                    resource, (page < 0 ? 0 : page) + 1, paginatorRows, getSort());

            Collections.sort(tasks, getComparator());
            return tasks.iterator();
        }
    }
}
