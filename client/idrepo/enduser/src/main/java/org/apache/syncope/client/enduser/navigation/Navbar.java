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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.pages.BaseExtPage;
import org.apache.syncope.client.enduser.pages.Logout;
import org.apache.syncope.client.enduser.pages.Self;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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

    private final List<WebMarkupContainer> navbarItems = new ArrayList<>();

    public Navbar(final String id, final List<Class<? extends BaseExtPage>> extPageClasses) {
        super(id);
        setOutputMarkupId(true);

        WebMarkupContainer detailsLI = new WebMarkupContainer("detailsLI");
        detailsLI.setMarkupId("self");
        navbarItems.add(detailsLI);
        add(detailsLI);

        BookmarkablePageLink<Page> detailsLink = new BookmarkablePageLink<>("detailsLILink", Self.class);
        detailsLink.setOutputMarkupId(true);
        detailsLink.add(new Label("detailsLILabel", getString("details")));
        detailsLI.add(detailsLink);

        WebMarkupContainer extLI = new WebMarkupContainer("extensionsLI");
        extLI.setOutputMarkupPlaceholderTag(true);
        extLI.setVisible(!extPageClasses.isEmpty());
        add(extLI);

        extPages = new ListView<Class<? extends BaseExtPage>>("extPages", extPageClasses) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BaseExtPage>> item) {
                WebMarkupContainer extPageLI = new WebMarkupContainer("extPageLI");
                item.add(extPageLI);
                extPageLI.setMarkupId(item.getModelObject().getSimpleName().toLowerCase());
                navbarItems.add(extPageLI);

                ExtPage ann = item.getModelObject().getAnnotation(ExtPage.class);

                BookmarkablePageLink<Page> extLIPageLink =
                        new BookmarkablePageLink<>("extPageLILink", item.getModelObject());
                extLIPageLink.setOutputMarkupId(true);
                extLIPageLink.add(new Label("extPageLabel", ann.label()));
                extPageLI.add(extLIPageLink);
            }
        };
        extPages.setOutputMarkupId(true);
        extPages.setVisible(true);
        extLI.add(extPages);

        WebMarkupContainer logoLinkWmc = new WebMarkupContainer("logoIcon");
        logoLinkWmc.add(new AjaxEventBehavior("click") {

            private static final long serialVersionUID = -4255753643957306394L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                setResponsePage(getApplication().getHomePage());
            }
        });
        add(logoLinkWmc);

        @SuppressWarnings("unchecked")
        final Class<? extends WebPage> beforeLogout = (Class<? extends WebPage>) SyncopeEnduserSession.get().
                getAttribute(Constants.BEFORE_LOGOUT_PAGE);
        if (beforeLogout == null) {
            add(new BookmarkablePageLink<>("logout", Logout.class));
        } else {
            add(new AjaxLink<Page>("logout") {

                private static final long serialVersionUID = -4889563567201424183L;

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);

                    AjaxCallListener ajaxCallListener = new AjaxCallListener();
                    ajaxCallListener.onPrecondition("return confirm('" + getString("confirmGlobalLogout") + "');");
                    attributes.getAjaxCallListeners().add(ajaxCallListener);
                }

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    setResponsePage(beforeLogout);
                }
            });
        }
    }

    public ListView<Class<? extends BaseExtPage>> getExtPages() {
        return extPages;
    }

    public void setActiveNavItem(final String id) {
        navbarItems.stream().
                filter(containingLI -> containingLI.getMarkupId().equals(id)).findFirst().
                ifPresent(found -> found.add(new Behavior() {

            private static final long serialVersionUID = -5775607340182293596L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {
                tag.put("class", "active");
            }
        }));
    }
}
