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
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class NotificationTaskDirectoryPanel
        extends TaskDirectoryPanel<NotificationTaskTO> implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final String notification;

    private final AnyTypeKind anyTypeKind;

    private final String entityKey;

    protected NotificationTaskDirectoryPanel(
            final TaskRestClient restClient,
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef) {

        super(restClient, null, multiLevelPanelRef, pageRef, false);
        this.notification = notification;
        this.anyTypeKind = anyTypeKind;
        this.entityKey = entityKey;
        initResultTable();
    }

    protected abstract void viewMailBody(MailTemplateFormat format, String content, AjaxRequestTarget target);

    @Override
    protected List<IColumn<NotificationTaskTO, String>> getColumns() {
        List<IColumn<NotificationTaskTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("sender", this), "sender", "sender"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("subject", this), "subject", "subject"));

        columns.add(new CollectionPropertyColumn<>(
                new StringResourceModel("recipients", this), "recipients"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        return columns;
    }

    @Override
    public ActionsPanel<NotificationTaskTO> getActions(final IModel<NotificationTaskTO> model) {
        final ActionsPanel<NotificationTaskTO> panel = super.getActions(model);
        final NotificationTaskTO taskTO = model.getObject();

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTaskTO modelObject) {
                viewTaskExecs(taskTO, target);
            }
        }, ActionLink.ActionType.VIEW, IdRepoEntitlement.TASK_READ);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTaskTO modelObject) {
                viewMailBody(MailTemplateFormat.TEXT, taskTO.getTextBody(), target);
            }
        }, ActionLink.ActionType.TEXT, IdRepoEntitlement.TASK_READ);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTaskTO modelObject) {
                viewMailBody(MailTemplateFormat.HTML, taskTO.getHtmlBody(), target);
            }
        }, ActionLink.ActionType.HTML, IdRepoEntitlement.TASK_READ);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTaskTO modelObject) {
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
            public void onClick(final AjaxRequestTarget target, final NotificationTaskTO modelObject) {
                try {
                    restClient.delete(TaskType.NOTIFICATION, taskTO.getKey());
                    updateResultTable(target);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
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
    protected NotificationTasksProvider dataProvider() {
        return new NotificationTasksProvider(notification, anyTypeKind, entityKey, rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS;
    }

    protected class NotificationTasksProvider extends TaskDataProvider<NotificationTaskTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final String notification;

        private final AnyTypeKind anyTypeKind;

        private final String entityKey;

        public NotificationTasksProvider(
                final String notification,
                final AnyTypeKind anyTypeKind,
                final String entityKey,
                final int paginatorRows) {

            super(paginatorRows, TaskType.NOTIFICATION);
            this.notification = notification;
            this.anyTypeKind = anyTypeKind;
            this.entityKey = entityKey;
        }

        @Override
        public long size() {
            return restClient.count(anyTypeKind, entityKey, notification);
        }

        @Override
        public Iterator<NotificationTaskTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.listNotificationTasks(
                    notification, anyTypeKind, entityKey, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }
}
