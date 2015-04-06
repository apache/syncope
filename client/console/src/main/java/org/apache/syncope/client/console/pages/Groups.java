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
package org.apache.syncope.client.console.pages;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractSearchResultPanel;
import org.apache.syncope.client.console.panels.GroupSearchPanel;
import org.apache.syncope.client.console.panels.GroupSearchResultPanel;
import org.apache.syncope.client.console.panels.GroupSummaryPanel;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.client.console.wicket.markup.html.tree.TreeGroupPanel;
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
 * Groups WebPage.
 */
public class Groups extends BasePage {

    private static final long serialVersionUID = -2147758241610831969L;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 800;

    @SpringBean
    private GroupRestClient restClient;

    private final ModalWindow editGroupWin;

    private final WebMarkupContainer groupTabsContainer;

    public Groups(final PageParameters parameters) {
        super(parameters);

        groupTabsContainer = new WebMarkupContainer("groupTabsContainer");
        groupTabsContainer.setOutputMarkupId(true);
        add(groupTabsContainer);

        editGroupWin = new ModalWindow("editGroupWin");
        editGroupWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editGroupWin.setInitialHeight(WIN_HEIGHT);
        editGroupWin.setInitialWidth(WIN_WIDTH);
        editGroupWin.setCookieName("edit-group-modal");
        add(editGroupWin);

        final TreeGroupPanel treePanel = new TreeGroupPanel("treePanel");
        treePanel.setOutputMarkupId(true);
        groupTabsContainer.add(treePanel);

        final GroupSummaryPanel summaryPanel = new GroupSummaryPanel.Builder("summaryPanel")
                .window(editGroupWin).callerPageRef(Groups.this.getPageReference()).build();
        groupTabsContainer.add(summaryPanel);

        editGroupWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final GroupSummaryPanel summaryPanel = (GroupSummaryPanel) groupTabsContainer.get("summaryPanel");

                final TreeNodeClickUpdate data = new TreeNodeClickUpdate(target,
                        summaryPanel == null || summaryPanel.getSelectedNode() == null
                        ? 0
                        : summaryPanel.getSelectedNode().getKey());

                send(getPage(), Broadcast.BREADTH, data);

                if (modalResult) {
                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    modalResult = false;
                }

            }
        });

        final AbstractSearchResultPanel searchResult =
                new GroupSearchResultPanel("searchResult", true, null, getPageReference(), restClient);
        add(searchResult);

        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final GroupSearchPanel searchPanel = new GroupSearchPanel.Builder("searchPanel").build();
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

            final GroupSummaryPanel summaryPanel = new GroupSummaryPanel.Builder("summaryPanel")
                    .window(editGroupWin).callerPageRef(Groups.this.getPageReference())
                    .selectedNodeId(update.getSelectedNodeId()).build();

            groupTabsContainer.addOrReplace(summaryPanel);
            update.getTarget().add(groupTabsContainer);
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
