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
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.core.settings.SingleThemeProvider;
import java.util.Collection;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.protocol.ws.api.WebSocketResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.MustChangePassword;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.themes.AdminLTE;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.wicket.protocol.ws.WebSocketAwareResourceIsolationRequestCycleListener;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class SyncopeWebApplication extends WicketBootSecuredWebApplication {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    public static SyncopeWebApplication get() {
        return (SyncopeWebApplication) WebApplication.get();
    }

    @Autowired
    protected ConsoleProperties props;

    @Autowired
    protected ClassPathScanImplementationLookup lookup;

    @Autowired
    protected ServiceOps serviceOps;

    @Autowired
    protected ExternalResourceProvider resourceProvider;

    @Autowired
    protected AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider;

    @Autowired
    protected AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider;

    @Autowired
    protected AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps;

    @Autowired
    protected StatusProvider statusProvider;

    @Autowired
    protected VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider;

    @Autowired
    protected ImplementationInfoProvider implementationInfoProvider;

    @Autowired
    protected ApplicationContext ctx;

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

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));

        lookup.getIdRepoPageClasses().
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
            getRequestCycleListeners().add(new WebSocketAwareResourceIsolationRequestCycleListener());
        }

        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse && !(cycle.getResponse() instanceof WebSocketResponse)) {
                    props.getSecurityHeaders().
                            forEach((name, value) -> ((WebResponse) cycle.getResponse()).setHeader(name, value));
                }
            }
        });
        getCspSettings().blocking().unsafeInline();

        mountPage("/login", getSignInPageClass());

        for (Class<? extends AbstractResource> resource : lookup.getClasses(AbstractResource.class)) {
            Resource annotation = resource.getAnnotation(Resource.class);
            try {
                AbstractResource instance = resource.getDeclaredConstructor().newInstance();

                LOG.debug("Mounting {} under {}", resource.getName(), annotation.path());
                mountResource(annotation.path(), new ResourceReference(annotation.key()) {

                    protected static final long serialVersionUID = -128426276529456602L;

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

    public ClassPathScanImplementationLookup getLookup() {
        return lookup;
    }

    public Class<? extends BasePage> getPageClass(final String name) {
        return props.getPage().get(name);
    }

    public ThreadPoolTaskExecutor newThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setCorePoolSize(props.getTopology().getCorePoolSize());
        executor.setMaxPoolSize(props.getTopology().getMaxPoolSize());
        executor.setQueueCapacity(props.getTopology().getQueueCapacity());
        executor.initialize();
        return executor;
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

    public String getDefaultAnyPanelClass() {
        return props.getDefaultAnyPanelClass();
    }

    public String getAnonymousUser() {
        return props.getAnonymousUser();
    }

    public String getAnonymousKey() {
        return props.getAnonymousKey();
    }

    public String getReconciliationReportKey() {
        return props.getReconciliationReportKey();
    }

    public long getMaxWaitTimeInSeconds() {
        return props.getMaxWaitTimeOnApplyChanges();
    }

    public int getMaxUploadFileSizeMB() {
        return props.getMaxUploadFileSizeMB();
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

    public Collection<PolicyTabProvider> getPolicyTabProviders() {
        return ctx.getBeansOfType(PolicyTabProvider.class).values();
    }
}
