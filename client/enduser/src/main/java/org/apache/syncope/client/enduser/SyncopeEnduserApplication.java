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

import org.apache.syncope.client.enduser.resources.UserAuthentication;
import java.io.File;
import java.io.Serializable;
import org.apache.syncope.client.enduser.pages.HomePage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.syncope.client.enduser.resources.CaptchaResource;
import org.apache.syncope.client.enduser.resources.InfoResource;
import org.apache.syncope.client.enduser.resources.LoginResource;
import org.apache.syncope.client.enduser.resources.LogoutResource;
import org.apache.syncope.client.enduser.resources.SchemaResource;
import org.apache.syncope.client.enduser.resources.SecurityQuestionResource;
import org.apache.syncope.client.enduser.resources.SyncopeAnyClassTypeResource;
import org.apache.syncope.client.enduser.resources.SyncopeAnyTypeResource;
import org.apache.syncope.client.enduser.resources.SyncopeGroupResource;
import org.apache.syncope.client.enduser.resources.SyncopeResourceResource;
import org.apache.syncope.client.enduser.resources.UserSelfChangePassword;
import org.apache.syncope.client.enduser.resources.UserSelfConfirmPasswordReset;
import org.apache.syncope.client.enduser.resources.UserSelfCreateResource;
import org.apache.syncope.client.enduser.resources.UserSelfPasswordReset;
import org.apache.syncope.client.enduser.resources.UserSelfReadResource;
import org.apache.syncope.client.enduser.resources.UserSelfUpdateResource;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

public class SyncopeEnduserApplication extends WebApplication implements Serializable {

    private static final long serialVersionUID = -6445919351044845120L;

    private static final String ENDUSER_PROPERTIES = "enduser.properties";

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            new Locale[] {
                Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR")
            }));

    public static SyncopeEnduserApplication get() {
        return (SyncopeEnduserApplication) WebApplication.get();
    }

    private String version;

    private String site;

    private String license;

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
        try {
            props.load(getClass().getResourceAsStream("/" + ENDUSER_PROPERTIES));
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
        Args.notNull(version, "<version> not set");
        site = props.getProperty("site");
        Args.notNull(site, "<site> not set");
        license = props.getProperty("license");
        Args.notNull(license, "<license> not set");
        anonymousUser = props.getProperty("anonymousUser");
        Args.notNull(anonymousUser, "<anonymousUser> not set");
        anonymousKey = props.getProperty("anonymousKey");
        Args.notNull(anonymousKey, "<anonymousKey> not set");

        captchaEnabled = Boolean.parseBoolean(props.getProperty("captcha"));
        Args.notNull(captchaEnabled, "<captcha> not set");

        xsrfEnabled = Boolean.parseBoolean(props.getProperty("xsrf"));
        Args.notNull(xsrfEnabled, "<xsrf> not set");

        String scheme = props.getProperty("scheme");
        Args.notNull(scheme, "<scheme> not set");
        String host = props.getProperty("host");
        Args.notNull(host, "<host> not set");
        String port = props.getProperty("port");
        Args.notNull(port, "<port> not set");
        String rootPath = props.getProperty("rootPath");
        Args.notNull(rootPath, "<rootPath> not set");

        clientFactory = new SyncopeClientFactoryBean().setAddress(scheme + "://" + host + ":" + port + "/" + rootPath);
        clientFactory.setContentType(SyncopeClientFactoryBean.ContentType.JSON);

        // resource to provide login functionality managed by wicket
        mountResource("/api/login", new ResourceReference("login") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new LoginResource();
            }
        });

        // resource to provide logout functionality managed by wicket
        mountResource("/api/logout", new ResourceReference("logout") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new LogoutResource();
            }
        });
        
        mountResource("/api/self/islogged", new ResourceReference("userAuthentication") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserAuthentication();
            }
        });

        // resource to retrieve info about logged user
        mountResource("/api/self/read", new ResourceReference("userSelfRead") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfReadResource();
            }
        });

        // resource to provide user self create functionality managed by wicket
        mountResource("/api/self/create", new ResourceReference("userSelfCreate") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfCreateResource();
            }
        });

        // resource to provide user self update functionality managed by wicket
        mountResource("/api/self/update", new ResourceReference("userSelfUpdate") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfUpdateResource();
            }
        });

        mountResource("/api/self/requestPasswordReset", new ResourceReference("userSelfPasswordReset") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfPasswordReset();
            }
        });


        mountResource("/api/self/confirmPasswordReset", new ResourceReference("userSelfConfirmPasswordReset") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfConfirmPasswordReset();
            }
        });

        mountResource("/api/self/changePassword", new ResourceReference("userSelfChangePassword") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new UserSelfChangePassword();
            }
        });

        mountResource("/api/schemas", new ResourceReference("schemas") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SchemaResource();
            }
        });

        mountResource("/api/resources", new ResourceReference("resources") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SyncopeResourceResource();
            }
        });

        mountResource("/api/securityQuestions", new ResourceReference("securityQuestions") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SecurityQuestionResource();
            }
        });

        mountResource("/api/securityQuestions/byUser/${username}", new ResourceReference("securityQuestions") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SecurityQuestionResource();
            }
        });

        mountResource("/api/info", new ResourceReference("info") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new InfoResource();
            }
        });

        // resource to get a fresh captcha image
        mountResource("/api/captcha", new ResourceReference("captcha") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new CaptchaResource();
            }
        });

        mountResource("/api/groups", new ResourceReference("groups") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SyncopeGroupResource();
            }
        });

        mountResource("/api/auxiliaryClasses", new ResourceReference("auxClasses") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SyncopeAnyClassTypeResource();
            }
        });

        mountResource("/api/anyTypes", new ResourceReference("anyType") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new SyncopeAnyTypeResource();
            }
        });

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
