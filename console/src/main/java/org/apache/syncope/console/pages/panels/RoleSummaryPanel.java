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
package org.apache.syncope.console.pages.panels;

import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.markup.html.tree.TreeActionLinkPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleSummaryPanel extends Panel {

    private static final long serialVersionUID = 643769814985593156L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RoleSummaryPanel.class);

    @SpringBean
    private RoleRestClient restClient;

    private RoleTO selectedNode;

    private final Fragment fragment;

    private RoleTabPanel roleTabPanel;

    private TreeActionLinkPanel actionLink;

    private final PageReference callerPageRef;

    private final ModalWindow window;

    public RoleSummaryPanel(final String id, final ModalWindow window, final PageReference callerPageRef) {
        this(id, window, callerPageRef, null);
    }

    public RoleSummaryPanel(final String id, final ModalWindow window, final PageReference callerPageRef,
            Long selectedNodeId) {

        super(id);

        this.callerPageRef = callerPageRef;
        this.window = window;

        if (selectedNodeId == null || selectedNodeId == 0) {
            this.selectedNode = null;
        } else {
            try {
                this.selectedNode = restClient.read(selectedNodeId);
            } catch (SyncopeClientCompositeErrorException e) {
                LOG.error("Could not read {}", selectedNodeId, e);
                this.selectedNode = null;
                selectedNodeId = null;
            }
        }

        fragment = new Fragment("roleSummaryPanel", selectedNodeId == null ? "fakerootFrag"
                : (selectedNodeId == 0 ? "rootPanel" : "roleViewPanel"), this);

        if (selectedNodeId != null) {
            if (selectedNodeId == 0) {
                actionLink = new TreeActionLinkPanel("actionLink", selectedNodeId, new Model(), window, callerPageRef);
                actionLink.setOutputMarkupId(true);
                fragment.add(actionLink);
            } else {
                roleTabPanel = new RoleTabPanel("nodeViewPanel", selectedNode, window, callerPageRef);
                roleTabPanel.setOutputMarkupId(true);
                fragment.add(roleTabPanel);
            }
        }
        add(fragment);
    }

    public RoleTO getSelectedNode() {
        return selectedNode;
    }
}
