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
import java.text.MessageFormat;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ConnectorModal;
import org.apache.syncope.client.console.panels.ResourceModal;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.confirmation.ConfirmationModalBehavior;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toggle panel.
 */
public class TopologyTogglePanel extends TogglePanel {

    private static final long serialVersionUID = -2025535531121434056L;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyTogglePanel.class);

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final WebMarkupContainer container;

    private final PageReference pageRef;

    public TopologyTogglePanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        modal.size(Modal.Size.Large);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        add(container);

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
                target.add(modal);
                modal.header(new ResourceModel("task.generic.list", "Generic tasks"));
                modal.show(true);
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
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorModal(modal, pageRef, model)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, StandardEntitlement.CONNECTOR_CREATE);

                modal.show(true);
            }
        };
        fragment.add(create);

        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, StandardEntitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "connectorActions", this);

        final AjaxLink<String> delete = new IndicatingAjaxLink<String>("delete") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(Long.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While deleting resource {}", node.getKey(), e);
                }
                ((BasePage) getPage()).getNotificationPanel().refresh(target);
            }
        };

        fragment.add(delete);
        delete.add(new ConfirmationModalBehavior());

        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, StandardEntitlement.CONNECTOR_DELETE);

        final AjaxLink<String> create = new IndicatingAjaxLink<String>("create") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(Long.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceModal<>(modal, pageRef, model, true)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, StandardEntitlement.RESOURCE_CREATE);

                modal.show(true);
            }
        };
        fragment.add(create);

        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, StandardEntitlement.RESOURCE_CREATE);

        final AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = connectorRestClient.read(Long.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorModal(modal, pageRef, model)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, StandardEntitlement.CONNECTOR_UPDATE);

                modal.show(true);
            }
        };
        fragment.add(edit);

        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, StandardEntitlement.CONNECTOR_UPDATE);

        return fragment;
    }

    private Fragment getResurceFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "resourceActions", this);

        final AjaxLink<String> delete = new IndicatingAjaxLink<String>("delete") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey().toString());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While deleting resource {}", node.getKey(), e);
                }
                ((BasePage) getPage()).getNotificationPanel().refresh(target);
            }
        };
        fragment.add(delete);

        delete.add(new ConfirmationModalBehavior());

        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, StandardEntitlement.RESOURCE_DELETE);

        final AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceModal<>(modal, pageRef, model, false)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, StandardEntitlement.RESOURCE_UPDATE);

                modal.show(true);
            }
        };
        fragment.add(edit);
        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, StandardEntitlement.RESOURCE_UPDATE);

        final AjaxLink<String> propagation = new IndicatingAjaxLink<String>("propagation") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(modal);
                modal.header(new ResourceModel("task.propagation.list", "Propagation tasks"));
                modal.show(true);
            }
        };
        fragment.add(propagation);
        MetaDataRoleAuthorizationStrategy.authorize(propagation, ENABLE, StandardEntitlement.TASK_LIST);

        final AjaxLink<String> synchronization = new IndicatingAjaxLink<String>("synchronization") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(modal);
                modal.header(new ResourceModel("task.synchronization.list", "Synchronization tasks"));
                modal.show(true);
            }
        };
        fragment.add(synchronization);
        MetaDataRoleAuthorizationStrategy.authorize(synchronization, ENABLE, StandardEntitlement.TASK_LIST);

        final AjaxLink<String> push = new IndicatingAjaxLink<String>("push") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(modal);
                modal.header(new ResourceModel("task.push.list", "Push tasks"));
                modal.show(true);
            }
        };
        fragment.add(push);
        MetaDataRoleAuthorizationStrategy.authorize(push, ENABLE, StandardEntitlement.TASK_LIST);

        return fragment;
    }
}
