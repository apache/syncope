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
package org.apache.syncope.client.console.clientapps;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ClientApps extends Panel {

    private static final long serialVersionUID = 5755172996408318813L;

    @SpringBean
    protected ClientAppRestClient clientAppRestClient;

    public ClientApps(final String id, final PageReference pageRef) {
        super(id);

        add(new AjaxBootstrapTabbedPanel<>("clientApps", buildTabList(pageRef)));
    }

    protected List<ITab> buildTabList(final PageReference pageRef) {
        List<ITab> tabs = new ArrayList<>(3);

        tabs.add(new AbstractTab(Model.of(ClientAppType.CASSP.name())) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new CASSPDirectoryPanel(panelId, clientAppRestClient, pageRef);
            }
        });

        tabs.add(new AbstractTab(Model.of(ClientAppType.SAML2SP.name())) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SAML2SPDirectoryPanel(panelId, clientAppRestClient, pageRef);
            }
        });

        tabs.add(new AbstractTab(Model.of(ClientAppType.OIDCRP.name())) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                return new OIDCRPDirectoryPanel(panelId, clientAppRestClient, pageRef);
            }
        });

        return tabs;
    }
}
