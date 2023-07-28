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
import org.apache.syncope.client.console.panels.OIDCProvidersDirectoryPanel;
import org.apache.syncope.client.console.rest.OIDCProviderRestClient;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.common.lib.types.OIDCC4UIEntitlement;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtPage(label = "OIDC 1.0 C4UI", icon = "fab fa-openid",
        listEntitlement = OIDCC4UIEntitlement.OP_READ, priority = 300)
public class OIDCC4UI extends BaseExtPage {

    private static final long serialVersionUID = -599601954212606001L;

    @SpringBean
    protected OIDCProviderRestClient oidcProviderRestClient;

    public OIDCC4UI(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    private List<ITab> buildTabList() {

        final List<ITab> tabs = new ArrayList<>(1);

        tabs.add(new AbstractTab(new ResourceModel("op")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new OIDCProvidersDirectoryPanel(panelId, oidcProviderRestClient, getPageReference());
            }
        });

        return tabs;
    }
}
