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
import java.io.Serializable;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.panels.RealmModalPanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel.ChosenRealm;
import org.apache.syncope.client.console.panels.WizardModalPanel;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.tasks.TemplatesTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private final TemplatesTogglePanel templates;

    private final RealmChoicePanel realmChoicePanel;

    private final WebMarkupContainer content;

    private final BaseModal<RealmTO> modal;

    private final BaseModal<Serializable> templateModal;

    public Realms(final PageParameters parameters) {
        super(parameters);

        templates = new TemplatesTogglePanel(BaseModal.CONTENT_ID, this, getPageReference()) {

            private static final long serialVersionUID = 4828350561653999922L;

            @Override
            protected Serializable onApplyInternal(
                    final TemplatableTO targetObject, final String type, final AnyTO anyTO) {
                targetObject.getTemplates().put(type, anyTO);
                new RealmRestClient().update(RealmTO.class.cast(targetObject));
                return targetObject;
            }
        };

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

        content.add(templates);

        templateModal = new BaseModal<Serializable>("templateModal") {

            private static final long serialVersionUID = 5787433530654262016L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        templateModal.size(Modal.Size.Large);
        content.add(templateModal);

        modal.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(realmChoicePanel.reloadRealmTree(target));
                target.add(content);
                modal.show(false);
            }
        });

        templateModal.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(content);
                templateModal.show(false);
            }
        });

        try {
            updateRealmContent(realmChoicePanel.getCurrentRealm(), parameters.get("selectedIndex").toInteger());
        } catch (Exception e) {
            updateRealmContent(realmChoicePanel.getCurrentRealm(), 0);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ChosenRealm) {
            @SuppressWarnings("unchecked")
            final ChosenRealm<RealmTO> choosenRealm = ChosenRealm.class.cast(event.getPayload());
            updateRealmContent(choosenRealm.getObj(), 0);
            choosenRealm.getTarget().add(content);
        } else if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            final AjaxWizard.NewItemEvent<?> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            final WizardModalPanel<?> modalPanel = newItemEvent.getModalPanel();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && modalPanel != null) {
                final IModel<Serializable> model = new CompoundPropertyModel<>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getResourceModel());
                newItemEvent.getTarget().add(templateModal.setContent(modalPanel));
                templateModal.show(true);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                templateModal.close(newItemEvent.getTarget());
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                SyncopeConsoleSession.get().getNotificationPanel().refresh(newItemEvent.getTarget());
                templateModal.close(newItemEvent.getTarget());
            }
        }
    }

    private WebMarkupContainer updateRealmContent(final RealmTO realmTO, final int selectedIndex) {
        if (realmTO == null) {
            return content;
        }
        content.addOrReplace(new Realm("body", realmTO, getPageReference(), selectedIndex) {

            private static final long serialVersionUID = 8221398624379357183L;

            @Override
            protected void onClickTemplate(final AjaxRequestTarget target) {
                templates.setTargetObject(realmTO);
                templates.toggle(target, true);
            }

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

                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    updateRealmContent(parent, selectedIndex);
                    target.add(content);
                } catch (Exception e) {
                    LOG.error("While deleting realm", e);
                    // Escape line breaks
                   SyncopeConsoleSession.get().error(e.getMessage().replace("\n", " "));
                }
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }
        });
        return content;
    }
}
