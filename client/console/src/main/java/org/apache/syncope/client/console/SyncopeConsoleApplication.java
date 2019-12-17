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

import com.google.common.net.HttpHeaders;
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
import org.apache.syncope.client.console.annotations.Resource;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.MustChangePassword;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.themes.AdminLTE;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.protocol.ws.WebSocketAwareCsrfPreventionRequestCycleListener;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.protocol.ws.api.WebSocketResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeConsoleApplication extends AuthenticatedWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleApplication.class);

    private static final String CONSOLE_PROPERTIES = "console.properties";

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            Locale.ENGLISH, Locale.CANADA_FRENCH, Locale.ITALIAN, Locale.JAPANESE, new Locale("pt", "BR"),
            new Locale("ru")
    ));

    public static SyncopeConsoleApplication get() {
        return (SyncopeConsoleApplication) WebApplication.get();
    }

    private String anonymousUser;

    private String anonymousKey;

    private String reconciliationReportKey;

    private String scheme;

    private String host;

    private String port;

    private String rootPath;

    private String useGZIPCompression;

    private Integer maxUploadFileSizeMB;

    private Integer maxWaitTime;

    private Integer corePoolSize;

    private Integer maxPoolSize;

    private Integer queueCapacity;

    private List<String> domains;

    private Map<String, Class<? extends BasePage>> pageClasses;

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
        Properties props = PropertyUtils.read(getClass(), CONSOLE_PROPERTIES, "console.directory").getLeft();

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
        maxUploadFileSizeMB = props.getProperty("maxUploadFileSizeMB") == null
                ? null
                : Integer.valueOf(props.getProperty("maxUploadFileSizeMB"));

        maxWaitTime = Integer.valueOf(props.getProperty("maxWaitTimeOnApplyChanges", "30"));

        // Resource connections check thread pool size
        corePoolSize = Integer.valueOf(props.getProperty("topology.corePoolSize", "5"));
        maxPoolSize = Integer.valueOf(props.getProperty("topology.maxPoolSize", "10"));
        queueCapacity = Integer.valueOf(props.getProperty("topology.queueCapacity", "50"));

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

        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV2());

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));

        ClassPathScanImplementationLookup lookup = (ClassPathScanImplementationLookup) getServletContext().
                getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
        lookup.getPageClasses().
                forEach(cls -> MetaDataRoleAuthorizationStrategy.authorize(cls, Constants.ROLE_AUTHENTICATED));

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        if (BooleanUtils.toBoolean(props.getProperty("x-forward"))) {
            XForwardedRequestWrapperFactory.Config config = new XForwardedRequestWrapperFactory.Config();
            config.setProtocolHeader(props.getProperty("x-forward.protocol.header", HttpHeaders.X_FORWARDED_PROTO));
            config.setHttpServerPort(Integer.valueOf(props.getProperty("x-forward.http.port", "80")));
            config.setHttpsServerPort(Integer.valueOf(props.getProperty("x-forward.https.port", "443")));

            XForwardedRequestWrapperFactory factory = new XForwardedRequestWrapperFactory();
            factory.setConfig(config);
            getFilterFactoryManager().add(factory);
        }

        if (BooleanUtils.toBoolean(props.getProperty("csrf"))) {
            getRequestCycleListeners().add(new WebSocketAwareCsrfPreventionRequestCycleListener());
        }
        getRequestCycleListeners().add(new SyncopeConsoleRequestCycleListener());
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse && !(cycle.getResponse() instanceof WebSocketResponse)) {
                    setSecurityHeaders(props, (WebResponse) cycle.getResponse());
                }
            }
        });

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

    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + StringUtils.prependIfMissing(rootPath, "/")).
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
