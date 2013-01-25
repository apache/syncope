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
package org.apache.syncope.console;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.syncope.client.services.proxy.ConfigurationServiceProxy;
import org.apache.syncope.client.services.proxy.ConnectorServiceProxy;
import org.apache.syncope.client.services.proxy.EntitlementServiceProxy;
import org.apache.syncope.client.services.proxy.LoggerServiceProxy;
import org.apache.syncope.client.services.proxy.NotificationServiceProxy;
import org.apache.syncope.client.services.proxy.PolicyServiceProxy;
import org.apache.syncope.client.services.proxy.ReportServiceProxy;
import org.apache.syncope.client.services.proxy.ResourceServiceProxy;
import org.apache.syncope.client.services.proxy.RoleServiceProxy;
import org.apache.syncope.client.services.proxy.SchemaServiceProxy;
import org.apache.syncope.client.services.proxy.SpringServiceProxy;
import org.apache.syncope.client.services.proxy.TaskServiceProxy;
import org.apache.syncope.client.services.proxy.UserRequestServiceProxy;
import org.apache.syncope.client.services.proxy.UserServiceProxy;
import org.apache.syncope.client.services.proxy.WorkflowServiceProxy;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.ConnectorService;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Custom Syncope Session class.
 */
public class SyncopeSession extends WebSession {

    private static final long serialVersionUID = 7743446298924805872L;

    private String userId;

    private String coreVersion;

    private Roles roles = new Roles();

    protected String baseURL;

    private final RestTemplate restTemplate;

    private final HashMap<Class<?>, SpringServiceProxy> services = new HashMap<Class<?>, SpringServiceProxy>();

    public static SyncopeSession get() {
        return (SyncopeSession) Session.get();
    }

    public SyncopeSession(final Request request) {
        super(request);

        final ApplicationContext applicationContext =
                WebApplicationContextUtils.getWebApplicationContext(WebApplication.get().getServletContext());

        restTemplate = applicationContext.getBean(RestTemplate.class);
        baseURL = applicationContext.getBean("baseURL", String.class);

        setupRESTClients();
    }

    protected void setupRESTClients() {
        services.put(ConfigurationService.class, new ConfigurationServiceProxy(baseURL, restTemplate));
        services.put(ConnectorService.class, new ConnectorServiceProxy(baseURL, restTemplate));
        services.put(EntitlementService.class, new EntitlementServiceProxy(baseURL, restTemplate));
        services.put(LoggerService.class, new LoggerServiceProxy(baseURL, restTemplate));
        services.put(NotificationService.class, new NotificationServiceProxy(baseURL, restTemplate));
        services.put(PolicyService.class, new PolicyServiceProxy(baseURL, restTemplate));
        services.put(ReportService.class, new ReportServiceProxy(baseURL, restTemplate));
        services.put(ResourceService.class, new ResourceServiceProxy(baseURL, restTemplate));
        services.put(RoleService.class, new RoleServiceProxy(baseURL, restTemplate));
        services.put(SchemaService.class, new SchemaServiceProxy(baseURL, restTemplate));
        services.put(TaskService.class, new TaskServiceProxy(baseURL, restTemplate));
        services.put(UserRequestService.class, new UserRequestServiceProxy(baseURL, restTemplate));
        services.put(UserService.class, new UserServiceProxy(baseURL, restTemplate));
        services.put(WorkflowService.class, new WorkflowServiceProxy(baseURL, restTemplate));
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> service) {
        return (T) services.get(service);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getCoreVersion() {
        return coreVersion;
    }

    public void setCoreVersion(String coreVersion) {
        this.coreVersion = coreVersion;
    }

    public void setEntitlements(final String[] entitlements) {
        String[] defensiveCopy = entitlements.clone();
        roles = new Roles(defensiveCopy);
    }

    public Roles getEntitlements() {
        return roles;
    }

    public boolean isAuthenticated() {
        return getUserId() != null;
    }

    public boolean hasAnyRole(final Roles roles) {
        return this.roles.hasAnyRole(roles);
    }

    public SimpleDateFormat getDateFormat() {
        String language = "en";
        if (getLocale() != null) {
            language = getLocale().getLanguage();
        }

        SimpleDateFormat formatter;
        if ("it".equals(language)) {
            formatter = new SimpleDateFormat(Constants.ITALIAN_DATE_FORMAT);
        } else {
            formatter = new SimpleDateFormat(Constants.ENGLISH_DATE_FORMAT);
        }

        return formatter;
    }
}
