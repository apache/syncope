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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.layout.IdMUserFormLayoutInfo;
import org.apache.syncope.client.console.layout.LinkedAccountForm;
import org.apache.syncope.client.console.layout.LinkedAccountFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.status.LinkedAccountStatusPanel;
import org.apache.syncope.client.console.status.ReconTaskPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.LinkedAccountWizardBuilder;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedAccountModalPanel extends Panel implements ModalPanel {

    private static final long serialVersionUID = -4603032036433309900L;

    protected static final Logger LOG = LoggerFactory.getLogger(LinkedAccountModalPanel.class);

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    protected LinkedAccountForm wizard;

    protected final WizardMgtPanel<LinkedAccountTO> list;

    private final AjaxLink<LinkedAccountTO> addAjaxLink;

    protected ActionLinksTogglePanel<LinkedAccountTO> actionTogglePanel;

    protected final List<LinkedAccountTO> linkedAccountTOs;

    @SuppressWarnings("unchecked")
    public LinkedAccountModalPanel(
            final IModel<UserTO> model,
            final PageReference pageRef,
            final boolean reconciliationOnly) {

        super(BaseModal.CONTENT_ID, model);

        final MultilevelPanel mlp = new MultilevelPanel("mlpContainer");
        mlp.setOutputMarkupId(true);

        setOutputMarkupId(true);

        actionTogglePanel = new ActionLinksTogglePanel<>("toggle", pageRef);
        add(actionTogglePanel);

        AnyLayout anyLayout = AnyLayoutUtils.fetch(
                roleRestClient,
                anyTypeRestClient.listAnyTypes().stream().map(AnyTypeTO::getKey).collect(Collectors.toList()));
        LinkedAccountFormLayoutInfo linkedAccountFormLayoutInfo =
                anyLayout.getUser() instanceof IdMUserFormLayoutInfo
                ? IdMUserFormLayoutInfo.class.cast(anyLayout.getUser()).getLinkedAccountFormLayoutInfo()
                : new LinkedAccountFormLayoutInfo();

        try {
            wizard = linkedAccountFormLayoutInfo.getFormClass().getConstructor(
                    IModel.class,
                    LinkedAccountFormLayoutInfo.class,
                    UserRestClient.class,
                    AnyTypeRestClient.class,
                    PageReference.class).
                    newInstance(model, linkedAccountFormLayoutInfo, userRestClient, anyTypeRestClient, pageRef);
        } catch (Exception e) {
            LOG.error("Error instantiating form layout", e);
            wizard = new LinkedAccountWizardBuilder(
                    model, linkedAccountFormLayoutInfo, userRestClient, anyTypeRestClient, pageRef);
        }

        ListViewPanel.Builder<LinkedAccountTO> builder = new ListViewPanel.Builder<>(
                LinkedAccountTO.class, pageRef) {

            private static final long serialVersionUID = -5322423525438435153L;

            @Override
            protected LinkedAccountTO getActualItem(final LinkedAccountTO item, final List<LinkedAccountTO> list) {
                return item == null
                        ? null
                        : list.stream().filter(
                                in -> ((item.getKey() == null && in.getKey() == null)
                                || (in.getKey() != null && in.getKey().equals(item.getKey())))).
                                findAny().orElse(null);
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
                checkAddButton(model.getObject().getRealm());

                linkedAccountTOs.clear();
                linkedAccountTOs.addAll(model.getObject().getLinkedAccounts());
                sortLinkedAccounts();

                ListViewPanel.class.cast(list).refreshList(linkedAccountTOs);

                // change modal footer visibility
                send(LinkedAccountModalPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
            }

            @Override
            protected Component getValueComponent(final String key, final LinkedAccountTO bean) {
                if ("suspended".equalsIgnoreCase(key)) {
                    Label label = new Label("field", StringUtils.EMPTY);
                    if (bean.isSuspended()) {
                        label.add(new AttributeModifier("class", "fa fa-check"));
                        label.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
                    }
                    return label;
                }
                return super.getValueComponent(key, bean);
            }

            @Override
            protected ActionLinksTogglePanel<LinkedAccountTO> getTogglePanel() {
                return actionTogglePanel;
            }
        };

        linkedAccountTOs = new ArrayList<>(model.getObject().getLinkedAccounts());
        sortLinkedAccounts();

        builder.setItems(linkedAccountTOs);
        builder.includes("connObjectKeyValue", "resource", "suspended");
        builder.setReuseItem(false);
        builder.withChecks(ListViewPanel.CheckAvailability.NONE);

        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = 2555747430358755813L;

            @Override
            public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                mlp.next(linkedAccountTO.getResource(), new LinkedAccountStatusPanel(
                        linkedAccountTO.getResource(),
                        model.getObject().getType(),
                        linkedAccountTO.getConnObjectKeyValue()),
                        target);
                target.add(mlp);

                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
            }
        }, ActionLink.ActionType.VIEW, IdRepoEntitlement.USER_READ);

        if (!reconciliationOnly) {
            builder.addAction(new ActionLink<>() {

                private static final long serialVersionUID = 2555747430358755813L;

                @Override
                public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                    try {
                        send(LinkedAccountModalPanel.this, Broadcast.DEPTH,
                                new AjaxWizard.NewItemActionEvent<>(linkedAccountTO, 1, target).
                                        setTitleModel(new StringResourceModel("inner.edit.linkedAccount",
                                                LinkedAccountModalPanel.this, Model.of(linkedAccountTO))));
                    } catch (SyncopeClientException e) {
                        LOG.error("While attempting to create new linked account", e);
                        SyncopeConsoleSession.get().onException(e);
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }

                    send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                            new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                }
            }, ActionLink.ActionType.EDIT, IdRepoEntitlement.USER_UPDATE);

            builder.addAction(new ActionLink<>() {

                private static final long serialVersionUID = 2555747430358755813L;

                @Override
                public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                    try {
                        linkedAccountTO.setSuspended(!linkedAccountTO.isSuspended());
                        LinkedAccountUR linkedAccountUR = new LinkedAccountUR.Builder().
                                operation(PatchOperation.ADD_REPLACE).
                                linkedAccountTO(linkedAccountTO).build();

                        UserUR req = new UserUR();
                        req.setKey(model.getObject().getKey());
                        req.getLinkedAccounts().add(linkedAccountUR);
                        model.setObject(userRestClient.update(model.getObject().getETagValue(), req).getEntity());

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (SyncopeClientException e) {
                        LOG.error("While toggling status of linked account", e);
                        SyncopeConsoleSession.get().onException(e);
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }

                    checkAddButton(model.getObject().getRealm());
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    send(LinkedAccountModalPanel.this, Broadcast.DEPTH, new ListViewPanel.ListViewReload<>(target));
                }
            }, ActionLink.ActionType.ENABLE, IdRepoEntitlement.USER_UPDATE);
        }

        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = 2555747430358755813L;

            @Override
            public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                mlp.next("PUSH " + linkedAccountTO.getResource(),
                        new ReconTaskPanel(
                                linkedAccountTO.getResource(),
                                new PushTaskTO(),
                                model.getObject().getType(),
                                null,
                                ConnIdSpecialName.UID + "==" + linkedAccountTO.getConnObjectKeyValue(),
                                true,
                                mlp,
                                pageRef),
                        target);
                target.add(mlp);

                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
            }
        }, ActionLink.ActionType.RECONCILIATION_PUSH,
                String.format("%s,%s", IdRepoEntitlement.USER_READ, IdRepoEntitlement.TASK_EXECUTE));

        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = 2555747430358755813L;

            @Override
            public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                mlp.next("PULL " + linkedAccountTO.getResource(),
                        new ReconTaskPanel(
                                linkedAccountTO.getResource(),
                                new PullTaskTO(),
                                model.getObject().getType(),
                                null,
                                ConnIdSpecialName.UID + "==" + linkedAccountTO.getConnObjectKeyValue(),
                                true,
                                mlp,
                                pageRef),
                        target);
                target.add(mlp);

                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
            }
        }, ActionLink.ActionType.RECONCILIATION_PULL,
                String.format("%s,%s", IdRepoEntitlement.USER_READ, IdRepoEntitlement.TASK_EXECUTE));

        if (!reconciliationOnly) {
            builder.addAction(new ActionLink<>() {

                private static final long serialVersionUID = 2555747430358755813L;

                @Override
                public void onClick(final AjaxRequestTarget target, final LinkedAccountTO linkedAccountTO) {
                    try {
                        LinkedAccountUR linkedAccountUR = new LinkedAccountUR.Builder().
                                operation(PatchOperation.DELETE).
                                linkedAccountTO(linkedAccountTO).build();

                        UserUR req = new UserUR();
                        req.setKey(model.getObject().getKey());
                        req.getLinkedAccounts().add(linkedAccountUR);
                        model.setObject(userRestClient.update(model.getObject().getETagValue(), req).getEntity());
                        linkedAccountTOs.remove(linkedAccountTO);

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (Exception e) {
                        LOG.error("While removing linked account {}", linkedAccountTO.getKey(), e);
                        SyncopeConsoleSession.get().onException(e);
                    }

                    checkAddButton(model.getObject().getRealm());
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    send(LinkedAccountModalPanel.this, Broadcast.DEPTH, new ListViewPanel.ListViewReload<>(target));
                }
            }, ActionLink.ActionType.DELETE, IdRepoEntitlement.USER_UPDATE, true);
        }

        builder.addNewItemPanelBuilder(wizard);

        list = builder.build(MultilevelPanel.FIRST_LEVEL_ID);
        list.setOutputMarkupId(true);
        list.setReadOnly(!SyncopeConsoleSession.get().
                owns(IdRepoEntitlement.USER_UPDATE, model.getObject().getRealm()));

        addAjaxLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(LinkedAccountModalPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));

                // this opens the wizard (set above) in CREATE mode
                send(list, Broadcast.DEPTH, new AjaxWizard.NewItemActionEvent<>(new LinkedAccountTO(), target).
                        setTitleModel(
                                new StringResourceModel("inner.create.linkedAccount", LinkedAccountModalPanel.this)));
            }
        };
        list.addOrReplaceInnerObject(addAjaxLink.setEnabled(!reconciliationOnly).setVisible(!reconciliationOnly));

        add(mlp.setFirstLevel(list));
    }

    private void sortLinkedAccounts() {
        linkedAccountTOs.sort(Comparator.comparing(LinkedAccountTO::getConnObjectKeyValue));
    }

    private void checkAddButton(final String realm) {
        addAjaxLink.setVisible(SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_UPDATE, realm));
    }
}
