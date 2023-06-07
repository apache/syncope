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
import org.apache.syncope.client.console.annotations.AMPage;
import org.apache.syncope.client.console.panels.AMSessionPanel;
import org.apache.syncope.client.console.panels.SRARouteDirectoryPanel;
import org.apache.syncope.client.console.panels.SRAStatisticsPanel;
import org.apache.syncope.client.console.rest.SRARouteRestClient;
import org.apache.syncope.client.console.rest.SRASessionRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@AMPage(label = "SRA", icon = "fas fa-share-alt", listEntitlement = "", priority = 100)
public class SRA extends BasePage {

    private static final long serialVersionUID = 9200112197134882164L;

    @SpringBean
    private SRARouteRestClient sraRouteRestClient;

    @SpringBean
    private ServiceOps serviceOps;

    public SRA(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));
        body.setOutputMarkupId(true);

        List<NetworkService> instances = serviceOps.list(NetworkService.Type.SRA);

        AjaxLink<?> push = new AjaxLink<>("push") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    sraRouteRestClient.push();
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While pushing to SRA", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) getPageReference().getPage()).getNotificationPanel().refresh(target);
            }
        };
        push.setEnabled(!instances.isEmpty() && SyncopeConsoleSession.get().owns(AMEntitlement.SRA_ROUTE_PUSH));
        body.add(push);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        AjaxBootstrapTabbedPanel<ITab> tabbedPanel =
                new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(instances));
        content.add(tabbedPanel);

        body.add(content);
    }

    private List<ITab> buildTabList(final List<NetworkService> instances) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new ResourceModel("routes")) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SRARouteDirectoryPanel(panelId, sraRouteRestClient, getPageReference());
            }
        });

        if (!instances.isEmpty()) {
            tabs.add(new AbstractTab(new ResourceModel("metrics")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new SRAStatisticsPanel(panelId, instances);
                }
            });
        }

        if (!instances.isEmpty() && SyncopeConsoleSession.get().owns(AMEntitlement.SRA_SESSION_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("sessions")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AMSessionPanel(panelId, new SRASessionRestClient(instances),
                            AMEntitlement.SRA_SESSION_LIST, AMEntitlement.SRA_SESSION_DELETE, getPageReference());
                }
            });
        }

        return tabs;
    }
}
