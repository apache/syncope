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

import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.panels.AttrListDirectoryPanel;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard.EditItemActionEvent;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.model.IModel;

public class ClientAppPropertiesDirectoryPanel<T extends ClientAppTO> extends AttrListDirectoryPanel {

    private static final long serialVersionUID = 9072805604972532678L;

    protected final BaseModal<T> propertiesModal;

    protected final ClientAppType type;

    protected final IModel<T> model;

    public ClientAppPropertiesDirectoryPanel(
            final String id,
            final ClientAppRestClient restClient,
            final BaseModal<T> propertiesModal,
            final ClientAppType type,
            final IModel<T> model,
            final PageReference pageRef) {

        super(id, restClient, pageRef, false);

        this.propertiesModal = propertiesModal;
        this.type = type;
        this.model = model;

        setOutputMarkupId(true);

        enableUtilityButton();
        setFooterVisibility(false);

        addNewItemPanelBuilder(new ClientAppPropertyWizardBuilder(
                type, model.getObject(), new Attr(), restClient, pageRef), true);

        initResultTable();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            propertiesModal.close(target);
        } else if (event.getPayload() instanceof EditItemActionEvent) {
            @SuppressWarnings("unchecked")
            EditItemActionEvent<T> payload = (EditItemActionEvent<T>) event.getPayload();
            payload.getTarget().ifPresent(actionTogglePanel::close);
        }
        super.onEvent(event);
    }

    @Override
    protected AttrListProvider dataProvider() {
        return new ClientAppPropertiesProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_CLIENTAPP_PROPERTIES_PAGINATOR_ROWS;
    }

    @Override
    protected ActionsPanel<Attr> getActions(final IModel<Attr> model) {
        ActionsPanel<Attr> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                send(ClientAppPropertiesDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.CLIENTAPP_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                try {
                    ClientAppPropertiesDirectoryPanel.this.model.getObject().getProperties().remove(model.getObject());
                    ((ClientAppRestClient) restClient).update(
                            type, ClientAppPropertiesDirectoryPanel.this.model.getObject());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getSchema(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.CLIENTAPP_UPDATE, true);

        return panel;
    }

    protected final class ClientAppPropertiesProvider extends AttrListProvider {

        private static final long serialVersionUID = -185944053385660794L;

        private ClientAppPropertiesProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        protected List<Attr> list() {
            return model.getObject().getProperties();
        }
    }
}
