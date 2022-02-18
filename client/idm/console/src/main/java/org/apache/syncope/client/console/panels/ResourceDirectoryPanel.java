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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.commons.ResourceDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.status.ResourceStatusModal;
import org.apache.syncope.client.console.tasks.PropagationTasks;
import org.apache.syncope.client.console.tasks.PullTasks;
import org.apache.syncope.client.console.tasks.PushTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.resources.ResourceProvisionPanel;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
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
import org.apache.wicket.model.StringResourceModel;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResourceDirectoryPanel extends
        DirectoryPanel<Serializable, Serializable, ResourceDataProvider, ResourceRestClient> {

    private static final long serialVersionUID = -5223129956783782225L;

    private String keyword;

    private final BaseModal<Serializable> propTaskModal;

    private final BaseModal<Serializable> schedTaskModal;

    private final BaseModal<Serializable> provisionModal;

    private final BaseModal<Serializable> historyModal;

    protected ResourceDirectoryPanel(final String id, final ResourceDirectoryPanel.Builder builder) {
        super(id, builder);

        if (SyncopeConsoleSession.get().owns("RESOURCE_CREATE")) {
            MetaDataRoleAuthorizationStrategy.authorizeAll(addAjaxLink, RENDER);
        } else {
            MetaDataRoleAuthorizationStrategy.unauthorizeAll(addAjaxLink, RENDER);
        }

        setShowResultPage(false);
        modal.size(Modal.Size.Large);
        initResultTable();

        restClient = builder.restClient;

        propTaskModal = new BaseModal<>(Constants.OUTER);
        propTaskModal.size(Modal.Size.Large);
        addOuterObject(propTaskModal);

        schedTaskModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = -6165152045136958913L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        schedTaskModal.size(Modal.Size.Large);
        addOuterObject(schedTaskModal);

        provisionModal = new BaseModal<>(Constants.OUTER);
        provisionModal.size(Modal.Size.Large);
        provisionModal.addSubmitButton();
        addOuterObject(provisionModal);

        historyModal = new BaseModal<>(Constants.OUTER);
        historyModal.size(Modal.Size.Large);
        addOuterObject(historyModal);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ResourceSearchEvent) {
            ResourceSearchEvent payload = (ResourceSearchEvent) event.getPayload();
            AjaxRequestTarget target = payload.getTarget();
            if (StringUtils.isNotEmpty(payload.getKeyword())) {
                keyword = payload.getKeyword().toLowerCase();
            }
            updateResultTable(target);
        } else {
            super.onEvent(event);
        }
    }

    @Override
    protected ResourceDataProvider dataProvider() {
        dataProvider = new ResourceDataProvider(rows, pageRef, keyword);
        return dataProvider;
    }

    public ResourceDataProvider getDataProvider() {
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
                new ResourceModel("key"), "keySortParam", "key"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("connectorDisplayName"), "connectorDisplayNameSortParam", "connectorDisplayName"));
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

            private static final long serialVersionUID = -7220222653598674870L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ResourceTO resource = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());
                ConnInstanceTO connInstance = ConnectorRestClient.read(resource.getConnector());

                IModel<ResourceTO> model = new CompoundPropertyModel<>(resource);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(resource, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(IdMEntitlement.RESOURCE_UPDATE, connInstance.getAdminRealm())
                                        ? AjaxWizard.Mode.EDIT
                                        : AjaxWizard.Mode.READONLY)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.edit"), model.getObject().getKey())));
                modal.show(true);
            }
        }, ActionLink.ActionType.EDIT, String.format("%s,%s", IdMEntitlement.RESOURCE_READ,
                IdMEntitlement.RESOURCE_UPDATE));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -6467344504797047254L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ResourceTO resource = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());
                ConnInstanceTO connInstance = ConnectorRestClient.read(resource.getConnector());

                if (SyncopeConsoleSession.get().
                        owns(IdMEntitlement.RESOURCE_UPDATE, connInstance.getAdminRealm())) {

                    provisionModal.addSubmitButton();
                } else {
                    provisionModal.removeSubmitButton();
                }

                IModel<ResourceTO> model = new CompoundPropertyModel<>(resource);
                provisionModal.setFormModel(model);

                target.add(provisionModal.setContent(
                        new ResourceProvisionPanel(provisionModal, resource, connInstance.getAdminRealm(), pageRef)));

                provisionModal.header(new Model<>(MessageFormat.format(getString("resource.edit"),
                        model.getObject().getKey())));
                provisionModal.show(true);
            }
        }, ActionLink.ActionType.MAPPING, String.format("%s,%s", IdMEntitlement.RESOURCE_READ,
                IdMEntitlement.RESOURCE_UPDATE));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -1448897313753684142L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ResourceTO resource = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());

                target.add(propTaskModal.setContent(new ConnObjects(resource, pageRef)));
                propTaskModal.header(new StringResourceModel("resource.explore.list", Model.of(model.getObject())));
                propTaskModal.show(true);
            }
        }, ActionLink.ActionType.EXPLORE_RESOURCE, IdMEntitlement.RESOURCE_LIST_CONNOBJECT);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 4800323783814856195L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(propTaskModal.setContent(
                        new PropagationTasks(propTaskModal, ((ResourceTO) model.getObject()).getKey(), pageRef)));
                propTaskModal.header(new Model<>(MessageFormat.format(getString("task.propagation.list"),
                        ((ResourceTO) model.getObject()).getKey())));
                propTaskModal.show(true);
            }
        }, ActionLink.ActionType.PROPAGATION_TASKS, IdRepoEntitlement.TASK_LIST);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4699610013584898667L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(schedTaskModal.setContent(new PullTasks(schedTaskModal, pageRef,
                        ((ResourceTO) model.getObject()).getKey())));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.pull.list"),
                        ((ResourceTO) model.getObject()).getKey())));
                schedTaskModal.show(true);
            }
        }, ActionLink.ActionType.PULL_TASKS, IdRepoEntitlement.TASK_LIST);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 2042227976628604686L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(schedTaskModal.setContent(new PushTasks(schedTaskModal, pageRef,
                        ((ResourceTO) model.getObject()).getKey())));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.push.list"),
                        ((ResourceTO) model.getObject()).getKey())));
                schedTaskModal.show(true);
            }
        }, ActionLink.ActionType.PUSH_TASKS, IdRepoEntitlement.TASK_LIST);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5962061673680621813L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ResourceTO modelObject = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());
                target.add(propTaskModal.setContent(
                        new ResourceStatusModal(propTaskModal, pageRef, modelObject)));
                propTaskModal.header(new Model<>(MessageFormat.format(getString("resource.reconciliation"),
                        ((ResourceTO) model.getObject()).getKey())));
                propTaskModal.show(true);
            }
        }, ActionLink.ActionType.RECONCILIATION_RESOURCE, IdRepoEntitlement.USER_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5432034353017728766L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                ResourceTO modelObject = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());

                target.add(historyModal.setContent(new AuditHistoryModal<>(
                        historyModal,
                        AuditElements.EventCategoryType.LOGIC,
                        "ResourceLogic",
                        modelObject,
                        IdMEntitlement.RESOURCE_UPDATE,
                        pageRef) {

                    private static final long serialVersionUID = -3712506022627033811L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            ResourceTO updated = MAPPER.readValue(json, ResourceTO.class);
                            ResourceRestClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While restoring resource {}", ((ResourceTO) model.getObject()).getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                historyModal.header(
                        new Model<>(MessageFormat.format(getString("resource.menu.history"),
                                ((ResourceTO) model.getObject()).getKey())));

                historyModal.show(true);
            }
        }, ActionLink.ActionType.VIEW_AUDIT_HISTORY, String.format("%s,%s", IdMEntitlement.RESOURCE_READ,
                IdRepoEntitlement.AUDIT_LIST));

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 7019899256702149874L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                try {
                    ResourceTO resource = ResourceRestClient.read(((ResourceTO) model.getObject()).getKey());
                    resource.setKey("Copy of " + resource.getKey());
                    // reset some resource objects keys
                    if (resource.getOrgUnit() != null) {
                        resource.getOrgUnit().setKey(null);
                        resource.getOrgUnit().getItems().forEach(item -> item.setKey(null));
                    }
                    resource.getProvisions().forEach(provision -> {
                        provision.setKey(null);
                        if (provision.getMapping() != null) {
                            provision.getMapping().getItems().forEach(item -> item.setKey(null));
                            provision.getMapping().getLinkingItems().clear();
                        }
                        provision.getVirSchemas().clear();
                    });
                    target.add(modal.setContent(new ResourceWizardBuilder(resource, pageRef).
                            build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                    modal.header(new Model<>(MessageFormat.format(getString("resource.clone"), resource.getKey())));
                    modal.show(true);
                } catch (SyncopeClientException e) {
                    LOG.error("While cloning resource {}", ((ResourceTO) model.getObject()).getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.CLONE, IdMEntitlement.RESOURCE_CREATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 4516186028545701573L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                try {
                    ResourceRestClient.delete(((ResourceTO) model.getObject()).getKey());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');",
                            ((ResourceTO) model.getObject()).getKey()));
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", ((ResourceTO) model.getObject()).getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdMEntitlement.RESOURCE_DELETE, true);

        return panel;
    }

    public static class ResourceSearchEvent implements Serializable {

        private static final long serialVersionUID = 213974502541311941L;

        private final AjaxRequestTarget target;

        private final String keyword;

        public ResourceSearchEvent(final AjaxRequestTarget target, final String keyword) {
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
    
    public static class Builder extends DirectoryPanel.Builder<Serializable, Serializable, ResourceRestClient> {

        private static final long serialVersionUID = -1391308721262593468L;

        public Builder(final PageReference pageRef) {
            super(new ResourceRestClient(), pageRef);
            setShowResultPage(false);
        }

        @Override
        protected WizardMgtPanel<Serializable> newInstance(final String id, final boolean wizardInModal) {
            return new ResourceDirectoryPanel(id, this);
        }
    }
}
