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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

public abstract class LiveSyncTaskDirectoryPanel extends ProvisioningTaskDirectoryPanel<LiveSyncTaskTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    protected LiveSyncTaskDirectoryPanel(
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String resource,
            final PageReference pageRef) {

        super(restClient, baseModal, multiLevelPanelRef, TaskType.LIVE_SYNC, new LiveSyncTaskTO(), resource, pageRef);
    }

    @Override
    protected List<IColumn<LiveSyncTaskTO, String>> getTrailingFieldColumns() {
        List<IColumn<LiveSyncTaskTO, String>> columns = super.getTrailingFieldColumns();
        columns.removeIf(column -> column instanceof PropertyColumn<?, ?> propertyColumn
                && propertyColumn.getPropertyExpression().contains("Exec"));
        return columns;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Set.of();
    }

    @Override
    public ActionsPanel<LiveSyncTaskTO> getActions(final IModel<LiveSyncTaskTO> model) {
        ActionsPanel<LiveSyncTaskTO> panel = super.getActions(model);
        panel.getActions().removeIf(action -> action.getType() == ActionLink.ActionType.CLONE);
        return panel;
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_PULL_TASKS_PAGINATOR_ROWS;
    }

    @Override
    protected void onDelete(final AjaxRequestTarget target) {
        target.add(addAjaxLink);
    }

    @Override
    protected ProvisioningTasksProvider<LiveSyncTaskTO> dataProvider() {
        return new ProvisioningTasksProvider<>(TaskType.LIVE_SYNC, rows) {

            private static final long serialVersionUID = -8658004154067744714L;

            @Override
            public long size() {
                long size = super.size();
                addAjaxLink.setEnabled(size == 0);
                return size;
            }
        };
    }

    @Override
    protected void addFurtherActions(final ActionsPanel<LiveSyncTaskTO> panel, final IModel<LiveSyncTaskTO> model) {
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final LiveSyncTaskTO ignore) {
                LiveSyncTaskDirectoryPanel.this.getTogglePanel().close(target);
                templates.setTargetObject(model.getObject());
                templates.toggle(target, true);
            }
        }, ActionLink.ActionType.TEMPLATE, IdRepoEntitlement.TASK_UPDATE).disableIndicator();
    }
}
