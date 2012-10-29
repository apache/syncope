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

import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.pages.panels.ResultSetPanel;
import org.apache.syncope.console.pages.panels.SelectOnlyResultSetPanel;
import org.apache.syncope.console.pages.panels.UserSearchPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;

public class UserOwnerSelectModalPage extends BaseModalPage {

    private static final long serialVersionUID = 2106489458494696439L;

    public UserOwnerSelectModalPage(final PageReference pageRef, final ModalWindow window) {

        super();

        final SelectOnlyResultSetPanel searchResult =
                new SelectOnlyResultSetPanel("searchResult", true, null, pageRef, window);
        add(searchResult);

        final Form searchForm = new Form("searchForm");
        add(searchForm);

        final UserSearchPanel searchPanel = new UserSearchPanel("searchPanel");
        searchForm.add(searchPanel);

        searchForm.add(new IndicatingAjaxButton("search", new ResourceModel("search")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final NodeCond searchCond = searchPanel.buildSearchCond();
                doSearch(target, searchCond, searchResult);

                Session.get().getFeedbackMessages().clear();
                target.add(searchPanel.getSearchFeedback());
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchPanel.getSearchFeedback());
            }
        });

        add(new CloseOnESCBehavior(window));
    }

    private void doSearch(final AjaxRequestTarget target, final NodeCond searchCond,
            final ResultSetPanel resultsetPanel) {

        if (searchCond == null || !searchCond.checkValidity()) {
            error(getString("search_error"));
            return;
        }

        resultsetPanel.search(searchCond, target);
    }
}
