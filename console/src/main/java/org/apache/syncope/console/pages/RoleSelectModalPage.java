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
package org.apache.syncope.console.pages;

import java.lang.reflect.Constructor;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansion;
import org.apache.syncope.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansionModel;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRoleProvider;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RoleSelectModalPage extends BaseModalPage {

    private static final long serialVersionUID = 2106489458494696439L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private final NestedTree<DefaultMutableTreeNode> tree;

    public RoleSelectModalPage(final PageReference pageRef, final ModalWindow window, final Class payloadClass) {
        super();

        final ITreeProvider<DefaultMutableTreeNode> treeProvider = new TreeRoleProvider(roleTreeBuilder, true);
        final DefaultMutableTreeNodeExpansionModel treeModel = new DefaultMutableTreeNodeExpansionModel();

        tree = new DefaultNestedTree<DefaultMutableTreeNode>("treeTable", treeProvider, treeModel) {

            private static final long serialVersionUID = 7137658050662575546L;

            @Override
            protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> node) {
                final DefaultMutableTreeNode treeNode = node.getObject();
                final RoleTO roleTO = (RoleTO) treeNode.getUserObject();

                return new Folder<DefaultMutableTreeNode>(id, RoleSelectModalPage.this.tree, node) {

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

                        try {
                            Constructor constructor = payloadClass.getConstructor(Long.class);
                            Object payload = constructor.newInstance(roleTO.getId());

                            send(pageRef.getPage(), Broadcast.BREADTH, payload);
                        } catch (Exception e) {
                            LOG.error("Could not send role select event", e);
                        }

                        window.close(target);
                    }
                };
            }
        };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        DefaultMutableTreeNodeExpansion.get().expandAll();

        this.add(tree);

        add(new CloseOnESCBehavior(window));
    }
}
