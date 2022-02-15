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
import java.lang.reflect.Constructor;
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
import org.apache.syncope.client.console.panels.DelegationSelectionPanel;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.ResponseHolder;
import org.apache.syncope.client.console.topology.TabularTopology;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.syncope.client.console.widgets.ExtAlertWidget;
import org.apache.syncope.client.console.widgets.RemediationsWidget;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePage extends WebPage implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1571997737305598502L;

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    protected static final HeaderItem META_IE_EDGE = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    protected final WebMarkupContainer body;

    protected NotificationPanel notificationPanel;

    protected RemediationsWidget remediationWidget;

    public BasePage() {
        this(null);
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        body = new WebMarkupContainer("body");
        Serializable leftMenuCollapse = SyncopeConsoleSession.get().getAttribute(Constants.MENU_COLLAPSE);
        if ((leftMenuCollapse instanceof Boolean) && ((Boolean) leftMenuCollapse)) {
            body.add(new AttributeAppender("class", " sidebar-collapse"));
        }
        add(body);

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        body.addOrReplace(notificationPanel.setOutputMarkupId(true));

        // header, footer
        String username = SyncopeConsoleSession.get().getSelfTO().getUsername();
        if (SyncopeConsoleSession.get().getDelegatedBy() != null) {
            username += " (" + SyncopeConsoleSession.get().getDelegatedBy() + ")";
        }
        body.add(new Label("username", username));

        remediationWidget = new RemediationsWidget("remediationWidget", getPageReference());
        body.add(remediationWidget.setRenderBodyOnly(true));

        // right sidebar
        PlatformInfo platformInfo = SyncopeConsoleSession.get().getPlatformInfo();
        Label version = new Label("version", platformInfo.getVersion());
        String versionLink = StringUtils.isNotBlank(platformInfo.getBuildNumber())
                && platformInfo.getVersion().endsWith("-SNAPSHOT")
                ? "https://gitbox.apache.org/repos/asf?p=syncope.git;a=commit;h="
                + platformInfo.getBuildNumber()
                : "https://cwiki.apache.org/confluence/display/SYNCOPE/Fusion";
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
                    HttpResourceStream stream = new HttpResourceStream(
                            new ResponseHolder(new ConfRestClient().dbExport()));

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(stream.getFilename() == null
                            ? SyncopeConsoleSession.get().getDomain() + "Content.xml"
                            : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);
                    rsrh.setCacheDuration(Duration.NONE);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().onException(e);
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
        if (SyncopeConsoleApplication.get().getDefaultTopologyClass().contains("TabularTopology")) {
            link = BookmarkablePageLinkBuilder.build("topology", TabularTopology.class);
        } else {
            link = BookmarkablePageLinkBuilder.build("topology", Topology.class);
        }
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER,
                String.format("%s,%s",
                        StandardEntitlement.CONNECTOR_LIST,
                        StandardEntitlement.RESOURCE_LIST));
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

        liContainer = new WebMarkupContainer(getLIContainerId("audit"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("audit", Audit.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.AUDIT_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("implementations"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("implementations", Implementations.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.IMPLEMENTATION_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("logs"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("logs", Logs.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.LOG_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("types"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, StandardEntitlement.ANYTYPECLASS_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("security"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("security", Security.class);
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
                SyncopeConsoleSession.get().setAttribute(Constants.MENU_COLLAPSE,
                        SyncopeConsoleSession.get().getAttribute(Constants.MENU_COLLAPSE) == null
                        ? true
                        : !(Boolean) SyncopeConsoleSession.get().getAttribute(Constants.MENU_COLLAPSE));
            }
        });
        body.add(new Label("domain", SyncopeConsoleSession.get().getDomain()));

        WebMarkupContainer delegationsContainer = new WebMarkupContainer("delegationsContainer");
        body.add(delegationsContainer.setOutputMarkupPlaceholderTag(true).
                setVisible(!SyncopeConsoleSession.get().getDelegations().isEmpty()));
        delegationsContainer.add(new Label("delegationsHeader", new ResourceModel("delegations")));
        delegationsContainer.add(new ListView<String>("delegations", SyncopeConsoleSession.get().getDelegations()) {

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new DelegationSelectionPanel("delegation", item.getModelObject()));
            }
        });

        body.add(new IndicatingOnConfirmAjaxLink<String>("endDelegation", "confirmDelegation", true) {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SyncopeConsoleSession.get().setDelegatedBy(null);
                setResponsePage(Dashboard.class);
            }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).
                setVisible(SyncopeConsoleSession.get().getDelegatedBy() != null));

        @SuppressWarnings("unchecked")
        Class<? extends WebPage> beforeLogout = (Class<? extends WebPage>) SyncopeConsoleSession.get().
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
        ClassPathScanImplementationLookup lookup = (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);

        List<Class<? extends ExtAlertWidget<?>>> extAlertWidgetClasses = lookup.getExtAlertWidgetClasses();
        ListView<Class<? extends ExtAlertWidget<?>>> extAlertWidgets = new ListView<Class<? extends ExtAlertWidget<?>>>(
                "extAlertWidgets", extAlertWidgetClasses) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<Class<? extends ExtAlertWidget<?>>> item) {
                try {
                    Constructor<? extends ExtAlertWidget<?>> constructor =
                            item.getModelObject().getDeclaredConstructor(String.class, PageReference.class);
                    ExtAlertWidget<?> widget = constructor.newInstance("extAlertWidget", getPageReference());

                    SyncopeConsoleSession.get().setAttribute(widget.getClass().getName(), widget);

                    item.add(widget.setRenderBodyOnly(true));
                } catch (Exception e) {
                    LOG.error("Could not instantiate {}", item.getModelObject().getName(), e);
                }
            }
        };
        body.add(extAlertWidgets);

        List<Class<? extends BaseExtPage>> extPageClasses = lookup.getExtPageClasses();

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

    public RemediationsWidget getRemediationWidget() {
        return remediationWidget;
    }
}
