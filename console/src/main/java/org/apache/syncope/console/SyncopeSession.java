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
import java.util.List;
import java.util.Locale;
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

    public static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(new Locale[] {
        Locale.ENGLISH, Locale.ITALIAN});

    private String userId;

    private String coreVersion;

    private Roles roles = new Roles();

    private RestTemplate restTemplate;

    public static SyncopeSession get() {
        return (SyncopeSession) Session.get();
    }

    public SyncopeSession(final Request request) {
        super(request);

        final ApplicationContext applicationContext =
                WebApplicationContextUtils.getWebApplicationContext(WebApplication.get().getServletContext());

        restTemplate = applicationContext.getBean(RestTemplate.class);
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

    public DateFormat getDateFormat() {
        final Locale locale = getLocale() == null ? Locale.ENGLISH : getLocale();

        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    }
}
