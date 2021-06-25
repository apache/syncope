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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.layout.AnyLayout;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.LabelPanel;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyPanel extends Panel implements ModalPanel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(AnyPanel.class);

    protected static final String DIRECTORY_PANEL_ID = "searchResult";

    @FunctionalInterface
    public interface DirectoryPanelSupplier {

        Panel supply(
                String id,
                AnyTypeTO anyTypeTO,
                RealmTO realmTO,
                AnyLayout anyLayout,
                PageReference pageRef);
    }

    protected static DirectoryPanelSupplier DEFAULT_DIRECTORYPANEL_SUPPLIER =
            (id, anyTypeTO, realmTO, anyLayout, pageRef) -> {

                Panel panel;
                String fiql;

                String realm;
                String dynRealm;
                if (StringUtils.startsWith(realmTO.getFullPath(), SyncopeConstants.ROOT_REALM)) {
                    realm = RealmsUtils.getFullPath(realmTO.getFullPath());
                    dynRealm = null;
                } else {
                    realm = SyncopeConstants.ROOT_REALM;
                    dynRealm = realmTO.getKey();
                }

                switch (anyTypeTO.getKind()) {
                    case USER:
                        fiql = dynRealm == null
                                ? SyncopeClient.getUserSearchConditionBuilder().
                                        is(Constants.KEY_FIELD_NAME).notNullValue().query()
                                : SyncopeClient.getUserSearchConditionBuilder().
                                        inDynRealms(dynRealm).query();

                        UserTO userTO = new UserTO();
                        userTO.setRealm(RealmsUtils.getFullPath(realmTO.getFullPath()));
                        panel = new UserDirectoryPanel.Builder(
                                AnyTypeClassRestClient.list(anyTypeTO.getClasses()),
                                anyTypeTO.getKey(),
                                pageRef).setRealm(realm).setDynRealm(dynRealm).setFiltered(true).
                                setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(
                                AnyLayoutUtils.newLayoutInfo(
                                        userTO, anyTypeTO.getClasses(), anyLayout.getUser(), pageRef)).
                                build(id);
                        MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.RENDER,
                                IdRepoEntitlement.USER_SEARCH);
                        break;

                    case GROUP:
                        fiql = dynRealm == null
                                ? SyncopeClient.getGroupSearchConditionBuilder().
                                        is(Constants.KEY_FIELD_NAME).notNullValue().query()
                                : SyncopeClient.getGroupSearchConditionBuilder().inDynRealms(dynRealm).query();

                        GroupTO groupTO = new GroupTO();
                        groupTO.setRealm(RealmsUtils.getFullPath(realmTO.getFullPath()));
                        panel = new GroupDirectoryPanel.Builder(
                                AnyTypeClassRestClient.list(anyTypeTO.getClasses()),
                                anyTypeTO.getKey(),
                                pageRef).setRealm(realm).setDynRealm(dynRealm).setFiltered(true).
                                setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(
                                AnyLayoutUtils.newLayoutInfo(
                                        groupTO, anyTypeTO.getClasses(), anyLayout.getGroup(), pageRef)).
                                build(id);
                        // list of group is available to all authenticated users
                        break;

                    case ANY_OBJECT:
                        fiql = dynRealm == null
                                ? SyncopeClient.getAnyObjectSearchConditionBuilder(anyTypeTO.getKey()).
                                        is(Constants.KEY_FIELD_NAME).notNullValue().query()
                                : SyncopeClient.getAnyObjectSearchConditionBuilder(anyTypeTO.getKey()).
                                        inDynRealms(dynRealm).query();

                        AnyObjectTO anyObjectTO = new AnyObjectTO();
                        anyObjectTO.setRealm(RealmsUtils.getFullPath(realmTO.getFullPath()));
                        anyObjectTO.setType(anyTypeTO.getKey());
                        panel = new AnyObjectDirectoryPanel.Builder(
                                AnyTypeClassRestClient.list(anyTypeTO.getClasses()),
                                anyTypeTO.getKey(),
                                pageRef).setRealm(realm).setDynRealm(dynRealm).setFiltered(true).
                                setFiql(fiql).setWizardInModal(true).addNewItemPanelBuilder(
                                AnyLayoutUtils.newLayoutInfo(anyObjectTO, anyTypeTO.getClasses(),
                                        anyLayout.getAnyObjects().get(anyTypeTO.getKey()), pageRef)).
                                build(id);
                        MetaDataRoleAuthorizationStrategy.authorize(
                                panel, WebPage.RENDER, AnyEntitlement.SEARCH.getFor(anyTypeTO.getKey()));
                        break;

                    default:
                        panel = new LabelPanel(id, null);
                }
                return panel;
            };

    protected final AnyTypeTO anyTypeTO;

    protected final RealmTO realmTO;

    protected final PageReference pageRef;

    protected AbstractSearchPanel searchPanel;

    protected final Panel directoryPanel;

    public AnyPanel(
            final String id,
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final AnyLayout anyLayout,
            final boolean enableSearch,
            final PageReference pageRef) {

        this(id, anyTypeTO, realmTO, anyLayout, enableSearch, DEFAULT_DIRECTORYPANEL_SUPPLIER, pageRef);
    }

    public AnyPanel(
            final String id,
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final AnyLayout anyLayout,
            final boolean enableSearch,
            final DirectoryPanelSupplier directoryPanelSupplier,
            final PageReference pageRef) {

        super(id);
        this.anyTypeTO = anyTypeTO;
        this.realmTO = realmTO;
        this.pageRef = pageRef;
        // ------------------------
        // Accordion
        // ------------------------
        Model<Integer> model = Model.of(-1);
        Accordion accordion = new Accordion("accordionPanel",
                List.of(new AbstractTab(new ResourceModel("search.result")) {

                    protected static final long serialVersionUID = 1037272333056449377L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        searchPanel = getSearchPanel(panelId);
                        return searchPanel;
                    }

                }), model) {

            protected static final long serialVersionUID = -3056452800492734900L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    protected static final long serialVersionUID = 6250423506463465679L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                    }
                }.setBody(tab.getTitle()).setEscapeModelStrings(false);
            }
        };
        accordion.setOutputMarkupId(true);
        add(accordion.setEnabled(enableSearch).setVisible(enableSearch));

        directoryPanel = createDirectoryPanel(anyTypeTO, realmTO, anyLayout, directoryPanelSupplier);
        add(directoryPanel);
        // ------------------------
    }

    protected Panel createDirectoryPanel(
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final AnyLayout anyLayout,
            final DirectoryPanelSupplier directoryPanelSupplier) {

        return directoryPanelSupplier.supply(DIRECTORY_PANEL_ID, anyTypeTO, realmTO, anyLayout, pageRef);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();

            send(AnyPanel.this.directoryPanel, Broadcast.BREADTH,
                    new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));

            String precond = realmTO.getFullPath().startsWith(SyncopeConstants.ROOT_REALM)
                    ? StringUtils.EMPTY
                    : String.format("$dynRealms=~%s;", realmTO.getKey());

            switch (anyTypeTO.getKind()) {
                case USER:
                    UserDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(
                            precond + SearchUtils.buildFIQL(
                                    AnyPanel.this.searchPanel.getModel().getObject(),
                                    SyncopeClient.getUserSearchConditionBuilder(),
                                    AnyPanel.this.searchPanel.getAvailableSchemaTypes(),
                                    SearchUtils.NO_CUSTOM_CONDITION),
                            target);
                    break;

                case GROUP:
                    GroupDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(
                            precond + SearchUtils.buildFIQL(
                                    AnyPanel.this.searchPanel.getModel().getObject(),
                                    SyncopeClient.getGroupSearchConditionBuilder(),
                                    AnyPanel.this.searchPanel.getAvailableSchemaTypes(),
                                    SearchUtils.NO_CUSTOM_CONDITION),
                            target);
                    break;

                case ANY_OBJECT:
                    AnyObjectDirectoryPanel.class.cast(AnyPanel.this.directoryPanel).search(
                            precond + SearchUtils.buildFIQL(
                                    AnyPanel.this.searchPanel.getModel().getObject(),
                                    SyncopeClient.getAnyObjectSearchConditionBuilder(anyTypeTO.getKey()),
                                    AnyPanel.this.searchPanel.getAvailableSchemaTypes(),
                                    SearchUtils.NO_CUSTOM_CONDITION),
                            target);
                    break;

                default:
            }
        } else {
            super.onEvent(event);
        }
    }

    protected AbstractSearchPanel getSearchPanel(final String id) {
        List<SearchClause> clauses = new ArrayList<>();
        SearchClause clause = new SearchClause();
        clauses.add(clause);

        AbstractSearchPanel panel;
        switch (anyTypeTO.getKind()) {
            case USER:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty(Constants.USERNAME_FIELD_NAME);

                panel = new UserSearchPanel.Builder(
                        new ListModel<>(clauses)).required(true).enableSearch().build(id);
                break;

            case GROUP:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty(Constants.NAME_FIELD_NAME);

                panel = new GroupSearchPanel.Builder(
                        new ListModel<>(clauses)).required(true).enableSearch().build(id);
                break;

            case ANY_OBJECT:
                clause.setComparator(SearchClause.Comparator.EQUALS);
                clause.setType(SearchClause.Type.ATTRIBUTE);
                clause.setProperty(Constants.NAME_FIELD_NAME);

                panel = new AnyObjectSearchPanel.Builder(anyTypeTO.getKey(),
                        new ListModel<>(clauses)).required(true).enableSearch().build(id);
                break;

            default:
                panel = null;
        }

        return panel;
    }
}
