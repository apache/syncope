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

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ConnectorModal;
import org.apache.syncope.client.console.panels.ResourceModal;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyNodePanel extends Panel {

    private static final long serialVersionUID = -8775095410207013913L;

    protected static final Logger LOG = LoggerFactory.getLogger(TopologyNodePanel.class);

    private final ModalWindow modal;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    public TopologyNodePanel(
            final String id,
            final TopologyNode node,
            final PageReference pageRef,
            final ModalWindow modal) {
        super(id);

        final String resourceName = node.getDisplayName().length() > 20
                ? node.getDisplayName().subSequence(0, 19) + "..."
                : node.getDisplayName();

        add(new Label("label", resourceName));

        final String title;

        switch (node.getKind()) {
            case SYNCOPE:
                title = "";
                add(getSyncopeFragment(node, pageRef));
                break;
            case CONNECTOR_SERVER:
                title = node.getDisplayName();
                add(getConnectorServerFragment(node, pageRef));
                break;
            case CONNECTOR:
                title = (StringUtils.isBlank(node.getConnectionDisplayName())
                        ? "" : node.getConnectionDisplayName() + ":") + node.getDisplayName();
                add(getConnectorFragment(node, pageRef));
                break;
            default:
                title = node.getDisplayName().length() > 20 ? node.getDisplayName() : "";
                add(getResurceFragment(node, pageRef));
        }

        if (StringUtils.isNotEmpty(title)) {
            add(AttributeModifier.append("data-original-title", title));
        }

        this.setMarkupId(node.getDisplayName());

        this.modal = modal;
    }

    private Fragment getSyncopeFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "syncopeActions", this);
        fragment.setOutputMarkupId(true);
        return fragment;
    }

    private Fragment getConnectorServerFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "syncopeActions", this);
        return fragment;
    }

    private Fragment getConnectorFragment(final TopologyNode node, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "connectorWithNoResourceActions", this);
        fragment.setOutputMarkupId(true);

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
            }
        };
        fragment.add(delete);

        final AjaxLink<String> create = new ClearIndicatingAjaxLink<String>("create", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {
                final ResourceTO resourceTO = new ResourceTO();
                resourceTO.setConnector(Long.class.cast(node.getKey()));
                resourceTO.setConnectorDisplayName(node.getDisplayName());

                modal.setContent(new ResourceModal(modal, pageRef, resourceTO, true));

                modal.setTitle(getString("resource.new"));
                modal.show(target);
            }
        };
        fragment.add(create);

        final AjaxLink<String> edit = new ClearIndicatingAjaxLink<String>("edit", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {

                modal.setContent(new ConnectorModal(
                        modal,
                        pageRef,
                        connectorRestClient.read(Long.class.cast(node.getKey()))));

                modal.setTitle(MessageFormat.format(getString("connector.edit"), node.getKey()));
                modal.show(target);
            }
        };
        fragment.add(edit);

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
            }
        };
        fragment.add(delete);

        final AjaxLink<String> edit = new ClearIndicatingAjaxLink<String>("edit", pageRef) {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClickInternal(final AjaxRequestTarget target) {

                modal.setContent(new ResourceModal(
                        modal,
                        pageRef,
                        resourceRestClient.read(node.getKey().toString()),
                        false));

                modal.setTitle(MessageFormat.format(getString("resource.edit"), node.getKey()));
                modal.show(target);
            }
        };
        fragment.add(edit);

        return fragment;
    }
}
