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

import java.io.Serializable;
import org.apache.syncope.client.console.commons.XMLRolesReader;
import org.apache.syncope.client.console.pages.GroupModalPage;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupSummaryPanel extends Panel {

    private static final long serialVersionUID = 643769814985593156L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GroupSummaryPanel.class);

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    @SpringBean
    private GroupRestClient restClient;

    private GroupTO selectedNode;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 4164563358509351832L;

        private String id;

        private ModalWindow window;

        private PageReference callerPageRef;

        private Long selectedNodeId = null;

        public Builder(final String id) {
            this.id = id;
        }

        public GroupSummaryPanel.Builder window(final ModalWindow window) {
            this.window = window;
            return this;
        }

        public GroupSummaryPanel.Builder callerPageRef(final PageReference callerPageRef) {
            this.callerPageRef = callerPageRef;
            return this;
        }

        public GroupSummaryPanel.Builder selectedNodeId(final Long selectedNodeId) {
            this.selectedNodeId = selectedNodeId;
            return this;
        }

        public GroupSummaryPanel build() {
            return new GroupSummaryPanel(this);
        }
    }

    private GroupSummaryPanel(final Builder builder) {
        super(builder.id);

        if (builder.selectedNodeId == null || builder.selectedNodeId == 0) {
            selectedNode = null;
        } else {
            try {
                selectedNode = restClient.read(builder.selectedNodeId);
            } catch (SyncopeClientException e) {
                LOG.error("Could not read {}", builder.selectedNodeId, e);
                selectedNode = null;
                builder.selectedNodeId = null;
            }
        }

        Fragment fragment = new Fragment("groupSummaryPanel",
                builder.selectedNodeId == null
                        ? "fakerootFrag"
                        : (builder.selectedNodeId == 0 ? "rootPanel" : "groupViewPanel"),
                this);

        if (builder.selectedNodeId != null) {
            if (builder.selectedNodeId == 0) {
                @SuppressWarnings("rawtypes")
                final ActionLinksPanel links = new ActionLinksPanel("actionLinks", new Model(), builder.callerPageRef);
                links.setOutputMarkupId(true);
                fragment.add(links);

                links.addWithRoles(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        builder.window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new GroupModalPage(builder.callerPageRef, builder.window, new GroupTO());
                            }
                        });

                        builder.window.show(target);
                    }
                }, ActionLink.ActionType.CREATE, xmlRolesReader.getEntitlement("Groups", "create"));
            } else {
                GroupTabPanel groupTabPanel =
                        new GroupTabPanel("nodeViewPanel", selectedNode, builder.window, builder.callerPageRef);
                groupTabPanel.setOutputMarkupId(true);
                fragment.add(groupTabPanel);
            }
        }
        add(fragment);
    }

    public GroupTO getSelectedNode() {
        return selectedNode;
    }
}
