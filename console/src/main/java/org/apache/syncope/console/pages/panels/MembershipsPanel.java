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

import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.pages.MembershipModalPage;
import org.apache.syncope.console.pages.UserModalPage;
import org.apache.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.apache.syncope.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansion;
import org.apache.syncope.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansionModel;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRoleProvider;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MembershipsPanel extends Panel {

    private static final long serialVersionUID = -2559791301973107191L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private ListView<MembershipTO> membershipsView;

    private final UserTO userTO;

    private final NestedTree<DefaultMutableTreeNode> tree;

    public MembershipsPanel(final String id, final UserTO userTO, final boolean templateMode) {
        super(id);
        this.userTO = userTO;

        final WebMarkupContainer membershipsContainer = new WebMarkupContainer("membershipsContainer");
        membershipsContainer.setOutputMarkupId(true);
        add(membershipsContainer);

        final ModalWindow membershipWin = new ModalWindow("membershipWin");
        membershipWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        membershipWin.setCookieName("create-membership-modal");
        add(membershipWin);

        final ITreeProvider<DefaultMutableTreeNode> treeProvider = new TreeRoleProvider(roleTreeBuilder, true);
        final DefaultMutableTreeNodeExpansionModel treeModel = new DefaultMutableTreeNodeExpansionModel();

        tree = new DefaultNestedTree<DefaultMutableTreeNode>("treeTable", treeProvider, treeModel) {

            private static final long serialVersionUID = 7137658050662575546L;

            @Override
            protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> node) {
                final DefaultMutableTreeNode treeNode = node.getObject();
                final RoleTO roleTO = (RoleTO) treeNode.getUserObject();

                return new Folder<DefaultMutableTreeNode>(id, MembershipsPanel.this.tree, node) {

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

                        membershipWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = 7661763358801821185L;

                            private MembershipTO membershipTO;

                            @Override
                            public Page createPage() {

                                for (MembershipTO memberTO : membershipsView.getList()) {
                                    if (memberTO.getRoleId() == roleTO.getId()) {
                                        return new MembershipModalPage(getPage().getPageReference(),
                                                membershipWin, memberTO, templateMode);
                                    }
                                }
                                membershipTO = new MembershipTO();
                                membershipTO.setRoleId(roleTO.getId());
                                membershipTO.setRoleName(roleTO.getName());

                                return new MembershipModalPage(getPage().getPageReference(), membershipWin,
                                        membershipTO, templateMode);
                            }
                        });
                        membershipWin.show(target);
                    }
                };
            }
        };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        DefaultMutableTreeNodeExpansion.get().expandAll();

        this.add(tree);

        membershipsView = new ListView<MembershipTO>("memberships",
                new PropertyModel<List<? extends MembershipTO>>(userTO, "memberships")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = (MembershipTO) item.getDefaultModelObject();

                item.add(new Label("roleId", new Model(membershipTO.getRoleId())));
                item.add(new Label("roleName", new Model(membershipTO.getRoleName())));

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        membershipWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new MembershipModalPage(getPage().getPageReference(), membershipWin,
                                        membershipTO, templateMode);

                            }
                        });
                        membershipWin.show(target);
                    }
                };
                item.add(editLink);

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink("deleteLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        userTO.removeMembership(membershipTO);
                        target.add(membershipsContainer);
                    }
                };
                item.add(deleteLink);
            }
        };

        membershipsContainer.add(membershipsView);

        setWindowClosedCallback(membershipWin, membershipsContainer);
    }

    private void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {

        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final UserTO updatedUserTO = ((UserModalPage) getPage()).getUserTO();

                MembershipsPanel.this.userTO.setMemberships(updatedUserTO.getMemberships());
                target.add(container);
            }
        });
    }
}
