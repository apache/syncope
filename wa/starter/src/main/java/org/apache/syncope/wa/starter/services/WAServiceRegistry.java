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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.mapping.RegisteredServiceMapper;
import org.apereo.cas.services.AbstractServiceRegistry;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class WAServiceRegistry extends AbstractServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(WAServiceRegistry.class);

    protected final WARestClient waRestClient;

    protected final RegisteredServiceMapper registeredServiceMapper;

    public WAServiceRegistry(
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
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return List.of();
        }

        LOG.info("Loading application definitions");
        return waRestClient.getService(WAClientAppService.class).list().stream().
                map(registeredServiceMapper::toRegisteredService).
                filter(Objects::nonNull).
                toList();
    }

    @Override
    public RegisteredService findServiceById(final long id) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        }

        LOG.info("Searching for application definition by id {}", id);
        return registeredServiceMapper.toRegisteredService(
                waRestClient.getService(WAClientAppService.class).read(id, null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RegisteredService> T findServiceById(final long id, final Class<T> clazz) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        }

        LOG.info("Searching for application definition by id {} and type {}", id, clazz);
        if (clazz.isInstance(OidcRegisteredService.class)) {
            return (T) registeredServiceMapper.toRegisteredService(
                    waRestClient.getService(WAClientAppService.class).read(id, ClientAppType.OIDCRP));
        }
        if (clazz.isInstance(SamlRegisteredService.class)) {
            return (T) registeredServiceMapper.toRegisteredService(
                    waRestClient.getService(WAClientAppService.class).read(id, ClientAppType.SAML2SP));
        }
        return (T) registeredServiceMapper.toRegisteredService(
                waRestClient.getService(WAClientAppService.class).read(id, ClientAppType.CASSP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RegisteredService> T findServiceByExactServiceName(final String name, final Class<T> clazz) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        }

        LOG.info("Searching for application definition by name {} and type {}", name, clazz);
        if (clazz.isInstance(OidcRegisteredService.class)) {
            return (T) registeredServiceMapper.toRegisteredService(waRestClient.
                    getService(WAClientAppService.class).read(name, ClientAppType.OIDCRP));
        }
        if (clazz.isInstance(SamlRegisteredService.class)) {
            return (T) registeredServiceMapper.toRegisteredService(waRestClient.
                    getService(WAClientAppService.class).read(name, ClientAppType.SAML2SP));
        }
        return (T) registeredServiceMapper.toRegisteredService(waRestClient.
                getService(WAClientAppService.class).read(name, ClientAppType.CASSP));
    }

    @Override
    public RegisteredService findServiceByExactServiceName(final String name) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch application definitions");
            return null;
        }

        LOG.info("Searching for application definition by name {}", name);
        return registeredServiceMapper.toRegisteredService(
                waRestClient.getService(WAClientAppService.class).read(name, null));
    }
}
