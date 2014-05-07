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

import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.AbstractSearchResultPanel;
import org.apache.syncope.console.pages.panels.RoleSearchPanel;
import org.apache.syncope.console.pages.panels.RoleSearchResultPanel;
import org.apache.syncope.console.pages.panels.RoleSummaryPanel;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRolePanel;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Roles WebPage.
 */
public class Roles extends BasePage {

    private static final long serialVersionUID = -2147758241610831969L;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 800;

    @SpringBean
    private RoleRestClient restClient;

    private final ModalWindow editRoleWin;

    private final WebMarkupContainer roleTabsContainer;

    public Roles(final PageParameters parameters) {
        super(parameters);

        roleTabsContainer = new WebMarkupContainer("roleTabsContainer");
        roleTabsContainer.setOutputMarkupId(true);
        add(roleTabsContainer);

        editRoleWin = new ModalWindow("editRoleWin");
        editRoleWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleWin.setInitialHeight(WIN_HEIGHT);
        editRoleWin.setInitialWidth(WIN_WIDTH);
        editRoleWin.setCookieName("edit-role-modal");
        add(editRoleWin);

        final TreeRolePanel treePanel = new TreeRolePanel("treePanel");
        treePanel.setOutputMarkupId(true);
        roleTabsContainer.add(treePanel);

        final RoleSummaryPanel summaryPanel = new RoleSummaryPanel.Builder("summaryPanel")
                .window(editRoleWin).callerPageRef(Roles.this.getPageReference()).build();
        roleTabsContainer.add(summaryPanel);

        editRoleWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final RoleSummaryPanel summaryPanel = (RoleSummaryPanel) roleTabsContainer.get("summaryPanel");

                final TreeNodeClickUpdate data = new TreeNodeClickUpdate(target,
                        summaryPanel == null || summaryPanel.getSelectedNode() == null
                        ? 0
                        : summaryPanel.getSelectedNode().getId());

                send(getPage(), Broadcast.BREADTH, data);

                if (modalResult) {
                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    modalResult = false;
                }

            }
        });

        final AbstractSearchResultPanel searchResult =
                 new RoleSearchResultPanel("searchResult", true, null, getPageReference(), restClient);
        add(searchResult);

        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final RoleSearchPanel searchPanel = new RoleSearchPanel.Builder("searchPanel").build();
        searchForm.add(searchPanel);

        searchForm.add(new ClearIndicatingAjaxButton("search", new ResourceModel("search"), getPageReference()) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                final String fiql = searchPanel.buildFIQL();
                LOG.debug("Node condition {}", fiql);

                doSearch(target, fiql, searchResult);

                Session.get().getFeedbackMessages().clear();
                searchPanel.getSearchFeedback().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                searchPanel.getSearchFeedback().refresh(target);
            }
        });
    }

    private void doSearch(final AjaxRequestTarget target, final String fiql,
            final AbstractSearchResultPanel resultsetPanel) {

        if (fiql == null) {
            error(getString(Constants.SEARCH_ERROR));
            return;
        }

        resultsetPanel.search(fiql, target);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {
            final TreeNodeClickUpdate update = (TreeNodeClickUpdate) event.getPayload();

            final RoleSummaryPanel summaryPanel = new RoleSummaryPanel.Builder("summaryPanel")
                    .window(editRoleWin).callerPageRef(Roles.this.getPageReference())
                    .selectedNodeId(update.getSelectedNodeId()).build();

            roleTabsContainer.addOrReplace(summaryPanel);
            update.getTarget().add(roleTabsContainer);
        }
    }

    public static class TreeNodeClickUpdate {

        private final AjaxRequestTarget target;

        private Long selectedNodeId;

        public TreeNodeClickUpdate(final AjaxRequestTarget target, final Long selectedNodeId) {
            this.target = target;
            this.selectedNodeId = selectedNodeId;
        }

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
