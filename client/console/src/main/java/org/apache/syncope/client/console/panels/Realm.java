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

import com.googlecode.wicket.jquery.core.panel.LabelPanel;
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.AnyWizardBuilder;
import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Realm extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(Realm.class);

    private final RealmTO realmTO;

    private final List<AnyTypeTO> anyTypeTOs;

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private final UserRestClient userRestClient = new UserRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    private final AnyObjectRestClient anyObjectRestClient = new AnyObjectRestClient();

    private final PageReference pageRef;

    @SuppressWarnings({ "unchecked", "unchecked" })
    public Realm(final String id, final RealmTO realmTO, final PageReference pageRef) {
        super(id);
        this.realmTO = realmTO;
        this.anyTypeTOs = anyTypeRestClient.getAll();
        this.pageRef = pageRef;

        add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef)));
    }

    public RealmTO getRealmTO() {
        return realmTO;
    }

    private List<ITab> buildTabList(final PageReference pageReference) {

        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("DETAILS")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                final RealmDetails panel = new RealmDetails(panelId, realmTO, false);
                panel.setEnabled(false);
                return panel;
            }
        });

        for (final AnyTypeTO anyTypeTO : anyTypeTOs) {

            tabs.add(new AbstractTab(new Model<>(anyTypeTO.getKey())) {

                private static final long serialVersionUID = -5861786415855103549L;

                @Override
                public Panel getPanel(final String panelId) {
                    return getAnyPanel(panelId, pageReference, anyTypeTO);
                }
            });
        }
        return tabs;
    }

    private Panel getAnyPanel(final String id, final PageReference pageReference, final AnyTypeTO anyTypeTO) {
        final Panel panel;

        switch (anyTypeTO.getKind()) {
            case USER:
                final UserTO userTO = new UserTO();
                userTO.setRealm(realmTO.getFullPath());
                panel = new UserSearchResultPanel.Builder(
                        false, null, pageReference, userRestClient,
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        realmTO.getFullPath(),
                        anyTypeTO.getKey()).
                        addNewItemPanelBuilder(new UserWizardBuilder(
                                        BaseModal.CONTENT_ID, userTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getFeedbackPanel()).
                        build(id);
                break;
            case GROUP:
                final GroupTO groupTO = new GroupTO();
                groupTO.setRealm(realmTO.getFullPath());
                panel = new GroupSearchResultPanel.Builder(
                        false, null, pageReference, groupRestClient,
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        realmTO.getFullPath(),
                        anyTypeTO.getKey()).
                        addNewItemPanelBuilder(new GroupWizardBuilder(
                                        BaseModal.CONTENT_ID, groupTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getFeedbackPanel()).
                        build(id);
                break;
            case ANY_OBJECT:
                final AnyObjectTO anyObjectTO = new AnyObjectTO();
                anyObjectTO.setRealm(realmTO.getFullPath());
                anyObjectTO.setType(anyTypeTO.getKey());
                panel = new AnyObjectSearchResultPanel.Builder(
                        false, null, pageReference, anyObjectRestClient,
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        realmTO.getFullPath(),
                        anyTypeTO.getKey()).
                        addNewItemPanelBuilder(new AnyWizardBuilder<AnyObjectTO>(
                                        BaseModal.CONTENT_ID, anyObjectTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getFeedbackPanel()).
                        build(id);
                break;
            default:
                panel = new LabelPanel(id, null);
        }
        return panel;
    }
}
