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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.panels.RealmDirectoryPanel;
import org.apache.syncope.client.console.panels.search.RealmSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmPage extends BasePage {

    private static final long serialVersionUID = -810612679853772333L;

    @SpringBean
    protected RealmRestClient realmRestClient;

    protected final RealmDirectoryPanel realmDirectoryPanel;

    protected RealmSearchPanel searchPanel;

    public RealmPage(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        List<SearchClause> clauses = new ArrayList<>();

        Model<Integer> model = Model.of(-1);
        body.add(new Accordion("accordionPanel",
                List.of(new AbstractTab(new ResourceModel("search.result")) {

                    private static final long serialVersionUID = 1037272333056449377L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        searchPanel = new RealmSearchPanel.Builder(new ListModel<>(clauses), getPageReference()).
                                required(true).enableSearch().build(panelId);
                        return searchPanel;
                    }
                }), model) {

            private static final long serialVersionUID = -3056452800492734900L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 6250423506463465679L;

                    @Override
                    public void onClick(final org.apache.wicket.ajax.AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                    }
                }.setBody(tab.getTitle()).setEscapeModelStrings(false);
            }
        });

        realmDirectoryPanel = new RealmDirectoryPanel("directoryPanel", realmRestClient, getPageReference());
        realmDirectoryPanel.setOutputMarkupId(true);
        body.add(realmDirectoryPanel);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent payload && searchPanel != null) {
            List<SearchClause> clauses = searchPanel.getModel().getObject();
            String base = clauses.stream().
                    filter(RealmPage::isBaseClause).
                    map(SearchClause::getValue).
                    filter(StringUtils::isNotBlank).
                    findFirst().
                    orElse(StringUtils.EMPTY);

            List<SearchClause> fiqlClauses = clauses.stream()
                    .filter(clause -> !isBaseClause(clause))
                    .filter(RealmPage::isValid)
                    .toList();

            realmDirectoryPanel.search(base, Optional.ofNullable(SearchUtils.buildFIQL(
                            fiqlClauses,
                            SyncopeClient.getRealmFiqlSearchConditionBuilder())).orElse(StringUtils.EMPTY),
                    payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    private static boolean isBaseClause(final SearchClause clause) {
        return clause.getType() == SearchClause.Type.CUSTOM && "fullPath".equalsIgnoreCase(clause.getProperty());
    }

    private static boolean isValid(final SearchClause clause) {
        return clause.getProperty() != null && clause.getComparator() != null;
    }
}
