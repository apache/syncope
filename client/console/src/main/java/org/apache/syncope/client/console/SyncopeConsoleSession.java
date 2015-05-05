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

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SyncopeConsoleSession extends AuthenticatedWebSession {

    private static final long serialVersionUID = 747562246415852166L;

    public static final String AUTHENTICATED = "AUTHENTICATED";

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleSession.class);

    private final SyncopeClientFactoryBean clientFactory;

    private final String version;

    private final SyncopeTO syncopeTO;

    private final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<Class<?>, Object>());

    private SyncopeClient client;

    private String username;

    private String password;

    private UserTO selfTO;

    private Map<String, Set<String>> auth;

    private Roles roles;

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    public SyncopeConsoleSession(final Request request) {
        super(request);

        ApplicationContext ctx = WebApplicationContextUtils.
                getWebApplicationContext(WebApplication.get().getServletContext());

        clientFactory = ctx.getBean(SyncopeClientFactoryBean.class).
                setContentType(SyncopeClientFactoryBean.ContentType.JSON);
        String anonymousUser = ctx.getBean("anonymousUser", String.class);
        String anonymousKey = ctx.getBean("anonymousKey", String.class);

        version = ctx.getBean("version", String.class);

        syncopeTO = clientFactory.create(anonymousUser, anonymousKey).getService(SyncopeService.class).info();
    }

    public String getVersion() {
        return version;
    }

    public SyncopeTO getSyncopeTO() {
        return syncopeTO;
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = clientFactory.create(username, password);

            Pair<Map<String, Set<String>>, UserTO> self = client.self();
            auth = self.getKey();
            selfTO = self.getValue();

            this.username = username;
            this.password = password;
            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    public UserTO getSelfTO() {
        return selfTO;
    }

    public boolean owns(final String entitlement) {
        return auth.containsKey(entitlement);
    }

    @Override
    public Roles getRoles() {
        if (isSignedIn() && roles == null && auth != null) {
            roles = new Roles(auth.keySet().toArray(new String[] {}));
            roles.add(AUTHENTICATED);
        }

        return roles;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedService(final Class<T> serviceClass) {
        T service;
        if (services.containsKey(serviceClass)) {
            service = (T) services.get(serviceClass);
        } else {
            service = client.getService(serviceClass);
            services.put(serviceClass, service);
        }

        return service;
    }

    public <T> T getService(final Class<T> serviceClass) {
        return getCachedService(serviceClass);
    }

    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T serviceInstance = getCachedService(serviceClass);
        WebClient.client(serviceInstance).match(new EntityTag(etag), false);

        return serviceInstance;
    }

    public <T> T getService(final MediaType mediaType, final Class<T> serviceClass) {
        T service;

        synchronized (clientFactory) {
            SyncopeClientFactoryBean.ContentType preType = clientFactory.getContentType();

            clientFactory.setContentType(SyncopeClientFactoryBean.ContentType.fromString(mediaType.toString()));
            service = clientFactory.create(username, password).getService(serviceClass);
            clientFactory.setContentType(preType);
        }

        return service;
    }

    public <T> void resetClient(final Class<T> service) {
        T serviceInstance = getCachedService(service);
        WebClient.client(serviceInstance).reset();
    }

    public DateFormat getDateFormat() {
        final Locale locale = getLocale() == null ? Locale.ENGLISH : getLocale();

        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    }
}
