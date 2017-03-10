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
import org.apache.syncope.client.console.panels.RealmChoicePanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel.ChosenRealm;
import org.apache.syncope.client.console.panels.WizardModalPanel;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.tasks.TemplatesTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.ResultPage;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
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
            LOG.debug("Unexpected error", e);
            updateRealmContent(realmChoicePanel.getCurrentRealm(), 0);
        }
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
                final IModel<Serializable> model = new CompoundPropertyModel<Serializable>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getResourceModel());
                newItemEvent.getTarget().add(templateModal.setContent(modalPanel));
                templateModal.show(true);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                templateModal.close(newItemEvent.getTarget());
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                ((BasePage) getPage()).getNotificationPanel().refresh(newItemEvent.getTarget());
                templateModal.close(newItemEvent.getTarget());
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
                modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID = 8804221891699487139L;

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.add(realmChoicePanel.reloadRealmTree(target));

                        if (modal.getContent() instanceof ResultPage) {
                            updateRealmContent(RealmTO.class.cast(
                                    ResultPage.class.cast(modal.getContent()).getItem()), selectedIndex);
                            target.add(content);
                        }

                        modal.show(false);
                    }
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
                ((BasePage) Realms.this.getPage()).getNotificationPanel().refresh(target);
            }
        });
        return content;
    }
}
