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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.HistoryConfList;
import org.apache.syncope.client.console.panels.ConnObjects;
import org.apache.syncope.client.console.wizards.resources.ConnectorWizardBuilder;
import org.apache.syncope.client.console.wizards.resources.ResourceWizardBuilder;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.status.ResourceStatusModal;
import org.apache.syncope.client.console.tasks.PropagationTasks;
import org.apache.syncope.client.console.tasks.PushTasks;
import org.apache.syncope.client.console.tasks.SchedTasks;
import org.apache.syncope.client.console.tasks.PullTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.resources.AbstractResourceWizardBuilder;
import org.apache.syncope.client.console.wizards.resources.ResourceProvisionPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class TopologyTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final WebMarkupContainer container;

    protected final BaseModal<Serializable> propTaskModal;

    protected final BaseModal<Serializable> schedTaskModal;

    protected final BaseModal<Serializable> provisionModal;

    private final BaseModal<Serializable> historyModal;

    public TopologyTogglePanel(final String id, final PageReference pageRef) {
        super(id, "topologyTogglePanel", pageRef);

        modal.size(Modal.Size.Large);
        setFooterVisibility(false);

        propTaskModal = new BaseModal<>("outer");
        propTaskModal.size(Modal.Size.Large);
        addOuterObject(propTaskModal);

        schedTaskModal = new BaseModal<Serializable>("outer") {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        schedTaskModal.size(Modal.Size.Large);
        addOuterObject(schedTaskModal);

        provisionModal = new BaseModal<>("outer");
        provisionModal.size(Modal.Size.Large);
        provisionModal.addSubmitButton();
        addOuterObject(provisionModal);

        historyModal = new BaseModal<>("outer");
        historyModal.size(Modal.Size.Large);
        addOuterObject(historyModal);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void toggleWithContent(final AjaxRequestTarget target, final TopologyNode node) {
        setHeader(target, node.getDisplayName());

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
                send(pageRef.getPage(), Broadcast.DEPTH, new UpdateEvent(node.getKey(), target));
            }
        });

        switch (node.getKind()) {
            case SYNCOPE:
                container.addOrReplace(getSyncopeFragment(pageRef));
                break;
            case CONNECTOR_SERVER:
                container.addOrReplace(getLocationFragment(node, pageRef));
                break;
            case FS_PATH:
                container.addOrReplace(getLocationFragment(node, pageRef));
                break;
            case CONNECTOR:
                container.addOrReplace(getConnectorFragment(node, pageRef));
                break;
            case RESOURCE:
                container.addOrReplace(getResurceFragment(node, pageRef));
                break;
            default:
                container.addOrReplace(getEmptyFragment());
        }

        target.add(container);

        this.toggle(target, true);
    }

    private Fragment getEmptyFragment() {
        return new Fragment("actions", "emptyFragment", this);
    }

    private Fragment getSyncopeFragment(final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "syncopeActions", this);

        AjaxLink<String> reload = new IndicatingOnConfirmAjaxLink<String>("reload", "connectors.confirm.reload", true) {

            private static final long serialVersionUID = -2075933173666007020L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.reload();
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While reloading all connectors", e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        fragment.add(reload);
        MetaDataRoleAuthorizationStrategy.authorize(reload, RENDER, StandardEntitlement.CONNECTOR_RELOAD);

        AjaxLink<String> tasks = new IndicatingAjaxLink<String>("tasks") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new SchedTasks(schedTaskModal, pageRef)));
                schedTaskModal.header(new ResourceModel("task.custom.list"));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }
        };
        fragment.add(tasks);
        MetaDataRoleAuthorizationStrategy.authorize(tasks, RENDER, StandardEntitlement.TASK_LIST);

        return fragment;
    }

    private Fragment getLocationFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "locationActions", this);

        AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation(node.getKey());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(modelObject, pageRef).
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
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, StandardEntitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "connectorActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(String.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, StandardEntitlement.CONNECTOR_DELETE);
        fragment.add(delete);

        AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(String.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(modelObject, pageRef).
                        build(BaseModal.CONTENT_ID, AjaxWizard.Mode.CREATE)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));
                modal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(create, RENDER, StandardEntitlement.RESOURCE_CREATE);
        fragment.add(create);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ConnInstanceTO connInstance = connectorRestClient.read(String.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(connInstance);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorWizardBuilder(connInstance, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(StandardEntitlement.CONNECTOR_UPDATE, connInstance.getAdminRealm())
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
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.CONNECTOR_READ);
        fragment.add(edit);

        AjaxLink<String> history = new IndicatingAjaxLink<String>("history") {

            private static final long serialVersionUID = -1876519166660008562L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                String connKey = String.class.cast(node.getKey());
                ConnInstanceTO connInstance = connectorRestClient.read(connKey);

                target.add(historyModal.setContent(
                        new HistoryConfList<>(historyModal, connKey, pageRef, connInstance)));

                historyModal.header(
                        new Model<>(MessageFormat.format(getString("connector.menu.history"), node.getDisplayName())));

                historyModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(history, RENDER, StandardEntitlement.CONNECTOR_HISTORY_LIST);
        fragment.add(history);

        return fragment;
    }

    private Fragment getResurceFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "resourceActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, RENDER, StandardEntitlement.RESOURCE_DELETE);
        fragment.add(delete);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO resource = resourceRestClient.read(node.getKey());
                ConnInstanceTO connInstance = connectorRestClient.read(resource.getConnector());

                IModel<ResourceTO> model = new CompoundPropertyModel<>(resource);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceWizardBuilder(resource, pageRef).
                        build(BaseModal.CONTENT_ID,
                                SyncopeConsoleSession.get().
                                        owns(StandardEntitlement.RESOURCE_UPDATE, connInstance.getAdminRealm())
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
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.RESOURCE_READ);
        fragment.add(edit);

        AjaxLink<String> status = new IndicatingAjaxLink<String>("status") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO modelObject = resourceRestClient.read(node.getKey());
                target.add(propTaskModal.setContent(
                        new ResourceStatusModal(propTaskModal, pageRef, modelObject)));
                propTaskModal.header(new Model<>(MessageFormat.format(getString("resource.provisioning.status"),
                        node.getKey())));
                propTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(status, RENDER, StandardEntitlement.USER_UPDATE);
        fragment.add(status);

        AjaxLink<String> provision = new IndicatingAjaxLink<String>("provision") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                ResourceTO resource = resourceRestClient.read(node.getKey());
                ConnInstanceTO connInstance = connectorRestClient.read(resource.getConnector());

                if (SyncopeConsoleSession.get().
                        owns(StandardEntitlement.RESOURCE_UPDATE, connInstance.getAdminRealm())) {

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
        MetaDataRoleAuthorizationStrategy.authorize(edit, RENDER, StandardEntitlement.RESOURCE_READ);
        fragment.add(provision);

        AjaxLink<String> explore = new IndicatingAjaxLink<String>("explore") {

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
        MetaDataRoleAuthorizationStrategy.authorize(explore, RENDER, StandardEntitlement.RESOURCE_LIST_CONNOBJECT);
        fragment.add(explore);

        AjaxLink<String> propagation = new IndicatingAjaxLink<String>("propagation") {

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
        MetaDataRoleAuthorizationStrategy.authorize(propagation, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(propagation);

        AjaxLink<String> pull = new IndicatingAjaxLink<String>("pull") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PullTasks(schedTaskModal, pageRef, node.getKey())));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.pull.list"), node.getKey())));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(pull, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(pull);

        AjaxLink<String> push = new IndicatingAjaxLink<String>("push") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(schedTaskModal.setContent(new PushTasks(schedTaskModal, pageRef, node.getKey())));
                schedTaskModal.header(new Model<>(MessageFormat.format(getString("task.push.list"), node.getKey())));
                schedTaskModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(push, RENDER, StandardEntitlement.TASK_LIST);
        fragment.add(push);

        AjaxLink<String> history = new IndicatingAjaxLink<String>("history") {

            private static final long serialVersionUID = -1876519166660008562L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                String resourceKey = String.class.cast(node.getKey());
                final ResourceTO modelObject = resourceRestClient.read(String.class.cast(node.getKey()));

                target.add(historyModal.setContent(
                        new HistoryConfList<>(historyModal, resourceKey, pageRef, modelObject)));

                historyModal.header(
                        new Model<>(MessageFormat.format(getString("resource.menu.history"), node.getDisplayName())));

                historyModal.show(true);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(history, RENDER, StandardEntitlement.RESOURCE_HISTORY_LIST);
        fragment.add(history);

        // [SYNCOPE-1161] - Option to clone a resource
        AjaxLink<String> clone = new IndicatingOnConfirmAjaxLink<String>("clone", "confirmClone", true) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    ResourceTO resource = resourceRestClient.read(node.getKey());
                    resource.setKey("Copy of " + resource.getKey());
                    // reset some resource objects keys
                    if (resource.getOrgUnit() != null) {
                        resource.getOrgUnit().setKey(null);
                        for (ItemTO item : resource.getOrgUnit().getItems()) {
                            item.setKey(null);
                        }
                    }
                    for (ProvisionTO provision : resource.getProvisions()) {
                        provision.setKey(null);
                        if (provision.getMapping() != null) {
                            for (ItemTO item : provision.getMapping().getItems()) {
                                item.setKey(null);
                            }
                            provision.getMapping().getLinkingItems().clear();
                        }
                        provision.getVirSchemas().clear();
                    }
                    resourceRestClient.create(resource);

                    // refresh Topology
                    send(pageRef.getPage(), Broadcast.DEPTH, new AbstractResourceWizardBuilder.CreateEvent(
                            resource.getKey(),
                            resource.getKey(),
                            TopologyNode.Kind.RESOURCE,
                            resource.getConnector(),
                            target));

                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While cloning resource {}", node.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

        };
        MetaDataRoleAuthorizationStrategy.authorize(clone, RENDER, StandardEntitlement.RESOURCE_CREATE);
        fragment.add(clone);

        return fragment;
    }

    public final class UpdateEvent {

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
