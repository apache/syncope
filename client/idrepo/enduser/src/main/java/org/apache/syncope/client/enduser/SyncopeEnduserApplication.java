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
package org.apache.syncope.client.enduser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.init.EnduserInitializer;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.*;
import org.apache.syncope.client.enduser.panels.Sidebar;
import org.apache.syncope.client.enduser.themes.AdminLTE;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.protocol.ws.WebSocketAwareCsrfPreventionRequestCycleListener;
import org.apache.wicket.protocol.ws.api.WebSocketResponse;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class SyncopeEnduserApplication extends AuthenticatedWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserApplication.class);

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    private static final String CUSTOM_FORM_LAYOUT_FILE = "customFormLayout.json";

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            Locale.ENGLISH, Locale.CANADA_FRENCH, Locale.ITALIAN, Locale.JAPANESE, new Locale("pt", "BR"),
            new Locale("ru")
    ));

    public static SyncopeEnduserApplication get() {
        return (SyncopeEnduserApplication) WebApplication.get();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String anonymousUser;

    private String anonymousKey;

    private String scheme;

    private String host;

    private String port;

    private String rootPath;

    private String useGZIPCompression;

    private Integer maxUploadFileSizeMB;

    private Integer maxWaitTime;

    private boolean captchaEnabled;

    private List<String> domains;

    private String adminUser;

    private Class<? extends Sidebar> sidebar;

    private Map<String, Class<? extends BasePage>> pageClasses;

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

        Properties props = PropertyUtils.read(getClass(), ENDUSER_PROPERTIES, "enduser.directory");

        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser>");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey>");
        adminUser = props.getProperty("adminUser", "admin");
        Args.notNull(adminUser, "<adminUser>");

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

        captchaEnabled = Boolean.parseBoolean(props.getProperty("captcha"));
        Args.notNull(captchaEnabled, "<captcha>");

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

        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV2());

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));

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
        getRequestCycleListeners().add(new SyncopeEnduserRequestCycleListener());
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse && !(cycle.getResponse() instanceof WebSocketResponse)) {
                    setSecurityHeaders(props, (WebResponse) cycle.getResponse());
                }
            }
        });

        mountPage("/login", getSignInPageClass());
        mountPage("/confirmpasswordreset", SelfConfirmPasswordReset.class);

        ClassPathScanImplementationLookup lookup = (ClassPathScanImplementationLookup) getServletContext().
                getAttribute(EnduserInitializer.CLASSPATH_LOOKUP);
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

        // read customFormAttributes.json
        File enduserDir;
        try (InputStream is = getClass().getResourceAsStream('/' + CUSTOM_FORM_LAYOUT_FILE)) {
            customFormLayout = MAPPER.readValue(is, new TypeReference<UserFormLayoutInfo>() {
            });
            enduserDir = new File(props.getProperty("enduser.directory"));
            boolean existsEnduserDir = enduserDir.exists() && enduserDir.canRead() && enduserDir.isDirectory();
            if (existsEnduserDir) {
                File customFormLayoutFile = FileUtils.getFile(enduserDir, CUSTOM_FORM_LAYOUT_FILE);
                if (customFormLayoutFile.exists()
                        && customFormLayoutFile.canRead()
                        && customFormLayoutFile.isFile()) {
                    customFormLayout = MAPPER.readValue(FileUtils.openInputStream(customFormLayoutFile),
                            new TypeReference<UserFormLayoutInfo>() {
                    });
                }
            }
            FileAlterationObserver observer = existsEnduserDir
                    ? new FileAlterationObserver(enduserDir,
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_LAYOUT_FILE))
                    : new FileAlterationObserver(getClass().getResource('/' + CUSTOM_FORM_LAYOUT_FILE).getFile(),
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_LAYOUT_FILE));

            FileAlterationMonitor customFormLayoutMonitor = new FileAlterationMonitor(5000);

            FileAlterationListener listener = new FileAlterationListenerAdaptor() {

                @Override
                public void onFileChange(final File file) {
                    try {
                        LOG.trace("{} has changed. Reloading form attributes customization configuration.",
                                CUSTOM_FORM_LAYOUT_FILE);
                        customFormLayout = MAPPER.readValue(FileUtils.openInputStream(file),
                                new TypeReference<UserFormLayoutInfo>() {
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
                                new TypeReference<UserFormLayoutInfo>() {
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

        // enable component path
        if (getDebugSettings().isAjaxDebugModeEnabled()) {
            getDebugSettings().setComponentPathAttributeName("syncope-path");
        }
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return SyncopeEnduserSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return SyncopeEnduserSession.get().isAuthenticated()
                && SyncopeEnduserSession.get().getSelfTO().isMustChangePassword()
                ? MustChangePassword.class
                : SyncopeEnduserSession.get().isAuthenticated()
                ? getPageClass("profile", Dashboard.class)
                : Login.class;
    }

    public Class<? extends BasePage> getPageClass(final String key) {
        return pageClasses.get(key);
    }

    public Class<? extends BasePage> getPageClass(final String key, final Class<? extends BasePage> defaultValue) {
        return pageClasses.getOrDefault(key, defaultValue);
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public Integer getMaxUploadFileSizeMB() {
        return maxUploadFileSizeMB;
    }

    public Integer getMaxWaitTimeInSeconds() {
        return maxWaitTime;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
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
                        getService(DomainOps.class).list().stream().map(Domain::getKey).
                        collect(Collectors.toList());
                domains.add(0, SyncopeConstants.MASTER_DOMAIN);
                domains = ListUtils.unmodifiableList(domains);
            }
        }
        return domains;
    }

    public static void extractAttrsFromExt(final String extAttrs, final UserTO userTO) {
        try {
            Set<Attr> attrs = MAPPER.readValue(extAttrs, new TypeReference<Set<Attr>>() {
            });
            Optional<Attr> username = attrs.stream().
                    filter(attr -> attr.getSchema().equals("username")).
                    findFirst();
            if (username.isPresent()) {
                userTO.setUsername(username.get().getValues().get(0));
                attrs.remove(username.get());
            }
            userTO.getPlainAttrs().addAll(attrs);
        } catch (IOException e) {
            LOG.error("While extracting ext attributes", e);
        }
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

    public Class<? extends Sidebar> getSidebar() {
        return sidebar;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public UserFormLayoutInfo getCustomFormLayout() {
        return customFormLayout;
    }

    public void setCustomFormLayout(final UserFormLayoutInfo customFormAttributes) {
        this.customFormLayout = customFormAttributes;
    }
}
