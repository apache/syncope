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
package org.apache.syncope.client.console.notifications;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.events.EventCategory;
import org.apache.syncope.client.console.notifications.NotificationDirectoryPanel.NotificationProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class NotificationDirectoryPanel
        extends DirectoryPanel<NotificationTO, NotificationWrapper, NotificationProvider, NotificationRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    @SpringBean
    protected NotificationRestClient notificationRestClient;

    @SpringBean
    protected AuditRestClient auditRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    protected final IModel<List<EventCategory>> eventCategories = new LoadableDetachableModel<List<EventCategory>>() {

        private static final long serialVersionUID = 4659376149825914247L;

        @Override
        protected List<EventCategory> load() {
            // cannot notify about WA events
            return auditRestClient.events().stream().
                    filter(c -> c.getType() != OpEvent.CategoryType.WA).collect(Collectors.toList());
        }
    };

    protected final BaseModal<String> utilityModal = new BaseModal<>(Constants.OUTER);

    public NotificationDirectoryPanel(
            final String id,
            final NotificationRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        disableCheckBoxes();

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);

        modal.size(Modal.Size.Large);
        altDefaultModal.size(Modal.Size.Large);

        addNewItemPanelBuilder(new NotificationWizardBuilder(
                new NotificationTO(),
                notificationRestClient,
                anyTypeRestClient,
                implementationRestClient,
                schemaRestClient,
                eventCategories,
                pageRef), true);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.NOTIFICATION_CREATE);
    }

    @Override
    protected List<IColumn<NotificationTO, String>> getColumns() {
        List<IColumn<NotificationTO, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("sender", this), "sender", "sender"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("subject", this), "subject", "subject"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("template", this), "template", "template"));
        columns.add(new CollectionPropertyColumn<>(
                new StringResourceModel("events", this), "events"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("active", this), "active", "active"));
        return columns;
    }

    @Override
    public ActionsPanel<NotificationTO> getActions(final IModel<NotificationTO> model) {
        ActionsPanel<NotificationTO> actions = super.getActions(model);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                send(NotificationDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new NotificationWrapper(restClient.read(model.getObject().getKey())), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.NOTIFICATION_UPDATE);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                target.add(utilityModal.setContent(new NotificationTasks(model.getObject().getKey(), pageRef)));
                utilityModal.header(new StringResourceModel("notification.tasks", model));
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.NOTIFICATION_TASKS, IdRepoEntitlement.TASK_LIST);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.NOTIFICATION_DELETE, true);

        return actions;
    }

    @Override
    protected NotificationProvider dataProvider() {
        return new NotificationProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_NOTIFICATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class NotificationProvider extends DirectoryDataProvider<NotificationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        protected final SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final long first, final long count) {
            List<NotificationTO> notifications = restClient.list();
            notifications.sort(comparator);
            return notifications.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<NotificationTO> model(final NotificationTO notification) {
            return Model.of(notification);
        }
    }
}
