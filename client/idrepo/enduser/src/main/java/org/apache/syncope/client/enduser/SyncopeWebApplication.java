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
package org.apache.syncope.client.enduser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giffing.wicket.spring.boot.starter.app.WicketBootStandardWebApplication;
import com.google.common.net.HttpHeaders;
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.enduser.pages.SelfConfirmPasswordReset;
import org.apache.syncope.client.enduser.panels.Sidebar;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.themes.AdminLTE;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.ResourceIsolationRequestCycleListener;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SyncopeWebApplication extends WicketBootStandardWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    private static final String CUSTOM_FORM_LAYOUT_FILE = "customFormLayout.json";

    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR"), new Locale("ru"), Locale.JAPANESE);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static SyncopeWebApplication get() {
        return (SyncopeWebApplication) WebApplication.get();
    }

    @Autowired
    private ClassPathScanImplementationLookup lookup;

    @Autowired
    private ServiceOps serviceOps;

    @Value("${adminUser}")
    private String adminUser;

    @Value("${anonymousUser}")
    protected String anonymousUser;

    @Value("${anonymousKey}")
    protected String anonymousKey;

    @Value("${useGZIPCompression:false}")
    protected boolean useGZIPCompression;

    @Value("${captchaEnabled:false}")
    private boolean captchaEnabled;

    @Value("${maxUploadFileSizeMB:#{null}}")
    protected Integer maxUploadFileSizeMB;

    @Value("${maxWaitTime:30}")
    protected Integer maxWaitTime;

    @Value("${corePoolSize:5}")
    protected Integer corePoolSize;

    @Value("${maxPoolSize:10}")
    protected Integer maxPoolSize;

    @Value("${queueCapacity:50}")
    protected Integer queueCapacity;

    private FileAlterationMonitor customFormLayoutMonitor;

    private Map<String, Class<? extends BasePage>> pageClasses;

    private Class<? extends Sidebar> sidebar;

    private UserFormLayoutInfo customFormLayout;

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
                                StringUtils.substringAfter(className, "page."), (Class<? extends BasePage>) clazz);
                    } else {
                        LOG.warn("{} does not extend {}, ignoring...", clazz.getName(), BasePage.class.getName());
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("While looking for class identified by property '{}'", className, e);
                }
            }
        }
    }

    protected static void setSecurityHeaders(final Properties props, final WebResponse response) {
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

        // read enduser.properties
        Properties props = PropertyUtils.read(getClass(), ENDUSER_PROPERTIES, "enduser.directory");

        // read customFormLayout.json
        try (InputStream is = SyncopeWebApplication.class.getResourceAsStream('/' + CUSTOM_FORM_LAYOUT_FILE)) {
            customFormLayout = MAPPER.readValue(is, new TypeReference<>() {
            });
            File enduserDir = new File(props.getProperty("enduser.directory"));
            boolean existsEnduserDir = enduserDir.exists() && enduserDir.canRead() && enduserDir.isDirectory();
            if (existsEnduserDir) {
                File customFormLayoutFile = FileUtils.getFile(enduserDir, CUSTOM_FORM_LAYOUT_FILE);
                if (customFormLayoutFile.exists()
                        && customFormLayoutFile.canRead()
                        && customFormLayoutFile.isFile()) {
                    customFormLayout = MAPPER.readValue(FileUtils.openInputStream(customFormLayoutFile),
                        new TypeReference<>() {
                        });
                }
            }
            FileAlterationObserver observer = existsEnduserDir
                    ? new FileAlterationObserver(
                            enduserDir,
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_LAYOUT_FILE))
                    : new FileAlterationObserver(
                            SyncopeWebApplication.class.getResource('/' + CUSTOM_FORM_LAYOUT_FILE).getFile(),
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_LAYOUT_FILE));

            customFormLayoutMonitor = new FileAlterationMonitor(5000);

            FileAlterationListener listener = new FileAlterationListenerAdaptor() {

                @Override
                public void onFileChange(final File file) {
                    try {
                        LOG.trace("{} has changed. Reloading form attributes customization configuration.",
                                CUSTOM_FORM_LAYOUT_FILE);
                        customFormLayout = MAPPER.readValue(FileUtils.openInputStream(file),
                            new TypeReference<>() {
                            });
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_LAYOUT_FILE, e);
                    }
                }

                @Override
                public void onFileCreate(final File file) {
                    try {
                        LOG.trace("{} has been created. Loading form attributes customization configuration.",
                                CUSTOM_FORM_LAYOUT_FILE);
                        customFormLayout = MAPPER.readValue(FileUtils.openInputStream(file),
                            new TypeReference<>() {
                            });
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_LAYOUT_FILE, e);
                    }
                }

                @Override
                public void onFileDelete(final File file) {
                    LOG.trace("{} has been deleted. Resetting form attributes customization configuration.",
                            CUSTOM_FORM_LAYOUT_FILE);
                    customFormLayout = null;
                }
            };

            observer.addListener(listener);
            customFormLayoutMonitor.addObserver(observer);
            customFormLayoutMonitor.start();
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + CUSTOM_FORM_LAYOUT_FILE, e);
        }

        // process page properties
        pageClasses = new HashMap<>();
        populatePageClasses(props);
        pageClasses = Collections.unmodifiableMap(pageClasses);

        buildSidebarClass(props);

        // Application settings
        IBootstrapSettings settings = new BootstrapSettings();

        // set theme provider
        settings.setThemeProvider(new SingleThemeProvider(new AdminLTE()));

        // install application settings
        Bootstrap.install(this, settings);

        getResourceSettings().setUseMinifiedResources(true);
        getResourceSettings().setUseDefaultOnMissingResource(true);
        getResourceSettings().setThrowExceptionOnMissingResource(false);

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        getRequestCycleListeners().add(new SyncopeUIRequestCycleListener() {

            @Override
            protected boolean isSignedIn() {
                return SyncopeEnduserSession.get().isAuthenticated();
            }

            @Override
            protected void invalidateSession() {
                SyncopeEnduserSession.get().invalidate();
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
            getRequestCycleListeners().add(new ResourceIsolationRequestCycleListener());
        }
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse) {
                    setSecurityHeaders(props, (WebResponse) cycle.getResponse());
                }
            }
        });
        getCspSettings().blocking().unsafeInline();

        // Confirm password reset page
        mountPage("/confirmpasswordreset", SelfConfirmPasswordReset.class);

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
    protected void onDestroy() {
        if (customFormLayoutMonitor != null) {
            try {
                customFormLayoutMonitor.stop(0);
            } catch (Exception e) {
                LOG.error("{} While stopping file monitor", CUSTOM_FORM_LAYOUT_FILE, e);
            }
        }
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return SyncopeEnduserSession.get().isAuthenticated()
                && SyncopeEnduserSession.get().isMustChangePassword()
                ? MustChangePassword.class
                : SyncopeEnduserSession.get().isAuthenticated()
                ? getPageClass("profile", Dashboard.class)
                : getSignInPageClass();
    }

    public ClassPathScanImplementationLookup getLookup() {
        return lookup;
    }

    @SuppressWarnings("unchecked")
    private void buildSidebarClass(final Properties props) {
        try {
            Class<?> clazz = ClassUtils.getClass(props.getProperty("sidebar", Sidebar.class.getCanonicalName()));
            if (Sidebar.class.isAssignableFrom(clazz)) {
                sidebar = (Class<? extends Sidebar>) clazz;
            } else {
                LOG.warn("{} does not extend {}, ignoring...", clazz.getName(), Sidebar.class.getName());
            }
        } catch (ClassNotFoundException e) {
            LOG.error("While looking for class identified by property 'sidebar'", e);
        }
    }

    public UserFormLayoutInfo getCustomFormLayout() {
        return customFormLayout;
    }

    public Class<? extends Sidebar> getSidebar() {
        return sidebar;
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeEnduserSession(request);
    }

    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                setUseCompression(useGZIPCompression);
    }

    public Class<? extends BasePage> getPageClass(final String key) {
        return pageClasses.get(key);
    }

    public Class<? extends BasePage> getPageClass(final String key, final Class<? extends BasePage> defaultValue) {
        return pageClasses.getOrDefault(key, defaultValue);
    }

    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public Integer getMaxUploadFileSizeMB() {
        return maxUploadFileSizeMB;
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

    public Integer getMaxWaitTimeInSeconds() {
        return maxWaitTime;
    }

}
