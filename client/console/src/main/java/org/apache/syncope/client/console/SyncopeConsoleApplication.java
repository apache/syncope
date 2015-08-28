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
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.resources.FilesystemResource;
import org.apache.syncope.client.console.resources.WorkflowDefGETResource;
import org.apache.syncope.client.console.resources.WorkflowDefPUTResource;
import org.apache.syncope.client.console.themes.AdminLTE;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SyncopeConsoleApplication extends AuthenticatedWebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleApplication.class);

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            new Locale[] {
                Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR")
            }));

    private static final String ACTIVITI_MODELER_CONTEXT = "activiti-modeler";

    @Override
    protected void init() {
        super.init();
        IBootstrapSettings settings = new BootstrapSettings();
        settings.setThemeProvider(new SingleThemeProvider(new AdminLTE()));
        Bootstrap.install(this, settings);

        // best place to do this is in Application#init()
        //Bootstrap.install(this);
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
        getResourceSettings().setThrowExceptionOnMissingResource(true);
        getJavaScriptLibrarySettings().setJQueryReference(new DynamicJQueryResourceReference());

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.EMPTY)) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends BasePage> clazz = (Class<? extends BasePage>) ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                MetaDataRoleAuthorizationStrategy.authorize(clazz, SyncopeConsoleSession.AUTHENTICATED);
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        getRequestCycleListeners().add(new SyncopeConsoleRequestCycleListener());

        mountPage("/login", getSignInPageClass());

        final String activitiModelerDirectory = WebApplicationContextUtils.getWebApplicationContext(
                WebApplication.get().getServletContext()).getBean("activitiModelerDirectory", String.class);
        mountResource("/" + ACTIVITI_MODELER_CONTEXT, new ResourceReference(ACTIVITI_MODELER_CONTEXT) {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new FilesystemResource(ACTIVITI_MODELER_CONTEXT, activitiModelerDirectory);
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
        return Dashboard.class;
    }

}
