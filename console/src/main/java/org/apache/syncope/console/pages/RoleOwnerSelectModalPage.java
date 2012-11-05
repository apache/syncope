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

import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.pages.panels.RoleDetailsPanel;
import org.apache.syncope.to.RoleTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RoleOwnerSelectModalPage extends BaseModalPage {

    private static final long serialVersionUID = 2106489458494696439L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    public RoleOwnerSelectModalPage(final PageReference pageRef, final ModalWindow window) {

        super();

        BaseTree tree = new LinkTree("treeTable", roleTreeBuilder.build()) {

            private static final long serialVersionUID = -5514696922119256101L;

            @Override
            protected IModel getNodeTextModel(final IModel model) {
                return new PropertyModel(model, "userObject.displayName");
            }

            @Override
            protected void onNodeLinkClicked(final Object node, final BaseTree tree, final AjaxRequestTarget target) {
                final RoleTO roleTO = (RoleTO) ((DefaultMutableTreeNode) node).getUserObject();

                send(pageRef.getPage(), Broadcast.BREADTH, new RoleDetailsPanel.RoleOwnerSelectPayload(roleTO.getId()));
                window.close(target);
            }
        };
        tree.setOutputMarkupId(true);
        tree.getTreeState().expandAll();
        this.add(tree);

        add(new CloseOnESCBehavior(window));
    }
}
