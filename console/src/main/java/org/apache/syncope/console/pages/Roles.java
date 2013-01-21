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

import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.console.pages.panels.AbstractSearchResultPanel;
import org.apache.syncope.console.pages.panels.RoleSearchPanel;
import org.apache.syncope.console.pages.panels.RoleSearchResultPanel;
import org.apache.syncope.console.pages.panels.RoleSummaryPanel;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRolePanel;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
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

    private static final int WIN_WIDTH = 750;

    @SpringBean
    private RoleRestClient restClient;

    private final ModalWindow editRoleWin;

    private final WebMarkupContainer container;

    public Roles(final PageParameters parameters) {
        super(parameters);

        editRoleWin = new ModalWindow("editRoleWin");
        editRoleWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleWin.setInitialHeight(WIN_HEIGHT);
        editRoleWin.setInitialWidth(WIN_WIDTH);
        editRoleWin.setCookieName("edit-role-modal");
        add(editRoleWin);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final TreeRolePanel treePanel = new TreeRolePanel("treePanel");
        treePanel.setOutputMarkupId(true);
        container.add(treePanel);

        final RoleSummaryPanel summaryPanel = new RoleSummaryPanel("summaryPanel", editRoleWin,
                Roles.this.getPageReference());
        container.add(summaryPanel);

        editRoleWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final RoleSummaryPanel summaryPanel = (RoleSummaryPanel) container.get("summaryPanel");

                final TreeNodeClickUpdate data = new TreeNodeClickUpdate(target,
                        summaryPanel == null || summaryPanel.getSelectedNode() == null
                        ? 0
                        : summaryPanel.getSelectedNode().getId());

                send(getPage(), Broadcast.BREADTH, data);
                target.add(container);
                if (modalResult) {
                    getSession().info(getString("operation_succeeded"));
                    target.add(feedbackPanel);
                    modalResult = false;
                }
            }
        });

        container.add(editRoleWin);

        final AbstractSearchResultPanel searchResult =
                new RoleSearchResultPanel("searchResult", true, null, getPageReference(), restClient);
        add(searchResult);

        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final RoleSearchPanel searchPanel = new RoleSearchPanel("searchPanel");
        searchForm.add(searchPanel);

        searchForm.add(new IndicatingAjaxButton("search", new ResourceModel("search")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final NodeCond searchCond = searchPanel.buildSearchCond();
                LOG.debug("Node condition {}", searchCond);

                doSearch(target, searchCond, searchResult);

                Session.get().getFeedbackMessages().clear();
                target.add(searchPanel.getSearchFeedback());
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchPanel.getSearchFeedback());
            }
        });
    }

    private void doSearch(final AjaxRequestTarget target, final NodeCond searchCond,
            final AbstractSearchResultPanel resultsetPanel) {

        if (searchCond == null || !searchCond.isValid()) {
            error(getString("search_error"));
            return;
        }

        resultsetPanel.search(searchCond, target);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeClickUpdate) {
            final TreeNodeClickUpdate update = (TreeNodeClickUpdate) event.getPayload();

            final RoleSummaryPanel summaryPanel = new RoleSummaryPanel("summaryPanel", editRoleWin,
                    Roles.this.getPageReference(), update.getSelectedNodeId());

            container.addOrReplace(summaryPanel);
            update.getTarget().add(this);
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
