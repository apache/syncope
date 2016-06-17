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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.commons.AnyTypeComparator;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
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

    public Realm(final String id, final RealmTO realmTO, final PageReference pageRef, final int selectedIndex) {
        super(id);
        this.realmTO = realmTO;
        this.anyTypeTOs = anyTypeRestClient.list();

        AjaxBootstrapTabbedPanel<ITab> tabbedPanel =
                new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(pageRef));
        tabbedPanel.setSelectedTab(selectedIndex);
        add(tabbedPanel);
    }

    public RealmTO getRealmTO() {
        return realmTO;
    }

    private List<ITab> buildTabList(final PageReference pageRef) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("DETAILS")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                ActionLinksPanel<RealmTO> actionLinksPanel = ActionLinksPanel.<RealmTO>builder().
                        add(new ActionLink<RealmTO>(realmTO) {

                            private static final long serialVersionUID = 2802988981431379827L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final RealmTO modelObject) {
                                onClickTemplate(target);
                            }
                        }, ActionLink.ActionType.TEMPLATE, StandardEntitlement.REALM_UPDATE).
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

                RealmDetails panel = new RealmDetails(panelId, realmTO, actionLinksPanel, false);
                panel.setContentEnabled(false);
                actionLinksPanel.setEnabled(true);
                return panel;
            }
        });

        final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> formLayoutInfo =
                FormLayoutInfoUtils.fetch(anyTypeTOs);

        Collections.sort(anyTypeTOs, new AnyTypeComparator());
        for (final AnyTypeTO anyTypeTO : anyTypeTOs) {
            tabs.add(new AbstractTab(new Model<>(anyTypeTO.getKey())) {

                private static final long serialVersionUID = -5861786415855103549L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new AnyPanel(panelId, anyTypeTO, realmTO, formLayoutInfo, true, pageRef);
                }
            });
        }

        return tabs;
    }

    protected abstract void onClickTemplate(final AjaxRequestTarget target);

    protected abstract void onClickCreate(final AjaxRequestTarget target);

    protected abstract void onClickEdit(final AjaxRequestTarget target, final RealmTO realmTO);

    protected abstract void onClickDelete(final AjaxRequestTarget target, final RealmTO realmTO);

}
