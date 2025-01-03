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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.ConnectorDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ConnectorDirectoryPanel extends
        DirectoryPanel<Serializable, Serializable, ConnectorDataProvider, ConnectorRestClient> {

    private static final long serialVersionUID = 2041468935602350821L;

    @SpringBean
    protected AuditRestClient auditRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    protected String keyword;

    protected ConnectorDirectoryPanel(final String id, final ConnectorDirectoryPanel.Builder builder) {
        super(id, builder);

        if (SyncopeConsoleSession.get().owns("CONNECTOR_CREATE")) {
            MetaDataRoleAuthorizationStrategy.authorizeAll(addAjaxLink, RENDER);
        } else {
            MetaDataRoleAuthorizationStrategy.unauthorizeAll(addAjaxLink, RENDER);
        }

        setShowResultPanel(false);
        modal.size(Modal.Size.Large);
        initResultTable();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof final ConnectorSearchEvent payload) {
            AjaxRequestTarget target = payload.getTarget();
            if (StringUtils.isNotBlank(payload.getKeyword())) {
                keyword = payload.getKeyword().toLowerCase();
            }
            updateResultTable(target);
        } else {
            super.onEvent(event);
        }
    }

    @Override
    protected ConnectorDataProvider dataProvider() {
        dataProvider = new ConnectorDataProvider(restClient, rows, pageRef, keyword);
        return dataProvider;
    }

    public ConnectorDataProvider getDataProvider() {
        return dataProvider;
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_PARAMETERS_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<Serializable, String>> getColumns() {
        final List<IColumn<Serializable, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new ResourceModel("displayName"), "displayNameSortParam", "displayName"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("connectorName"), "connectorNameSortParam", "connectorName"));
        return columns;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.singletonList(ActionLink.ActionType.DELETE);
    }

    @Override
    public ActionsPanel<Serializable> getActions(final IModel<Serializable> model) {
        final ActionsPanel<Serializable> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 8345646188740279483L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(((ConnInstanceTO) model.getObject()).getKey());
                modelObject.setConnectorDisplayName(((ConnInstanceTO) model.getObject()).getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model.getObject());

                target.add(modal.setContent(new ResourceWizardBuilder(
                        modelObject, resourceRestClient, restClient, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"),
                        model.getObject().getKey())));
                modal.show(true);

                target.add(modal);
            }

        }, ActionLink.ActionType.CREATE_RESOURCE, String.format("%s", IdMEntitlement.RESOURCE_CREATE));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 8200500789152854321L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ConnInstanceTO connInstance = restClient.read(((ConnInstanceTO) model.getObject()).getKey());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(connInstance);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(connInstance, restClient, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(IdMEntitlement.CONNECTOR_UPDATE, connInstance.getAdminRealm())
                                ? AjaxWizard.Mode.EDIT
                                : AjaxWizard.Mode.READONLY)));

                modal.header(
                        new Model<>(MessageFormat.format(getString("connector.edit"), connInstance.getDisplayName())));
                modal.show(true);

            }
        }, ActionLink.ActionType.EDIT, String.format("%s,%s", IdMEntitlement.CONNECTOR_READ,
                IdMEntitlement.CONNECTOR_UPDATE));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 1085863437941911947L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ConnInstanceTO modelObject = restClient.read(((ConnInstanceTO) model.getObject()).getKey());

                target.add(altDefaultModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "ConnectorLogic",
                        modelObject,
                        IdMEntitlement.CONNECTOR_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -3225348282675513648L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            ConnInstanceTO updated = MAPPER.readValue(json, ConnInstanceTO.class);
                            restClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While restoring connector {}", ((ConnInstanceTO) model.getObject()).getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                altDefaultModal.header(
                        new Model<>(MessageFormat.format(getString("connector.menu.history"),
                                ((ConnInstanceTO) model.getObject()).getDisplayName())));

                altDefaultModal.show(true);
            }

        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY,
                String.format("%s,%s", IdMEntitlement.CONNECTOR_READ, IdRepoEntitlement.AUDIT_LIST));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -1544718936080799146L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                try {
                    restClient.delete(((ConnInstanceTO) model.getObject()).getKey());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');",
                            ((ConnInstanceTO) model.getObject()).getKey()));

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", ((ConnInstanceTO) model.getObject()).getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

        }, ActionLink.ActionType.DELETE, IdMEntitlement.CONNECTOR_DELETE, true);

        return panel;
    }

    public static class ConnectorSearchEvent implements Serializable {

        private static final long serialVersionUID = -282052400565266028L;

        private final AjaxRequestTarget target;

        private final String keyword;

        public ConnectorSearchEvent(final AjaxRequestTarget target, final String keyword) {
            this.target = target;
            this.keyword = keyword;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getKeyword() {
            return keyword;
        }
    }

    public static class Builder extends DirectoryPanel.Builder<Serializable, Serializable, ConnectorRestClient> {

        private static final long serialVersionUID = 6128427903964630093L;

        public Builder(final ConnectorRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
            setShowResultPage(false);
        }

        @Override
        protected WizardMgtPanel<Serializable> newInstance(final String id, final boolean wizardInModal) {
            return new ConnectorDirectoryPanel(id, this);
        }
    }
}
