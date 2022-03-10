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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.annotations.AMPage;
import org.apache.syncope.client.console.authprofiles.AuthProfileDirectoryPanel;
import org.apache.syncope.client.console.panels.AuthModuleDirectoryPanel;
import org.apache.syncope.client.console.clientapps.ClientApps;
import org.apache.syncope.client.console.panels.OIDC;
import org.apache.syncope.client.console.panels.SAML2;
import org.apache.syncope.client.console.panels.WAConfigDirectoryPanel;
import org.apache.syncope.client.console.panels.AMSessionPanel;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.rest.WASessionRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@AMPage(label = "WA", icon = "fas fa-id-card", listEntitlement = "", priority = 200)
public class WA extends BasePage {

    private static final long serialVersionUID = 9200112197134882164L;

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    private ServiceOps serviceOps;

    private String waPrefix = "";

    public WA(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));
        body.setOutputMarkupId(true);

        List<NetworkService> instances = serviceOps.list(NetworkService.Type.WA);

        AjaxLink<?> push = new AjaxLink<>("push") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    WAConfigRestClient.push();
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While pushing to WA", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) getPageReference().getPage()).getNotificationPanel().refresh(target);
            }
        };
        push.setEnabled(!instances.isEmpty() && SyncopeConsoleSession.get().owns(AMEntitlement.WA_CONFIG_PUSH));
        body.add(push);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        AjaxBootstrapTabbedPanel<ITab> tabbedPanel =
                new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(instances));
        content.add(tabbedPanel);

        body.add(content);

        if (!instances.isEmpty()) {
            String actuatorEndpoint = instances.get(0).getAddress() + "actuator/env";
            try {
                Response response = WebClient.create(
                        actuatorEndpoint,
                        SyncopeWebApplication.get().getAnonymousUser(),
                        SyncopeWebApplication.get().getAnonymousKey(),
                        null).
                        accept(MediaType.APPLICATION_JSON_TYPE).get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    JsonNode env = MAPPER.readTree((InputStream) response.getEntity());
                    if (env.has("propertySources")) {
                        for (JsonNode propertySource : env.get("propertySources")) {
                            if (propertySource.has("properties")) {
                                JsonNode properties = propertySource.get("properties");
                                if (properties.has("cas.server.prefix")) {
                                    JsonNode prefix = properties.get("cas.server.prefix");
                                    if (prefix.has("value")) {
                                        waPrefix = StringUtils.removeEnd(prefix.get("value").asText(), "/");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("While contacting {}", actuatorEndpoint, e);
            }
        }
    }

    private List<ITab> buildTabList(final List<NetworkService> instances) {
        List<ITab> tabs = new ArrayList<>();

        if (SyncopeConsoleSession.get().owns(AMEntitlement.AUTH_MODULE_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("authModules")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AuthModuleDirectoryPanel(panelId, getPageReference());
                }
            });
        }

        if (SyncopeConsoleSession.get().owns(AMEntitlement.CLIENTAPP_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("clientApps")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new ClientApps(panelId, getPageReference());
                }
            });
        }

        tabs.add(new AbstractTab(Model.of("SAML 2.0")) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SAML2(panelId, waPrefix, getPageReference());
            }
        });

        tabs.add(new AbstractTab(Model.of("OIDC 1.0")) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new OIDC(panelId, waPrefix, getPageReference());
            }
        });

        if (SyncopeConsoleSession.get().owns(AMEntitlement.WA_CONFIG_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("config")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new WAConfigDirectoryPanel(panelId, getPageReference());
                }
            });
        }

        if (SyncopeConsoleSession.get().owns(AMEntitlement.AUTH_PROFILE_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("authProfiles")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AuthProfileDirectoryPanel(panelId, getPageReference());
                }
            });
        }

        if (!instances.isEmpty() && SyncopeConsoleSession.get().owns(AMEntitlement.WA_SESSION_LIST)) {
            tabs.add(new AbstractTab(new ResourceModel("sessions")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AMSessionPanel(panelId, new WASessionRestClient(instances),
                            AMEntitlement.WA_SESSION_LIST, AMEntitlement.WA_SESSION_DELETE, getPageReference());
                }
            });
        }

        return tabs;
    }
}
