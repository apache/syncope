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

import static org.apache.wicket.Component.ENABLE;

import java.io.Serializable;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ConnectorModal;
import org.apache.syncope.client.console.panels.ResourceModal;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.confirmation.ConfirmationModalBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyNodePanel extends Panel implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -8775095410207013913L;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyNodePanel.class);

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final BaseModal<Serializable> modal;

    public TopologyNodePanel(
            final String id,
            final TopologyNode node,
            final BaseModal<Serializable> modal,
            final PageReference pageRef) {

        super(id);

        final String resourceName = node.getDisplayName().length() > 20
                ? node.getDisplayName().subSequence(0, 19) + "..."
                : node.getDisplayName();

        add(new Label("label", resourceName));

        final String title;

        switch (node.getKind()) {
            case SYNCOPE:
                title = "";
                add(getSyncopeFragment());
                add(new AttributeAppender("class", "topology_root", " "));
                break;
            case CONNECTOR_SERVER:
                title = node.getDisplayName();
                add(getLocationFragment(node, pageRef));
                add(new AttributeAppender("class", "topology_cs", " "));
                break;
            case FS_PATH:
                title = node.getDisplayName();
                add(getLocationFragment(node, pageRef));
                add(new AttributeAppender("class", "topology_cs", " "));
                break;
            case CONNECTOR:
                title = (StringUtils.isBlank(node.getConnectionDisplayName())
                        ? "" : node.getConnectionDisplayName() + ":") + node.getDisplayName();
                add(getConnectorFragment(node, pageRef));
                add(new AttributeAppender("class", "topology_conn", " "));
                break;
            default:
                title = node.getDisplayName().length() > 20 ? node.getDisplayName() : "";
                add(getResurceFragment(node, pageRef));
                add(new AttributeAppender("class", "topology_res", " "));
        }

        if (StringUtils.isNotEmpty(title)) {
            add(AttributeModifier.append("data-original-title", title));
        }

        this.setMarkupId(node.getDisplayName());

        this.modal = modal;
        BasePage.class.cast(pageRef.getPage()).setWindowClosedCallback(modal, null);
    }

    private Fragment getSyncopeFragment() {
        return new Fragment("actions", "syncopeActions", this);
    }

    private Fragment getLocationFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "locationActions", this);

        final AjaxLink<String> create = new ClearIndicatingAjaxLink<String>("create", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = new ConnInstanceTO();
                modelObject.setLocation(node.getKey().toString());

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorModal(modal, pageRef, model)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, Entitlement.CONNECTOR_CREATE);

                modal.show(true);
            }
        };
        fragment.add(create);

        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, Entitlement.CONNECTOR_CREATE);

        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "connectorActions", this);

        final AjaxLink<String> delete = new ClearIndicatingAjaxLink<String>("delete", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.delete(Long.class.cast(node.getKey()));
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While deleting resource {}", node.getKey(), e);
                }
                ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
            }
        };
        fragment.add(delete);
        delete.add(new ConfirmationModalBehavior());

        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, Entitlement.CONNECTOR_DELETE);

        final AjaxLink<String> create = new ClearIndicatingAjaxLink<String>("create", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                final ResourceTO modelObject = new ResourceTO();
                modelObject.setConnector(Long.class.cast(node.getKey()));
                modelObject.setConnectorDisplayName(node.getDisplayName());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceModal(modal, pageRef, model, true)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.new"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, Entitlement.RESOURCE_CREATE);

                modal.show(true);
            }
        };
        fragment.add(create);

        MetaDataRoleAuthorizationStrategy.authorize(create, ENABLE, Entitlement.RESOURCE_CREATE);

        final AjaxLink<String> edit = new ClearIndicatingAjaxLink<String>("edit", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                final ConnInstanceTO modelObject = connectorRestClient.read(Long.class.cast(node.getKey()));

                final IModel<ConnInstanceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ConnectorModal(modal, pageRef, model)));

                modal.header(new Model<>(MessageFormat.format(getString("connector.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, Entitlement.CONNECTOR_UPDATE);

                modal.show(true);
            }
        };
        fragment.add(edit);

        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, Entitlement.CONNECTOR_UPDATE);

        return fragment;
    }

    private Fragment getResurceFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "resourceActions", this);

        final AjaxLink<String> delete = new ClearIndicatingAjaxLink<String>("delete", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                try {
                    resourceRestClient.delete(node.getKey().toString());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getKey()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While deleting resource {}", node.getKey(), e);
                }

                ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
            }
        };
        fragment.add(delete);

        delete.add(new ConfirmationModalBehavior());

        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, Entitlement.RESOURCE_DELETE);

        final AjaxLink<String> edit = new ClearIndicatingAjaxLink<String>("edit", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                final ResourceTO modelObject = resourceRestClient.read(node.getKey().toString());

                final IModel<ResourceTO> model = new CompoundPropertyModel<>(modelObject);
                modal.setFormModel(model);

                target.add(modal.setContent(new ResourceModal(modal, pageRef, model, false)));

                modal.header(new Model<>(MessageFormat.format(getString("resource.edit"), node.getKey())));

                MetaDataRoleAuthorizationStrategy.
                        authorize(modal.addSumbitButton(), ENABLE, Entitlement.RESOURCE_UPDATE);

                modal.show(true);
            }
        };
        fragment.add(edit);

        MetaDataRoleAuthorizationStrategy.authorize(edit, ENABLE, Entitlement.RESOURCE_UPDATE);

        return fragment;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }
}
