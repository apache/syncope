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

import static org.apache.syncope.common.lib.types.AnyTypeKind.USER;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Realm extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(Realm.class);

    private final RealmTO realmTO;

    private static Integer MARKUP_ID = 0;

    @SpringBean
    private AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private GroupRestClient groupRestClient;

    @SpringBean
    private AnyObjectRestClient anyObjectRestClient;

    public Realm(final String id, final RealmTO realmTO, final PageReference pageReference) {
        super(id);
        this.realmTO = realmTO;

        List<AnyTypeMenuItem> anyMenu = new ArrayList<>();
        List<AnySearchResultPanel> anyList = new ArrayList<>();

        for (AnyTypeTO anyTypeTO : anyTypeRestClient.list()) {
            anyMenu.add(new AnyTypeMenuItem(anyTypeTO.getKey(), MARKUP_ID.toString()));

            switch (anyTypeTO.getKind()) {
                case USER:
                    anyList.add(
                            new UserSearchResultPanel(anyTypeTO.getKey(), "anytype-contentitem", MARKUP_ID.toString(),
                                    false, null, pageReference, userRestClient, anyTypeRestClient.getAnyTypeClass(
                                            anyTypeTO.getClasses()), realmTO.getFullPath()));
                    break;
                case GROUP:
                    anyList.add(
                            new GroupSearchResultPanel(anyTypeTO.getKey(), "anytype-contentitem", MARKUP_ID.toString(),
                                    false, null, pageReference, groupRestClient, anyTypeRestClient.getAnyTypeClass(
                                            anyTypeTO.getClasses()), realmTO.getFullPath()));
                    break;
                case ANY_OBJECT:
                    anyList.add(
                            new AnySearchResultPanel(anyTypeTO.getKey(), "anytype-contentitem", MARKUP_ID.toString(),
                                    false, null, pageReference, anyObjectRestClient, anyTypeRestClient.getAnyTypeClass(
                                            anyTypeTO.getClasses()), realmTO.getFullPath()));
                    break;
                default:
            }
            MARKUP_ID++;
        }

        ListView<AnyTypeMenuItem> menuListView = new ListView<AnyTypeMenuItem>("anytype-menu", anyMenu) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<AnyTypeMenuItem> item) {
                item.add(item.getModelObject());
                if (item.getIndex() == 0) {
                    item.add(new AttributeModifier("class", "active"));
                }
            }
        };
        add(menuListView);

        add(new ListView<AnySearchResultPanel>("anytype-content", anyList) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<AnySearchResultPanel> item) {
                item.setMarkupId(item.getModelObject().getCustomMarkupId());
                item.add(item.getModelObject());
                if (item.getIndex() == 0) {
                    item.add(new AttributeModifier("class", "tab-pane active"));
                }
            }
        });
    }

    public RealmTO getRealmTO() {
        return realmTO;
    }

}
