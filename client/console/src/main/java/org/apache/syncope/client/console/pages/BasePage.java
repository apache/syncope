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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.HttpResourceStream;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.widgets.ApprovalsWidget;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
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
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePage extends WebPage implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1571997737305598502L;

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    protected static final HeaderItem META_IE_EDGE = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    protected final WebMarkupContainer body;

    protected NotificationPanel notificationPanel;

    protected ApprovalsWidget approvalsWidget;

    public BasePage() {
        this(null);
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        body = new WebMarkupContainer("body");
        Serializable leftMenuCollapse = SyncopeConsoleSession.get().getAttribute(SyncopeConsoleSession.MENU_COLLAPSE);
        if ((leftMenuCollapse instanceof Boolean) && ((Boolean) leftMenuCollapse)) {
            body.add(new AttributeAppender("class", " sidebar-collapse"));
        }
        add(body);

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        body.addOrReplace(notificationPanel.setOutputMarkupId(true));

        // header, footer
        body.add(new Label("username", SyncopeConsoleSession.get().getSelfTO().getUsername()));

        approvalsWidget = new ApprovalsWidget("approvalsWidget", getPageReference());
        body.add(approvalsWidget.setRenderBodyOnly(true));

        // right sidebar
        PlatformInfo platformInfo = SyncopeConsoleSession.get().getPlatformInfo();
        Label version = new Label("version", platformInfo.getVersion());
        String versionLink = StringUtils.isNotBlank(platformInfo.getBuildNumber())
                && platformInfo.getVersion().endsWith("-SNAPSHOT")
                ? "https://git-wip-us.apache.org/repos/asf?p=syncope.git;a=commit;h="
                + platformInfo.getBuildNumber()
                : "https://cwiki.apache.org/confluence/display/SYNCOPE/Jazz";
        version.add(new AttributeModifier("onclick", "window.open('" + versionLink + "', '_blank')"));
        body.add(version);

        SystemInfo systemInfo = SyncopeConsoleSession.get().getSystemInfo();
        body.add(new Label("hostname", systemInfo.getHostname()));
        body.add(new Label("processors", systemInfo.getAvailableProcessors()));
        body.add(new Label("os", systemInfo.getOs()));
        body.add(new Label("jvm", systemInfo.getJvm()));

        Link<Void> dbExportLink = new Link<Void>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream = new HttpResourceStream(new ConfRestClient().dbExport());

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(stream.getFilename() == null
                            ? SyncopeConsoleSession.get().getDomain() + "Content.xml"
                            : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(
                dbExportLink, WebPage.RENDER, StandardEntitlement.CONFIGURATION_EXPORT);
        body.add(dbExportLink);

        // menu
        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("dashboard"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("dashboard", Dashboard.class));

        liContainer = new WebMarkupContainer(getLIContainerId("realms"));
        body.add(liContainer);

        BookmarkablePageLink<? extends BasePage> link = BookmarkablePageLinkBuilder.build("realms", Realms.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.REALM_LIST);

        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("topology"));
        body.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("topology", Topology.class);
        StringBuilder bld = new StringBuilder();
        bld.append(StandardEntitlement.CONNECTOR_LIST).append(",").
                append(StandardEntitlement.RESOURCE_LIST).append(",");
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, bld.toString());
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("reports"));
        body.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("reports", Reports.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.REPORT_LIST);
        liContainer.add(link);

        WebMarkupContainer confLIContainer = new WebMarkupContainer(getLIContainerId("configuration"));
        body.add(confLIContainer);
        WebMarkupContainer confULContainer = new WebMarkupContainer(getULContainerId("configuration"));
        confLIContainer.add(confULContainer);

        liContainer = new WebMarkupContainer(getLIContainerId("workflow"));
        liContainer.setOutputMarkupPlaceholderTag(true);
        liContainer.setVisible(platformInfo.getUserWorkflowAdapter().contains("Flowable"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("workflow", Workflow.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.WORKFLOW_DEF_GET);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("audit"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("audit", Audit.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.AUDIT_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("logs"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("logs", Logs.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.LOG_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("securityquestions"));
        confULContainer.add(liContainer);
        bld = new StringBuilder();
        bld.append(StandardEntitlement.SECURITY_QUESTION_CREATE).append(",").
                append(StandardEntitlement.SECURITY_QUESTION_DELETE).append(",").
                append(StandardEntitlement.SECURITY_QUESTION_UPDATE);
        link = BookmarkablePageLinkBuilder.build("securityquestions", SecurityQuestions.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, bld.toString());
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("types"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.SCHEMA_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("administration"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("administration", Administration.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.ROLE_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("policies"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.POLICY_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("notifications"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.NOTIFICATION_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("parameters"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("parameters", Parameters.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.CONFIGURATION_LIST);
        liContainer.add(link);

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

        @SuppressWarnings("unchecked")
        final Class<? extends WebPage> beforeLogout = (Class<? extends WebPage>) SyncopeConsoleSession.get().
                getAttribute(Constants.BEFORE_LOGOUT_PAGE);
        if (beforeLogout == null) {
            body.add(new BookmarkablePageLink<>("logout", Logout.class));
        } else {
            body.add(new AjaxLink<Page>("logout") {

                private static final long serialVersionUID = -7978723352517770644L;

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
        List<Class<? extends BaseExtPage>> extPageClasses = classPathScanImplementationLookup.getExtPageClasses();

        WebMarkupContainer extensionsLI = new WebMarkupContainer(getLIContainerId("extensions"));
        extensionsLI.setOutputMarkupPlaceholderTag(true);
        extensionsLI.setVisible(!extPageClasses.isEmpty());
        body.add(extensionsLI);

        ListView<Class<? extends BaseExtPage>> extPages = new ListView<Class<? extends BaseExtPage>>(
                "extPages", extPageClasses) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BaseExtPage>> item) {
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
        extensionsLI.add(extPages);

        if (getPage() instanceof BaseExtPage) {
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
        response.render(new PriorityHeaderItem(META_IE_EDGE));
    }

    private String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    private String getULContainerId(final String linkId) {
        return linkId + "UL";
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return Constants.VEIL_INDICATOR_MARKUP_ID;
    }

    public NotificationPanel getNotificationPanel() {
        return notificationPanel;
    }

    public ApprovalsWidget getApprovalsWidget() {
        return approvalsWidget;
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
