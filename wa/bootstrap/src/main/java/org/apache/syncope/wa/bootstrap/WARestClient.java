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
package org.apache.syncope.wa.bootstrap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apereo.cas.util.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class WARestClient {

    protected static final Logger LOG = LoggerFactory.getLogger(WARestClient.class);

    protected final String anonymousUser;

    protected final String anonymousKey;

    protected final boolean useGZIPCompression;

    protected final String serviceDiscoveryAddress;

    protected final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    private SyncopeClient client;

    public WARestClient(
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression,
            final String serviceDiscoveryAddress) {

        this.anonymousUser = anonymousUser;
        this.anonymousKey = anonymousKey;
        this.useGZIPCompression = useGZIPCompression;
        this.serviceDiscoveryAddress = serviceDiscoveryAddress;
    }

    protected Optional<NetworkService> getCore() {
        try {
            ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
            if (ctx == null) {
                return Optional.empty();
            }

            Collection<ServiceOps> serviceOpsList = ctx.getBeansOfType(ServiceOps.class).values();
            if (serviceOpsList.isEmpty()) {
                return Optional.empty();
            }

            ServiceOps serviceOps = serviceOpsList.iterator().next();

            if (serviceOps.list(NetworkService.Type.WA).
                    stream().anyMatch(s -> s.getAddress().equals(serviceDiscoveryAddress))) {

                return Optional.of(serviceOps.get(NetworkService.Type.CORE));
            }

            return Optional.empty();
        } catch (KeymasterException e) {
            LOG.trace(e.getMessage());
        }
        return Optional.empty();
    }

    public SyncopeClient getSyncopeClient() {
        synchronized (this) {
            if (client == null) {
                getCore().ifPresent(core -> {
                    try {
                        client = new SyncopeClientFactoryBean().
                                setAddress(core.getAddress()).
                                setUseCompression(useGZIPCompression).
                                create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey));
                    } catch (Exception e) {
                        LOG.error("Could not init SyncopeClient", e);
                    }
                });
            }

            return client;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(final Class<T> serviceClass) {
        if (!isReady()) {
            throw new IllegalStateException("Syncope core is not yet ready");
        }

        T service;
        if (services.containsKey(serviceClass)) {
            service = (T) services.get(serviceClass);
        } else {
            service = getSyncopeClient().getService(serviceClass);
            services.put(serviceClass, service);
        }

        return service;
    }

    public boolean isReady() {
        try {
            return getCore().isPresent();
        } catch (Exception e) {
            LOG.trace("While checking Core's availability: {}", e.getMessage());
        }
        return false;
    }
}
