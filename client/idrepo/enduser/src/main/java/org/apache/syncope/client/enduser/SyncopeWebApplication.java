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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.assets.SyncopeEnduserCss;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.enduser.pages.Self;
import org.apache.syncope.client.enduser.pages.SelfConfirmPasswordReset;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.CsrfPreventionRequestCycleListener;
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
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncopeWebApplication extends WicketBootStandardWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    private static final String CUSTOM_FORM_ATTRIBUTES_FILE = "customFormAttributes.json";

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

    private boolean useGZIPCompression;

    private String adminUser;

    private String anonymousUser;

    private String anonymousKey;

    private boolean captchaEnabled;

    private Integer maxWaitTime;

    private Integer corePoolSize;

    private Integer maxPoolSize;

    private Integer queueCapacity;

    private Integer maxUploadFileSizeMB;

    private FileAlterationMonitor customFormAttributesMonitor;

    private Map<String, CustomAttributesInfo> customFormAttributes;

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

        // read enduser.properties
        Properties props = PropertyUtils.read(getClass(), ENDUSER_PROPERTIES, "enduser.directory");

        adminUser = props.getProperty("adminUser");
        Args.notNull(adminUser, "<adminUser>");
        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser>");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey>");

        captchaEnabled = Boolean.parseBoolean(props.getProperty("captcha"));
        Args.notNull(captchaEnabled, "<captcha>");

        useGZIPCompression = BooleanUtils.toBoolean(props.getProperty("useGZIPCompression"));
        Args.notNull(useGZIPCompression, "<useGZIPCompression>");
        maxUploadFileSizeMB = props.getProperty("maxUploadFileSizeMB") == null
                ? null
                : Integer.valueOf(props.getProperty("maxUploadFileSizeMB"));

        maxWaitTime = Integer.valueOf(props.getProperty("maxWaitTimeOnApplyChanges", "30"));

        // Resource connections check thread pool size
        corePoolSize = Integer.valueOf(props.getProperty("executor.corePoolSize", "5"));
        maxPoolSize = Integer.valueOf(props.getProperty("executor.maxPoolSize", "10"));
        queueCapacity = Integer.valueOf(props.getProperty("executor.queueCapacity", "50"));

        // read customFormAttributes.json
        File enduserDir;
        try (InputStream is = getClass().getResourceAsStream('/' + CUSTOM_FORM_ATTRIBUTES_FILE)) {
            customFormAttributes = MAPPER.readValue(is,
                    new TypeReference<HashMap<String, CustomAttributesInfo>>() {
            });
            enduserDir = new File(props.getProperty("enduser.directory"));
            boolean existsEnduserDir = enduserDir.exists() && enduserDir.canRead() && enduserDir.isDirectory();
            if (existsEnduserDir) {
                File customFormAttributesFile = FileUtils.getFile(enduserDir, CUSTOM_FORM_ATTRIBUTES_FILE);
                if (customFormAttributesFile.exists()
                        && customFormAttributesFile.canRead()
                        && customFormAttributesFile.isFile()) {
                    customFormAttributes = MAPPER.readValue(FileUtils.openInputStream(customFormAttributesFile),
                            new TypeReference<HashMap<String, CustomAttributesInfo>>() {
                    });
                }
            }
            FileAlterationObserver observer = existsEnduserDir
                    ? new FileAlterationObserver(enduserDir,
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_ATTRIBUTES_FILE))
                    : new FileAlterationObserver(getClass().getResource('/' + CUSTOM_FORM_ATTRIBUTES_FILE).getFile(),
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_FORM_ATTRIBUTES_FILE));

            customFormAttributesMonitor = new FileAlterationMonitor(5000);

            FileAlterationListener listener = new FileAlterationListenerAdaptor() {

                @Override
                public void onFileChange(final File file) {
                    try {
                        LOG.trace("{} has changed. Reloading form attributes customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE);
                        customFormAttributes = MAPPER.readValue(FileUtils.openInputStream(file),
                                new TypeReference<HashMap<String, CustomAttributesInfo>>() {
                        });
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE, e);
                    }
                }

                @Override
                public void onFileCreate(final File file) {
                    try {
                        LOG.trace("{} has been created. Loading form attributes customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE);
                        customFormAttributes = MAPPER.readValue(FileUtils.openInputStream(file),
                                new TypeReference<HashMap<String, CustomAttributesInfo>>() {
                        });
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE, e);
                    }
                }

                @Override
                public void onFileDelete(final File file) {
                    LOG.trace("{} has been deleted. Resetting form attributes customization configuration.",
                            CUSTOM_FORM_ATTRIBUTES_FILE);
                    customFormAttributes = null;
                }
            };

            observer.addListener(listener);
            customFormAttributesMonitor.addObserver(observer);
            customFormAttributesMonitor.start();
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + CUSTOM_FORM_ATTRIBUTES_FILE, e);
        }

        // Application settings
        IBootstrapSettings settings = new BootstrapSettings();

        // install application settings
        Bootstrap.install(this, settings);

        getResourceSettings().setUseMinifiedResources(true);

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV2());

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);
        getMarkupSettings().setStripComments(true);

        // add some css assets as Java Wicket resource in order to set Bootstrap css as a dependency of those
        // and stop it to override the custom css rules
        getHeaderContributorListeners().add(new IHeaderContributor() {

            private static final long serialVersionUID = -8955205747168484695L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(SyncopeEnduserCss.INSTANCE));
            }
        });

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
            config.setHttpServerPort(Integer.valueOf(props.getProperty("x-forward.http.port", "80")));
            config.setHttpsServerPort(Integer.valueOf(props.getProperty("x-forward.https.port", "443")));

            XForwardedRequestWrapperFactory factory = new XForwardedRequestWrapperFactory();
            factory.setConfig(config);
            getFilterFactoryManager().add(factory);
        }

        if (BooleanUtils.toBoolean(props.getProperty("csrf"))) {
            getRequestCycleListeners().add(new CsrfPreventionRequestCycleListener());
        }
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse) {
                    setSecurityHeaders(props, (WebResponse) cycle.getResponse());
                }
            }
        });

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
        if (customFormAttributesMonitor != null) {
            try {
                customFormAttributesMonitor.stop(0);
            } catch (Exception e) {
                LOG.error("{} While stopping file monitor", CUSTOM_FORM_ATTRIBUTES_FILE, e);
            }
        }
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return SyncopeEnduserSession.get().isAuthenticated()
                && SyncopeEnduserSession.get().getSelfTO().isMustChangePassword()
                ? MustChangePassword.class
                : SyncopeEnduserSession.get().isAuthenticated()
                ? Self.class
                : Login.class;
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

    protected static Class<? extends WebPage> getSignInPageClass() {
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

    public Map<String, CustomAttributesInfo> getCustomFormAttributes() {
        return customFormAttributes;
    }

    public void setCustomFormAttributes(final Map<String, CustomAttributesInfo> customFormAttributes) {
        this.customFormAttributes.clear();
        this.customFormAttributes.putAll(customFormAttributes);
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
}
