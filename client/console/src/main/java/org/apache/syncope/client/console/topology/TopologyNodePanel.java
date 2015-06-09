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
import org.apache.syncope.client.console.pages.ResourceModalPage;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyNodePanel extends Panel {

    private static final long serialVersionUID = -8775095410207013913L;

    protected static final Logger LOG = LoggerFactory.getLogger(TopologyNodePanel.class);

    private static final int RESOURCE_MODAL_WIN_HEIGHT = 600;

    private static final int RESOURCE_MODAL_WIN_WIDTH = 800;

    public TopologyNodePanel(
            final String id, final TopologyNode node, final PageReference pageRef, final BaseRestClient restClient) {
        super(id);

        final String resourceName = node.getDisplayName().length() > 20
                ? node.getDisplayName().subSequence(0, 19) + "..."
                : node.getDisplayName();

        add(new Label("label", resourceName));

        final String title;

        switch (node.getKind()) {
            case SYNCOPE:
                title = "";
                add(getSyncopeFragment(node, (ResourceRestClient) restClient, pageRef));
                break;
            case CONNECTOR_SERVER:
                title = node.getDisplayName();
                add(getConnectorServerFragment(node, (ResourceRestClient) restClient, pageRef));
                break;
            case CONNECTOR:
                title = (StringUtils.isBlank(node.getConnectionDisplayName())
                        ? "" : node.getConnectionDisplayName() + ":") + node.getDisplayName();
                add(getConnectorFragment(node, (ResourceRestClient) restClient, pageRef));
                break;
            default:
                title = node.getDisplayName().length() > 20 ? node.getDisplayName() : "";
                add(getResurceFragment(node, (ResourceRestClient) restClient, pageRef));
        }

        if (StringUtils.isNotEmpty(title)) {
            add(AttributeModifier.append("data-original-title", title));
        }

        this.setMarkupId(node.getDisplayName());
    }

    private Fragment getSyncopeFragment(
            final TopologyNode node, final ResourceRestClient restClient, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "syncopeActions", this);

        final ModalWindow createWin = new ModalWindow("createWin");
        fragment.add(createWin);

        createWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createWin.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        createWin.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        createWin.setTitle(new ResourceModel("connector.new"));
        createWin.setCookieName("connector-modal");

        return fragment;
    }

    private Fragment getConnectorServerFragment(
            final TopologyNode node, final ResourceRestClient restClient, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "syncopeActions", this);

        final ModalWindow createWin = new ModalWindow("createWin");
        fragment.add(createWin);

        createWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createWin.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        createWin.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        createWin.setCookieName("connector-modal");
        createWin.setTitle(new ResourceModel("connector.new"));

        return fragment;
    }

    private Fragment getConnectorFragment(
            final TopologyNode node, final ResourceRestClient restClient, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "connectorWithNoResourceActions", this);

        final ModalWindow createWin = new ModalWindow("createWin");
        fragment.add(createWin);

        createWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createWin.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        createWin.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        createWin.setCookieName("resource-modal");
        createWin.setTitle(new ResourceModel("resource.new"));

        final ModalWindow editWin = new ModalWindow("editWin");
        fragment.add(editWin);

        editWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editWin.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        editWin.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        editWin.setCookieName("connector-modal");
        editWin.setTitle(MessageFormat.format(getString("connector.edit"), node.getKey()));

        return fragment;
    }

    private Fragment getResurceFragment(
            final TopologyNode node, final ResourceRestClient restClient, final PageReference pageRef) {
        final Fragment fragment = new Fragment("actions", "resourceActions", this);

        final ModalWindow editWin = new ModalWindow("editWin");
        fragment.add(editWin);

        editWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editWin.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        editWin.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        editWin.setCookieName("resource-modal");
        editWin.setTitle(MessageFormat.format(getString("resource.edit"), node.getKey()));

        final AjaxLink<String> delete = new IndicatingAjaxLink<String>("delete") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    restClient.delete(node.getKey().toString());
                    target.appendJavaScript(String.format("jsPlumb.remove('%s');", node.getDisplayName()));
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While deleting resource {}", node.getKey(), e);
                }
            }
        };
        fragment.add(delete);

        final AjaxLink<String> edit = new IndicatingAjaxLink<String>("edit") {

            private static final long serialVersionUID = 3776750333491622263L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                editWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new ResourceModalPage(
                                pageRef,
                                editWin,
                                restClient.read(node.getKey().toString()),
                                false);
                    }
                });

                editWin.show(target);
            }
        };
        fragment.add(edit);

        return fragment;
    }
}
