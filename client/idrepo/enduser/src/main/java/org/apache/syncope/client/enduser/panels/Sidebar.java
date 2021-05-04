/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.enduser.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.syncope.client.enduser.pages.EditChangePassword;
import org.apache.syncope.client.enduser.pages.EditSecurityQuestion;
import org.apache.syncope.client.enduser.pages.EditUser;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

public class Sidebar extends Panel {

    private static final long serialVersionUID = 8091307811313529503L;

    private final List<WebMarkupContainer> navbarItems = new ArrayList<>();

    protected WebMarkupContainer profileULContainer;

    protected WebMarkupContainer profileLIContainer;

    public Sidebar(
            final String id,
            final PageReference pageReference,
            final List<Class<? extends BasePage>> extPageClasses) {

        super(id);

        buildBaseSidebar();

        RepeatingView listItems = new RepeatingView("listItems");
        add(listItems);

        extPageClasses.forEach(ext -> {
            WebMarkupContainer list = new WebMarkupContainer(listItems.newChildId());
            BookmarkablePageLink<Page> externalLink =
                    new BookmarkablePageLink<>("extPageLILink", ext);
            ExtPage ann = ext.getAnnotation(ExtPage.class);
            externalLink.add(new Label("extPageIcon").add(new AttributeModifier("class", "fa " + ann.icon())));
            externalLink.add(new Label("extPageLabel", ann.label()));
            list.add(externalLink);
            listItems.add(list);
        });

        // set 'active' menu item for everything but extensions
        // 1. check if current class is set to top-level menu
        Component containingLI = this.get(getLIContainerId(
                pageReference.getPage().getClass().getSimpleName().toLowerCase()));
        // 2. if not, check if it is under 'Configuration'
        if (containingLI == null) {
            containingLI = profileULContainer.get(
                    getLIContainerId(pageReference.getPage().getClass().getSimpleName().toLowerCase()));
        }
        // 3. when found, set CSS coordinates for menu
        if (containingLI != null) {
            containingLI.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", "active");
                }
            });

            if (profileULContainer.getId().equals(containingLI.getParent().getId())) {
                profileULContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview-menu menu-open");
                        tag.put("style", "display: block;");
                    }

                });

                profileLIContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview active");
                    }
                });
            }
        }
    }

    protected void buildBaseSidebar() {
        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("home"));
        add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build(
                "home", SyncopeEnduserApplication.get().getPageClass("profile", Dashboard.class)));

        profileLIContainer = new WebMarkupContainer(getLIContainerId("profile"));
        add(profileLIContainer);
        profileULContainer = new WebMarkupContainer(getULContainerId("profile"));
        profileLIContainer.add(profileULContainer);

        liContainer = new WebMarkupContainer(getLIContainerId("edituser"));
        profileULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("edituser", EditUser.class));

        liContainer = new WebMarkupContainer(getLIContainerId("editchangepassword"));
        profileULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("editchangepassword", EditChangePassword.class));

        liContainer = new WebMarkupContainer(getLIContainerId("editsecurityquestion"));
        profileULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("editsecurityquestion", EditSecurityQuestion.class));
        liContainer.setOutputMarkupPlaceholderTag(true);
        liContainer.setVisible(SyncopeEnduserSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions());
    }

    protected String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    protected String getULContainerId(final String linkId) {
        return linkId + "UL";
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
