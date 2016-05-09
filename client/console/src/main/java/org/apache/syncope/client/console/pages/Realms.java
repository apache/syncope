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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.panels.RealmModalPanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel.ChosenRealm;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private final RealmChoicePanel realmChoicePanel;

    private final WebMarkupContainer content;

    private final BaseModal<RealmTO> modal;

    public Realms(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        content = new WebMarkupContainer("content");

        realmChoicePanel = new RealmChoicePanel("realmChoicePanel", getPageReference());
        realmChoicePanel.setOutputMarkupId(true);
        content.add(realmChoicePanel);

        content.add(new Label("body", "Root realm"));
        content.setOutputMarkupId(true);
        body.add(content);

        modal = new BaseModal<>("modal");
        modal.size(Modal.Size.Large);
        content.add(modal);

        modal.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(realmChoicePanel.reloadRealmTree(target));
                target.add(content);
                modal.show(false);
            }
        });

        updateRealmContent(realmChoicePanel.getCurrentRealm());
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ChosenRealm) {
            @SuppressWarnings("unchecked")
            final ChosenRealm<RealmTO> choosenRealm = ChosenRealm.class.cast(event.getPayload());
            updateRealmContent(choosenRealm.getObj());
            choosenRealm.getTarget().add(content);
        }
    }

    private WebMarkupContainer updateRealmContent(final RealmTO realmTO) {
        if (realmTO == null) {
            return content;
        }
        content.addOrReplace(new Realm("body", realmTO, getPageReference()) {

            private static final long serialVersionUID = 8221398624379357183L;

            @Override
            protected void onClickCreate(final AjaxRequestTarget target) {
                modal.header(new ResourceModel("newRealm"));

                RealmTO newRealmTO = new RealmTO();
                modal.setFormModel(newRealmTO);

                RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        newRealmTO,
                        realmChoicePanel.getCurrentRealm().getFullPath(),
                        StandardEntitlement.REALM_CREATE,
                        true);
                target.add(modal.setContent(panel));

                modal.addSubmitButton();
                modal.show(true);
            }

            @Override
            protected void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO) {
                modal.header(new StringResourceModel("editRealm", Model.of(realmTO)));

                modal.setFormModel(realmTO);

                RealmModalPanel panel = new RealmModalPanel(
                        modal,
                        Realms.this.getPageReference(),
                        realmTO,
                        realmTO.getFullPath(),
                        StandardEntitlement.REALM_UPDATE,
                        false);
                target.add(modal.setContent(panel));

                modal.addSubmitButton();
                modal.show(true);
            }

            @Override
            protected void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO) {
                try {
                    if (realmTO.getKey() == null) {
                        throw new Exception("Root realm cannot be deleted");
                    }
                    realmRestClient.delete(realmTO.getFullPath());
                    RealmTO parent = realmChoicePanel.moveToParentRealm(realmTO.getKey());
                    target.add(realmChoicePanel.reloadRealmTree(target));

                    info(getString(Constants.OPERATION_SUCCEEDED));
                    updateRealmContent(parent);
                    target.add(content);
                } catch (Exception e) {
                    LOG.error("While deleting realm", e);
                    // Escape line breaks
                    error(e.getMessage().replace("\n", " "));
                }
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }
        });
        return content;
    }
}
