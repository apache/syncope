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
package org.apache.syncope.client.console.pages;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.panels.RealmModalPanel;
import org.apache.syncope.client.console.panels.RealmSidebarPanel;
import org.apache.syncope.client.console.panels.RealmSidebarPanel.ControlSidebarClick;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private final RealmSidebarPanel realmSidebarPanel;

    private final WebMarkupContainer content;

    private final BaseModal<RealmTO> modal;

    public Realms(final PageParameters parameters) {
        super(parameters);

        realmSidebarPanel = new RealmSidebarPanel("realmSidebarPanel", getPageReference());
        realmSidebarPanel.setMarkupId("sidebar");
        realmSidebarPanel.setOutputMarkupId(true);
        add(realmSidebarPanel);

        content = new WebMarkupContainer("content");
        content.add(new Label("header", "Root realm"));
        content.add(new Label("body", "Root realm"));
        content.setOutputMarkupId(true);
        add(content);

        modal = new BaseModal<>("modal");
        content.add(modal);

        modal.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(realmSidebarPanel.reloadRealmTree());
                target.add(updateRealmContent(realmSidebarPanel.getCurrentRealm()));
                modal.show(false);
            }
        });

        updateRealmContent(realmSidebarPanel.getCurrentRealm());
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ControlSidebarClick) {
            @SuppressWarnings("unchecked")
            final ControlSidebarClick<RealmTO> controlSidebarClick = ControlSidebarClick.class.cast(event.getPayload());
            updateRealmContent(controlSidebarClick.getObj());
            controlSidebarClick.getTarget().add(content);
        }
    }

    private WebMarkupContainer updateRealmContent(final RealmTO realmTO) {
        content.addOrReplace(new Label("header", realmTO.getName()));
        content.addOrReplace(new Realm("body", realmTO, getPageReference()) {

            private static final long serialVersionUID = 8221398624379357183L;

            @Override
            protected void onClickCreate(final AjaxRequestTarget target) {
                modal.header(new ResourceModel("createRealm"));

                final RealmTO newRealmTO = new RealmTO();

                modal.setFormModel(newRealmTO);

                final RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        newRealmTO,
                        realmSidebarPanel.getCurrentRealm().getFullPath(),
                        StandardEntitlement.REALM_CREATE,
                        true);
                target.add(modal.setContent(panel));

                modal.addSumbitButton();
                modal.show(true);
            }

            @Override
            protected void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO) {
                modal.header(Model.of(realmSidebarPanel.getCurrentRealm().getName()));

                modal.setFormModel(realmTO);

                final RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        realmTO,
                        realmTO.getFullPath(),
                        StandardEntitlement.REALM_UPDATE,
                        false);
                target.add(modal.setContent(panel));

                modal.addSumbitButton();
                modal.show(true);
            }

            @Override
            protected void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO) {
                try {
                    if (realmTO.getKey() == 0) {
                        throw new Exception("Root realm cannot be deleted");
                    }
                    realmRestClient.delete(realmTO.getFullPath());
                    target.add(realmSidebarPanel.reloadRealmTree());
                    target.add(updateRealmContent(realmSidebarPanel.getCurrentRealm()));
                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While deleting realm", e);
                    // Excape line breaks
                    getSession().error(getString(Constants.ERROR) + ": " + e.getMessage().replace("\n", " "));
                }
                getNotificationPanel().refresh(target);
            }
        });
        return content;
    }
}
