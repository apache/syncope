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
import org.apache.syncope.client.console.rest.CamelRoutesRestClient;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.syncope.client.console.panels.CamelRoutesDirectoryPanel;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@ExtPage(label = "Camel Routes", icon = "fa fa-road", listEntitlement = CamelEntitlement.ROUTE_LIST, priority = 100)
public class CamelRoutes extends BaseExtPage {

    private static final long serialVersionUID = 1965360932245590233L;

    public static final String PREF_CAMEL_ROUTES_PAGINATOR_ROWS = "camel.routes.paginator.rows";

    public CamelRoutes(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        AjaxBootstrapTabbedPanel<ITab> tabbedPanel = new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList());
        content.add(tabbedPanel);

        MetaDataRoleAuthorizationStrategy.authorize(content, ENABLE, CamelEntitlement.ROUTE_LIST);
        body.add(content);
    }

    private List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>(3);

        tabs.add(new AbstractTab(new Model<>(AnyTypeKind.USER.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                CamelRoutesDirectoryPanel panel =
                        new CamelRoutesDirectoryPanel(panelId, getPageReference(), AnyTypeKind.USER);
                panel.setEnabled(CamelRoutesRestClient.isCamelEnabledFor(AnyTypeKind.USER));
                return panel;
            }
        });

        tabs.add(new AbstractTab(new Model<>(AnyTypeKind.GROUP.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                CamelRoutesDirectoryPanel panel =
                        new CamelRoutesDirectoryPanel(panelId, getPageReference(), AnyTypeKind.GROUP);
                panel.setEnabled(CamelRoutesRestClient.isCamelEnabledFor(AnyTypeKind.GROUP));
                return panel;
            }
        });

        tabs.add(new AbstractTab(new Model<>(AnyTypeKind.ANY_OBJECT.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                CamelRoutesDirectoryPanel panel =
                        new CamelRoutesDirectoryPanel(panelId, getPageReference(), AnyTypeKind.ANY_OBJECT);
                panel.setEnabled(CamelRoutesRestClient.isCamelEnabledFor(AnyTypeKind.ANY_OBJECT));
                return panel;
            }
        });

        return tabs;
    }

}
