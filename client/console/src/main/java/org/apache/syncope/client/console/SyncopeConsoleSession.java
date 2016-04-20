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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeConsoleSession extends AuthenticatedWebSession {

    private static final long serialVersionUID = 747562246415852166L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleSession.class);

    public static final String AUTHENTICATED = "AUTHENTICATED";

    public static final String MENU_COLLAPSE = "MENU_COLLAPSE";

    private final PlatformInfo platformInfo;

    private final List<String> domains;

    private String domain;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<Class<?>, Object>());

    private SyncopeClient client;

    private String username;

    private String password;

    private UserTO selfTO;

    private Map<String, Set<String>> auth;

    private Roles roles;

    private NotificationPanel notificationPanel;

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    public SyncopeConsoleSession(final Request request) {
        super(request);

        SyncopeClient anonymousClient = SyncopeConsoleApplication.get().getClientFactory().create(
                SyncopeConsoleApplication.get().getAnonymousUser(),
                SyncopeConsoleApplication.get().getAnonymousKey());

        platformInfo = anonymousClient.getService(SyncopeService.class).platform();
        domains = new ArrayList<>();
        domains.add(SyncopeConstants.MASTER_DOMAIN);
        CollectionUtils.collect(anonymousClient.getService(DomainService.class).list(),
                EntityTOUtils.<DomainTO>keyTransformer(), domains);
    }

    public void execute(final Runnable command) {
        executorService.execute(command);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(
            final Runnable command,
            final long initialDelay,
            final long period,
            final TimeUnit unit) {

        return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        executorService.shutdownNow();
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return StringUtils.isBlank(domain) ? SyncopeConstants.MASTER_DOMAIN : domain;
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = SyncopeConsoleApplication.get().getClientFactory().
                    setDomain(getDomain()).create(username, password);

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

    public void refreshAuth() {
        authenticate(username, password);
        roles = null;
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

        WebClient.client(service).
                type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

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

        synchronized (SyncopeConsoleApplication.get().getClientFactory()) {
            SyncopeClientFactoryBean.ContentType preType = SyncopeConsoleApplication.get().getClientFactory().
                    getContentType();

            SyncopeConsoleApplication.get().getClientFactory().
                    setContentType(SyncopeClientFactoryBean.ContentType.fromString(mediaType.toString()));
            service = SyncopeConsoleApplication.get().getClientFactory().
                    create(username, password).getService(serviceClass);
            SyncopeConsoleApplication.get().getClientFactory().setContentType(preType);
        }

        return service;
    }

    public <T> void resetClient(final Class<T> service) {
        T serviceInstance = getCachedService(service);
        WebClient.client(serviceInstance).reset();
    }

    public FastDateFormat getDateFormat() {
        Locale locale = getLocale() == null ? Locale.ENGLISH : getLocale();
        return FastDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    }

    public NotificationPanel getNotificationPanel() {
        if (notificationPanel == null) {
            notificationPanel = new NotificationPanel(Constants.FEEDBACK);
            notificationPanel.setOutputMarkupId(true);
        }
        return notificationPanel;
    }
}
