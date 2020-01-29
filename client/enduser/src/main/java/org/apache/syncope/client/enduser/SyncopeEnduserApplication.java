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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.enduser.pages.HomePage;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.init.EnduserInitializer;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.client.enduser.model.CustomTemplateInfo;
import org.apache.syncope.client.enduser.resources.CaptchaResource;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeEnduserApplication extends WebApplication implements Serializable {

    private static final long serialVersionUID = -6445919351044845120L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserApplication.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    private static final String CUSTOM_FORM_ATTRIBUTES_FILE = "customFormAttributes.json";

    private static final String CUSTOM_TEMPLATE_FILE = "customTemplate.json";

    public static SyncopeEnduserApplication get() {
        return (SyncopeEnduserApplication) WebApplication.get();
    }

    private String domain;

    private String adminUser;

    private String anonymousUser;

    private String anonymousKey;

    private boolean captchaEnabled;

    private boolean xsrfEnabled;

    private Integer maxUploadFileSizeMB;

    private SyncopeClientFactoryBean clientFactory;

    private FileAlterationMonitor customFormAttributesMonitor;

    private Map<String, CustomAttributesInfo> customFormAttributes;

    private FileAlterationMonitor customTemplateMonitor;

    private CustomTemplateInfo customTemplate;

    @Override
    protected void init() {
        super.init();

        // read enduser.properties
        Properties props = PropertyUtils.read(getClass(), ENDUSER_PROPERTIES, "enduser.directory").getLeft();

        domain = props.getProperty("domain", SyncopeConstants.MASTER_DOMAIN);
        adminUser = props.getProperty("adminUser");
        Args.notNull(adminUser, "<adminUser>");
        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser>");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey>");

        captchaEnabled = Boolean.parseBoolean(props.getProperty("captcha"));
        Args.notNull(captchaEnabled, "<captcha>");

        xsrfEnabled = Boolean.parseBoolean(props.getProperty("xsrf"));
        Args.notNull(xsrfEnabled, "<xsrf>");

        String scheme = props.getProperty("scheme");
        Args.notNull(scheme, "<scheme>");
        String host = props.getProperty("host");
        Args.notNull(host, "<host>");
        String port = props.getProperty("port");
        Args.notNull(port, "<port>");
        String rootPath = props.getProperty("rootPath");
        Args.notNull(rootPath, "<rootPath>");
        String useGZIPCompression = props.getProperty("useGZIPCompression");
        Args.notNull(useGZIPCompression, "<useGZIPCompression>");
        maxUploadFileSizeMB = props.getProperty("maxUploadFileSizeMB") == null
                ? null
                : Integer.valueOf(props.getProperty("maxUploadFileSizeMB"));

        clientFactory = new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + StringUtils.prependIfMissing(rootPath, "/")).
                setContentType(SyncopeClientFactoryBean.ContentType.JSON).
                setUseCompression(BooleanUtils.toBoolean(useGZIPCompression));

        // read customFormAttributes.json
        File enduserDir;
        try (InputStream is = getClass().getResourceAsStream("/" + CUSTOM_FORM_ATTRIBUTES_FILE)) {
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
                    : new FileAlterationObserver(getClass().getResource("/" + CUSTOM_FORM_ATTRIBUTES_FILE).getFile(),
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
                        LOG.error("{} While reading form attributes customization configuration.",
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
                        LOG.error("{} While reading form attributes customization configuration.",
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

        // read customTemplate.json
        try (InputStream is = getClass().getResourceAsStream("/" + CUSTOM_TEMPLATE_FILE)) {
            customTemplate = MAPPER.readValue(is, CustomTemplateInfo.class);
            enduserDir = new File(props.getProperty("enduser.directory"));
            boolean existsEnduserDir = enduserDir.exists() && enduserDir.canRead() && enduserDir.isDirectory();
            if (existsEnduserDir) {
                File customTemplateFile = FileUtils.getFile(enduserDir, CUSTOM_TEMPLATE_FILE);
                if (customTemplateFile.exists()
                        && customTemplateFile.canRead()
                        && customTemplateFile.isFile()) {
                    customTemplate = MAPPER.readValue(FileUtils.openInputStream(customTemplateFile),
                            CustomTemplateInfo.class);
                }
            }
            FileAlterationObserver observer = existsEnduserDir
                    ? new FileAlterationObserver(enduserDir,
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_TEMPLATE_FILE))
                    : new FileAlterationObserver(getClass().getResource("/" + CUSTOM_TEMPLATE_FILE).getFile(),
                            pathname -> StringUtils.contains(pathname.getPath(), CUSTOM_TEMPLATE_FILE));

            customTemplateMonitor = new FileAlterationMonitor(5000);

            FileAlterationListener listener = new FileAlterationListenerAdaptor() {

                @Override
                public void onFileChange(final File file) {
                    try {
                        LOG.trace("{} has changed. Reloading app customization configuration.",
                                CUSTOM_TEMPLATE_FILE);
                        customTemplate = MAPPER.readValue(FileUtils.openInputStream(file),
                                CustomTemplateInfo.class);
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE, e);
                    }
                }

                @Override
                public void onFileCreate(final File file) {
                    try {
                        LOG.trace("{} has been created. Loading app customization configuration.",
                                CUSTOM_TEMPLATE_FILE);
                        customTemplate = MAPPER.readValue(FileUtils.openInputStream(file),
                                CustomTemplateInfo.class);
                    } catch (IOException e) {
                        LOG.error("{} While reading app customization configuration.",
                                CUSTOM_FORM_ATTRIBUTES_FILE, e);
                    }
                }

                @Override
                public void onFileDelete(final File file) {
                    LOG.trace("{} has been deleted. Resetting app customization configuration.",
                            CUSTOM_TEMPLATE_FILE);
                    customTemplate = null;
                }
            };

            observer.addListener(listener);
            customTemplateMonitor.addObserver(observer);
            customTemplateMonitor.start();
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + CUSTOM_TEMPLATE_FILE, e);
        }

        // mount resources
        ClassPathScanImplementationLookup lookup = (ClassPathScanImplementationLookup) getServletContext().
                getAttribute(EnduserInitializer.CLASSPATH_LOOKUP);
        for (Class<? extends AbstractResource> resource : lookup.getResources()) {
            Resource annotation = resource.getAnnotation(Resource.class);
            if (annotation == null) {
                LOG.debug("No @Resource annotation found on {}, ignoring", resource.getName());
            } else {
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
        }
        //mount captcha resource only if captcha is enabled
        if (captchaEnabled) {
            mountResource("/api/captcha", new ResourceReference("captcha") {

                private static final long serialVersionUID = -128426276529456602L;

                @Override
                public IResource getResource() {
                    return new CaptchaResource();
                }
            });
        }

        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public void onEndRequest(final RequestCycle cycle) {
                if (cycle.getResponse() instanceof WebResponse) {
                    WebResponse response = (WebResponse) cycle.getResponse();
                    response.setHeader("X-XSS-Protection", "1; mode=block");
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    response.setHeader("X-Frame-Options", "sameorigin");
                }
            }
        });
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
        if (customTemplateMonitor != null) {
            try {
                customTemplateMonitor.stop(0);
            } catch (Exception e) {
                LOG.error("{} While stopping file monitor", CUSTOM_TEMPLATE_FILE, e);
            }
        }
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeEnduserSession(request);
    }

    public String getDomain() {
        return domain;
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

    public SyncopeClientFactoryBean getClientFactory() {
        return clientFactory;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public boolean isXsrfEnabled() {
        return xsrfEnabled;
    }

    public Integer getMaxUploadFileSizeMB() {
        return maxUploadFileSizeMB;
    }

    public Map<String, CustomAttributesInfo> getCustomFormAttributes() {
        return customFormAttributes;
    }

    public void setCustomFormAttributes(final Map<String, CustomAttributesInfo> customFormAttributes) {
        this.customFormAttributes.clear();
        this.customFormAttributes.putAll(customFormAttributes);
    }

    public void setCustomTemplate(final CustomTemplateInfo customTemplate) {
        this.customTemplate = customTemplate;
    }

    public CustomTemplateInfo getCustomTemplate() {
        return customTemplate;
    }
}
