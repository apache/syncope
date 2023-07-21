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
import org.apache.syncope.client.console.panels.SAML2IdPsDirectoryPanel;
import org.apache.syncope.client.console.panels.SAML2SPPanel;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.client.console.rest.SAML2SPRestClient;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.common.lib.types.SAML2SP4UIEntitlement;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtPage(label = "SAML 2.0 SP4UI", icon = "fas fa-sign-in-alt",
        listEntitlement = SAML2SP4UIEntitlement.IDP_READ, priority = 400)
public class SAML2SP4UI extends BaseExtPage {

    private static final long serialVersionUID = -4837201407211278956L;

    @SpringBean
    protected SAML2IdPsRestClient saml2IdPsRestClient;

    @SpringBean
    protected SAML2SPRestClient saml2SPRestClient;

    public SAML2SP4UI(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    protected List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new ResourceModel("idps")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SAML2IdPsDirectoryPanel(panelId, saml2IdPsRestClient, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("sp")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SAML2SPPanel(panelId, saml2SPRestClient);
            }
        });

        return tabs;
    }
}
