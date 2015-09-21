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

import static org.apache.wicket.Component.ENABLE;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.panels.RealmModalPanel;
import org.apache.syncope.client.console.panels.RealmSidebarPanel;
import org.apache.syncope.client.console.panels.RealmSidebarPanel.ControlSidebarClick;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    @SpringBean
    private RealmRestClient realmRestClient;

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

                if (isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    setModalResult(false);
                }
            }
        });

        setupDeleteLink();
        setupCreateLink();
        setupEditLink();

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
        content.addOrReplace(new Realm("body", realmTO, getPageReference()));
        return content;
    }

    private void setupDeleteLink() {

        final AjaxLink<Void> deleteLink = new ClearIndicatingAjaxLink<Void>("deleteLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                try {
                    final RealmTO toBeDeleted = realmSidebarPanel.getCurrentRealm();

                    if (toBeDeleted.getKey() == 0) {
                        throw new Exception("Root realm cannot be deleted");
                    }

                    realmRestClient.delete(toBeDeleted.getFullPath());

                    target.add(realmSidebarPanel.reloadRealmTree());
                    target.add(updateRealmContent(realmSidebarPanel.getCurrentRealm()));

                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                } catch (Exception e) {
                    LOG.error("While deleting realm", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    getFeedbackPanel().refresh(target);
                }
            }
        };

        if (SyncopeConsoleSession.get().owns(Entitlement.REALM_DELETE)) {
            MetaDataRoleAuthorizationStrategy.authorize(deleteLink, ENABLE, Entitlement.REALM_DELETE);
        }

        content.addOrReplace(deleteLink);
    }

    private void setupCreateLink() {

        final AjaxLink<Void> createLink = new ClearIndicatingAjaxLink<Void>("createLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                modal.header(new ResourceModel("createRealm"));

                final RealmTO realmTO = new RealmTO();
                modal.setFormModel(realmTO);

                final RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        realmTO,
                        realmSidebarPanel.getCurrentRealm().getFullPath(),
                        Entitlement.REALM_CREATE,
                        true);
                target.add(modal.setContent(panel));

                modal.addSumbitButton();
                modal.show(true);
            }
        };

        if (SyncopeConsoleSession.get().owns(Entitlement.REALM_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE, Entitlement.REALM_CREATE);
        }

        content.addOrReplace(createLink);
    }

    private void setupEditLink() {
        final AjaxLink<Void> editLink = new ClearIndicatingAjaxLink<Void>("editLink", getPageReference()) {

            private static final long serialVersionUID = -6957616042924610290L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                modal.header(Model.of(realmSidebarPanel.getCurrentRealm().getName()));

                final RealmTO realmTO = realmSidebarPanel.getCurrentRealm();
                modal.setFormModel(realmTO);

                final RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        realmTO,
                        realmTO.getFullPath(),
                        Entitlement.REALM_UPDATE,
                        false);
                target.add(modal.setContent(panel));

                modal.addSumbitButton();
                modal.show(true);
            }
        };

        if (SyncopeConsoleSession.get().owns(Entitlement.REALM_UPDATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(editLink, ENABLE, Entitlement.REALM_UPDATE);
        }

        content.addOrReplace(editLink);
    }
}
