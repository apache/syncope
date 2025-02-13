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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.layout.AnyLayoutWrapper;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.RoleDirectoryPanel.RoleDataProvider;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.role.RoleWrapper;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RoleDirectoryPanel extends DirectoryPanel<RoleTO, RoleWrapper, RoleDataProvider, RoleRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    protected final BaseModal<String> utilityModal = new BaseModal<>(Constants.OUTER);

    protected final BaseModal<Serializable> membersModal = new BaseModal<>(Constants.OUTER);

    protected RoleDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.ROLE_CREATE);
        setReadOnly(!SyncopeConsoleSession.get().owns(IdRepoEntitlement.ROLE_UPDATE));

        disableCheckBoxes();
        setShowResultPanel(true);

        modal.size(Modal.Size.Large);
        initResultTable();

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);
        utilityModal.addSubmitButton();

        addOuterObject(membersModal);
        membersModal.size(Modal.Size.Large);
    }

    @Override
    protected RoleDataProvider dataProvider() {
        return new RoleDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_ROLE_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<RoleTO, String>> getColumns() {
        final List<IColumn<RoleTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new ResourceModel("entitlements", "Entitlements"), null, "entitlements"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("realms"), null, "realms"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("dynRealms"), null, "dynRealms"));

        return columns;
    }

    @Override
    public ActionsPanel<RoleTO> getActions(final IModel<RoleTO> model) {
        final ActionsPanel<RoleTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                send(RoleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new RoleWrapper(restClient.read(model.getObject().getKey())), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.ROLE_READ);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                RoleTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(RoleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(new RoleWrapper(clone), target));
            }
        }, ActionLink.ActionType.CLONE, IdRepoEntitlement.ROLE_CREATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                AnyTypeTO userType = anyTypeRestClient.read(AnyTypeKind.USER.name());

                AnyLayout layout = AnyLayoutUtils.fetch(restClient, anyTypeRestClient.list());

                ModalPanel anyPanel = new AnyPanel.Builder<>(
                        layout.getAnyPanelClass(), BaseModal.CONTENT_ID, userType, null, layout, false, pageRef).
                        build((id, anyTypeTO, realmTO, anyLayout, pageRef) -> {

                            String query = SyncopeClient.getUserSearchConditionBuilder().
                                    inRoles(model.getObject().getKey()).query();

                            Panel panel = new UserDirectoryPanel.Builder(
                                    anyTypeClassRestClient.list(anyTypeTO.getClasses()),
                                    userRestClient,
                                    anyTypeTO.getKey(),
                                    pageRef).
                                    setRealm(SyncopeConstants.ROOT_REALM).
                                    setFiltered(true).
                                    setFiql(query).
                                    disableCheckBoxes().
                                    addNewItemPanelBuilder(AnyLayoutUtils.newLayoutInfo(
                                            new UserTO(),
                                            anyTypeTO.getClasses(),
                                            anyLayout.getUser(),
                                            userRestClient,
                                            pageRef), false).
                                    setWizardInModal(false).build(id);

                            MetaDataRoleAuthorizationStrategy.authorize(
                                    panel,
                                    WebPage.RENDER,
                                    IdRepoEntitlement.USER_SEARCH);

                            return panel;
                        });

                membersModal.header(new StringResourceModel("role.members", RoleDirectoryPanel.this, model));
                membersModal.setContent(anyPanel);
                membersModal.show(true);
                target.add(membersModal);
            }
        }, ActionLink.ActionType.MEMBERS, IdRepoEntitlement.USER_SEARCH);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                AnyLayoutWrapper wrapper = new AnyLayoutWrapper(
                        model.getObject().getKey(),
                        AnyLayoutUtils.defaultIfEmpty(
                                restClient.readAnyLayout(model.getObject().getKey()), anyTypeRestClient.list()));

                utilityModal.header(new ResourceModel("console.layout.info", "JSON Content"));
                utilityModal.setContent(new JsonEditorPanel(
                        utilityModal, new PropertyModel<>(wrapper, "content"), false, pageRef) {

                    private static final long serialVersionUID = -8927036362466990179L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            restClient.setAnyLayout(wrapper.getKey(), wrapper.getContent());

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            modal.show(false);
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While updating console layout for role {}", wrapper.getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                });
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.LAYOUT_EDIT, IdRepoEntitlement.ROLE_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.ROLE_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of(ActionLink.ActionType.DELETE);
    }

    protected class RoleDataProvider extends DirectoryDataProvider<RoleTO> {

        private static final long serialVersionUID = 6267494272884913376L;

        private final SortableDataProviderComparator<RoleTO> comparator;

        public RoleDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<RoleTO> iterator(final long first, final long count) {
            List<RoleTO> result = restClient.list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<RoleTO> model(final RoleTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<RoleTO, RoleWrapper, RoleRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final RoleRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
        }

        @Override
        protected WizardMgtPanel<RoleWrapper> newInstance(final String id, final boolean wizardInModal) {
            return new RoleDirectoryPanel(id, this);
        }
    }
}
