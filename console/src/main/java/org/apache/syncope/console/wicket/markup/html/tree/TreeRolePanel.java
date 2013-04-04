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
package org.apache.syncope.console.wicket.markup.html.tree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.Roles.TreeNodeClickUpdate;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TreeRolePanel extends Panel {

    private static final long serialVersionUID = 1762003213871836869L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    final WebMarkupContainer treeContainer;

    private NestedTree<DefaultMutableTreeNode> tree;

    public TreeRolePanel(final String id) {
        super(id);

        treeContainer = new WebMarkupContainer("treeContainer");
        treeContainer.setOutputMarkupId(true);
        add(treeContainer);
        updateTree();
    }

    private void updateTree() {
        final ITreeProvider<DefaultMutableTreeNode> treeProvider = new TreeRoleProvider(roleTreeBuilder, true);
        final DefaultMutableTreeNodeExpansionModel treeModel = new DefaultMutableTreeNodeExpansionModel();

        tree = new DefaultNestedTree<DefaultMutableTreeNode>("treeTable", treeProvider, treeModel) {

            private static final long serialVersionUID = 7137658050662575546L;

            @Override
            protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> node) {
                final DefaultMutableTreeNode treeNode = node.getObject();
                final RoleTO roleTO = (RoleTO) treeNode.getUserObject();

                return new Folder<DefaultMutableTreeNode>(id, TreeRolePanel.this.tree, node) {

                    private static final long serialVersionUID = 9046323319920426493L;

                    @Override
                    protected boolean isClickable() {
                        return true;
                    }

                    @Override
                    protected IModel<?> newLabelModel(final IModel<DefaultMutableTreeNode> model) {
                        return new Model<String>(roleTO.getDisplayName());
                    }

                    @Override
                    protected void onClick(final AjaxRequestTarget target) {
                        super.onClick(target);

                        send(getPage(), Broadcast.BREADTH, new TreeNodeClickUpdate(target, roleTO.getId()));
                    }
                };
            }
        };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        DefaultMutableTreeNodeExpansion.get().expandAll();

        MetaDataRoleAuthorizationStrategy.authorize(tree, ENABLE, xmlRolesReader.getAllAllowedRoles("Roles", "read"));

        treeContainer.addOrReplace(tree);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {
            final TreeNodeClickUpdate update = (TreeNodeClickUpdate) event.getPayload();
            updateTree();
            update.getTarget().add(treeContainer);
        }
    }
}
