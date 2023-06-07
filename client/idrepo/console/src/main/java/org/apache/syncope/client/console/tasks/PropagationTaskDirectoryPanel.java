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
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.TaskDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public abstract class PropagationTaskDirectoryPanel
        extends TaskDirectoryPanel<PropagationTaskTO> implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final String resource;

    protected PropagationTaskDirectoryPanel(
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String resource,
            final PageReference pageRef) {

        super(restClient, baseModal, multiLevelPanelRef, pageRef, false);
        this.resource = resource;
        initResultTable();
    }

    @Override
    protected List<IColumn<PropagationTaskTO, String>> getColumns() {
        List<IColumn<PropagationTaskTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("operation", this), "operation", "operation"));

        if (resource == null) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("resource", this), "resource", "resource"));
        } else {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("anyTypeKind", this), "anyTypeKind", "anyTypeKind") {

                private static final long serialVersionUID = 3344577098912281394L;

                @Override
                public IModel<?> getDataModel(final IModel<PropagationTaskTO> rowModel) {
                    if (rowModel.getObject().getAnyTypeKind() == null) {
                        return Model.of(SyncopeConstants.REALM_ANYTYPE);
                    } else {
                        return super.getDataModel(rowModel);
                    }
                }
            });
        }

        columns.add(new PropertyColumn<>(
                new StringResourceModel("entityKey", this), "entityKey", "entityKey"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("connObjectKey", this), "connObjectKey", "connObjectKey"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        return columns;
    }

    @Override
    public ActionsPanel<PropagationTaskTO> getActions(final IModel<PropagationTaskTO> model) {
        final ActionsPanel<PropagationTaskTO> panel = super.getActions(model);
        final PropagationTaskTO taskTO = model.getObject();

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                PropagationTaskDirectoryPanel.this.getTogglePanel().close(target);
                viewTaskExecs(taskTO, target);
            }
        }, ActionLink.ActionType.VIEW_EXECUTIONS, IdRepoEntitlement.TASK_READ);

        // [SYNCOPE-1115] - Display attributes for propagation tasks
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 9206257220553949594L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                PropagationTaskDirectoryPanel.this.getTogglePanel().close(target);
                viewTaskDetails(modelObject, target);
            }
        }, ActionLink.ActionType.VIEW_DETAILS, IdRepoEntitlement.TASK_READ);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                try {
                    restClient.startExecution(taskTO.getKey(), null);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While running {}", taskTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.EXECUTE, IdRepoEntitlement.TASK_EXECUTE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PropagationTaskTO modelObject) {
                try {
                    restClient.delete(TaskType.PROPAGATION, taskTO.getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                    PropagationTaskDirectoryPanel.this.getTogglePanel().close(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", taskTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.TASK_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        List<ActionType> batches = new ArrayList<>();
        batches.add(ActionType.DELETE);
        batches.add(ActionType.EXECUTE);
        return batches;
    }

    @Override
    protected PropagationTasksProvider dataProvider() {
        return new PropagationTasksProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_PROPAGATION_TASKS_PAGINATOR_ROWS;
    }

    protected class PropagationTasksProvider extends TaskDataProvider<PropagationTaskTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        public PropagationTasksProvider(final int paginatorRows) {
            super(paginatorRows, TaskType.PROPAGATION);
        }

        @Override
        public long size() {
            return restClient.count(resource, TaskType.PROPAGATION);
        }

        @Override
        public Iterator<PropagationTaskTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.listPropagationTasks(
                    resource, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }

    protected abstract void viewTaskDetails(PropagationTaskTO taskTO, AjaxRequestTarget target);
}
