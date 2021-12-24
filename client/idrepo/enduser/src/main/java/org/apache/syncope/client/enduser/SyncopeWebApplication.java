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
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.enduser.pages.SelfConfirmPasswordReset;
import org.apache.syncope.client.enduser.panels.Sidebar;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.themes.AdminLTE;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class SyncopeWebApplication extends WicketBootStandardWebApplication {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR"), new Locale("ru"), Locale.JAPANESE);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public static SyncopeWebApplication get() {
        return (SyncopeWebApplication) WebApplication.get();
    }

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected EnduserProperties props;

    @Autowired
    protected ClassPathScanImplementationLookup lookup;

    @Autowired
    protected ServiceOps serviceOps;

    protected UserFormLayoutInfo customFormLayout;

    @Override
    protected void init() {
        super.init();

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

        if (props.isxForward()) {
            XForwardedRequestWrapperFactory.Config config = new XForwardedRequestWrapperFactory.Config();
            config.setProtocolHeader(props.getxForwardProtocolHeader());
            config.setHttpServerPort(props.getxForwardHttpPort());
            config.setHttpsServerPort(props.getxForwardHttpsPort());

            XForwardedRequestWrapperFactory factory = new XForwardedRequestWrapperFactory();
            factory.setConfig(config);
            getFilterFactoryManager().add(factory);
        }

        if (props.isCsrf()) {
            getRequestCycleListeners().add(new ResourceIsolationRequestCycleListener());
        }

        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse) {
                    props.getSecurityHeaders().
                            forEach((name, value) -> ((WebResponse) cycle.getResponse()).setHeader(name, value));
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

                LOG.debug("Mounting {} under {}", resource.getName(), annotation.path());
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

        try (InputStream is = resourceLoader.getResource(props.getCustomFormLayout()).getInputStream()) {
            customFormLayout = MAPPER.readValue(is, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + props.getCustomFormLayout(), e);
        }

        // enable component path
        if (getDebugSettings().isAjaxDebugModeEnabled()) {
            getDebugSettings().setComponentPathAttributeName("syncope-path");
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

    public UserFormLayoutInfo getCustomFormLayout() {
        return customFormLayout;
    }

    public Class<? extends Sidebar> getSidebar() {
        return props.getSidebar();
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeEnduserSession(request);
    }

    public SyncopeClient newAnonymousClient() {
        return newClientFactory().create(
                new AnonymousAuthenticationHandler(props.getAnonymousUser(), props.getAnonymousKey()));
    }

    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                setUseCompression(props.isUseGZIPCompression());
    }

    public Class<? extends BasePage> getPageClass(final String name) {
        return props.getPage().get(name);
    }

    public Class<? extends BasePage> getPageClass(final String name, final Class<? extends BasePage> defaultValue) {
        return props.getPage().getOrDefault(name, defaultValue);
    }

    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    public String getAdminUser() {
        return props.getAdminUser();
    }

    public String getAnonymousUser() {
        return props.getAnonymousUser();
    }

    public String getAnonymousKey() {
        return props.getAnonymousKey();
    }

    public boolean isCaptchaEnabled() {
        return props.isCaptcha();
    }

    public long getMaxWaitTimeInSeconds() {
        return props.getMaxWaitTimeOnApplyChanges();
    }

    public Integer getMaxUploadFileSizeMB() {
        return props.getMaxUploadFileSizeMB();
    }
}
