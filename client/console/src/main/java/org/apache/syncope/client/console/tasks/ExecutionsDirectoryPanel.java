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
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel.SecondLevel;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.console.tasks.ExecutionsDirectoryPanel.ExecProvider;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class ExecutionsDirectoryPanel
        extends DirectoryPanel<ExecTO, ExecTO, ExecProvider, ExecutionRestClient> {

    private static final long serialVersionUID = 2039393934721149162L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private final String key;

    public ExecutionsDirectoryPanel(
            final MultilevelPanel multiLevelPanelRef,
            final String key,
            final ExecutionRestClient executionRestClient,
            final PageReference pageRef) {
        this(null, multiLevelPanelRef, key, executionRestClient, pageRef);
    }

    public ExecutionsDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String key,
            final ExecutionRestClient executionRestClient,
            final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, false);

        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        restClient = executionRestClient;
        setOutputMarkupId(true);
        this.key = key;
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

        columns.add(new KeyPropertyColumn<ExecTO>(new ResourceModel("key"), "key", "key"));

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
                                next(new StringResourceModel("execution.view", ExecutionsDirectoryPanel.this, model).
                                        getObject(), new ExecMessage(model.getObject().getMessage()), target);
                            }
                        }, ActionLink.ActionType.VIEW, StandardEntitlement.TASK_READ).
                        add(new ActionLink<ExecTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                                try {
                                    restClient.deleteExecution(taskExecutionTO.getKey());
                                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                } catch (SyncopeClientException scce) {
                                   SyncopeConsoleSession.get().error(scce.getMessage());
                                }
                                target.add(ExecutionsDirectoryPanel.this);
                                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.TASK_DELETE);

                addFurtherAcions(panel, model);

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

    protected void addFurtherAcions(final ActionLinksPanel.Builder<ExecTO> panel, final IModel<ExecTO> model) {
    }

    @Override
    protected ExecProvider dataProvider() {
        return new ExecProvider(key, rows);
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

    protected class ExecProvider extends DirectoryDataProvider<ExecTO> {

        private static final long serialVersionUID = 8943636537120648961L;

        private final SortableDataProviderComparator<ExecTO> comparator;

        private final String taskKey;

        public ExecProvider(final String taskKey, final int paginatorRows) {
            super(paginatorRows);
            this.taskKey = taskKey;
            comparator = new SortableDataProviderComparator<>(this);
            setSort("end", SortOrder.DESCENDING);
        }

        public SortableDataProviderComparator<ExecTO> getComparator() {
            return comparator;
        }

        @Override
        public Iterator<ExecTO> iterator(final long first, final long count) {
            final int page = ((int) first / paginatorRows);
            List<ExecTO> list = restClient.listExecutions(taskKey, (page < 0 ? 0 : page) + 1, paginatorRows);
            Collections.sort(list, comparator);
            return list.iterator();
        }

        @Override
        public long size() {
            return restClient.countExecutions(taskKey);
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
