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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.GroupTreeBuilder;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.pages.MembershipModalPage;
import org.apache.syncope.client.console.pages.UserModalPage;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.ajax.markup.html.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansion;
import org.apache.syncope.client.console.wicket.markup.html.tree.DefaultMutableTreeNodeExpansionModel;
import org.apache.syncope.client.console.wicket.markup.html.tree.TreeGroupProvider;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
    private GroupTreeBuilder groupTreeBuilder;

    private final ListView<MembershipTO> membView;

    private final UserTO userTO;

    private final StatusPanel statusPanel;

    private final NestedTree<DefaultMutableTreeNode> tree;

    public MembershipsPanel(final String id, final UserTO userTO, final Mode mode,
            final StatusPanel statusPanel, final PageReference pageRef) {

        super(id);
        this.userTO = userTO;
        this.statusPanel = statusPanel;

        final WebMarkupContainer membershipsContainer = new WebMarkupContainer("membershipsContainer");
        membershipsContainer.setOutputMarkupId(true);
        add(membershipsContainer);

        final ModalWindow membWin = new ModalWindow("membershipWin");
        membWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        membWin.setCookieName("create-membership-modal");
        add(membWin);

        final ITreeProvider<DefaultMutableTreeNode> treeProvider = new TreeGroupProvider(groupTreeBuilder, true);
        final DefaultMutableTreeNodeExpansionModel treeModel = new DefaultMutableTreeNodeExpansionModel();

        tree = new DefaultNestedTree<DefaultMutableTreeNode>("treeTable", treeProvider, treeModel) {

            private static final long serialVersionUID = 7137658050662575546L;

            @Override
            protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> node) {
                final DefaultMutableTreeNode treeNode = node.getObject();
                final GroupTO groupTO = (GroupTO) treeNode.getUserObject();

                return new Folder<DefaultMutableTreeNode>(id, MembershipsPanel.this.tree, node) {

                    private static final long serialVersionUID = 9046323319920426493L;

                    @Override
                    protected boolean isClickable() {
                        return true;
                    }

                    @Override
                    protected IModel<?> newLabelModel(final IModel<DefaultMutableTreeNode> model) {
                        return new Model<String>(groupTO.getDisplayName());
                    }

                    @Override
                    protected void onClick(final AjaxRequestTarget target) {
                        if (groupTO.getKey() > 0) {
                            membWin.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = 7661763358801821185L;

                                @Override
                                public Page createPage() {
                                    PageReference pageRef = getPage().getPageReference();

                                    for (MembershipTO membTO : membView.getList()) {
                                        if (membTO.getGroupKey() == groupTO.getKey()) {
                                            return new MembershipModalPage(pageRef, membWin, membTO, mode);
                                        }
                                    }
                                    MembershipTO membTO = new MembershipTO();
                                    membTO.setGroupKey(groupTO.getKey());
                                    membTO.setGroupName(groupTO.getName());

                                    return new MembershipModalPage(pageRef, membWin, membTO, mode);
                                }
                            });
                            membWin.show(target);
                        }
                    }
                };
            }
        };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        DefaultMutableTreeNodeExpansion.get().expandAll();

        this.add(tree);

        membView = new ListView<MembershipTO>("memberships",
                new PropertyModel<List<? extends MembershipTO>>(userTO, "memberships")) {

                    private static final long serialVersionUID = 9101744072914090143L;

                    @Override
                    protected void populateItem(final ListItem<MembershipTO> item) {
                        final MembershipTO membershipTO = (MembershipTO) item.getDefaultModelObject();

                        item.add(new Label("groupId", new Model<Long>(membershipTO.getGroupKey())));
                        item.add(new Label("groupName", new Model<String>(membershipTO.getGroupName())));

                        AjaxLink editLink = new ClearIndicatingAjaxLink("editLink", pageRef) {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            protected void onClickInternal(final AjaxRequestTarget target) {
                                membWin.setPageCreator(new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID = -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new MembershipModalPage(getPage().getPageReference(), membWin,
                                                membershipTO, mode);

                                    }
                                });
                                membWin.show(target);
                            }
                        };
                        item.add(editLink);

                        AjaxLink deleteLink = new IndicatingOnConfirmAjaxLink("deleteLink", pageRef) {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            protected void onClickInternal(final AjaxRequestTarget target) {
                                userTO.getMemberships().remove(membershipTO);
                                ((UserModalPage) getPage()).getUserTO().getMemberships().remove(membershipTO);
                                target.add(membershipsContainer);

                                GroupTO groupTO = groupTreeBuilder.findGroup(membershipTO.getGroupKey());
                                Set<String> resourcesToRemove = groupTO == null
                                        ? Collections.<String>emptySet() : groupTO.getResources();
                                if (!resourcesToRemove.isEmpty()) {
                                    Set<String> resourcesAssignedViaMembership = new HashSet<>();
                                    for (MembershipTO membTO : userTO.getMemberships()) {
                                        groupTO = groupTreeBuilder.findGroup(membTO.getGroupKey());
                                        if (groupTO != null) {
                                            resourcesAssignedViaMembership.addAll(groupTO.getResources());
                                        }
                                    }
                                    resourcesToRemove.removeAll(resourcesAssignedViaMembership);
                                    resourcesToRemove.removeAll(userTO.getResources());
                                }

                                StatusUtils.update(
                                        userTO, statusPanel, target, Collections.<String>emptySet(), resourcesToRemove);
                            }
                        };
                        item.add(deleteLink);
                    }
                };

        membershipsContainer.add(membView);

        setWindowClosedCallback(membWin, membershipsContainer);
    }

    private void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final UserTO updatedUserTO = ((UserModalPage) getPage()).getUserTO();
                if (!userTO.equals(updatedUserTO)) {
                    if (updatedUserTO.getMemberships().size() > userTO.getMemberships().size()) {
                        Set<Long> diff = new HashSet<Long>(updatedUserTO.getMembershipMap().keySet());
                        diff.removeAll(userTO.getMembershipMap().keySet());

                        Set<String> resourcesToAdd = new HashSet<>();
                        for (Long diffMembId : diff) {
                            long groupId = updatedUserTO.getMembershipMap().get(diffMembId).getGroupKey();
                            GroupTO groupTO = groupTreeBuilder.findGroup(groupId);
                            resourcesToAdd.addAll(groupTO.getResources());
                            StatusUtils.update(
                                    userTO, statusPanel, target, resourcesToAdd, Collections.<String>emptySet());
                        }
                    }

                    MembershipsPanel.this.userTO.getMemberships().clear();
                    MembershipsPanel.this.userTO.getMemberships().addAll(updatedUserTO.getMemberships());
                    target.add(container);
                }
            }
        });
    }
}
