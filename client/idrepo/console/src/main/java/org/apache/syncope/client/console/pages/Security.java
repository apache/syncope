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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.DelegationDirectoryPanel;
import org.apache.syncope.client.console.panels.DynRealmDirectoryPanel;
import org.apache.syncope.client.console.panels.RoleDirectoryPanel;
import org.apache.syncope.client.console.panels.SecurityQuestionsPanel;
import org.apache.syncope.client.console.rest.DelegationRestClient;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wizards.DelegationWizardBuilder;
import org.apache.syncope.client.console.wizards.role.RoleWizardBuilder;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Security extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected RealmRestClient realmRestClient;

    @SpringBean
    protected DelegationRestClient delegationRestClient;

    @SpringBean
    protected DynRealmRestClient dynRealmRestClient;

    @SpringBean
    protected SecurityQuestionRestClient securityQuestionRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    public Security(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));
        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.setMarkupId("security");
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    protected List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.ROLE_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("roles")) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new RoleDirectoryPanel.Builder(roleRestClient, getPageReference()) {

                        private static final long serialVersionUID = -5960765294082359003L;

                    }.addNewItemPanelBuilder(new RoleWizardBuilder(
                            new RoleTO(),
                            roleRestClient,
                            realmRestClient,
                            dynRealmRestClient,
                            getPageReference()), true).build(panelId);
                }
            });
        }

        tabs.add(new AbstractTab(new ResourceModel("delegations")) {

            private static final long serialVersionUID = 29722178554609L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DelegationDirectoryPanel.Builder(delegationRestClient, getPageReference()) {

                    private static final long serialVersionUID = 30231721305047L;

                }.addNewItemPanelBuilder(new DelegationWizardBuilder(
                        new DelegationTO(),
                        userRestClient,
                        delegationRestClient,
                        getPageReference()),
                        true).build(panelId);
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("dynRealms")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DynRealmDirectoryPanel.Builder(dynRealmRestClient, getPageReference()) {

                    private static final long serialVersionUID = -5960765294082359003L;

                }.build(panelId);
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("securityQuestions")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SecurityQuestionsPanel(panelId, securityQuestionRestClient, getPageReference());
            }
        });

        return tabs;
    }
}
