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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyPanel extends Panel implements ModalPanel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(AnyPanel.class);

    private final AnyTypeTO anyTypeTO;

    private final RealmTO realmTO;

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> formLayoutInfo;

    private final PageReference pageRef;

    private AbstractSearchPanel searchPanel;

    private final Panel directoryPanel;

    public AnyPanel(
            final String id,
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> formLayoutInfo,
            final boolean enableSearch,
            final PageReference pageRef) {

        super(id);
        this.anyTypeTO = anyTypeTO;
        this.realmTO = realmTO;
        this.formLayoutInfo = formLayoutInfo;
        this.pageRef = pageRef;
        // ------------------------
        // Accordion
        // ------------------------
        final Model<Integer> model = Model.of(-1);
        final StringResourceModel res = new StringResourceModel("search.result", this, new Model<>(anyTypeTO));

        final Accordion accordion = new Accordion("accordionPanel",
                Collections.<ITab>singletonList(new AbstractTab(res) {

                    private static final long serialVersionUID = 1037272333056449377L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        searchPanel = getSearchPanel(panelId);
                        return searchPanel;
                    }

                }), model) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("style", "color: #337ab7 !important");
                    }

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                    }
                }.setBody(res);
            }
        };
        accordion.setOutputMarkupId(true);
        add(accordion.setEnabled(enableSearch).setVisible(enableSearch));

        directoryPanel = getDirectoryPanel("searchResult");
        add(directoryPanel);
        // ------------------------
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            final AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();

            switch (anyTypeTO.getKind()) {
                case USER:
                    UserDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(SearchUtils.buildFIQL(
                            AnyPanel.this.searchPanel.getModel().getObject(),
                            SyncopeClient.getUserSearchConditionBuilder()), target);
                    break;
                case GROUP:
                    GroupDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(SearchUtils.buildFIQL(
                            AnyPanel.this.searchPanel.getModel().getObject(),
                            SyncopeClient.getGroupSearchConditionBuilder()), target);
                    break;
                case ANY_OBJECT:
                    AnyObjectDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(SearchUtils.buildFIQL(
                            AnyPanel.this.searchPanel.getModel().getObject(),
                            SyncopeClient.getAnyObjectSearchConditionBuilder(anyTypeTO.getKey())), target);
                    break;
                default:
            }
            target.add(directoryPanel);
        } else {
            super.onEvent(event);
        }
    }

    private AbstractSearchPanel getSearchPanel(final String id) {
        final AbstractSearchPanel panel;

        final List<SearchClause> clauses = new ArrayList<>();
        final SearchClause clause = new SearchClause();
        clauses.add(clause);

        switch (anyTypeTO.getKind()) {
            case USER:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty("username");

                panel = new UserSearchPanel.Builder(
                        new ListModel<>(clauses)).required(false).enableSearch().build(id);
                break;
            case GROUP:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty("name");

                panel = new GroupSearchPanel.Builder(
                        new ListModel<>(clauses)).required(false).enableSearch().build(id);
                break;
            case ANY_OBJECT:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty("name");

                panel = new AnyObjectSearchPanel.Builder(anyTypeTO.getKey(),
                        new ListModel<>(clauses)).required(false).enableSearch().build(id);
                break;
            default:
                panel = null;
        }
        return panel;
    }

    protected Panel getDirectoryPanel(final String id) {
        final Panel panel;
        final String fiql;
        switch (anyTypeTO.getKind()) {
            case USER:
                fiql = SyncopeClient.getUserSearchConditionBuilder().is("key").notNullValue().query();
                final UserTO userTO = new UserTO();
                userTO.setRealm(realmTO.getFullPath());
                panel = new UserDirectoryPanel.Builder(
                        anyTypeClassRestClient.list(anyTypeTO.getClasses()),
                        anyTypeTO.getKey(),
                        pageRef).setRealm(realmTO.getFullPath()).setFiltered(true).
                        setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(FormLayoutInfoUtils.instantiate(
                        userTO,
                        anyTypeTO.getClasses(),
                        formLayoutInfo.getLeft(),
                        pageRef)).build(id);
                MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.RENDER, StandardEntitlement.USER_SEARCH);
                break;

            case GROUP:
                fiql = SyncopeClient.getGroupSearchConditionBuilder().is("key").notNullValue().query();
                final GroupTO groupTO = new GroupTO();
                groupTO.setRealm(realmTO.getFullPath());
                panel = new GroupDirectoryPanel.Builder(
                        anyTypeClassRestClient.list(anyTypeTO.getClasses()),
                        anyTypeTO.getKey(),
                        pageRef).setRealm(realmTO.getFullPath()).setFiltered(true).
                        setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(FormLayoutInfoUtils.instantiate(
                        groupTO,
                        anyTypeTO.getClasses(),
                        formLayoutInfo.getMiddle(),
                        pageRef)).build(id);
                // list of group is available to all authenticated users
                break;

            case ANY_OBJECT:
                fiql = SyncopeClient.getAnyObjectSearchConditionBuilder(anyTypeTO.getKey()).is("key").notNullValue().
                        query();
                final AnyObjectTO anyObjectTO = new AnyObjectTO();
                anyObjectTO.setRealm(realmTO.getFullPath());
                anyObjectTO.setType(anyTypeTO.getKey());
                panel = new AnyObjectDirectoryPanel.Builder(
                        anyTypeClassRestClient.list(anyTypeTO.getClasses()),
                        anyTypeTO.getKey(),
                        pageRef).setRealm(realmTO.getFullPath()).setFiltered(true).
                        setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(FormLayoutInfoUtils.instantiate(
                        anyObjectTO,
                        anyTypeTO.getClasses(),
                        formLayoutInfo.getRight().get(anyTypeTO.getKey()),
                        pageRef)).build(id);
                MetaDataRoleAuthorizationStrategy.authorize(
                        panel, WebPage.RENDER, AnyEntitlement.SEARCH.getFor(anyTypeTO.getKey()));
                break;

            default:
                panel = new LabelPanel(id, null);
        }
        return panel;
    }

}
