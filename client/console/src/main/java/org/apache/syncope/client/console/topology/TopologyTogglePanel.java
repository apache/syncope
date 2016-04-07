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
import org.apache.syncope.client.console.panels.ConnectorModal;
import org.apache.syncope.client.console.panels.ResourceModal;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.tasks.PropagationTasks;
import org.apache.syncope.client.console.tasks.PushTasks;
import org.apache.syncope.client.console.tasks.SchedTasks;
import org.apache.syncope.client.console.tasks.PullTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class TopologyTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final WebMarkupContainer container;

    private final PageReference pageRef;

    protected final BaseModal<Serializable> resourceModal;

    protected final BaseModal<Serializable> taskModal;

    public TopologyTogglePanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        resourceModal = new BaseModal<>("outer");
        resourceModal.addSumbitButton();
        resourceModal.size(Modal.Size.Large);
        addOuterObject(resourceModal);

        taskModal = new BaseModal<>("outer");
        taskModal.size(Modal.Size.Large);
        addOuterObject(taskModal);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void toggleWithContent(final AjaxRequestTarget target, final TopologyNode node) {
        setHeader(target, node.getDisplayName());

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
        final Fragment fragment = new Fragment("actions", "syncopeActions", this);

        final AjaxLink<String> tasks = new IndicatingAjaxLink<String>("tasks") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(taskModal.setContent(new SchedTasks(taskModal, pageRef)));
                taskModal.header(new ResourceModel("task.custom.list"));
                taskModal.show(true);
            }
        };
        fragment.add(tasks);

        MetaDataRoleAuthorizationStrategy.authorize(tasks, ENABLE, StandardEntitlement.TASK_LIST);

        return fragment;
    }

    private Fragment getLocationFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "locationActions", this);

        final AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation(node.getKey().toString());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                resourceModal.setFormModel(model);

                target.add(resourceModal.setContent(new ConnectorModal(resourceModal, pageRef, model)));

                resourceModal.header(new Model<>(MessageFormat.format(getString("connector.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(resourceModal.addSumbitButton(), ENABLE, StandardEntitlement.CONNECTOR_CREATE);

                resourceModal.show(true);
            }
        };
        fragment.add(create);

        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, StandardEntitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "connectorActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(Long.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, StandardEntitlement.CONNECTOR_DELETE);
        fragment.add(delete);

        AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(Long.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                resourceModal.setFormModel(model);

                target.add(resourceModal.setContent(new ResourceModal<>(resourceModal, pageRef, model, true)));

                resourceModal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(resourceModal.addSumbitButton(), ENABLE, StandardEntitlement.RESOURCE_CREATE);

                resourceModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, StandardEntitlement.RESOURCE_CREATE);
        fragment.add(create);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = connectorRestClient.read(Long.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                resourceModal.setFormModel(model);

                target.add(resourceModal.setContent(new ConnectorModal(resourceModal, pageRef, model)));

                resourceModal.header(new Model<>(MessageFormat.format(getString("connector.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(resourceModal.addSumbitButton(), ENABLE, StandardEntitlement.CONNECTOR_UPDATE);

                resourceModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, StandardEntitlement.CONNECTOR_UPDATE);
        fragment.add(edit);

        return fragment;
    }

    private Fragment getResurceFragment(final TopologyNode node, final PageReference pageRef) {
        Fragment fragment = new Fragment("actions", "resourceActions", this);

        AjaxLink<String> delete = new IndicatingOnConfirmAjaxLink<String>("delete", true) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey().toString());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting resource {}", node.getKey(), e);
                    error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, StandardEntitlement.RESOURCE_DELETE);
        fragment.add(delete);

        AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                resourceModal.setFormModel(model);

                target.add(resourceModal.setContent(new ResourceModal<>(resourceModal, pageRef, model, false)));

                resourceModal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(resourceModal.addSumbitButton(), ENABLE, StandardEntitlement.RESOURCE_UPDATE);

                resourceModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, StandardEntitlement.RESOURCE_UPDATE);
        fragment.add(edit);

        AjaxLink<String> propagation = new IndicatingAjaxLink<String>("propagation") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            @SuppressWarnings("unchecked")
            public void onClick(final AjaxRequestTarget target) {
                target.add(taskModal.setContent(new PropagationTasks(taskModal, pageRef, node.getKey().toString())));
                taskModal.header(new ResourceModel("task.propagation.list", "Propagation tasks"));
                taskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(propagation, ENABLE, StandardEntitlement.TASK_LIST);
        fragment.add(propagation);

        AjaxLink<String> pull = new IndicatingAjaxLink<String>("pull") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(taskModal.setContent(new PullTasks(taskModal, pageRef, node.getKey().toString())));
                taskModal.header(new ResourceModel("task.pull.list"));
                taskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(pull, ENABLE, StandardEntitlement.TASK_LIST);
        fragment.add(pull);

        AjaxLink<String> push = new IndicatingAjaxLink<String>("push") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(taskModal.setContent(new PushTasks(taskModal, pageRef, node.getKey().toString())));
                taskModal.header(new ResourceModel("task.push.list", "Push tasks"));
                taskModal.show(true);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(push, ENABLE, StandardEntitlement.TASK_LIST);
        fragment.add(push);

        return fragment;
    }
}
