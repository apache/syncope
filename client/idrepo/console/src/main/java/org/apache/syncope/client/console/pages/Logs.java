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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.panels.LogsPanel;
import org.apache.syncope.client.console.rest.LoggerConf;
import org.apache.syncope.client.console.rest.LoggerConfOp;
import org.apache.syncope.client.console.rest.LoggerConfRestClient;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

public class Logs extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    @SpringBean
    private ServiceOps serviceOps;

    @SpringBean
    private DomainOps domainOps;

    @SpringBean
    private LoggingSystem loggingSystem;

    public Logs(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));
        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    private List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        List<Domain> domains = domainOps.list();

        List<NetworkService> coreInstances = serviceOps.list(NetworkService.Type.CORE);
        tabs.add(new AbstractTab(Model.of(NetworkService.Type.CORE.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new LogsPanel(panelId, new LoggerConfRestClient(coreInstances, domains), getPageReference());
            }
        });

        tabs.add(new AbstractTab(Model.of(NetworkService.Type.CONSOLE.name())) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new LogsPanel(panelId, new LoggerConfOp() {

                    private static final long serialVersionUID = 24740659553491L;

                    @Override
                    public List<LoggerConf> list() {
                        Collection<LoggerConfiguration> configurations = loggingSystem.getLoggerConfigurations();
                        if (configurations == null) {
                            return List.of();
                        }

                        return configurations.stream().map(conf -> {
                            LoggerConf loggerConf = new LoggerConf();
                            loggerConf.setKey(conf.getName());
                            loggerConf.setLevel(conf.getEffectiveLevel());
                            return loggerConf;
                        }).sorted(Comparator.comparing(LoggerConf::getKey)).collect(Collectors.toList());
                    }

                    @Override
                    public void setLevel(final String key, final LogLevel level) {
                        loggingSystem.setLogLevel(key, level);
                    }
                }, getPageReference());
            }
        });

        List<NetworkService> euInstances = serviceOps.list(NetworkService.Type.ENDUSER);
        if (!euInstances.isEmpty()) {
            tabs.add(new AbstractTab(Model.of(NetworkService.Type.ENDUSER.name())) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new LogsPanel(panelId, new LoggerConfRestClient(euInstances, domains), getPageReference());
                }
            });
        }

        List<NetworkService> waInstances = serviceOps.list(NetworkService.Type.WA);
        if (!waInstances.isEmpty()) {
            tabs.add(new AbstractTab(Model.of(NetworkService.Type.WA.name())) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new LogsPanel(panelId, new LoggerConfRestClient(waInstances, domains), getPageReference());
                }
            });
        }

        List<NetworkService> sraInstances = serviceOps.list(NetworkService.Type.SRA);
        if (!sraInstances.isEmpty()) {
            tabs.add(new AbstractTab(Model.of(NetworkService.Type.SRA.name())) {

                private static final long serialVersionUID = -6815067322125799251L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new LogsPanel(panelId, new LoggerConfRestClient(sraInstances, domains), getPageReference());
                }
            });
        }

        return tabs;
    }
}
