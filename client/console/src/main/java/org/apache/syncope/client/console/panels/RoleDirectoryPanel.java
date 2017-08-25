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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.ConsoleLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.RoleDirectoryPanel.RoleDataProvider;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.role.RoleWrapper;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class RoleDirectoryPanel extends DirectoryPanel<RoleTO, RoleWrapper, RoleDataProvider, RoleRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected final BaseModal<String> utilityModal = new BaseModal<>("outer");

    protected final BaseModal<Serializable> membersModal = new BaseModal<>("outer");

    protected RoleDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.ROLE_CREATE);
        setReadOnly(!SyncopeConsoleSession.get().owns(StandardEntitlement.ROLE_UPDATE));

        disableCheckBoxes();
        setShowResultPage(true);

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
        return Constants.PREF_ROLE_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<RoleTO, String>> getColumns() {
        final List<IColumn<RoleTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel("key"), "key", "key"));
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

        panel.add(new ActionLink<RoleTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                send(RoleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new RoleWrapper(new RoleRestClient().read(model.getObject().getKey())),
                                target));
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.ROLE_READ);

        panel.add(new ActionLink<RoleTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                final RoleTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(RoleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(new RoleWrapper(clone), target));
            }
        }, ActionLink.ActionType.CLONE, StandardEntitlement.ROLE_CREATE);

        panel.add(new ActionLink<RoleTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                final String query = SyncopeClient.getUserSearchConditionBuilder().and(
                        SyncopeClient.getUserSearchConditionBuilder().inRoles(model.getObject().getKey()),
                        SyncopeClient.getUserSearchConditionBuilder().is("key").notNullValue()).query();

                final AnyTypeRestClient typeRestClient = new AnyTypeRestClient();
                final AnyTypeClassRestClient classRestClient = new AnyTypeClassRestClient();

                final AnyTypeTO anyTypeTO = typeRestClient.read(AnyTypeKind.USER.name());

                ModalPanel panel = new AnyPanel(BaseModal.CONTENT_ID, anyTypeTO, null, null, false, pageRef) {

                    private static final long serialVersionUID = -7514498203393023415L;

                    @Override
                    protected Panel getDirectoryPanel(final String id) {
                        final Panel panel = new UserDirectoryPanel.Builder(
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
                                panel,
                                WebPage.RENDER,
                                StandardEntitlement.USER_SEARCH);

                        return panel;
                    }
                };

                membersModal.header(new StringResourceModel("role.members", RoleDirectoryPanel.this, model));
                membersModal.setContent(panel);
                membersModal.show(true);
                target.add(membersModal);
            }
        }, ActionLink.ActionType.MEMBERS, StandardEntitlement.USER_SEARCH);

        panel.add(new ActionLink<RoleTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                final ConsoleLayoutInfo info = new ConsoleLayoutInfo(model.getObject().getKey());
                info.setContent(restClient.readConsoleLayoutInfo(model.getObject().getKey()));

                utilityModal.header(new ResourceModel("console.layout.info", "JSON Content"));
                utilityModal.setContent(new JsonEditorPanel(
                        utilityModal, new PropertyModel<String>(info, "content"), false, pageRef) {

                    private static final long serialVersionUID = -8927036362466990179L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            restClient.setConsoleLayoutInfo(info.getKey(), info.getContent());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            modal.show(false);
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While updating onsole layout info for role {}", info.getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                });
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.LAYOUT_EDIT, StandardEntitlement.ROLE_UPDATE);
        panel.add(new ActionLink<RoleTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                            getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.ROLE_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>singletonList(ActionLink.ActionType.DELETE);
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<RoleTO, RoleWrapper, RoleRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final PageReference pageRef) {
            super(new RoleRestClient(), pageRef);
        }

        @Override
        protected WizardMgtPanel<RoleWrapper> newInstance(final String id, final boolean wizardInModal) {
            return new RoleDirectoryPanel(id, this);
        }
    }

    protected class RoleDataProvider extends DirectoryDataProvider<RoleTO> {

        private static final long serialVersionUID = 6267494272884913376L;

        private final SortableDataProviderComparator<RoleTO> comparator;

        private final RoleRestClient restClient = new RoleRestClient();

        public RoleDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<RoleTO> iterator(final long first, final long count) {
            List<RoleTO> result = restClient.list();
            Collections.sort(result, comparator);
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
}
