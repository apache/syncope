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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.panels.RealmChoicePanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel.ChosenRealm;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.tasks.TemplatesTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.ResultPage;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

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
                RealmRestClient.update(RealmTO.class.cast(targetObject));
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

        templateModal = new BaseModal<>("templateModal") {

            private static final long serialVersionUID = 5787433530654262016L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        templateModal.size(Modal.Size.Large);
        content.add(templateModal);

        modal.setWindowClosedCallback(target -> {
            target.add(realmChoicePanel.reloadRealmTree(target));
            target.add(content);
            modal.show(false);
        });

        templateModal.setWindowClosedCallback(target -> {
            target.add(content);
            templateModal.show(false);
        });

        updateRealmContent(realmChoicePanel.getCurrentRealm(), parameters.get("selectedIndex").toInt(0));
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ChosenRealm) {
            @SuppressWarnings("unchecked")
            ChosenRealm<RealmTO> choosenRealm = ChosenRealm.class.cast(event.getPayload());
            updateRealmContent(choosenRealm.getObj(), 0);
            choosenRealm.getTarget().add(content);
        } else if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            AjaxWizard.NewItemEvent<?> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            WizardModalPanel<?> modalPanel = newItemEvent.getModalPanel();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && modalPanel != null) {
                final IModel<Serializable> model = new CompoundPropertyModel<>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getResourceModel());
                newItemEvent.getTarget().ifPresent(t -> t.add(templateModal.setContent(modalPanel)));
                templateModal.show(true);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                if (newItemEvent.getTarget().isPresent()) {
                    templateModal.close(newItemEvent.getTarget().get());
                }
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                if (newItemEvent.getTarget().isPresent()) {
                    ((BasePage) getPage()).getNotificationPanel().refresh(newItemEvent.getTarget().get());
                    templateModal.close(newItemEvent.getTarget().get());
                }
            }
        }
    }

    private WebMarkupContainer updateRealmContent(final RealmTO realmTO, final int selectedIndex) {
        if (realmTO == null) {
            return content;
        }
        content.addOrReplace(new Realm("body", realmTO, Realms.this.getPageReference(), selectedIndex) {

            private static final long serialVersionUID = 8221398624379357183L;

            @Override
            protected void onClickTemplate(final AjaxRequestTarget target) {
                templates.setTargetObject(realmTO);
                templates.toggle(target, true);
            }

            @Override
            protected void setWindowClosedReloadCallback(final BaseModal<?> modal) {
                modal.setWindowClosedCallback(target -> {
                    if (modal.getContent() instanceof ResultPage) {
                        Serializable result = ResultPage.class.cast(modal.getContent()).getResult();

                        RealmTO newRealmTO = RealmTO.class.cast(ProvisioningResult.class.cast(result).getEntity());
                        // reload realmChoicePanel label too - SYNCOPE-1151
                        target.add(realmChoicePanel.reloadRealmTree(target, Model.of(newRealmTO)));
                        realmChoicePanel.setCurrentRealm(newRealmTO);
                        send(Realms.this, Broadcast.DEPTH, new ChosenRealm<>(newRealmTO, target));
                    } else {
                        target.add(realmChoicePanel.reloadRealmTree(target));
                    }
                    target.add(content);
                    modal.show(false);
                });
            }

            @Override
            protected void onClickCreate(final AjaxRequestTarget target) {
                this.wizardBuilder.setParentPath(realmChoicePanel.getCurrentRealm().getFullPath());
                send(this, Broadcast.EXACT, new AjaxWizard.NewItemActionEvent<RealmTO>(new RealmTO(), target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.new";
                    }
                });
            }

            @Override
            protected void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO) {
                this.wizardBuilder.setParentPath(realmTO.getFullPath());
                send(this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<RealmTO>(realmTO, target) {

                    @Override
                    public String getEventDescription() {
                        return "realm.edit";
                    }
                });
            }

            @Override
            protected void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO) {
                try {
                    if (realmTO.getKey() == null) {
                        throw new Exception("Root realm cannot be deleted");
                    }
                    RealmRestClient.delete(realmTO.getFullPath());
                    RealmTO parent = realmChoicePanel.moveToParentRealm(realmTO.getKey());
                    target.add(realmChoicePanel.reloadRealmTree(target));

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    updateRealmContent(parent, selectedIndex);
                    target.add(content);
                } catch (Exception e) {
                    LOG.error("While deleting realm", e);
                    // Escape line breaks
                    SyncopeConsoleSession.get().error(e.getMessage().replace("\n", " "));
                }
                ((BaseWebPage) Realms.this.getPage()).getNotificationPanel().refresh(target);
            }
        });
        return content;
    }

    public RealmChoicePanel getRealmChoicePanel() {
        return realmChoicePanel;
    }
}
