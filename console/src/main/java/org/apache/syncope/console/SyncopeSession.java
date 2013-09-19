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

import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.SyncopeClientFactoryBean;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Custom Syncope Session class.
 */
public class SyncopeSession extends WebSession {

    private static final long serialVersionUID = 7743446298924805872L;

    public static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(new Locale[] {
        Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR")});

    private String username;

    private String password;

    private String version;

    private Roles roles = new Roles();

    private final SyncopeClientFactoryBean clientFactory;

    private final Map<Class<?>, Object> restServices = new HashMap<Class<?>, Object>();

    public static SyncopeSession get() {
        return (SyncopeSession) Session.get();
    }

    public SyncopeSession(final Request request) {
        super(request);

        final ApplicationContext applicationContext =
                WebApplicationContextUtils.getWebApplicationContext(WebApplication.get().getServletContext());

        clientFactory = applicationContext.getBean(SyncopeClientFactoryBean.class);
    }

    public <T> T getService(final Class<T> service) {
        return getService(service, this.username, this.password);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(final Class<T> serviceClass, final String username, final String password) {
        T res;
        if (restServices.containsKey(serviceClass)) {
            res = (T) restServices.get(serviceClass);
        } else {
            res = clientFactory.create(username, password).getService(serviceClass);
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                restServices.put(serviceClass, res);
            }
        }

        return res;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setEntitlements(final String[] entitlements) {
        String[] defensiveCopy = entitlements.clone();
        roles = new Roles(defensiveCopy);
    }

    public Roles getEntitlements() {
        return roles;
    }

    public boolean isAuthenticated() {
        return getUsername() != null;
    }

    public boolean hasAnyRole(final Roles roles) {
        return this.roles.hasAnyRole(roles);
    }

    public DateFormat getDateFormat() {
        final Locale locale = getLocale() == null ? Locale.ENGLISH : getLocale();

        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    }
}
