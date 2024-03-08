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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.notifications.NotificationTasks;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.tasks.AnyPropagationTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class GroupDirectoryPanel extends AnyDirectoryPanel<GroupTO, GroupRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    protected final BaseModal<Serializable> typeExtensionsModal = new BaseModal<>(Constants.OUTER);

    protected final BaseModal<Serializable> membersModal = new BaseModal<>(Constants.OUTER);

    protected final MembersTogglePanel membersTogglePanel;

    protected GroupDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);

        typeExtensionsModal.size(Modal.Size.Large);
        addOuterObject(typeExtensionsModal);
        setWindowClosedReloadCallback(typeExtensionsModal);
        typeExtensionsModal.addSubmitButton();

        addOuterObject(membersModal);
        membersModal.size(Modal.Size.Large);

        membersTogglePanel = new MembersTogglePanel(page.getPageReference()) {

            private static final long serialVersionUID = -8765794727538618705L;

            @Override
            protected Serializable onApplyInternal(
                    final GroupTO groupTO, final String type, final AjaxRequestTarget target) {

                AnyTypeTO anyType = anyTypeRestClient.read(type);

                AnyLayout layout = AnyLayoutUtils.fetch(roleRestClient, anyTypeRestClient.list());

                ModalPanel anyPanel = new AnyPanel.Builder<>(
                        layout.getAnyPanelClass(), BaseModal.CONTENT_ID, anyType, null, layout, false, pageRef).
                        build((id, anyTypeTO, realmTO, anyLayout, pr) -> {

                            final Panel panel;
                            if (AnyTypeKind.USER.name().equals(type)) {
                                String query = SyncopeClient.getUserSearchConditionBuilder().and(
                                        SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()),
                                        SyncopeClient.getUserSearchConditionBuilder().
                                                is(Constants.KEY_FIELD_NAME).notNullValue()).query();

                                panel = new UserDirectoryPanel.Builder(
                                        anyTypeClassRestClient.list(
                                                anyTypeTO.getClasses()), userRestClient, anyTypeTO.getKey(), pr).
                                        setRealm(realm).
                                        setFiltered(true).
                                        setFiql(query).
                                        disableCheckBoxes().
                                        addNewItemPanelBuilder(
                                                AnyLayoutUtils.newLayoutInfo(
                                                        new UserTO(),
                                                        anyTypeTO.getClasses(),
                                                        anyLayout.getUser(),
                                                        userRestClient,
                                                        pr), false).
                                        setWizardInModal(false).build(id);

                                MetaDataRoleAuthorizationStrategy.authorize(
                                        panel, WebPage.RENDER, IdRepoEntitlement.USER_SEARCH);
                            } else {
                                String query = SyncopeClient.getAnyObjectSearchConditionBuilder(type).and(
                                        SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()),
                                        SyncopeClient.getUserSearchConditionBuilder().
                                                is(Constants.KEY_FIELD_NAME).notNullValue()).query();

                                panel = new AnyObjectDirectoryPanel.Builder(
                                        anyTypeClassRestClient.list(
                                                anyTypeTO.getClasses()), anyObjectRestClient, anyTypeTO.getKey(), pr).
                                        setRealm(realm).
                                        setFiltered(true).
                                        setFiql(query).
                                        disableCheckBoxes().
                                        addNewItemPanelBuilder(AnyLayoutUtils.newLayoutInfo(
                                                new AnyObjectTO(),
                                                anyTypeTO.getClasses(),
                                                layout.getAnyObjects().get(type),
                                                anyObjectRestClient,
                                                pr), false).
                                        setWizardInModal(false).build(id);

                                MetaDataRoleAuthorizationStrategy.authorize(
                                        panel, WebPage.RENDER, AnyEntitlement.SEARCH.getFor(anyTypeTO.getKey()));
                            }

                            return panel;
                        });

                membersModal.header(new StringResourceModel(
                        "group.members",
                        GroupDirectoryPanel.this,
                        Model.of(Pair.of(groupTO, new ResourceModel("anyType." + type, type).getObject()))));

                membersModal.setContent(anyPanel);
                membersModal.show(true);
                target.add(membersModal);

                return null;
            }
        };

        addOuterObject(membersTogglePanel);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_GROUP_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDefaultAttributeSelection() {
        return GroupDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = super.getHeader(componentId);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(displayAttributeModal.setContent(new GroupDisplayAttributesModalPanel<>(
                        displayAttributeModal,
                        page.getPageReference(),
                        plainSchemas.stream().map(PlainSchemaTO::getKey).collect(Collectors.toList()),
                        derSchemas.stream().map(DerSchemaTO::getKey).collect(Collectors.toList()))));
                displayAttributeModal.header(new ResourceModel("any.attr.display"));
                displayAttributeModal.show(true);
            }
        }, ActionType.CHANGE_VIEW, IdRepoEntitlement.GROUP_READ).hideLabel();
        return panel;
    }

    @Override
    public ActionsPanel<GroupTO> getActions(final IModel<GroupTO> model) {
        ActionsPanel<GroupTO> actions = super.getActions(model);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                send(GroupDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(new GroupWrapper(
                                restClient.read(model.getObject().getKey())), target));
            }
        }, ActionType.EDIT,
                String.format("%s,%s", IdRepoEntitlement.GROUP_READ, IdRepoEntitlement.GROUP_UPDATE)).
                setRealms(realm, model.getObject().getDynRealms());

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = 6242834621660352855L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                target.add(typeExtensionsModal.setContent(new TypeExtensionDirectoryPanel(
                        typeExtensionsModal, model.getObject(), pageRef)));
                typeExtensionsModal.header(new StringResourceModel("typeExtensions", model));
                typeExtensionsModal.show(true);
            }
        }, ActionType.TYPE_EXTENSIONS, IdRepoEntitlement.GROUP_UPDATE).
                setRealms(realm, model.getObject().getDynRealms());

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                membersTogglePanel.setHeader(target, model.getObject().getName());
                membersTogglePanel.setTargetObject(model.getObject());
                membersTogglePanel.toggle(target, model.getObject(), true);
            }

            @Override
            public boolean isIndicatorEnabled() {
                return false;
            }
        }, ActionType.MEMBERS,
                String.format("%s,%s", IdRepoEntitlement.GROUP_READ, IdRepoEntitlement.GROUP_UPDATE)).
                setRealms(realm, model.getObject().getDynRealms());

        ActionLink<GroupTO> provisionMembers = new ActionLink<GroupTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                try {
                    groupRestClient.provisionMembers(model.getObject().getKey(), ProvisionAction.PROVISION);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While provisioning members of group {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }.confirmMessage("confirmProvisionMembers");
        actions.add(
                provisionMembers,
                ActionType.PROVISION_MEMBERS,
                String.format("%s,%s", IdRepoEntitlement.TASK_CREATE, IdRepoEntitlement.TASK_EXECUTE),
                true).setRealm(realm);

        ActionLink<GroupTO> deprovisionMembers = new ActionLink<GroupTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                try {
                    groupRestClient.provisionMembers(model.getObject().getKey(), ProvisionAction.DEPROVISION);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While provisioning members of group {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }.confirmMessage("confirmDeprovisionMembers");
        actions.add(
                deprovisionMembers,
                ActionType.DEPROVISION_MEMBERS,
                String.format("%s,%s", IdRepoEntitlement.TASK_CREATE, IdRepoEntitlement.TASK_EXECUTE),
                true).setRealm(realm);

        SyncopeWebApplication.get().getAnyDirectoryPanelAdditionalActionLinksProvider().get(
                model.getObject(),
                realm,
                altDefaultModal,
                getString("any.edit", new Model<>(new GroupWrapper(model.getObject()))),
                this,
                pageRef).forEach(actions::add);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                target.add(utilityModal.setContent(new AnyPropagationTasks(
                        utilityModal, AnyTypeKind.GROUP, model.getObject().getKey(), pageRef)));
                utilityModal.header(new StringResourceModel("any.propagation.tasks", model));
                utilityModal.show(true);
            }
        }, ActionType.PROPAGATION_TASKS, IdRepoEntitlement.TASK_LIST);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                target.add(utilityModal.setContent(
                        new NotificationTasks(AnyTypeKind.GROUP, model.getObject().getKey(), pageRef)));
                utilityModal.header(new StringResourceModel("any.notification.tasks", model));
                utilityModal.show(true);
            }
        }, ActionType.NOTIFICATION_TASKS, IdRepoEntitlement.TASK_LIST);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -2878723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(altDefaultModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "GroupLogic",
                        model.getObject(),
                        IdRepoEntitlement.GROUP_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -5819724478921691835L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        GroupTO original = model.getObject();
                        try {
                            GroupTO updated = MAPPER.readValue(json, GroupTO.class);
                            GroupUR updateReq = AnyOperations.diff(updated, original, false);
                            ProvisioningResult<GroupTO> result =
                                    restClient.update(original.getETagValue(), updateReq);
                            model.getObject().setLastChangeDate(result.getEntity().getLastChangeDate());

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While restoring group {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                altDefaultModal.header(new Model<>(
                        getString("auditHistory.title", new Model<>(new GroupWrapper(model.getObject())))));

                altDefaultModal.show(true);
            }
        }, ActionType.VIEW_AUDIT_HISTORY,
                String.format("%s,%s", IdRepoEntitlement.GROUP_READ, IdRepoEntitlement.AUDIT_LIST)).
                setRealms(realm, model.getObject().getDynRealms());

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = 6242834621660352855L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                GroupTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(GroupDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(new GroupWrapper(clone), target));
            }

            @Override
            protected boolean statusCondition(final GroupTO modelObject) {
                return realm.startsWith(SyncopeConstants.ROOT_REALM);
            }
        }, ActionType.CLONE, IdRepoEntitlement.GROUP_CREATE).setRealm(realm);

        actions.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                try {
                    restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting group {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final GroupTO modelObject) {
                return realm.startsWith(SyncopeConstants.ROOT_REALM);
            }
        }, ActionType.DELETE, IdRepoEntitlement.GROUP_DELETE, true).setRealm(realm);

        return actions;
    }

    public static class Builder extends AnyDirectoryPanel.Builder<GroupTO, GroupRestClient> {

        private static final long serialVersionUID = 3844281520756293159L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final GroupRestClient groupRestClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, groupRestClient, type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<GroupTO>> newInstance(final String id, final boolean wizardInModal) {
            return new GroupDirectoryPanel(id, this, wizardInModal);
        }
    }
}
