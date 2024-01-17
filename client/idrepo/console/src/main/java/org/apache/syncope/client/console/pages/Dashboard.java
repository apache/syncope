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
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.DashboardAccessTokensPanel;
import org.apache.syncope.client.console.panels.DashboardControlPanel;
import org.apache.syncope.client.console.panels.DashboardExtensionsPanel;
import org.apache.syncope.client.console.panels.DashboardOverviewPanel;
import org.apache.syncope.client.console.panels.DashboardSystemPanel;
import org.apache.syncope.client.console.widgets.BaseExtWidget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Dashboard extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    public Dashboard(final PageParameters parameters) {
        super(parameters);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    private List<ITab> buildTabList() {
        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new ResourceModel("overview")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DashboardOverviewPanel(panelId);
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("accessTokens")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DashboardAccessTokensPanel(panelId, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("control")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DashboardControlPanel(panelId, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("system")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new DashboardSystemPanel(panelId);
            }
        });

        List<Class<? extends BaseExtWidget>> extWidgetClasses =
                SyncopeWebApplication.get().getLookup().getClasses(BaseExtWidget.class);
        if (!extWidgetClasses.isEmpty()) {
            tabs.add(new AbstractTab(new ResourceModel("extensions")) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new DashboardExtensionsPanel(panelId, extWidgetClasses, getPageReference());
                }
            });
        }

        return tabs;
    }
}
