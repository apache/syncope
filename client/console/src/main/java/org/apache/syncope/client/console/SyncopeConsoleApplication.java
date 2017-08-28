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
package org.apache.syncope.client.console;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.MustChangePassword;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.resources.FilesystemResource;
import org.apache.syncope.client.console.resources.WorkflowDefGETResource;
import org.apache.syncope.client.console.resources.WorkflowDefPUTResource;
import org.apache.syncope.client.console.themes.AdminLTE;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.CsrfPreventionRequestCycleListener;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeConsoleApplication extends AuthenticatedWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleApplication.class);

    private static final String CONSOLE_PROPERTIES = "console.properties";

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            new Locale[] {
                Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR"), new Locale("ru")
            }));

    private static final String FLOWABLE_MODELER_CONTEXT = "flowable-modeler";

    public static SyncopeConsoleApplication get() {
        return (SyncopeConsoleApplication) WebApplication.get();
    }

    private String site;

    private String anonymousUser;

    private String anonymousKey;

    private String flowableModelerDirectory;

    private String reconciliationReportKey;

    private String scheme;

    private String host;

    private String port;

    private String rootPath;

    private String useGZIPCompression;

    private List<String> domains;

    private Map<String, Class<? extends BasePage>> pageClasses;

    @SuppressWarnings("unchecked")
    protected void populatePageClasses(final Properties props) {
        Enumeration<String> propNames = (Enumeration<String>) props.<String>propertyNames();
        while (propNames.hasMoreElements()) {
            String name = propNames.nextElement();
            if (name.startsWith("page.")) {
                try {
                    Class<?> clazz = ClassUtils.getClass(props.getProperty(name));
                    if (BasePage.class.isAssignableFrom(clazz)) {
                        pageClasses.put(
                                StringUtils.substringAfter("page.", name), (Class<? extends BasePage>) clazz);
                    } else {
                        LOG.warn("{} does not extend {}, ignoring...", clazz.getName(), BasePage.class.getName());
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("While looking for class identified by property '{}'", name, e);
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // read console.properties
        Properties props = PropertyUtils.read(getClass(), CONSOLE_PROPERTIES, "console.directory").getLeft();

        site = props.getProperty("site");
        Args.notNull(site, "<site>");
        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser>");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey>");

        scheme = props.getProperty("scheme");
        Args.notNull(scheme, "<scheme>");
        host = props.getProperty("host");
        Args.notNull(host, "<host>");
        port = props.getProperty("port");
        Args.notNull(port, "<port>");
        rootPath = props.getProperty("rootPath");
        Args.notNull(rootPath, "<rootPath>");
        useGZIPCompression = props.getProperty("useGZIPCompression");
        Args.notNull(useGZIPCompression, "<useGZIPCompression>");

        String csrf = props.getProperty("csrf");

        // process page properties
        pageClasses = new HashMap<>();
        populatePageClasses(props);
        pageClasses = Collections.unmodifiableMap(pageClasses);

        // Application settings
        IBootstrapSettings settings = new BootstrapSettings();

        // set theme provider
        settings.setThemeProvider(new SingleThemeProvider(new AdminLTE()));

        // install application settings
        Bootstrap.install(this, settings);

        getResourceSettings().setUseMinifiedResources(true);

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getJavaScriptLibrarySettings().setJQueryReference(new DynamicJQueryResourceReference());

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));

        ClassPathScanImplementationLookup lookup = (ClassPathScanImplementationLookup) getServletContext().
                getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
        lookup.getPageClasses().
                forEach(cls -> MetaDataRoleAuthorizationStrategy.authorize(cls, SyncopeConsoleSession.AUTHENTICATED));

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        if (BooleanUtils.toBoolean(csrf)) {
            getRequestCycleListeners().add(new CsrfPreventionRequestCycleListener());
        }
        getRequestCycleListeners().add(new SyncopeConsoleRequestCycleListener());

        mountPage("/login", getSignInPageClass());

        flowableModelerDirectory = props.getProperty("flowableModelerDirectory");
        Args.notNull(flowableModelerDirectory, "<flowableModelerDirectory>");

        try {
            reconciliationReportKey = props.getProperty("reconciliationReportKey");
        } catch (NumberFormatException e) {
            LOG.error("While parsing reconciliationReportKey", e);
        }
        Args.notNull(reconciliationReportKey, "<reconciliationReportKey>");

        mountResource("/" + FLOWABLE_MODELER_CONTEXT, new ResourceReference(FLOWABLE_MODELER_CONTEXT) {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new FilesystemResource(FLOWABLE_MODELER_CONTEXT, flowableModelerDirectory);
            }

        });
        mountResource("/workflowDefGET", new ResourceReference("workflowDefGET") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new WorkflowDefGETResource();
            }
        });
        mountResource("/workflowDefPUT", new ResourceReference("workflowDefPUT") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new WorkflowDefPUTResource();
            }
        });

        // enable component path
        if (getDebugSettings().isAjaxDebugModeEnabled()) {
            getDebugSettings().setComponentPathAttributeName("syncope-path");
        }
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return SyncopeConsoleSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return AuthenticatedWebSession.get().isSignedIn()
                && SyncopeConsoleSession.get().owns(StandardEntitlement.MUST_CHANGE_PASSWORD)
                ? MustChangePassword.class
                : Dashboard.class;
    }

    public Class<? extends BasePage> getPageClass(final String key) {
        return pageClasses.get(key);
    }

    public String getSite() {
        return site;
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public String getFlowableModelerDirectory() {
        return flowableModelerDirectory;
    }

    public String getReconciliationReportKey() {
        return reconciliationReportKey;
    }

    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + "/" + rootPath).
                setUseCompression(BooleanUtils.toBoolean(useGZIPCompression));
    }

    public List<String> getDomains() {
        synchronized (LOG) {
            if (domains == null) {
                domains = newClientFactory().create(
                        new AnonymousAuthenticationHandler(anonymousUser, anonymousKey)).
                        getService(DomainService.class).list().stream().map(EntityTO::getKey).
                        collect(Collectors.toList());
                domains.add(0, SyncopeConstants.MASTER_DOMAIN);
                domains = ListUtils.unmodifiableList(domains);
            }
        }
        return domains;
    }
}
