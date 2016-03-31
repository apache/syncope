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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AbstractSearchResultPanel;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel.SecondLevel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.tasks.TaskExecutions.TaskExecProvider;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class TaskExecutions
        extends AbstractSearchResultPanel<ExecTO, ExecTO, TaskExecProvider, TaskRestClient> {

    private static final long serialVersionUID = 2039393934721149162L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    protected TaskRestClient taskRestClient = new TaskRestClient();

    private final AbstractTaskTO taskTO;

    public TaskExecutions(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final AbstractTaskTO taskTO,
            final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, false);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        restClient = taskRestClient;
        setOutputMarkupId(true);
        this.taskTO = taskTO;
        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<ExecTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    protected abstract void next(final String title, final SecondLevel secondLevel, final AjaxRequestTarget target);

    @Override
    protected List<IColumn<ExecTO, String>> getColumns() {
        final List<IColumn<ExecTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("key"), "key", "key"));

        columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("start"), "start", "start"));

        columns.add(new DatePropertyColumn<ExecTO>(new ResourceModel("end"), "end", "end"));

        columns.add(new PropertyColumn<ExecTO, String>(new ResourceModel("status"), "status", "status"));

        columns.add(new ActionColumn<ExecTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<ExecTO> getActions(final String componentId, final IModel<ExecTO> model) {
                final ExecTO taskExecutionTO = model.getObject();

                final ActionLinksPanel.Builder<ExecTO> panel = ActionLinksPanel.builder();

                panel.
                        add(new ActionLink<ExecTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                                next(new StringResourceModel("execution.view", TaskExecutions.this, model).getObject(),
                                        new ExecMessage(model.getObject().getMessage()), target);
                            }
                        }, ActionLink.ActionType.VIEW, StandardEntitlement.TASK_READ).
                        add(new ActionLink<ExecTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                                try {
                                    restClient.deleteExecution(taskExecutionTO.getKey());
                                    taskTO.getExecutions().remove(taskExecutionTO);
                                    info(getString(Constants.OPERATION_SUCCEEDED));
                                } catch (SyncopeClientException scce) {
                                    error(scce.getMessage());
                                }
                                target.add(TaskExecutions.this);
                                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.TASK_DELETE);

                return panel.build(componentId, model.getObject());
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
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
    protected TaskExecProvider dataProvider() {
        return new TaskExecProvider(taskTO.getKey(), rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_TASK_EXECS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionLink.ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionLink.ActionType.DELETE);
        return bulkActions;
    }

    protected class TaskExecProvider extends SearchableDataProvider<ExecTO> {

        private static final long serialVersionUID = 8943636537120648961L;

        private final SortableDataProviderComparator<ExecTO> comparator;

        private final Long taskId;

        public TaskExecProvider(final Long taskId, final int paginatorRows) {
            super(paginatorRows);
            this.taskId = taskId;
            comparator = new SortableDataProviderComparator<>(this);
        }

        public SortableDataProviderComparator<ExecTO> getComparator() {
            return comparator;
        }

        @Override
        public Iterator<ExecTO> iterator(final long first, final long count) {
            final int page = ((int) first / paginatorRows);
            List<ExecTO> list = taskRestClient.listExecutions(taskId, (page < 0 ? 0 : page) + 1, paginatorRows);
            Collections.sort(list, comparator);
            return list.iterator();
        }

        @Override
        public long size() {
            return taskRestClient.countExecutions(taskId);
        }

        @Override
        public IModel<ExecTO> model(final ExecTO taskExecution) {

            return new AbstractReadOnlyModel<ExecTO>() {

                private static final long serialVersionUID = 7485475149862342421L;

                @Override
                public ExecTO getObject() {
                    return taskExecution;
                }
            };
        }
    }
}
