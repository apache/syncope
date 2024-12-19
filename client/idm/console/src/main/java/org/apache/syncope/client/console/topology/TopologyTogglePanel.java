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
package org.apache.syncope.client.console.topology;

import com.fasterxml.jackson.databind.json.JsonMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.text.MessageFormat;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.panels.ConnObjects;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.status.ResourceStatusModal;
import org.apache.syncope.client.console.tasks.LiveSyncTask;
import org.apache.syncope.client.console.tasks.PropagationTasks;
import org.apache.syncope.client.console.tasks.PullTasks;
import org.apache.syncope.client.console.tasks.PushTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.client.console.wizards.resources.ResourceProvision;
import org.apache.syncope.client.console.wizards.resources.ResourceProvisionPanel;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TopologyTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final WebMarkupContainer container;

    protected final BaseModal<Serializable> propTaskModal;

    protected final BaseModal<Serializable> schedTaskModal;

    protected final BaseModal<Serializable> provisionModal;

    protected final BaseModal<Serializable> historyModal;

    public TopologyTogglePanel(final String id, final PageReference pageRef) {
        super(id, "topologyTogglePanel", pageRef);

        modal.size(Modal.Size.Large);
        setFooterVisibility(false);

        propTaskModal = new BaseModal<>(Constants.OUTER);
        propTaskModal.size(Modal.Size.Large);
        addOuterObject(propTaskModal);

        schedTaskModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = 389935548143327858L;

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

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void toggleWithContent(final AjaxRequestTarget target, final TopologyNode node) {
        setHeader(target, node.getDisplayName());

        modal.setWindowClosedCallback(t -> {
            modal.show(false);
            send(pageRef.getPage(), Broadcast.DEPTH, new UpdateEvent(node.getKey(), t));
        });

        switch (node.getKind()) {
            case SYNCOPE ->
                container.addOrReplace(getSyncopeFragment(pageRef));
            case CONNECTOR_SERVER ->
                container.addOrReplace(getLocationFragment(node, pageRef));
            case FS_PATH ->
                container.addOrReplace(getLocationFragment(node, pageRef));
            case CONNECTOR ->
                container.addOrReplace(getConnectorFragment(node, pageRef));
            case RESOURCE ->
                container.addOrReplace(getResourceFragment(node, pageRef));
            default ->
                container.addOrReplace(getEmptyFragment());
        }

        target.add(container);

        toggle(target, node, true);
    }

    @Override
    protected String getTargetKey(final Serializable modelObject) {
        if (modelObject instanceof ResourceProvision resourceProvision) {
            return resourceProvision.getKey();
        }
        if (modelObject instanceof TopologyNode topologyNode) {
            return topologyNode.getKey();
        }
        return super.getTargetKey(modelObject);
    }

    private Fragment getEmptyFragment() {
        return new Fragment("actions", "emptyFragment", this);
    }

    private Fragment getSyncopeFragment(final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "syncopeActions", this);

        AjaxLink<String> reload = new IndicatingOnConfirmAjaxLink<>("reload", "connectors.confirm.reload", true) {

            private static final long serialVersionUID = -2075933173666007020L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.reload();
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While reloading all connectors", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        fragment.add(reload);
        MetaDataRoleAuthorizationStrategy.authorize(reload, RENDER, IdMEntitlement.CONNECTOR_RELOAD);

        return fragment;
    }

    private Fragment getLocationFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "locationActions", this);

        AjaxLink<String> create = new IndicatingAjaxLink<>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation(node.getKey());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(modelObject, connectorRestClient, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.new"), node.getKey())));
                modal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        fragment.add(create);
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, IdMEntitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "connectorActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<>("delete", Constants.CONFIRM_DELETE, true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(String.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, IdMEntitlement.CONNECTOR_DELETE);
        fragment.add(delete);

        AjaxLink<String> create = new IndicatingAjaxLink<>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(String.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(
                        modelObject, resourceRestClient, connectorRestClient, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));
                modal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, IdMEntitlement.RESOURCE_CREATE);
        fragment.add(create);

        AjaxLink<String> edit = new IndicatingAjaxLink<>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ConnInstanceTO connInstance = connectorRestClient.read(String.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(connInstance);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(connInstance, connectorRestClient, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(IdMEntitlement.CONNECTOR_UPDATE, connInstance.getAdminRealm())
                                ? AjaxWizard.Mode.EDIT
                                : AjaxWizard.Mode.READONLY)));

                modal.header(
                        new Model<>(MessageFormat.format(getString("connector.edit"), connInstance.getDisplayName())));
                modal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, IdMEntitlement.CONNECTOR_READ);
        fragment.add(edit);

        AjaxLink<String> history = new IndicatingAjaxLink<>("history") {

            private static final long serialVersionUID = -1876519166660008562L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ConnInstanceTO modelObject = connectorRestClient.read(node.getKey());

                target.add(historyModal.setContent(new AuditHistoryModal<>(
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
                            connectorRestClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            toggle(target, false);
                        } catch (Exception e) {
                            LOG.error("While restoring connector {}", node.getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                historyModal.header(
                        new Model<>(MessageFormat.format(getString("connector.menu.history"), node.getDisplayName())));

                historyModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(history, RENDER,
                String.format("%s,%s", IdMEntitlement.CONNECTOR_READ, IdRepoEntitlement.AUDIT_LIST));
        fragment.add(history);

        return fragment;
    }

    private Fragment getResourceFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "resourceActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<>("delete", Constants.CONFIRM_DELETE, true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, IdMEntitlement.RESOURCE_DELETE);
        fragment.add(delete);

        AjaxLink<String> edit = new IndicatingAjaxLink<>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO resource = resourceRestClient.read(node.getKey());
                ConnInstanceTO connInstance = connectorRestClient.read(resource.getConnector());

                IModel<ResourceTO> model = new CompoundPropertyModel<>(resource);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(
                        resource, resourceRestClient, connectorRestClient, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(IdMEntitlement.RESOURCE_UPDATE, connInstance.getAdminRealm())
                                ? AjaxWizard.Mode.EDIT
                                : AjaxWizard.Mode.READONLY)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));
                modal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, IdMEntitlement.RESOURCE_READ);
        fragment.add(edit);

        AjaxLink<String> status = new IndicatingAjaxLink<>("reconciliation") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey());
                target.add(propTaskModal.setContent(new ResourceStatusModal(pageRef, modelObject)));
                propTaskModal.header(
                        new Model<>(MessageFormat.format(getString("resource.reconciliation"), node.getKey())));
                propTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(status, RENDER, IdRepoEntitlement.USER_UPDATE);
        fragment.add(status);

        AjaxLink<String> provision = new IndicatingAjaxLink<>("provision") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO resource = resourceRestClient.read(node.getKey());
                ConnInstanceTO connInstance = connectorRestClient.read(resource.getConnector());

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

                provisionModal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));
                provisionModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, IdMEntitlement.RESOURCE_READ);
        fragment.add(provision);

        AjaxLink<String> explore = new IndicatingAjaxLink<>("explore") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO resource = resourceRestClient.read(node.getKey());

                target.add(propTaskModal.setContent(new ConnObjects(resource, pageRef)));
                propTaskModal.header(new StringResourceModel("resource.explore.list", Model.of(node)));
                propTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(explore, RENDER, IdMEntitlement.RESOURCE_LIST_CONNOBJECT);
        fragment.add(explore);

        AjaxLink<String> propagation = new IndicatingAjaxLink<>("propagation") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            @SuppressWarnings("unchecked")
            public void onClick(final AjaxRequestTarget target) {
                target.add(propTaskModal.setContent(
                        new PropagationTasks(propTaskModal, node.getKey(), pageRef)));
                propTaskModal.header(
                        new Model<>(MessageFormat.format(getString("task.propagation.list"), node.getKey())));
                propTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(propagation, RENDER, IdRepoEntitlement.TASK_LIST);
        fragment.add(propagation);

        AjaxLink<String> pull = new IndicatingAjaxLink<>("pull") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PullTasks(schedTaskModal, node.getKey(), pageRef)));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.pull.list"), node.getKey())));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(pull, RENDER, IdRepoEntitlement.TASK_LIST);
        fragment.add(pull);

        AjaxLink<String> livesync = new IndicatingAjaxLink<>("livesync") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new LiveSyncTask(schedTaskModal, node.getKey(), pageRef)));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.livesync"), node.getKey())));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(livesync, RENDER, IdRepoEntitlement.TASK_READ);
        fragment.add(livesync);

        AjaxLink<String> push = new IndicatingAjaxLink<>("push") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PushTasks(schedTaskModal, node.getKey(), pageRef)));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.push.list"), node.getKey())));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(push, RENDER, IdRepoEntitlement.TASK_LIST);
        fragment.add(push);

        AjaxLink<String> history = new IndicatingAjaxLink<>("history") {

            private static final long serialVersionUID = -1876519166660008562L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey());

                target.add(historyModal.setContent(new AuditHistoryModal<>(
                        OpEvent.CategoryType.LOGIC,
                        "ResourceLogic",
                        modelObject,
                        IdMEntitlement.RESOURCE_UPDATE,
                        auditRestClient) {

                    private static final long serialVersionUID = -3712506022627033811L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        try {
                            ResourceTO updated = MAPPER.readValue(json, ResourceTO.class);
                            resourceRestClient.update(updated);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            toggle(target, false);
                        } catch (Exception e) {
                            LOG.error("While restoring resource {}", node.getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                historyModal.header(
                        new Model<>(MessageFormat.format(getString("resource.menu.history"), node.getDisplayName())));

                historyModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(history, RENDER,
                String.format("%s,%s", IdMEntitlement.RESOURCE_READ, IdRepoEntitlement.AUDIT_LIST));
        fragment.add(history);

        AjaxLink<String> clone = new IndicatingAjaxLink<>("clone") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    ResourceTO resource = resourceRestClient.read(node.getKey());
                    resource.setKey("Copy of " + node.getKey());

                    for (Provision provision : resource.getProvisions()) {
                        if (provision.getMapping() != null) {
                            provision.getMapping().getLinkingItems().clear();
                        }
                        provision.getVirSchemas().clear();
                    }
                    target.add(modal.setContent(new ResourceWizardBuilder(
                            resource, resourceRestClient, connectorRestClient, pageRef).
                            build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                    modal.header(new Model<>(MessageFormat.format(getString("resource.clone"), node.getKey())));
                    modal.show(true);

                } catch (SyncopeClientException e) {
                    LOG.error("While cloning resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(clone, RENDER, IdMEntitlement.RESOURCE_CREATE);
        fragment.add(clone);

        return fragment;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent<?> item
                && item.getResult() instanceof ConnInstanceTO connInstance) {

            // update Toggle Panel header
            item.getTarget().ifPresent(t -> setHeader(t, connInstance.getDisplayName()));
        }
    }

    public static final class UpdateEvent {

        private final AjaxRequestTarget target;

        private final String key;

        public UpdateEvent(final String key, final AjaxRequestTarget target) {
            this.target = target;
            this.key = key;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getKey() {
            return key;
        }
    }
}
