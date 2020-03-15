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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.annotations.AMPage;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.annotations.IdMPage;
import org.apache.syncope.client.ui.commons.HttpResourceStream;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.widgets.ExtAlertWidget;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePage extends BaseWebPage {

    private static final long serialVersionUID = 1571997737305598502L;

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    protected static final HeaderItem META_IE_EDGE = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    @SpringBean
    protected ClassPathScanImplementationLookup lookup;

    public BasePage() {
        this(null);
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        Serializable leftMenuCollapse = SyncopeConsoleSession.get().getAttribute(Constants.MENU_COLLAPSE);
        if ((leftMenuCollapse instanceof Boolean) && ((Boolean) leftMenuCollapse)) {
            body.add(new AttributeAppender("class", " sidebar-collapse"));
        }
        add(body);

        // header, footer
        body.add(new Label("username", SyncopeConsoleSession.get().getSelfTO().getUsername()));

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
                            new ResponseHolder(SyncopeRestClient.exportInternalStorageContent()));

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
        MetaDataRoleAuthorizationStrategy.authorize(dbExportLink, WebPage.RENDER, IdRepoEntitlement.KEYMASTER);
        body.add(dbExportLink);

        // menu
        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("dashboard"));
        body.add(liContainer);
        liContainer.add(BookmarkablePageLinkBuilder.build("dashboard", Dashboard.class));

        liContainer = new WebMarkupContainer(getLIContainerId("realms"));
        body.add(liContainer);

        BookmarkablePageLink<? extends BasePage> link = BookmarkablePageLinkBuilder.build("realms", Realms.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.REALM_LIST);

        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("reports"));
        body.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("reports", Reports.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.REPORT_LIST);
        liContainer.add(link);

        List<Class<? extends BasePage>> idmPageClasses = lookup.getIdMPageClasses();
        ListView<Class<? extends BasePage>> idmPages = new ListView<Class<? extends BasePage>>(
                "idmPages", idmPageClasses) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BasePage>> item) {
                WebMarkupContainer containingLI = new WebMarkupContainer("idmPageLI");
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

                IdMPage ann = item.getModelObject().getAnnotation(IdMPage.class);

                BookmarkablePageLink<Page> link = new BookmarkablePageLink<>("idmPage", item.getModelObject());
                link.add(new Label("idmPageLabel", ann.label()));
                if (StringUtils.isNotBlank(ann.listEntitlement())) {
                    MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, ann.listEntitlement());
                }
                containingLI.add(link);

                Label idmPageIcon = new Label("idmPageIcon");
                idmPageIcon.add(new AttributeModifier("class", "fa " + ann.icon()));
                link.add(idmPageIcon);
            }
        };
        idmPages.setRenderBodyOnly(true);
        idmPages.setOutputMarkupId(true);
        body.add(idmPages);

        List<Class<? extends BasePage>> amPageClasses = lookup.getAMPageClasses();
        ListView<Class<? extends BasePage>> amPages = new ListView<Class<? extends BasePage>>(
                "amPages", amPageClasses) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<Class<? extends BasePage>> item) {
                WebMarkupContainer containingLI = new WebMarkupContainer("amPageLI");
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

                AMPage ann = item.getModelObject().getAnnotation(AMPage.class);

                BookmarkablePageLink<Page> link = new BookmarkablePageLink<>("amPage", item.getModelObject());
                link.add(new Label("amPageLabel", ann.label()));
                if (StringUtils.isNotBlank(ann.listEntitlement())) {
                    MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, ann.listEntitlement());
                }
                containingLI.add(link);

                Label amPageIcon = new Label("amPageIcon");
                amPageIcon.add(new AttributeModifier("class", "fa " + ann.icon()));
                link.add(amPageIcon);
            }
        };
        amPages.setRenderBodyOnly(true);
        amPages.setOutputMarkupId(true);
        body.add(amPages);

        WebMarkupContainer keymasterLIContainer = new WebMarkupContainer(getLIContainerId("keymaster"));
        body.add(keymasterLIContainer);
        WebMarkupContainer keymasterULContainer = new WebMarkupContainer(getULContainerId("keymaster"));
        keymasterLIContainer.add(keymasterULContainer);

        if (SyncopeConstants.MASTER_DOMAIN.equals(SyncopeConsoleSession.get().getDomain())) {
            liContainer = new WebMarkupContainer(getLIContainerId("domains"));
            keymasterULContainer.add(liContainer);
            link = BookmarkablePageLinkBuilder.build("domains", Domains.class);
            MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.KEYMASTER);
            liContainer.add(link);

            liContainer = new WebMarkupContainer(getLIContainerId("networkservices"));
            keymasterULContainer.add(liContainer);
            link = BookmarkablePageLinkBuilder.build("networkservices", NetworkServices.class);
            MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.KEYMASTER);
            liContainer.add(link);
        }

        liContainer = new WebMarkupContainer(getLIContainerId("parameters"));
        keymasterULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("parameters", Parameters.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.KEYMASTER);
        liContainer.add(link);

        WebMarkupContainer confLIContainer = new WebMarkupContainer(getLIContainerId("configuration"));
        body.add(confLIContainer);
        WebMarkupContainer confULContainer = new WebMarkupContainer(getULContainerId("configuration"));
        confLIContainer.add(confULContainer);

        liContainer = new WebMarkupContainer(getLIContainerId("audit"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("audit", Audit.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.AUDIT_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("implementations"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("implementations", Implementations.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.IMPLEMENTATION_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("logs"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("logs", Logs.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.LOG_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("types"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.ANYTYPECLASS_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("security"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("security", Security.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER,
                String.format("%s,%s",
                        IdRepoEntitlement.ROLE_LIST,
                        IdRepoEntitlement.APPLICATION_LIST));
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("policies"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.POLICY_LIST);
        liContainer.add(link);

        liContainer = new WebMarkupContainer(getLIContainerId("notifications"));
        confULContainer.add(liContainer);
        link = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.RENDER, IdRepoEntitlement.NOTIFICATION_LIST);
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
        // 2. if not, check if it is under 'Keymaster'
        if (containingLI == null) {
            containingLI = keymasterULContainer.get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
        }
        // 3. if not, check if it is under 'Configuration'
        if (containingLI == null) {
            containingLI = confULContainer.get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
        }
        // 4. when found, set CSS coordinates for menu
        if (containingLI != null) {
            containingLI.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", "active");
                }
            });

            if (keymasterULContainer.getId().equals(containingLI.getParent().getId())) {
                keymasterULContainer.add(new Behavior() {

                    private static final long serialVersionUID = -5775607340182293596L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview-menu menu-open");
                        tag.put("style", "display: block;");
                    }

                });

                keymasterLIContainer.add(new Behavior() {

                    private static final long serialVersionUID = -5775607340182293596L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview active");
                    }
                });
            } else if (confULContainer.getId().equals(containingLI.getParent().getId())) {
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

        ListView<Class<? extends BaseExtPage>> extPages =
                new ListView<Class<? extends BaseExtPage>>("extPages", extPageClasses) {

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

    private static String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    private static String getULContainerId(final String linkId) {
        return linkId + "UL";
    }

    /**
     * Set a WindowClosedCallback for a Modal instance.
     *
     * @param modal window
     * @param container container
     */
    public static void setWindowClosedCallback(final BaseModal<?> modal, final WebMarkupContainer container) {
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
