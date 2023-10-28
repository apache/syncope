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
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel.SecondLevel;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.console.tasks.ExecutionsDirectoryPanel.ExecProvider;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class ExecutionsDirectoryPanel
        extends DirectoryPanel<ExecTO, ExecTO, ExecProvider, ExecutionRestClient> {

    private static final long serialVersionUID = 2039393934721149162L;

    protected final MultilevelPanel multiLevelPanelRef;

    protected final String key;

    public ExecutionsDirectoryPanel(
            final MultilevelPanel multiLevelPanelRef,
            final String key,
            final ExecutionRestClient executionRestClient,
            final PageReference pageRef) {

        super(MultilevelPanel.FIRST_LEVEL_ID, executionRestClient, pageRef, false);

        this.multiLevelPanelRef = multiLevelPanelRef;
        setOutputMarkupId(true);
        this.key = key;
        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<ExecTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(multiLevelPanelRef);
    }

    protected abstract void next(String title, SecondLevel secondLevel, AjaxRequestTarget target);

    @Override
    protected List<IColumn<ExecTO, String>> getColumns() {
        List<IColumn<ExecTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));

        columns.add(new DatePropertyColumn<>(new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<>(new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<>(new StringResourceModel("status", this), "status", "status"));

        columns.add(new PropertyColumn<>(new StringResourceModel("executor", this), "executor", "executor"));

        return columns;
    }

    @Override
    public ActionsPanel<ExecTO> getActions(final IModel<ExecTO> model) {
        ActionsPanel<ExecTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                ExecutionsDirectoryPanel.this.getTogglePanel().close(target);
                next(new StringResourceModel("execution.view", ExecutionsDirectoryPanel.this, model).
                        getObject(), new ExecMessage(model.getObject().getMessage()), target);
            }
        }, ActionLink.ActionType.VIEW, IdRepoEntitlement.TASK_READ);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                ExecutionsDirectoryPanel.this.getTogglePanel().close(target);
                try {
                    restClient.deleteExecution(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.TASK_DELETE, true);

        addFurtherActions(panel, model);

        return panel;
    }

    protected void addFurtherActions(final ActionsPanel<ExecTO> panel, final IModel<ExecTO> model) {
    }

    @Override
    protected ExecProvider dataProvider() {
        return new ExecProvider(key, rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_TASK_EXECS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        List<ActionLink.ActionType> batches = new ArrayList<>();
        batches.add(ActionLink.ActionType.DELETE);
        return batches;
    }

    protected class ExecProvider extends DirectoryDataProvider<ExecTO> {

        private static final long serialVersionUID = 8943636537120648961L;

        private final String taskKey;

        public ExecProvider(final String taskKey, final int paginatorRows) {
            super(paginatorRows);

            this.taskKey = taskKey;
            setSort("end", SortOrder.DESCENDING);
        }

        @Override
        public Iterator<ExecTO> iterator(final long first, final long count) {
            int page = (int) first / paginatorRows;
            return restClient.listExecutions(
                    taskKey, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }

        @Override
        public long size() {
            return restClient.countExecutions(taskKey);
        }

        @Override
        public IModel<ExecTO> model(final ExecTO taskExecution) {

            return new IModel<>() {

                private static final long serialVersionUID = 7485475149862342421L;

                @Override
                public ExecTO getObject() {
                    return taskExecution;
                }
            };
        }
    }
}
