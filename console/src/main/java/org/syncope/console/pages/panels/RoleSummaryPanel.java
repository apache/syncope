/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.RoleTO;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.wicket.markup.html.tree.TreeActionLinkPanel;

public class RoleSummaryPanel extends Panel {

    private static final long serialVersionUID = 643769814985593156L;

    @SpringBean
    private RoleRestClient restClient;

    private RoleTO selectedNode;

    private Fragment fragment;

    private RoleTabPanel roleTabPanel;

    private TreeActionLinkPanel actionLink;

    private PageReference callerPageRef;

    private ModalWindow window;

    public RoleSummaryPanel(final String id,
            final ModalWindow window, final PageReference callerPageRef) {

        super(id);

        this.callerPageRef = callerPageRef;
        this.window = window;

        fragment = new Fragment("rolePanel",
                this.selectedNode == null
                ? "fakerootFrag" : (this.selectedNode.getId() != 0
                ? "roleViewPanel" : "rootPanel"), this);

        if (this.selectedNode != null) {
            if (this.selectedNode.getId() != 0) {
                roleTabPanel =
                        new RoleTabPanel("nodeViewPanel",
                        selectedNode, window, callerPageRef);
                roleTabPanel.setOutputMarkupId(true);
                fragment.add(roleTabPanel);
            } else {
                actionLink =
                        new TreeActionLinkPanel("actionLink",
                        this.selectedNode.getId(),
                        new CompoundPropertyModel(this.selectedNode),
                        window, callerPageRef);
                fragment.add(actionLink);
            }
        }

        add(fragment);
    }

    public RoleTO getSelectedNode() {
        return selectedNode;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {

            final TreeNodeClickUpdate update =
                    (TreeNodeClickUpdate) event.getPayload();

            this.selectedNode =
                    restClient.readRole(update.getSelectedNodeId());

            fragment = new Fragment("rolePanel", (update.getSelectedNodeId()
                    != 0
                    ? "roleViewPanel" : "rootPanel"), this);

            if (update.getSelectedNodeId() != 0) {

                roleTabPanel =
                        new RoleTabPanel("nodeViewPanel",
                        this.selectedNode, window, callerPageRef);
                roleTabPanel.setOutputMarkupId(true);
                fragment.addOrReplace(roleTabPanel);
            } else {
                actionLink =
                        new TreeActionLinkPanel("actionLink",
                        update.getSelectedNodeId(),
                        new CompoundPropertyModel(this.selectedNode),
                        window, callerPageRef);
                actionLink.setOutputMarkupId(true);
                fragment.addOrReplace(actionLink);
            }

            replace(fragment);
            update.getTarget().add(this);
        }
    }

    public static class TreeNodeClickUpdate {

        private AjaxRequestTarget target;

        private Long selectedNodeId;

        public TreeNodeClickUpdate(final AjaxRequestTarget target,
                final Long selectedNodeId) {

            this.target = target;
            this.selectedNodeId = selectedNodeId;
        }

        /** @return ajax request target */
        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Long getSelectedNodeId() {
            return selectedNodeId;
        }

        public void setSelectedNodeId(final Long selectedNodeId) {
            this.selectedNodeId = selectedNodeId;
        }
    }
}