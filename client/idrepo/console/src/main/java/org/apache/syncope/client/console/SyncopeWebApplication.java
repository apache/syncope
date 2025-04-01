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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.annotations.UserFormFinalize;
import org.apache.syncope.client.console.commons.AccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.pages.MustChangePassword;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wizards.any.UserFormFinalizer;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.SyncopeUIRequestCycleListener;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.themes.AdminLTE;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.XForwardedRequestWrapperFactory;
import org.apache.wicket.protocol.ws.WebSocketAwareResourceIsolationRequestCycleListener;
import org.apache.wicket.protocol.ws.api.WebSocketResponse;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

public class SyncopeWebApplication extends WicketBootSecuredWebApplication {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeWebApplication.class);

    public static SyncopeWebApplication get() {
        return (SyncopeWebApplication) WebApplication.get();
    }

    protected final ConsoleProperties props;

    protected final ClassPathScanImplementationLookup lookup;

    protected final ServiceOps serviceOps;

    protected final ExternalResourceProvider resourceProvider;

    protected final AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider;

    protected final AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider;

    protected final AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps;

    protected final StatusProvider statusProvider;

    protected final VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider;

    protected final ImplementationInfoProvider implementationInfoProvider;

    protected final AccessPolicyConfProvider accessPolicyConfProvider;

    protected final List<PolicyTabProvider> policyTabProviders;

    protected final List<UserFormFinalizer> userFormFinalizers;

    protected final List<IResource> resources;

    public SyncopeWebApplication(
            final ConsoleProperties props,
            final ClassPathScanImplementationLookup lookup,
            final ServiceOps serviceOps,
            final ExternalResourceProvider resourceProvider,
            final AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider,
            final AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider,
            final AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps,
            final StatusProvider statusProvider,
            final VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider,
            final ImplementationInfoProvider implementationInfoProvider,
            final AccessPolicyConfProvider accessPolicyConfProvider,
            final List<PolicyTabProvider> policyTabProviders,
            final List<UserFormFinalizer> userFormFinalizers,
            final List<IResource> resources) {

        this.props = props;
        this.lookup = lookup;
        this.serviceOps = serviceOps;
        this.resourceProvider = resourceProvider;
        this.anyDirectoryPanelAdditionalActionsProvider = anyDirectoryPanelAdditionalActionsProvider;
        this.anyDirectoryPanelAdditionalActionLinksProvider = anyDirectoryPanelAdditionalActionLinksProvider;
        this.anyWizardBuilderAdditionalSteps = anyWizardBuilderAdditionalSteps;
        this.statusProvider = statusProvider;
        this.virSchemaDetailsPanelProvider = virSchemaDetailsPanelProvider;
        this.implementationInfoProvider = implementationInfoProvider;
        this.accessPolicyConfProvider = accessPolicyConfProvider;
        this.policyTabProviders = policyTabProviders;
        this.userFormFinalizers = userFormFinalizers;
        this.resources = resources;
    }

    protected SyncopeUIRequestCycleListener buildSyncopeUIRequestCycleListener() {
        return new SyncopeUIRequestCycleListener() {

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
        };
    }

    protected void initSecurity() {
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

        getCspSettings().blocking().unsafeInline();

        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse && !(cycle.getResponse() instanceof WebSocketResponse)) {
                    props.getSecurityHeaders().
                            forEach((name, value) -> ((WebResponse) cycle.getResponse()).setHeader(name, value));
                }
            }
        });
    }

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

        getRequestCycleListeners().add(buildSyncopeUIRequestCycleListener());

        initSecurity();

        mountPage("/login", getSignInPageClass());

        for (IResource resource : resources) {
            Class<?> resourceClass = AopUtils.getTargetClass(resource);
            Resource annotation = resourceClass.getAnnotation(Resource.class);
            if (annotation == null) {
                LOG.error("No @Resource annotation found, ignoring {}", resourceClass.getName());
            } else {
                LOG.debug("Mounting {} under {}", resourceClass.getName(), annotation.path());

                mountResource(annotation.path(), new ResourceReference(annotation.key()) {

                    private static final long serialVersionUID = -128426276529456602L;

                    @Override
                    public IResource getResource() {
                        return resource;
                    }
                });
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

    public SyncopeAnonymousClient newAnonymousClient(final String domain) {
        return newClientFactory().
                setDomain(domain).
                createAnonymous(props.getAnonymousUser(), props.getAnonymousKey());
    }

    public SyncopeClientFactoryBean newClientFactory() {
        return new SyncopeClientFactoryBean().
                setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                setUseCompression(props.isUseGZIPCompression());
    }

    public String getDefaultAnyPanelClass() {
        return props.getDefaultAnyPanelClass();
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

    public long getMaxWaitTimeInSeconds() {
        return props.getMaxWaitTimeOnApplyChanges();
    }

    public int getMaxUploadFileSizeMB() {
        return props.getMaxUploadFileSizeMB();
    }

    public boolean fullRealmsTree(final RealmRestClient restClient) {
        if (props.getRealmsFullTreeThreshold() <= 0) {
            return false;
        }

        RealmQuery query = RealmsUtils.buildBaseQuery();
        query.setPage(1);
        query.setSize(0);
        return restClient.search(query).getTotalCount() < props.getRealmsFullTreeThreshold();
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
        return policyTabProviders;
    }

    public List<UserFormFinalizer> getFormFinalizers(final AjaxWizard.Mode mode) {
        return userFormFinalizers.stream().filter(uff -> {
            Class<?> clazz = AopUtils.getTargetClass(uff);
            UserFormFinalize annotation = clazz.getAnnotation(UserFormFinalize.class);
            if (annotation == null) {
                LOG.error("No @UserFormFinalize annotation found, ignoring {}", clazz.getName());
                return false;
            }

            return annotation.mode() == mode;
        }).collect(Collectors.toList());
    }

    public AccessPolicyConfProvider getAccessPolicyConfProvider() {
        return accessPolicyConfProvider;
    }
}
