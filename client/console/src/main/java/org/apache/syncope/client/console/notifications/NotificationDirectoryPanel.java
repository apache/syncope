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
import java.io.Serializable;
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
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
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
import org.apache.wicket.model.StringResourceModel;

public class NotificationDirectoryPanel
        extends DirectoryPanel<NotificationTO, NotificationWrapper, NotificationProvider, NotificationRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>("outer");

    public NotificationDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, pageRef, true);
        disableCheckBoxes();

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);

        modal.size(Modal.Size.Large);
        altDefaultModal.size(Modal.Size.Large);

        addNewItemPanelBuilder(new NotificationWizardBuilder(new NotificationTO(), pageRef), true);

        restClient = new NotificationRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.NOTIFICATION_CREATE);
    }

    @Override
    protected List<IColumn<NotificationTO, String>> getColumns() {
        List<IColumn<NotificationTO, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<>(new StringResourceModel("key", this), "key"));
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
        final ActionsPanel<NotificationTO> panel = super.getActions(model);

        panel.add(new ActionLink<NotificationTO>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                send(NotificationDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new NotificationWrapper(restClient.read(model.getObject().getKey())), target));
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.NOTIFICATION_UPDATE);

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

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NotificationTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                            getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.NOTIFICATION_DELETE, true);

        return panel;
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

    protected class NotificationProvider extends DirectoryDataProvider<NotificationTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final long first, final long count) {
            List<NotificationTO> list = restClient.list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
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
