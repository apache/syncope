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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.XMLRolesReader;
import org.apache.syncope.client.console.pages.ResultStatusModalPage;
import org.apache.syncope.client.console.pages.GroupModalPage;
import org.apache.syncope.client.console.pages.Groups;
import org.apache.syncope.client.console.pages.StatusModalPage;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class GroupTabPanel extends Panel {

    private static final long serialVersionUID = 859236186975983959L;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    @SpringBean
    private GroupRestClient groupRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GroupTabPanel(final String id, final GroupTO selectedNode, final ModalWindow window,
            final PageReference pageRef) {

        super(id);

        this.add(new Label("displayName", selectedNode.getDisplayName()));

        final ActionLinksPanel links = new ActionLinksPanel("actionLinks", new Model(), pageRef);
        links.setOutputMarkupId(true);
        this.add(links);
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        GroupTO groupTO = new GroupTO();
                        groupTO.setParent(selectedNode.getKey());
                        return new GroupModalPage(pageRef, window, groupTO);
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.CREATE, xmlRolesReader.getEntitlement("Groups", "create"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new StatusModalPage<GroupTO>(pageRef, window, groupRestClient.read(selectedNode.getKey()));
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.MANAGE_RESOURCES, xmlRolesReader.getEntitlement("Groups", "update"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        GroupTO groupTO = groupRestClient.read(selectedNode.getKey());
                        return new GroupModalPage(pageRef, window, groupTO);
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.EDIT, xmlRolesReader.getEntitlement("Groups", "update"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    final GroupTO groupTO = groupRestClient.delete(selectedNode.getETagValue(), selectedNode.getKey());

                    ((Groups) pageRef.getPage()).setModalResult(true);

                    window.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            return new ResultStatusModalPage.Builder(window, groupTO).build();
                        }
                    });

                    window.show(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.OPERATION_ERROR) + ": " + e.getMessage());
                    ((Groups) pageRef.getPage()).getFeedbackPanel().refresh(target);
                }
            }
        }, ActionLink.ActionType.DELETE, xmlRolesReader.getEntitlement("Groups", "delete"));

        final Form form = new Form("groupForm");
        form.setModel(new CompoundPropertyModel(selectedNode));
        form.setOutputMarkupId(true);

        final GroupPanel groupPanel = new GroupPanel.Builder("groupPanel").
                form(form).groupTO(selectedNode).groupModalPageMode(Mode.ADMIN).build();
        groupPanel.setEnabled(false);
        form.add(groupPanel);

        final WebMarkupContainer userListContainer = new WebMarkupContainer("userListContainer");

        userListContainer.setOutputMarkupId(true);
        userListContainer.setEnabled(true);
        userListContainer.add(new UserSearchResultPanel("userList", true, null, pageRef, userRestClient));
        userListContainer.add(new ClearIndicatingAjaxButton("search", new ResourceModel("search"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                userListContainer.replace(new UserSearchResultPanel("userList",
                        true,
                        SyncopeClient.getUserSearchConditionBuilder().inGroups(selectedNode.getKey()).query(),
                        pageRef,
                        userRestClient));

                target.add(userListContainer);
            }
        });

        form.add(userListContainer);
        add(form);
    }
}
