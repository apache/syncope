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
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.pages.MembershipModalPage;
import org.apache.syncope.console.pages.UserModalPage;
import org.apache.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.to.UserTO;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MembershipsPanel extends Panel {

    private static final long serialVersionUID = -2559791301973107191L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private ListView<MembershipTO> membershipsView;

    private UserTO userTO = null;

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

        BaseTree tree = new LinkTree("treeTable", roleTreeBuilder.build()) {

            private static final long serialVersionUID = -5514696922119256101L;

            @Override
            protected IModel getNodeTextModel(final IModel model) {
                return new PropertyModel(model, "userObject.displayName");
            }

            @Override
            protected void onNodeLinkClicked(final Object node, final BaseTree tree, final AjaxRequestTarget target) {

                final RoleTO roleTO = (RoleTO) ((DefaultMutableTreeNode) node).getUserObject();

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

                        return new MembershipModalPage(getPage().getPageReference(), membershipWin, membershipTO,
                                templateMode);
                    }
                });
                membershipWin.show(target);
            }
        };

        tree.setOutputMarkupId(true);
        tree.getTreeState().expandAll();

        add(tree);

        membershipsView = new ListView<MembershipTO>("memberships", new PropertyModel<List<? extends MembershipTO>>(
                userTO, "memberships")) {

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

                MembershipsPanel.this.userTO.getMemberships().clear();
                MembershipsPanel.this.userTO.getMemberships().addAll(updatedUserTO.getMemberships());
                target.add(container);
            }
        });
    }
}
