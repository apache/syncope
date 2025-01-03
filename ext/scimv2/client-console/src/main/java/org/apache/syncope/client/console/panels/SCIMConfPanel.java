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
import org.apache.syncope.client.console.commons.ITabComponent;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.SCIMConfRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMConfPanel extends WizardMgtPanel<SCIMConf> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfPanel.class);

    @SpringBean
    protected SCIMConfRestClient scimConfRestClient;

    protected final SCIMConf scimConf;

    public SCIMConfPanel(
            final String id,
            final SCIMConf scimConf,
            final PageReference pageRef) {

        super(id, true);

        this.scimConf = scimConf;
        this.pageRef = pageRef;

        AjaxBootstrapTabbedPanel<ITab> tabbedPanel = new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList());
        tabbedPanel.setSelectedTab(0);
        addInnerObject(tabbedPanel);

        AjaxLink<String> saveButton = new AjaxLink<>("saveButton") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    scimConfRestClient.set(SCIMConfPanel.this.scimConf);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While setting SCIM configuration", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        addInnerObject(saveButton);

        setShowResultPanel(true);
    }

    protected List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new ITabComponent(new Model<>(getString("tab1")), getString("tab1")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SCIMConfGeneralPanel(panelId, scimConf);
            }
        });

        tabs.add(new ITabComponent(new Model<>(getString("tab2")), getString("tab2")) {

            private static final long serialVersionUID = 1998052474181916792L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfUserPanel(panelId, scimConf);
            }
        });

        tabs.add(new ITabComponent(new Model<>(getString("tab3")), getString("tab3")) {

            private static final long serialVersionUID = 1998052474181916792L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfEnterpriseUserPanel(panelId, scimConf);
            }
        });

        tabs.add(new ITabComponent(new Model<>(getString("tab4")), getString("tab4")) {

            private static final long serialVersionUID = 6645614456650987567L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfExtensionUserPanel(panelId, scimConf);
            }
        });

        tabs.add(new ITabComponent(new Model<>(getString("tab5")), getString("tab5")) {

            private static final long serialVersionUID = 1998052474181916792L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfGroupPanel(panelId, scimConf);
            }
        });

        return tabs;
    }
}
