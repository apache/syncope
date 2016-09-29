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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.notifications.NotificationTasks;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.status.AnyStatusModal;
import org.apache.syncope.client.console.tasks.AnyPropagationTasks;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BulkMembersActionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.springframework.util.ReflectionUtils;

public class GroupDirectoryPanel extends AnyDirectoryPanel<GroupTO, GroupRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    private final BaseModal<Serializable> typeExtensionsModal = new BaseModal<>("outer");

    protected final BaseModal<Serializable> membersModal = new BaseModal<>("outer");

    protected final MembersTogglePanel templates;

    protected GroupDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);

        typeExtensionsModal.size(Modal.Size.Large);
        addOuterObject(typeExtensionsModal);
        setWindowClosedReloadCallback(typeExtensionsModal);
        typeExtensionsModal.addSubmitButton();

        addOuterObject(membersModal);
        membersModal.size(Modal.Size.Large);

        templates = new MembersTogglePanel(page.getPageReference()) {

            private static final long serialVersionUID = -8765794727538618705L;

            @Override
            protected Serializable onApplyInternal(
                    final GroupTO groupTO, final String type, final AjaxRequestTarget target) {

                final AnyTypeRestClient typeRestClient = new AnyTypeRestClient();
                final AnyTypeClassRestClient classRestClient = new AnyTypeClassRestClient();

                final AnyTypeTO anyTypeTO = typeRestClient.read(type);

                ModalPanel panel = new AnyPanel(BaseModal.CONTENT_ID, anyTypeTO, null, null, false, pageRef) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Panel getDirectoryPanel(final String id) {

                        final Panel panel;

                        if (AnyTypeKind.USER.name().equals(type)) {
                            String query = SyncopeClient.getUserSearchConditionBuilder().and(
                                    SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()),
                                    SyncopeClient.getUserSearchConditionBuilder().is("key").notNullValue()).query();

                            panel = new UserDirectoryPanel.Builder(
                                    classRestClient.list(anyTypeTO.getClasses()), anyTypeTO.getKey(), pageRef).
                                    setRealm("/").
                                    setFiltered(true).
                                    setFiql(query).
                                    disableCheckBoxes().
                                    addNewItemPanelBuilder(FormLayoutInfoUtils.instantiate(
                                            new UserTO(),
                                            anyTypeTO.getClasses(),
                                            FormLayoutInfoUtils.fetch(typeRestClient.list()).getLeft(),
                                            pageRef), false).
                                    setWizardInModal(false).build(id);

                            MetaDataRoleAuthorizationStrategy.authorize(
                                    panel, WebPage.RENDER, StandardEntitlement.USER_SEARCH);
                        } else {
                            String query = SyncopeClient.getAnyObjectSearchConditionBuilder(type).and(
                                    SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()),
                                    SyncopeClient.getUserSearchConditionBuilder().is("key").notNullValue()).query();

                            panel = new AnyObjectDirectoryPanel.Builder(
                                    classRestClient.list(anyTypeTO.getClasses()), anyTypeTO.getKey(), pageRef).
                                    setRealm("/").
                                    setFiltered(true).
                                    setFiql(query).
                                    disableCheckBoxes().
                                    addNewItemPanelBuilder(FormLayoutInfoUtils.instantiate(
                                            new AnyObjectTO(),
                                            anyTypeTO.getClasses(),
                                            FormLayoutInfoUtils.fetch(typeRestClient.list()).getRight().get(type),
                                            pageRef), false).
                                    setWizardInModal(false).build(id);

                            MetaDataRoleAuthorizationStrategy.authorize(
                                    panel, WebPage.RENDER, AnyEntitlement.SEARCH.getFor(anyTypeTO.getKey()));
                        }

                        return panel;
                    }
                };

                membersModal.header(new StringResourceModel(
                        "group.members",
                        GroupDirectoryPanel.this,
                        Model.of(Pair.of(groupTO, type))));

                membersModal.setContent(panel);
                membersModal.show(true);
                target.add(membersModal);

                return null;
            }
        };

        addOuterObject(templates);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_GROUP_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<GroupTO, String>> getColumns() {
        final List<IColumn<GroupTO, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_DETAILS_VIEW)) {
            addPropertyColumn(name, ReflectionUtils.findField(GroupTO.class, name), columns);
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_PLAIN_ATTRS_VIEW)) {
            if (pSchemaNames.contains(name)) {
                columns.add(new AttrColumn<GroupTO>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_DER_ATTRS_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<GroupTO>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : GroupDisplayAttributesModalPanel.DEFAULT_SELECTION) {
                addPropertyColumn(name, ReflectionUtils.findField(GroupTO.class, name), columns);
            }

            prefMan.setList(getRequest(), getResponse(), Constants.PREF_GROUP_DETAILS_VIEW,
                    Arrays.asList(GroupDisplayAttributesModalPanel.DEFAULT_SELECTION));
        }

        columns.add(new ActionColumn<GroupTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<GroupTO> getActions(final String componentId, final IModel<GroupTO> model) {
                final ActionLinksPanel.Builder<GroupTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        templates.setTargetObject(model.getObject());
                        templates.toggle(target, true);
                    }

                    @Override
                    public boolean isIndicatorEnabled() {
                        return false;
                    }
                }, ActionType.MEMBERS, StandardEntitlement.GROUP_READ).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        IModel<AnyWrapper<GroupTO>> formModel = new CompoundPropertyModel<>(
                                new AnyWrapper<>(model.getObject()));
                        altDefaultModal.setFormModel(formModel);

                        target.add(altDefaultModal.setContent(new AnyStatusModal<>(
                                altDefaultModal,
                                pageRef,
                                formModel.getObject().getInnerObject(),
                                "resourceName",
                                false)));

                        altDefaultModal.header(new Model<>(
                                getString("any.edit", new Model<>(new AnyWrapper<>(model.getObject())))));

                        altDefaultModal.show(true);
                    }
                }, ActionType.MANAGE_RESOURCES, StandardEntitlement.GROUP_READ).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        send(GroupDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(new GroupWrapper(
                                        restClient.read(model.getObject().getKey())), target));
                    }
                }, ActionType.EDIT, StandardEntitlement.GROUP_READ).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = 6242834621660352855L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        target.add(typeExtensionsModal.setContent(new TypeExtensionDirectoryPanel(
                                typeExtensionsModal, model.getObject(), pageRef)));
                        typeExtensionsModal.header(new StringResourceModel("typeExtensions", model));
                        typeExtensionsModal.show(true);
                    }
                }, ActionType.TYPE_EXTENSIONS, StandardEntitlement.GROUP_UPDATE).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = 6242834621660352855L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        GroupTO clone = SerializationUtils.clone(model.getObject());
                        clone.setKey(null);
                        send(GroupDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.NewItemActionEvent<>(new GroupWrapper(clone), target));
                    }
                }, ActionType.CLONE, StandardEntitlement.GROUP_CREATE).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        target.add(utilityModal.setContent(new AnyPropagationTasks(
                                utilityModal, AnyTypeKind.GROUP, model.getObject().getKey(), pageRef)));
                        utilityModal.header(new StringResourceModel("any.propagation.tasks", model));
                        utilityModal.show(true);
                    }
                }, ActionType.PROPAGATION_TASKS, StandardEntitlement.TASK_LIST).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        target.add(utilityModal.setContent(
                                new NotificationTasks(AnyTypeKind.GROUP, model.getObject().getKey(), pageRef)));
                        utilityModal.header(new StringResourceModel("any.notification.tasks", model));
                        utilityModal.show(true);
                    }
                }, ActionType.NOTIFICATION_TASKS, StandardEntitlement.TASK_LIST).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        try {
                            restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.DELETE, StandardEntitlement.GROUP_DELETE).add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        try {
                            restClient.bulkMembersAction(model.getObject().getKey(), BulkMembersActionType.PROVISION);
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While provisioning members of group {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.PROVISION_MEMBERS,
                        String.format("%s,%s", StandardEntitlement.TASK_CREATE, StandardEntitlement.TASK_EXECUTE)).add(
                        new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        try {
                            restClient.bulkMembersAction(model.getObject().getKey(), BulkMembersActionType.DEPROVISION);
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While provisioning members of group {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.DEPROVISION_MEMBERS,
                        String.format("%s,%s", StandardEntitlement.TASK_CREATE, StandardEntitlement.TASK_EXECUTE));

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        target.add(displayAttributeModal.setContent(new GroupDisplayAttributesModalPanel<>(
                                displayAttributeModal, page.getPageReference(), pSchemaNames, dSchemaNames)));
                        displayAttributeModal.header(new ResourceModel("any.attr.display"));
                        displayAttributeModal.show(true);
                    }
                }, ActionType.CHANGE_VIEW, StandardEntitlement.GROUP_READ).add(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionType.RELOAD).build(componentId);
            }
        });

        return columns;
    }

    public static class Builder extends AnyDirectoryPanel.Builder<GroupTO, GroupRestClient> {

        private static final long serialVersionUID = 3844281520756293159L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new GroupRestClient(), type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<GroupTO>> newInstance(final String id, final boolean wizardInModal) {
            return new GroupDirectoryPanel(id, this, wizardInModal);
        }
    }
}
