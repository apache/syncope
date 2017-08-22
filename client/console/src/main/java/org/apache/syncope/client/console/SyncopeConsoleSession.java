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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

public class SyncopeConsoleSession extends AuthenticatedWebSession {

    private static final long serialVersionUID = 747562246415852166L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleSession.class);

    private static final ThreadPoolExecutorFactoryBean THREAD_POOL_FACTORY;

    public static final String AUTHENTICATED = "AUTHENTICATED";

    public static final String MENU_COLLAPSE = "MENU_COLLAPSE";

    static {
        THREAD_POOL_FACTORY = new ThreadPoolExecutorFactoryBean();
        THREAD_POOL_FACTORY.setThreadNamePrefix(SyncopeConsoleSession.class.getSimpleName());
        THREAD_POOL_FACTORY.setDaemon(true);
    }

    private final SyncopeClientFactoryBean clientFactory;

    private final SyncopeClient anonymousClient;

    private final PlatformInfo platformInfo;

    private final SystemInfo systemInfo;

    private String domain;

    private final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5, THREAD_POOL_FACTORY);

    private SyncopeClient client;

    private UserTO selfTO;

    private Map<String, Set<String>> auth;

    private Roles roles;

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    public SyncopeConsoleSession(final Request request) {
        super(request);

        clientFactory = SyncopeConsoleApplication.get().newClientFactory();
        anonymousClient = clientFactory.
                create(new AnonymousAuthenticationHandler(
                        SyncopeConsoleApplication.get().getAnonymousUser(),
                        SyncopeConsoleApplication.get().getAnonymousKey()));

        platformInfo = anonymousClient.getService(SyncopeService.class).platform();
        systemInfo = anonymousClient.getService(SyncopeService.class).system();
    }

    public MediaType getMediaType() {
        return clientFactory.getContentType().getMediaType();
    }

    public SyncopeClient getAnonymousClient() {
        return anonymousClient;
    }

    public void execute(final Runnable command) {
        executorService.execute(command);
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return StringUtils.isBlank(domain) ? SyncopeConstants.MASTER_DOMAIN : domain;
    }

    public String getJWT() {
        return client == null ? null : client.getJWT();
    }

    private void afterAuthentication() {
        refreshAuthorization();
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(username, password);

            afterAuthentication();

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    public boolean authenticate(final String jwt) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(jwt);

            afterAuthentication();

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        if (authenticated) {
            bind();
        }
        signIn(authenticated);

        return authenticated;
    }

    public void cleanup() {
        client = null;
        auth = null;
        selfTO = null;
    }

    @Override
    public void invalidate() {
        if (getJWT() != null) {
            if (client != null) {
                client.logout();
            }
            cleanup();
        }
        executorService.shutdown();
        super.invalidate();
    }

    @Override
    public void invalidateNow() {
        if (getJWT() != null) {
            if (client != null) {
                client.logout();
            }
            cleanup();
        }
        executorService.shutdownNow();
        super.invalidateNow();
    }

    public UserTO getSelfTO() {
        return selfTO;
    }

    public List<String> getAuthRealms() {
        List<String> sortable = new ArrayList<>();
        List<String> available = SetUniqueList.setUniqueList(sortable);
        auth.entrySet().forEach(entitlement -> {
            available.addAll(entitlement.getValue());
        });
        Collections.sort(sortable);
        return sortable;
    }

    public boolean owns(final String entitlements) {
        return owns(entitlements, SyncopeConstants.ROOT_REALM);
    }

    public boolean owns(final String entitlements, final String realm) {
        if (StringUtils.isEmpty(entitlements)) {
            return true;
        }

        for (String entitlement : entitlements.split(",")) {
            if (auth != null && auth.containsKey(entitlement) && (realm == null
                    || auth.get(entitlement).stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm)))) {
                return true;
            }
        }

        return false;
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
        client.refresh();
        roles = null;
    }

    public void refreshAuthorization() {
        Pair<Map<String, Set<String>>, UserTO> self = client.self();
        auth = self.getLeft();
        selfTO = self.getRight();
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

        WebClient.client(service).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

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

            clientFactory.
                    setContentType(SyncopeClientFactoryBean.ContentType.fromString(mediaType.toString()));
            service = clientFactory.create(getJWT()).getService(serviceClass);

            clientFactory.setContentType(preType);
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
}
