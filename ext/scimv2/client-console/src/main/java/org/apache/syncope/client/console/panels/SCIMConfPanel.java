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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.commons.ITabComponent;
import org.apache.syncope.client.console.rest.SCIMConfRestClient;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SCIMConfPanel extends WizardMgtPanel<SCIMConf> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfPanel.class);

    private final SCIMConf scimConfTO;

    public SCIMConfPanel(
            final String id,
            final SCIMConf scimConfTO,
            final PageReference pageRef) {
        super(id, true);

        this.scimConfTO = scimConfTO;
        this.pageRef = pageRef;

        setPageRef(pageRef);

        AjaxBootstrapTabbedPanel<ITab> tabbedPanel =
                new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList());
        tabbedPanel.setSelectedTab(0);
        addInnerObject(tabbedPanel);

        AjaxLink<String> saveButton = new AjaxLink<>("saveButton") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SCIMConfRestClient.set(SCIMConfPanel.this.scimConfTO);
            }
        };
        addInnerObject(saveButton);

        setShowResultPage(true);

        modal.size(Modal.Size.Large);
        setWindowClosedReloadCallback(modal);
    }

    private List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new ITabComponent(new Model<>(getString("tab1"))) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SCIMConfGeneralPanel(panelId, scimConfTO);
            }

            @Override
            public boolean isVisible() {
                return true;
            }
        });

        tabs.add(new ITabComponent(
                new Model<>(getString("tab2")), getString("tab2")) {

            private static final long serialVersionUID = 1998052474181916792L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfUserPanel(panelId, scimConfTO);
            }

            @Override
            public boolean isVisible() {
                return true;
            }
        });

        tabs.add(new ITabComponent(
                new Model<>(getString("tab3")), getString("tab3")) {

            private static final long serialVersionUID = 1998052474181916792L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new SCIMConfEnterpriseUserPanel(panelId, scimConfTO);
            }

            @Override
            public boolean isVisible() {
                return true;
            }
        });

        return tabs;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Panel customResultBody(final String panelId, final SCIMConf item, final Serializable result) {

        return null;
    }

}
