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
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.any.AnyObjectWizardBuilder;
import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Realm extends Panel {

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
                final ActionLinksPanel<RealmTO> actionLinksPanel = ActionLinksPanel.<RealmTO>builder(pageRef).
                        add(new ActionLink<RealmTO>(realmTO) {

                            private static final long serialVersionUID = 2802988981431379827L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final RealmTO modelObject) {
                                onClickCreate(target);
                            }
                        }, ActionLink.ActionType.CREATE, StandardEntitlement.REALM_CREATE).
                        add(new ActionLink<RealmTO>(realmTO) {

                            private static final long serialVersionUID = 2802988981431379828L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final RealmTO modelObject) {
                                onClickEdit(target, realmTO);
                            }
                        }, ActionLink.ActionType.EDIT, StandardEntitlement.REALM_UPDATE).
                        add(new ActionLink<RealmTO>(realmTO) {

                            private static final long serialVersionUID = 2802988981431379829L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final RealmTO modelObject) {
                                onClickDelete(target, realmTO);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.REALM_DELETE).
                        build("actions");

                final RealmDetails panel = new RealmDetails(panelId, realmTO, actionLinksPanel, false);
                panel.setContentEnabled(false);
                actionLinksPanel.setEnabled(true);
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
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        userRestClient,
                        anyTypeTO.getKey(),
                        pageReference).setRealm(realmTO.getFullPath()).
                        addNewItemPanelBuilder(new UserWizardBuilder(
                                BaseModal.CONTENT_ID, userTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getNotificationPanel()).
                        build(id);
                MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.RENDER, StandardEntitlement.USER_LIST);
                break;
            case GROUP:
                final GroupTO groupTO = new GroupTO();
                groupTO.setRealm(realmTO.getFullPath());
                panel = new GroupSearchResultPanel.Builder(
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        groupRestClient,
                        anyTypeTO.getKey(),
                        pageReference).setRealm(realmTO.getFullPath()).
                        addNewItemPanelBuilder(new GroupWizardBuilder(
                                BaseModal.CONTENT_ID, groupTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getNotificationPanel()).
                        build(id);
                // list of group is available to all authenticated users
                break;
            case ANY_OBJECT:
                final AnyObjectTO anyObjectTO = new AnyObjectTO();
                anyObjectTO.setRealm(realmTO.getFullPath());
                anyObjectTO.setType(anyTypeTO.getKey());
                panel = new AnyObjectSearchResultPanel.Builder(
                        anyTypeRestClient.getAnyTypeClass(anyTypeTO.getClasses().toArray(new String[] {})),
                        anyObjectRestClient,
                        anyTypeTO.getKey(),
                        pageReference).setRealm(realmTO.getFullPath()).
                        addNewItemPanelBuilder(new AnyObjectWizardBuilder(
                                BaseModal.CONTENT_ID, anyObjectTO, anyTypeTO.getClasses(), pageRef)).
                        addNotificationPanel(BasePage.class.cast(this.pageRef.getPage()).getNotificationPanel()).
                        build(id);
                MetaDataRoleAuthorizationStrategy.authorize(panel, WebPage.RENDER,
                        String.format("%s_%s", anyObjectTO.getType(), AnyEntitlement.LIST));
                break;
            default:
                panel = new LabelPanel(id, null);
        }
        return panel;
    }

    protected abstract void onClickCreate(final AjaxRequestTarget target);

    protected abstract void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO);

    protected abstract void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO);
}
