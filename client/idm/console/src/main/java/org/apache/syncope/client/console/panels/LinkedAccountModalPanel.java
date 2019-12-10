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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.LinkedAccountWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class LinkedAccountModalPanel extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -4603032036433309900L;

    private final LinkedAccountWizardBuilder wizard;

    private final WizardMgtPanel<LinkedAccountTO> list;

    private final AjaxLink<LinkedAccountTO> addAjaxLink;

    protected ActionLinksTogglePanel<LinkedAccountTO> actionTogglePanel;

    private UserRestClient userRestClient = new UserRestClient();

    private final List<LinkedAccountTO> linkedAccountTOs;

    @SuppressWarnings("unchecked")
    public LinkedAccountModalPanel(
            final BaseModal<?> modal,
            final UserTO userTO,
            final PageReference pageRef) {

        super((BaseModal<Serializable>) modal, pageRef);

        UserTO readUserTO = userRestClient.read(userTO.getKey());

        setOutputMarkupId(true);

        actionTogglePanel = new ActionLinksTogglePanel<>("toggle", pageRef);
        add(actionTogglePanel);

        wizard = new LinkedAccountWizardBuilder(readUserTO.getKey(), pageRef);

        final ListViewPanel.Builder<LinkedAccountTO> builder = new ListViewPanel.Builder<LinkedAccountTO>(
                LinkedAccountTO.class, pageRef) {

            private static final long serialVersionUID = -5322423525438435153L;

            @Override
            protected LinkedAccountTO getActualItem(
                    final LinkedAccountTO item, final List<LinkedAccountTO> list) {

                return item == null
                        ? null
                        : list.stream().filter(
                                in -> ((item.getKey() == null && in.getKey() == null)
                                || (in.getKey() != null && in.getKey().
                                equals(item.getKey())))).findAny().orElse(null);
            }

            @Override
            protected void customActionCallback(final AjaxRequestTarget target) {
                // change modal footer visibility
                send(LinkedAccountModalPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
                // change modal footer visibility
                send(LinkedAccountModalPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
                checkAddButton();

                linkedAccountTOs.clear();
                linkedAccountTOs.addAll(userRestClient.read(userTO.getKey()).getLinkedAccounts());
                sortLinkedAccounts();

                ListViewPanel.class.cast(list).refreshList(linkedAccountTOs);

                // change modal footer visibility
                send(LinkedAccountModalPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected ActionLinksTogglePanel<LinkedAccountTO> getTogglePanel() {
                return actionTogglePanel;
            }
        };

        linkedAccountTOs = new ArrayList<>(readUserTO.getLinkedAccounts());
        sortLinkedAccounts();

        builder.setItems(linkedAccountTOs);
        builder.includes("connObjectKeyValue", "resource", "suspended");
        builder.setReuseItem(false);
        builder.withChecks(ListViewPanel.CheckAvailability.NONE);

        builder.addAction(new ActionLink<LinkedAccountTO>() {

            private static final long serialVersionUID = 2555747430358755813L;

            @Override
            public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                try {
                    send(LinkedAccountModalPanel.this, Broadcast.DEPTH,
                            new AjaxWizard.NewItemActionEvent<>(linkedAccountTO, 1, target).setResourceModel(
                                    new StringResourceModel("inner.edit.linkedAccount",
                                            LinkedAccountModalPanel.this,
                                            Model.of(linkedAccountTO))));
                } catch (SyncopeClientException e) {
                    LOG.error("While contacting linked account", e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }

                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.USER_READ).
                addAction(new ActionLink<LinkedAccountTO>() {

                    private static final long serialVersionUID = 2555747430358755813L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                        try {
                            LinkedAccountUR linkedAccountPatch = new LinkedAccountUR.Builder().
                                    operation(PatchOperation.DELETE).
                                    linkedAccountTO(linkedAccountTO).build();
                            linkedAccountPatch.setLinkedAccountTO(linkedAccountTO);
                            UserUR patch = new UserUR();
                            patch.setKey(readUserTO.getKey());
                            patch.getLinkedAccounts().add(linkedAccountPatch);
                            userRestClient.update(userRestClient.read(userTO.getKey()).getETagValue(), patch);
                            linkedAccountTOs.remove(linkedAccountTO);

                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While removing linked account {}", linkedAccountTO.getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }

                        checkAddButton();
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        send(LinkedAccountModalPanel.this, Broadcast.DEPTH, new ListViewPanel.ListViewReload<>(target));
                    }
                }, ActionLink.ActionType.DELETE, IdRepoEntitlement.USER_UPDATE, true);

        builder.addNewItemPanelBuilder(wizard);

        list = builder.build("linkedAccountsList");
        list.setOutputMarkupId(true);
        list.setReadOnly(!SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_UPDATE));

        addAjaxLink = new AjaxLink<LinkedAccountTO>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));

                // this opens the wizard (set above) in CREATE mode
                send(list, Broadcast.DEPTH, new AjaxWizard.NewItemActionEvent<>(new LinkedAccountTO(), target).
                        setResourceModel(new StringResourceModel("inner.create.linkedAccount",
                                LinkedAccountModalPanel.this)));
            }
        };
        list.addOrReplaceInnerObject(addAjaxLink);
        add(list);
    }

    private void sortLinkedAccounts() {
        Collections.sort(linkedAccountTOs,
                (o1, o2) -> AnyTypeRestClient.KEY_COMPARATOR.compare(
                        o1.getConnObjectKeyValue(), o2.getConnObjectKeyValue()));
    }

    private void checkAddButton() {
        addAjaxLink.setVisible(SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_UPDATE));
    }
}
