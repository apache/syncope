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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.notifications.NotificationDirectoryPanel.NotificationProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class NotificationDirectoryPanel
        extends DirectoryPanel<NotificationTO, NotificationHandler, NotificationProvider, NotificationRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    public NotificationDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        altDefaultModal.size(Modal.Size.Large);

        addNewItemPanelBuilder(new NotificationWizardBuilder(new NotificationTO(), pageReference), true);

        restClient = new NotificationRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.NOTIFICATION_CREATE);
    }

    @Override
    protected List<IColumn<NotificationTO, String>> getColumns() {

        final List<IColumn<NotificationTO, String>> columns = new ArrayList<IColumn<NotificationTO, String>>();
        columns.add(new PropertyColumn<NotificationTO, String>(
                new StringResourceModel("key", this, null), "key", "key"));
        columns.add(new CollectionPropertyColumn<NotificationTO>(
                new StringResourceModel("events", this, null), "events", "events"));
        columns.add(new PropertyColumn<NotificationTO, String>(
                new StringResourceModel("subject", this, null), "subject", "subject"));
        columns.add(new PropertyColumn<NotificationTO, String>(
                new StringResourceModel("template", this, null), "template", "template"));
        columns.add(new PropertyColumn<NotificationTO, String>(
                new StringResourceModel("traceLevel", this, null), "traceLevel", "traceLevel"));
        columns.add(new PropertyColumn<NotificationTO, String>(
                new StringResourceModel("active", this, null), "active", "active"));

        columns.add(new ActionColumn<NotificationTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<NotificationTO> getActions(
                    final String componentId, final IModel<NotificationTO> model) {

                final ActionLinksPanel.Builder<NotificationTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<NotificationTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                        target.add(utilityModal.setContent(
                                new NotificationTasks(model.getObject().getKey(), pageRef)));
                        utilityModal.header(new StringResourceModel("notification.tasks", model));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.NOTIFICATION_TASKS, StandardEntitlement.TASK_LIST);

                panel.add(new ActionLink<NotificationTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                        send(NotificationDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(
                                        new NotificationHandler(restClient.read(model.getObject().getKey())), target));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.NOTIFICATION_UPDATE);

                panel.add(new ActionLink<NotificationTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                        try {
                            restClient.delete(model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.NOTIFICATION_DELETE);

                return panel.build(componentId);
            }
        });
        return columns;
    }

    @Override
    protected NotificationProvider dataProvider() {
        return new NotificationProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_NOTIFICATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    public class NotificationProvider extends DirectoryDataProvider<NotificationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<NotificationTO>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final long first, final long count) {
            final List<NotificationTO> list = restClient.getAllNotifications();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getAllNotifications().size();
        }

        @Override
        public IModel<NotificationTO> model(final NotificationTO notification) {
            return new AbstractReadOnlyModel<NotificationTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public NotificationTO getObject() {
                    return notification;
                }
            };
        }
    }

}
