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
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.SAML2IdPEntityRestClient;
import org.apache.syncope.client.console.rest.SAML2SPEntityRestClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SAML2 extends Panel {

    private static final long serialVersionUID = 7093557205333650002L;

    @SpringBean
    protected SAML2IdPEntityRestClient saml2IdPEntityRestClient;

    @SpringBean
    protected SAML2SPEntityRestClient saml2SPEntityRestClient;

    public SAML2(final String id, final String waPrefix, final PageReference pageRef) {
        super(id);

        add(new AjaxBootstrapTabbedPanel<>("saml2", buildTabList(waPrefix, pageRef)));
    }

    protected List<ITab> buildTabList(final String waPrefix, final PageReference pageRef) {
        List<ITab> tabs = new ArrayList<>();

        if (SyncopeConsoleSession.get().owns(AMEntitlement.SAML2_IDP_ENTITY_LIST, SyncopeConstants.ROOT_REALM)) {
            tabs.add(new AbstractTab(Model.of("Identity Provider")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new SAML2IdPEntityDirectoryPanel(panelId, saml2IdPEntityRestClient, waPrefix, pageRef);
                }
            });
        }

        if (SyncopeConsoleSession.get().owns(AMEntitlement.SAML2_SP_ENTITY_LIST, SyncopeConstants.ROOT_REALM)) {
            tabs.add(new AbstractTab(Model.of("Service Provider")) {

                private static final long serialVersionUID = 5211692813425391144L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new SAML2SPEntityDirectoryPanel(panelId, saml2SPEntityRestClient, waPrefix, pageRef);
                }
            });
        }

        return tabs;
    }
}
