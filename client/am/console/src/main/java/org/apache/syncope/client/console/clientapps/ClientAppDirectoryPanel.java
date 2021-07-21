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
package org.apache.syncope.client.console.clientapps;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalDirectoryPanel;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public abstract class ClientAppDirectoryPanel<T extends ClientAppTO>
        extends DirectoryPanel<T, T, DirectoryDataProvider<T>, ClientAppRestClient> {

    private static final long serialVersionUID = 1L;

    private final ClientAppType type;

    protected final BaseModal<T> propertiesModal;

    public ClientAppDirectoryPanel(final String id, final ClientAppType type, final PageReference pageRef) {
        super(id, pageRef, true);
        this.type = type;

        modal.addSubmitButton();
        modal.size(Modal.Size.Large);
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        propertiesModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        propertiesModal.size(Modal.Size.Large);
        propertiesModal.setWindowClosedCallback(target -> propertiesModal.show(false));
        addOuterObject(propertiesModal);

        disableCheckBoxes();
    }

    @Override
    protected List<IColumn<T, String>> getColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("clientAppId", this), "clientAppId", "clientAppId"));

        addCustomColumnFields(columns);

        return columns;
    }

    protected void addCustomColumnFields(final List<IColumn<T, String>> columns) {
    }

    @Override
    public ActionsPanel<T> getActions(final IModel<T> model) {
        ActionsPanel<T> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ClientAppTO ignore) {
                send(ClientAppDirectoryPanel.this, Broadcast.EXACT,
                    new AjaxWizard.EditItemActionEvent<>(
                        ClientAppRestClient.read(type, model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.CLIENTAPP_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ClientAppTO ignore) {
                model.setObject(ClientAppRestClient.read(type, model.getObject().getKey()));
                target.add(propertiesModal.setContent(new ModalDirectoryPanel<>(
                    propertiesModal,
                    new ClientAppPropertiesDirectoryPanel<>("panel", propertiesModal, type, model, pageRef),
                    pageRef)));
                propertiesModal.header(new Model<>(getString("properties.title", model)));
                propertiesModal.show(true);
            }
        }, ActionLink.ActionType.TYPE_EXTENSIONS, AMEntitlement.CLIENTAPP_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ClientAppTO ignore) {
                ClientAppTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                clone.setClientAppId(null);
                send(ClientAppDirectoryPanel.this, Broadcast.EXACT,
                    new AjaxWizard.EditItemActionEvent<>(clone, target));
            }
        }, ActionLink.ActionType.CLONE, AMEntitlement.CLIENTAPP_CREATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ClientAppTO ignore) {
                T clientAppTO = model.getObject();
                try {
                    ClientAppRestClient.delete(type, clientAppTO.getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", clientAppTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.CLIENTAPP_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected ClientAppDataProvider dataProvider() {
        return new ClientAppDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_CLIENTAPP_PAGINATOR_ROWS;
    }

    protected class ClientAppDataProvider extends DirectoryDataProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<T> comparator;

        ClientAppDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            List<T> list = ClientAppRestClient.list(type);
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return ClientAppRestClient.list(type).size();
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
