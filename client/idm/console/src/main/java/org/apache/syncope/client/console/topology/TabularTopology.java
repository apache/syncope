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
package org.apache.syncope.client.console.topology;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.annotations.IdMPage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Connectors;
import org.apache.syncope.client.console.pages.Resources;
import org.apache.syncope.client.console.panels.ConnidLocations;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

@IdMPage(label = "TabularTopology", icon = "fas fa-plug", listEntitlement = IdMEntitlement.RESOURCE_LIST, priority = 1)
public class TabularTopology extends BasePage {

    private static final long serialVersionUID = -4434385801124981824L;

    public TabularTopology() {
        TopologyWebSocketBehavior websocket = new TopologyWebSocketBehavior();
        body.add(websocket);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    protected List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("Resources")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new Resources(panelId, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new Model<>("Connectors")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new Connectors(panelId, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new Model<>("ConnectorServers")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ConnidLocations.Builder(getPageReference()) {

                    private static final long serialVersionUID = -2555113973787214723L;

                }.build(panelId);
            }
        });

        return tabs;
    }
}
