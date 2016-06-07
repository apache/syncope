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
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
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
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
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

    private static final String ACTIVITI_MODELER_CONTEXT = "activiti-modeler";

    public static SyncopeConsoleApplication get() {
        return (SyncopeConsoleApplication) WebApplication.get();
    }

    private String version;

    private String site;

    private String anonymousUser;

    private String anonymousKey;

    private String activitiModelerDirectory;

    private String reconciliationReportKey;

    private SyncopeClientFactoryBean clientFactory;

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
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/" + CONSOLE_PROPERTIES));
            File consoleDir = new File(props.getProperty("console.directory"));
            if (consoleDir.exists() && consoleDir.canRead() && consoleDir.isDirectory()) {
                File consoleDirProps = FileUtils.getFile(consoleDir, CONSOLE_PROPERTIES);
                if (consoleDirProps.exists() && consoleDirProps.canRead() && consoleDirProps.isFile()) {
                    props.clear();
                    props.load(FileUtils.openInputStream(consoleDirProps));
                }
            }
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + CONSOLE_PROPERTIES, e);
        }
        version = props.getProperty("version");
        Args.notNull(version, "<version> not set");
        site = props.getProperty("site");
        Args.notNull(site, "<site> not set");
        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser> not set");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey> not set");

        String scheme = props.getProperty("scheme");
        Args.notNull(scheme, "<scheme> not set");
        String host = props.getProperty("host");
        Args.notNull(host, "<host> not set");
        String port = props.getProperty("port");
        Args.notNull(port, "<port> not set");
        String rootPath = props.getProperty("rootPath");
        Args.notNull(rootPath, "<rootPath> not set");
        String useGZIPCompression = props.getProperty("useGZIPCompression");
        Args.notNull(rootPath, "<useGZIPCompression> not set");

        clientFactory = new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + "/" + rootPath).
                setUseCompression(BooleanUtils.toBoolean(useGZIPCompression));

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
        for (Class<? extends BasePage> clazz : lookup.getPageClasses()) {
            MetaDataRoleAuthorizationStrategy.authorize(clazz, SyncopeConsoleSession.AUTHENTICATED);
        }

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        getRequestCycleListeners().add(new SyncopeConsoleRequestCycleListener());

        mountPage("/login", getSignInPageClass());

        activitiModelerDirectory = props.getProperty("activitiModelerDirectory");
        Args.notNull(activitiModelerDirectory, "<activitiModelerDirectory> not set");

        try {
            reconciliationReportKey = props.getProperty("reconciliationReportKey");
        } catch (NumberFormatException e) {
            LOG.error("While parsing reconciliationReportKey", e);
        }
        Args.notNull(reconciliationReportKey, "<reconciliationReportKey> not set");

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

    public String getVersion() {
        return version;
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

    public String getActivitiModelerDirectory() {
        return activitiModelerDirectory;
    }

    public String getReconciliationReportKey() {
        return reconciliationReportKey;
    }

    public SyncopeClientFactoryBean getClientFactory() {
        return clientFactory;
    }

    public MediaType getMediaType() {
        return clientFactory.getContentType().getMediaType();
    }

}
