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
package org.apache.syncope.client.enduser.navigation;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.pages.BaseEnduserWebPage;
import org.apache.syncope.client.enduser.pages.BaseExtPage;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

public class Navbar extends Panel {

    private static final long serialVersionUID = 1323251762654401168L;

    private final ListView<Class<? extends BaseExtPage>> extPages;

    public Navbar(final String id, final List<Class<? extends BaseExtPage>> extPageClasses) {
        super(id);
        setOutputMarkupId(true);

        extPages = new ListView<Class<? extends BaseExtPage>>("extPages", extPageClasses) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BaseExtPage>> item) {
                WebMarkupContainer containingLI = new WebMarkupContainer("extPageLI");
                item.add(containingLI);
                if (item.getModelObject().equals(BaseEnduserWebPage.class)) {
                    containingLI.add(new Behavior() {

                        private static final long serialVersionUID = 1469628524240283489L;

                        @Override
                        public void onComponentTag(final Component component, final ComponentTag tag) {
                            tag.put("class", "active");
                        }
                    });
                }

                ExtPage ann = item.getModelObject().getAnnotation(ExtPage.class);

                BookmarkablePageLink<Page> link = new BookmarkablePageLink<>("extPage", item.getModelObject());
                link.add(new Label("extPageLabel", ann.label()));
                if (StringUtils.isNotBlank(ann.listEntitlement())) {
                    MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, ann.listEntitlement());
                }
                containingLI.add(link);

                Label extPageIcon = new Label("extPageIcon");
                extPageIcon.add(new AttributeModifier("class", "fa " + ann.icon()));
                link.add(extPageIcon);
            }
        };
        extPages.setOutputMarkupId(true);
        extPages.setVisible(!extPageClasses.isEmpty());
        add(extPages);
    }

    public ListView<Class<? extends BaseExtPage>> getExtPages() {
        return extPages;
    }

}
