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

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import org.apache.syncope.client.enduser.pages.HomePage;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.init.EnduserInitializer;
import org.apache.syncope.client.enduser.resources.CaptchaResource;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeEnduserApplication extends WebApplication implements Serializable {

    private static final long serialVersionUID = -6445919351044845120L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserApplication.class);

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    public static SyncopeEnduserApplication get() {
        return (SyncopeEnduserApplication) WebApplication.get();
    }

    private String version;

    private String site;

    private String license;

    private String domain;

    private String adminUser;

    private String anonymousUser;

    private String anonymousKey;

    private boolean captchaEnabled;

    private boolean xsrfEnabled;

    private SyncopeClientFactoryBean clientFactory;

    @Override
    protected void init() {
        super.init();

        // read enduser.properties
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/" + ENDUSER_PROPERTIES)) {
            props.load(is);
            File enduserDir = new File(props.getProperty("enduser.directory"));
            if (enduserDir.exists() && enduserDir.canRead() && enduserDir.isDirectory()) {
                File enduserDirProps = FileUtils.getFile(enduserDir, ENDUSER_PROPERTIES);
                if (enduserDirProps.exists() && enduserDirProps.canRead() && enduserDirProps.isFile()) {
                    props.clear();
                    props.load(FileUtils.openInputStream(enduserDirProps));
                }
            }
        } catch (Exception e) {
            throw new WicketRuntimeException("Could not read " + ENDUSER_PROPERTIES, e);
        }
        version = props.getProperty("version");
        Args.notNull(version, "<version>");
        site = props.getProperty("site");
        Args.notNull(site, "<site>");
        license = props.getProperty("license");
        Args.notNull(license, "<license>");
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

        clientFactory = new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + "/" + rootPath).
                setContentType(SyncopeClientFactoryBean.ContentType.JSON).
                setUseCompression(BooleanUtils.toBoolean(useGZIPCompression));

        // mount resources
        ClassPathScanImplementationLookup classPathScanImplementationLookup =
                (ClassPathScanImplementationLookup) getServletContext().
                        getAttribute(EnduserInitializer.CLASSPATH_LOOKUP);
        for (final Class<? extends AbstractResource> resource : classPathScanImplementationLookup.getResources()) {
            Resource annotation = resource.getAnnotation(Resource.class);
            if (annotation == null) {
                LOG.debug("No @Resource annotation found on {}, ignoring", resource.getName());
            } else {
                try {
                    final AbstractResource instance = resource.newInstance();

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
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeEnduserSession(request);
    }

    public String getVersion() {
        return version;
    }

    public String getSite() {
        return site;
    }

    public String getLicense() {
        return license;
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

}
