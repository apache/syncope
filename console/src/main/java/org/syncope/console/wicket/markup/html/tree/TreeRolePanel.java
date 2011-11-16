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
package org.syncope.console.wicket.markup.html.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.RoleTO;
import org.syncope.console.commons.RoleTreeBuilder;
import org.syncope.console.pages.panels.RoleSummaryPanel.TreeNodeClickUpdate;

public class TreeRolePanel extends Panel {

    private static final long serialVersionUID = 1762003213871836869L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    final WebMarkupContainer treeContainer;

    private BaseTree tree;

    public TreeRolePanel(final String id) {
        super(id);

        treeContainer = new WebMarkupContainer("treeContainer");
        treeContainer.setOutputMarkupId(true);
        add(treeContainer);

        updateTree();

        treeContainer.add(tree);
    }

    private void updateTree() {

        tree = new LinkTree("treeTable", roleTreeBuilder.build()) {

            private static final long serialVersionUID = -5514696922119256101L;

            @Override
            protected IModel getNodeTextModel(final IModel model) {
                return new PropertyModel(model, "userObject.displayName");
            }

            @Override
            protected void onNodeLinkClicked(final Object node,
                    final BaseTree baseTree, final AjaxRequestTarget target) {

                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                RoleTO unitObject = (RoleTO) treeNode.getUserObject();

                send(getPage(), Broadcast.BREADTH,
                        new TreeNodeClickUpdate(target, unitObject.getId()));

            }
        };

        tree.setOutputMarkupId(true);
        tree.getTreeState().expandAll();

        treeContainer.addOrReplace(tree);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {

            final TreeNodeClickUpdate update =
                    (TreeNodeClickUpdate) event.getPayload();

            updateTree();

            update.getTarget().add(treeContainer);
        }
    }
}
