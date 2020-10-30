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

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;
import com.google.common.net.HttpHeaders;
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.themes.AdminLTE;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.protocol.ws.WebSocketAwareCsrfPreventionRequestCycleListener;
import org.apache.wicket.protocol.ws.api.WebSocketResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.pages.MustChangePassword;
import org.apache.syncope.client.console.panels.AnyPanel;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncopeWebApplication extends WicketBootSecuredWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    private static final String CONSOLE_PROPERTIES = "console.properties";

    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH, Locale.CANADA_FRENCH, Locale.ITALIAN, Locale.JAPANESE, new Locale("pt", "BR"),
            new Locale("ru"));

    public static SyncopeWebApplication get() {
        return (SyncopeWebApplication) WebApplication.get();
    }

    @Autowired
    private ClassPathScanImplementationLookup lookup;

    @Autowired
    private ServiceOps serviceOps;

    private String anonymousUser;

    private String anonymousKey;

    private String reconciliationReportKey;

    private boolean useGZIPCompression;

    private Integer maxUploadFileSizeMB;

    private Integer maxWaitTime;

    private Integer corePoolSize;

    private Integer maxPoolSize;

    private Integer queueCapacity;

    @Autowired
    private ExternalResourceProvider resourceProvider;

    @Autowired
    private AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider;

    @Autowired
    private AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider;

    @Autowired
    private AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps;

    @Autowired
    private StatusProvider statusProvider;

    @Autowired
    private VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider;

    @Autowired
    private ImplementationInfoProvider implementationInfoProvider;

    @Autowired
    private PolicyTabProvider policyTabProvider;

    private Map<String, Class<? extends BasePage>> pageClasses;
    
    private String defaultAnyLayoutClass;

    @SuppressWarnings("unchecked")
    protected void populatePageClasses(final Properties props) {
        Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
        while (propNames.hasMoreElements()) {
            String className = propNames.nextElement();
            if (className.startsWith("page.")) {
                try {
                    Class<?> clazz = ClassUtils.getClass(props.getProperty(className));
                    if (BasePage.class.isAssignableFrom(clazz)) {
                        pageClasses.put(
                                StringUtils.substringAfter("page.", className), (Class<? extends BasePage>) clazz);
                    } else {
                        LOG.warn("{} does not extend {}, ignoring...", clazz.getName(), BasePage.class.getName());
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("While looking for class identified by property '{}'", className, e);
                }
            }
        }
    }

    protected void setSecurityHeaders(final Properties props, final WebResponse response) {
        @SuppressWarnings("unchecked")
        Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
        while (propNames.hasMoreElements()) {
            String name = propNames.nextElement();
            if (name.startsWith("security.headers.")) {
                response.setHeader(StringUtils.substringAfter(name, "security.headers."), props.getProperty(name));
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // read console.properties
        Properties props = PropertyUtils.read(getClass(), CONSOLE_PROPERTIES, "console.directory");

        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser>");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey>");

        useGZIPCompression = BooleanUtils.toBoolean(props.getProperty("useGZIPCompression"));
        Args.notNull(useGZIPCompression, "<useGZIPCompression>");
        maxUploadFileSizeMB = props.getProperty("maxUploadFileSizeMB") == null
                ? null
                : Integer.valueOf(props.getProperty("maxUploadFileSizeMB"));

        try {
            maxWaitTime = Integer.valueOf(props.getProperty("maxWaitTimeOnApplyChanges", "30"));
        } catch (NumberFormatException e) {
            LOG.error("Invalid value provided for 'maxWaitTimeOnApplyChanges': {}",
                    props.getProperty("maxWaitTimeOnApplyChanges"));
            maxWaitTime = 30;
        }

        // Resource connections check thread pool size
        try {
            corePoolSize = Integer.valueOf(props.getProperty("executor.corePoolSize", "5"));
        } catch (NumberFormatException e) {
            LOG.error("Invalid value provided for 'executor.corePoolSize': {}",
                    props.getProperty("executor.corePoolSize"));
            corePoolSize = 5;
        }
        try {
            maxPoolSize = Integer.valueOf(props.getProperty("executor.maxPoolSize", "10"));
        } catch (NumberFormatException e) {
            LOG.error("Invalid value provided for 'executor.maxPoolSize': {}",
                    props.getProperty("executor.maxPoolSize"));
            maxPoolSize = 10;
        }
        try {
            queueCapacity = Integer.valueOf(props.getProperty("executor.queueCapacity", "50"));
        } catch (NumberFormatException e) {
            LOG.error("Invalid value provided for 'executor.queueCapacity': {}",
                    props.getProperty("executor.queueCapacity"));
            maxPoolSize = 50;
        }

        // process page properties
        pageClasses = new HashMap<>();
        populatePageClasses(props);
        pageClasses = Collections.unmodifiableMap(pageClasses);

        defaultAnyLayoutClass = props.getProperty("default.any.panel.class", AnyPanel.class.getName());
        
        // Application settings
        IBootstrapSettings settings = new BootstrapSettings();

        // set theme provider
        settings.setThemeProvider(new SingleThemeProvider(new AdminLTE()));

        // install application settings
        Bootstrap.install(this, settings);

        getResourceSettings().setUseMinifiedResources(true);

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));

        lookup.getPageClasses().
                forEach(cls -> MetaDataRoleAuthorizationStrategy.authorize(cls, Constants.ROLE_AUTHENTICATED));

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        getRequestCycleListeners().add(new SyncopeUIRequestCycleListener() {

            @Override
            protected boolean isSignedIn() {
                return SyncopeConsoleSession.get().isSignedIn();
            }

            @Override
            protected void invalidateSession() {
                SyncopeConsoleSession.get().invalidate();
            }

            @Override
            protected IRequestablePage getErrorPage(final PageParameters errorParameters) {
                return new Login(errorParameters);
            }
        });

        if (BooleanUtils.toBoolean(props.getProperty("x-forward"))) {
            XForwardedRequestWrapperFactory.Config config = new XForwardedRequestWrapperFactory.Config();
            config.setProtocolHeader(props.getProperty("x-forward.protocol.header", HttpHeaders.X_FORWARDED_PROTO));
            try {
                config.setHttpServerPort(Integer.valueOf(props.getProperty("x-forward.http.port", "80")));
            } catch (NumberFormatException e) {
                LOG.error("Invalid value provided for 'x-forward.http.port': {}",
                        props.getProperty("x-forward.http.port"));
                config.setHttpServerPort(80);
            }
            try {
                config.setHttpsServerPort(Integer.valueOf(props.getProperty("x-forward.https.port", "443")));
            } catch (NumberFormatException e) {
                LOG.error("Invalid value provided for 'x-forward.https.port': {}",
                        props.getProperty("x-forward.https.port"));
                config.setHttpsServerPort(443);
            }

            XForwardedRequestWrapperFactory factory = new XForwardedRequestWrapperFactory();
            factory.setConfig(config);
            getFilterFactoryManager().add(factory);
        }

        if (BooleanUtils.toBoolean(props.getProperty("csrf"))) {
            getRequestCycleListeners().add(new WebSocketAwareCsrfPreventionRequestCycleListener());
        }
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse && !(cycle.getResponse() instanceof WebSocketResponse)) {
                    setSecurityHeaders(props, (WebResponse) cycle.getResponse());
                }
            }
        });
        getCspSettings().blocking().unsafeInline();

        mountPage("/login", getSignInPageClass());

        try {
            reconciliationReportKey = props.getProperty("reconciliationReportKey");
        } catch (NumberFormatException e) {
            LOG.error("While parsing reconciliationReportKey", e);
        }
        Args.notNull(reconciliationReportKey, "<reconciliationReportKey>");

        for (Class<? extends AbstractResource> resource : lookup.getResources()) {
            Resource annotation = resource.getAnnotation(Resource.class);
            try {
                AbstractResource instance = resource.getDeclaredConstructor().newInstance();

                mountResource(annotation.path(), new ResourceReference(annotation.key()) {

                    private static final long serialVersionUID = -128426276529456602L;

                    @Override
                    public IResource getResource() {
                        return instance;
                    }
                });
            } catch (Exception e) {
                LOG.error("Could not instantiate {}", resource.getName(), e);
            }
        }

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
                && SyncopeConsoleSession.get().getSelfTO().isMustChangePassword()
                ? MustChangePassword.class
                : Dashboard.class;
    }

    public Class<? extends BasePage> getPageClass(final String key) {
        return pageClasses.get(key);
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public String getReconciliationReportKey() {
        return reconciliationReportKey;
    }

    public Integer getMaxUploadFileSizeMB() {
        return maxUploadFileSizeMB;
    }

    public Integer getMaxWaitTimeInSeconds() {
        return maxWaitTime;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public Integer getQueueCapacity() {
        return queueCapacity;
    }

    public String getDefaultAnyLayoutClass() {
        return defaultAnyLayoutClass;
    }
    
    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                setUseCompression(useGZIPCompression);
    }

    public ExternalResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public AnyDirectoryPanelAdditionalActionsProvider getAnyDirectoryPanelAdditionalActionsProvider() {
        return anyDirectoryPanelAdditionalActionsProvider;
    }

    public AnyDirectoryPanelAdditionalActionLinksProvider getAnyDirectoryPanelAdditionalActionLinksProvider() {
        return anyDirectoryPanelAdditionalActionLinksProvider;
    }

    public AnyWizardBuilderAdditionalSteps getAnyWizardBuilderAdditionalSteps() {
        return anyWizardBuilderAdditionalSteps;
    }

    public StatusProvider getStatusProvider() {
        return statusProvider;
    }

    public VirSchemaDetailsPanelProvider getVirSchemaDetailsPanelProvider() {
        return virSchemaDetailsPanelProvider;
    }

    public ImplementationInfoProvider getImplementationInfoProvider() {
        return implementationInfoProvider;
    }

    public PolicyTabProvider getPolicyTabProvider() {
        return policyTabProvider;
    }
}
