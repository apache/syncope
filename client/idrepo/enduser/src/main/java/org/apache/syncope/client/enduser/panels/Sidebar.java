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
package org.apache.syncope.client.enduser.panels;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.layout.SidebarLayout;
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
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

public class Sidebar extends Panel {

    private static final long serialVersionUID = 8091307811313529503L;

    protected static String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    protected static String getULContainerId(final String linkId) {
        return linkId + "UL";
    }

    protected WebMarkupContainer dashboardLIContainer;

    protected WebMarkupContainer profileULContainer;

    protected WebMarkupContainer profileLIContainer;

    public Sidebar(
            final String id,
            final PageReference pageRef,
            final List<Class<? extends BasePage>> extPageClasses) {

        super(id);

        buildBaseSidebar();

        // set 'active' menu item for everything but extensions
        // 1. check if current class is set to top-level menu        
        WebMarkupContainer containingLI = null;
        if (dashboardLIContainer.getId().equals(
                getLIContainerId(pageRef.getPage().getClass().getSimpleName().toLowerCase()))) {

            containingLI = dashboardLIContainer;
        }
        // 2. if not, check if it is under 'Configuration'
        if (containingLI == null) {
            containingLI = (WebMarkupContainer) profileULContainer.get(
                    getLIContainerId(pageRef.getPage().getClass().getSimpleName().toLowerCase()));
        }
        // 3. when found, set CSS coordinates for menu
        if (containingLI != null) {
            StreamSupport.stream(containingLI.spliterator(), false).filter(Link.class::isInstance).
                    forEach(child -> child.add(new Behavior() {

                private static final long serialVersionUID = -5775607340182293596L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.append("class", "active", " ");
                }
            }));

            if (profileULContainer.getId().equals(containingLI.getParent().getId())) {
                profileULContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void renderHead(final Component component, final IHeaderResponse response) {
                        response.render(OnDomReadyHeaderItem.forScript("$('#profileLink').addClass('active')"));
                    }

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "nav nav-treeview");
                        tag.put("style", "display: block;");
                    }
                });

                profileLIContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "nav-item has-treeview menu-open");
                    }
                });
            }
        }

        ListView<Class<? extends BasePage>> extPages = new ListView<>("extPages", extPageClasses.stream().
                filter(epc -> SyncopeWebApplication.get().getCustomFormLayout().getSidebarLayout().
                isExtensionEnabled(StringUtils.remove(epc.getAnnotation(ExtPage.class).label(), StringUtils.SPACE))).
                collect(Collectors.toList())) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BasePage>> item) {
                WebMarkupContainer containingLI = new WebMarkupContainer("extPageLI");
                item.add(containingLI);

                ExtPage ann = item.getModelObject().getAnnotation(ExtPage.class);

                BookmarkablePageLink<Page> link = new BookmarkablePageLink<>("extPage", item.getModelObject());

                link.add(new Label("extPageLabel", ann.label()));

                if (item.getModelObject().equals(pageRef.getPage().getClass())) {
                    link.add(new Behavior() {

                        private static final long serialVersionUID = 1469628524240283489L;

                        @Override
                        public void renderHead(final Component component, final IHeaderResponse response) {
                            response.render(OnDomReadyHeaderItem.forScript(
                                    "$('#extensionsLink').addClass('active')"));
                        }

                        @Override
                        public void onComponentTag(final Component component, final ComponentTag tag) {
                            tag.append("class", "active", " ");
                        }
                    });
                }
                containingLI.add(link);

                Label extPageIcon = new Label("extPageIcon");
                extPageIcon.add(new AttributeModifier("class", "nav-icon " + ann.icon()));
                link.add(extPageIcon);
            }
        };

        add(extPages.setRenderBodyOnly(true).setOutputMarkupId(true));
    }

    protected void buildBaseSidebar() {
        SidebarLayout layout = SyncopeWebApplication.get().getCustomFormLayout().getSidebarLayout();

        dashboardLIContainer = new WebMarkupContainer(getLIContainerId("dashboard"));
        add(dashboardLIContainer);
        dashboardLIContainer.add(BookmarkablePageLinkBuilder.build(
                "home", SyncopeWebApplication.get().getPageClass("profile", Dashboard.class)));

        profileLIContainer = new WebMarkupContainer(getLIContainerId("profile"));
        add(profileLIContainer);
        profileULContainer = new WebMarkupContainer(getULContainerId("profile"));
        profileLIContainer.add(profileULContainer);
        profileLIContainer.setVisible(layout.isEditUserEnabled()
                || layout.isPasswordManagementEnabled()
                || (layout.isSecurityQuestionManagementEnabled()
                && SyncopeEnduserSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions()));

        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("edituser"));
        profileULContainer.add(liContainer);

        liContainer.add(BookmarkablePageLinkBuilder.build("edituser", EditUser.class)).
                setVisible(layout.isEditUserEnabled());
        liContainer = new WebMarkupContainer(getLIContainerId("editchangepassword"));
        profileULContainer.add(liContainer);

        liContainer.add(BookmarkablePageLinkBuilder.build("editchangepassword", EditChangePassword.class)).
                setVisible(layout.isPasswordManagementEnabled());

        liContainer = new WebMarkupContainer(getLIContainerId("editsecurityquestion"));
        profileULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("editsecurityquestion", EditSecurityQuestion.class));
        liContainer.setOutputMarkupPlaceholderTag(true);
        liContainer.setVisible(layout.isSecurityQuestionManagementEnabled()
                && SyncopeEnduserSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions());
    }
}
