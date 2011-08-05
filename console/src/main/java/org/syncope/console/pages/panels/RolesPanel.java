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

import java.util.Iterator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
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
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.RoleTreeBuilder;
import org.syncope.console.pages.MembershipModalPage;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;

public class RolesPanel extends Panel {

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private WebMarkupContainer membershipsContainer;

    public RolesPanel(final String id, final UserTO userTO) {
        super(id);

        final ModalWindow membershipWin = new ModalWindow("membershipWin");
        membershipWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        membershipWin.setPageMapName("create-membership-modal");
        membershipWin.setCookieName("create-membership-modal");
        add(membershipWin);

        final List<RoleTO> roles = roleRestClient.getAllRoles();
        BaseTree tree = new LinkTree("treeTable", roleTreeBuilder.build()) {

            @Override
            protected IModel getNodeTextModel(final IModel model) {
                return new PropertyModel(model, "userObject.displayName");
            }

            @Override
            protected void onNodeLinkClicked(final Object node,
                    final BaseTree tree, final AjaxRequestTarget target) {

                final RoleTO roleTO =
                        (RoleTO) ((DefaultMutableTreeNode) node).getUserObject();

                membershipWin.setPageCreator(new ModalWindow.PageCreator() {

                    private MembershipTO membershipTO;

                    @Override
                    public Page createPage() {
                        membershipTO = new MembershipTO();
                        membershipTO.setRoleId(roleTO.getId());

                        return new MembershipModalPage(getPage(),
                                membershipWin, membershipTO, userTO);
                    }
                });
                membershipWin.show(target);
            }
        };

        tree.getTreeState().expandAll();
        tree.updateTree();

        add(tree);

        ListView<MembershipTO> membershipsView = new ListView<MembershipTO>(
                "memberships", new PropertyModel<List<? extends MembershipTO>>(
                userTO, "memberships")) {

            @Override
            protected void populateItem(final ListItem item) {
                final MembershipTO membershipTO =
                        (MembershipTO) item.getDefaultModelObject();

                item.add(new Label("roleId", new Model(
                        membershipTO.getRoleId())));
                item.add(new Label("roleName", new Model(
                        getRoleName(membershipTO.getRoleId(), roles))));

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        membershipWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    @Override
                                    public Page createPage() {
                                        MembershipModalPage window =
                                                new MembershipModalPage(
                                                getPage(), membershipWin,
                                                membershipTO,
                                                userTO);

                                        return window;

                                    }
                                });
                        membershipWin.show(target);
                    }
                };
                item.add(editLink);

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        userTO.removeMembership(membershipTO);
                        target.addComponent(membershipsContainer);
                    }
                };
                item.add(deleteLink);
            }
        };

        membershipsContainer = new WebMarkupContainer("membershipsContainer");
        membershipsContainer.add(membershipsView);
        membershipsContainer.setOutputMarkupId(true);
        add(membershipsContainer);

        setWindowClosedCallback(membershipWin, membershipsContainer);
    }

    private String getRoleName(long roleId, List<RoleTO> roles) {
        boolean found = false;
        RoleTO roleTO;
        String result = null;
        for (Iterator<RoleTO> itor = roles.iterator();
                itor.hasNext() && !found;) {

            roleTO = itor.next();
            if (roleTO.getId() == roleId) {
                result = roleTO.getName();
            }
        }

        return result;
    }

    private void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);
                    }
                });
    }
}
