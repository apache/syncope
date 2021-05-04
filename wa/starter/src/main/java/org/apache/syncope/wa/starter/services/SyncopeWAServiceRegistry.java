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
package org.apache.syncope.wa.starter.services;

import org.apache.syncope.wa.starter.mapping.RegisteredServiceMapper;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.services.AbstractServiceRegistry;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;

public class SyncopeWAServiceRegistry extends AbstractServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAServiceRegistry.class);

    private final WARestClient waRestClient;

    private final RegisteredServiceMapper registeredServiceMapper;

    public SyncopeWAServiceRegistry(
            final WARestClient restClient,
            final RegisteredServiceMapper registeredServiceMapper,
            final ConfigurableApplicationContext applicationContext,
            final Collection<ServiceRegistryListener> serviceRegistryListeners) {

        super(applicationContext, serviceRegistryListeners);
        this.waRestClient = restClient;
        this.registeredServiceMapper = registeredServiceMapper;
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Saving registered services from WA is not supported");
    }

    @Override
    public boolean delete(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Deleting registered services from WA is not supported");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Bulk deleting registered services from WA is not supported");
    }

    @Override
    public Collection<RegisteredService> load() {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return List.of();
        } else {
            LOG.info("Loading application definitions");
            return waRestClient.getSyncopeClient().getService(WAClientAppService.class).list().stream().
                    map(registeredServiceMapper::toRegisteredService).
                    collect(Collectors.toList());
        }
    }

    @Override
    public RegisteredService findServiceById(final long id) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        } else {
            LOG.info("Searching for application definition by id {}", id);
            return registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                    getService(WAClientAppService.class).read(id, null));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> T findServiceById(final long id, final Class<T> clazz) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        } else {
            LOG.info("Searching for application definition by id {} and type {}", id, clazz);
            if (clazz.isInstance(OidcRegisteredService.class)) {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(id, ClientAppType.OIDCRP));
            } else if (clazz.isInstance(SamlRegisteredService.class)) {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(id, ClientAppType.SAML2SP));
            } else {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(id, ClientAppType.CASSP));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> T findServiceByExactServiceName(final String name, final Class<T> clazz) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        } else {
            LOG.info("Searching for application definition by name {} and type {}", name, clazz);
            if (clazz.isInstance(OidcRegisteredService.class)) {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(name, ClientAppType.OIDCRP));
            } else if (clazz.isInstance(SamlRegisteredService.class)) {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(name, ClientAppType.SAML2SP));
            } else {
                return (T) registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                        getService(WAClientAppService.class).read(name, ClientAppType.CASSP));
            }
        }
    }

    @Override
    public RegisteredService findServiceByExactServiceName(final String name) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        } else {
            LOG.info("Searching for application definition by name {}", name);
            return registeredServiceMapper.toRegisteredService(waRestClient.getSyncopeClient().
                    getService(WAClientAppService.class).read(name, null));
        }
    }
}
