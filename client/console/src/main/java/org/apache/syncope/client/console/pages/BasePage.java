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
package org.apache.syncope.client.console.pages;

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.NotificationAwareComponent;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePage extends WebPage implements NotificationAwareComponent, IAjaxIndicatorAware {

    private static final long serialVersionUID = 1571997737305598502L;

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    protected static final String CANCEL = "cancel";

    protected static final String SUBMIT = "submit";

    protected static final String APPLY = "apply";

    protected static final String FORM = "form";

    protected final HeaderItem meta = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    protected final WebMarkupContainer body;

    protected NotificationPanel notificationPanel;

    public BasePage() {
        this(null);
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        body = new WebMarkupContainer("body");
        Serializable leftMenuCollapse =
                SyncopeConsoleSession.get().getAttribute(SyncopeConsoleSession.MENU_COLLAPSE);
        if ((leftMenuCollapse instanceof Boolean) && ((Boolean) leftMenuCollapse)) {
            body.add(new AttributeAppender("class", " sidebar-collapse"));
        }
        add(body);

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        notificationPanel.setOutputMarkupId(true);
        body.addOrReplace(notificationPanel);

        // header, footer
        body.add(new Label("version", SyncopeConsoleApplication.get().getVersion()));
        body.add(new Label("username", SyncopeConsoleSession.get().getSelfTO().getUsername()));

        WebMarkupContainer todosContainer = new WebMarkupContainer("todosContainer");
        body.add(todosContainer);
        Label todos = new Label("todos", "0");
        todosContainer.add(todos);
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)) {
            todos.setDefaultModelObject(new UserWorkflowRestClient().getForms().size());
        }
        MetaDataRoleAuthorizationStrategy.authorize(
                todosContainer, WebPage.RENDER, StandardEntitlement.WORKFLOW_FORM_LIST);

        // menu
        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("dashboard"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("dashboard", Dashboard.class));

        liContainer = new WebMarkupContainer(getLIContainerId("realms"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("realms", Realms.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.REALM_LIST);

        liContainer = new WebMarkupContainer(getLIContainerId("topology"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("topology", Topology.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER,
                String.format("%s,%s", StandardEntitlement.CONNECTOR_LIST, StandardEntitlement.RESOURCE_LIST));

        liContainer = new WebMarkupContainer(getLIContainerId("reports"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("reports", Reports.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.REPORT_LIST);

        WebMarkupContainer confLIContainer = new WebMarkupContainer(getLIContainerId("configuration"));
        body.add(confLIContainer);
        WebMarkupContainer confULContainer = new WebMarkupContainer(getULContainerId("configuration"));
        confLIContainer.add(confULContainer);

        liContainer = new WebMarkupContainer(getLIContainerId("workflow"));
        confULContainer.add(liContainer);
        BookmarkablePageLink<Page> workflowLink = BookmarkablePageLinkBuilder.build("workflow", Workflow.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                workflowLink, WebPage.ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        liContainer.add(workflowLink);

        liContainer = new WebMarkupContainer(getLIContainerId("logs"));
        confULContainer.add(liContainer);
        BookmarkablePageLink<Page> logsLink = BookmarkablePageLinkBuilder.build("logs", Logs.class);
        MetaDataRoleAuthorizationStrategy.authorize(logsLink, WebPage.ENABLE, StandardEntitlement.LOG_LIST);
        liContainer.add(logsLink);
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.LOG_LIST);

        liContainer = new WebMarkupContainer(getLIContainerId("securityquestions"));
        confULContainer.add(liContainer);
        BookmarkablePageLink<Page> secuityQuestionsLink =
                BookmarkablePageLinkBuilder.build("securityquestions", SecurityQuestions.class);
        liContainer.add(secuityQuestionsLink);

        liContainer = new WebMarkupContainer(getLIContainerId("types"));
        confULContainer.add(liContainer);
        BookmarkablePageLink<Page> typesLink = BookmarkablePageLinkBuilder.build("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(typesLink, WebPage.ENABLE, StandardEntitlement.SCHEMA_LIST);
        liContainer.add(typesLink);

        liContainer = new WebMarkupContainer(getLIContainerId("roles"));
        confULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("roles", Roles.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.ROLE_LIST);

        liContainer = new WebMarkupContainer(getLIContainerId("policies"));
        confULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("policies", Policies.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.POLICY_LIST);

        liContainer = new WebMarkupContainer(getLIContainerId("layouts"));
        confULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("layouts", Layouts.class));

        liContainer = new WebMarkupContainer(getLIContainerId("notifications"));
        confULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("notifications", Notifications.class));
        MetaDataRoleAuthorizationStrategy.authorize(liContainer, WebPage.RENDER, StandardEntitlement.NOTIFICATION_LIST);

        liContainer = new WebMarkupContainer(getLIContainerId("parameters"));
        confULContainer.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("parameters", Parameters.class));
        MetaDataRoleAuthorizationStrategy.authorize(
                liContainer, WebPage.RENDER, StandardEntitlement.CONFIGURATION_LIST);

        body.add(new AjaxLink<Void>("collapse") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SyncopeConsoleSession.get().setAttribute(SyncopeConsoleSession.MENU_COLLAPSE,
                        SyncopeConsoleSession.get().getAttribute(SyncopeConsoleSession.MENU_COLLAPSE) == null
                        ? true
                        : !(Boolean) SyncopeConsoleSession.get().getAttribute(SyncopeConsoleSession.MENU_COLLAPSE));
            }
        });
        body.add(new Label("domain", SyncopeConsoleSession.get().getDomain()));
        body.add(new BookmarkablePageLink<Page>("logout", Logout.class));

        // set 'active' menu item for everything but extensions
        // 1. check if current class is set to top-level menu
        Component containingLI = body.get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
        // 2. if not, check if it is under 'Configuration'
        if (containingLI == null) {
            containingLI = confULContainer.get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
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

            if (confULContainer.getId().equals(containingLI.getParent().getId())) {
                confULContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview-menu menu-open");
                        tag.put("style", "display: block;");
                    }

                });

                confLIContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview active");
                    }
                });
            }
        }

        // Extensions
        ClassPathScanImplementationLookup classPathScanImplementationLookup =
                (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
        List<Class<? extends AbstractExtPage>> extPageClasses = classPathScanImplementationLookup.getExtPageClasses();

        WebMarkupContainer extensionsLI = new WebMarkupContainer(getLIContainerId("extensions"));
        extensionsLI.setOutputMarkupPlaceholderTag(true);
        extensionsLI.setVisible(!extPageClasses.isEmpty());
        body.add(extensionsLI);

        ListView<Class<? extends AbstractExtPage>> extPages = new ListView<Class<? extends AbstractExtPage>>(
                "extPages", extPageClasses) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends AbstractExtPage>> item) {
                WebMarkupContainer containingLI = new WebMarkupContainer("extPageLI");
                item.add(containingLI);
                if (item.getModelObject().equals(BasePage.this.getClass())) {
                    containingLI.add(new Behavior() {

                        private static final long serialVersionUID = 1469628524240283489L;

                        @Override
                        public void onComponentTag(final Component component, final ComponentTag tag) {
                            tag.put("class", "active");
                        }
                    });
                }

                BookmarkablePageLink<Page> link = new BookmarkablePageLink<>("extPage", item.getModelObject());
                containingLI.add(link);

                ExtPage ann = item.getModelObject().getAnnotation(ExtPage.class);

                link.add(new Label("extPageLabel", ann.label()));

                Label extPageIcon = new Label("extPageIcon");
                extPageIcon.add(new AttributeModifier("class", "fa " + ann.icon()));
                link.add(extPageIcon);
            }
        };
        extPages.setOutputMarkupId(true);
        extensionsLI.add(extPages);

        if (getPage() instanceof AbstractExtPage) {
            extPages.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", "treeview-menu menu-open");
                    tag.put("style", "display: block;");
                }

            });

            extensionsLI.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", "treeview active");
                }
            });
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(new PriorityHeaderItem(meta));
    }

    @Override
    public NotificationPanel getNotificationPanel() {
        return notificationPanel;
    }

    private String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    private String getULContainerId(final String linkId) {
        return linkId + "UL";
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }

    /**
     * Set a WindowClosedCallback for a Modal instance.
     *
     * @param modal window
     * @param container container
     */
    public void setWindowClosedCallback(final BaseModal<?> modal, final WebMarkupContainer container) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                if (container != null) {
                    target.add(container);
                }
            }
        });
    }
}
