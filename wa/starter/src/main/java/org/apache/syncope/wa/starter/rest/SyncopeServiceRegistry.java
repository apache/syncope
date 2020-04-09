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
package org.apache.syncope.wa.starter.rest;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.service.RegisteredClientAppService;
import org.apache.syncope.wa.WARestClient;
import org.apache.syncope.wa.mapper.RegisteredServiceMapper;
import org.apereo.cas.services.AbstractServiceRegistry;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class SyncopeServiceRegistry extends AbstractServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeServiceRegistry.class);

    private final WARestClient restClient;

    private final RegisteredServiceMapper mapper;

    public SyncopeServiceRegistry(final WARestClient restClient,
            final ConfigurableApplicationContext applicationContext,
            final Collection<ServiceRegistryListener> serviceRegistryListeners) {

        super(applicationContext, serviceRegistryListeners);
        this.restClient = restClient;
        this.mapper = new RegisteredServiceMapper();
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        if (WARestClient.isReady()) {
            LOG.info("Create application definitions");
            Response response =
                    restClient.getSyncopeClient().getService(RegisteredClientAppService.class).create(mapper.
                            fromRegisteredService(registeredService));
            if (response.getStatusInfo().getStatusCode() == Response.Status.CREATED.getStatusCode()) {
                return registeredService;
            }
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return null;
    }

    @Override
    public boolean delete(final RegisteredService registeredService) {
        if (WARestClient.isReady()) {
            LOG.info("Delete application definitions");
            return restClient.getSyncopeClient().getService(RegisteredClientAppService.class).
                    delete(registeredService.getName());
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return false;
    }

    @Override
    public Collection<RegisteredService> load() {
        if (WARestClient.isReady()) {
            LOG.info("Loading application definitions");
            return restClient.getSyncopeClient().getService(RegisteredClientAppService.class).list().stream().
                    map(clientApp -> mapper.toRegisteredService(clientApp)).collect(Collectors.toList());
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return List.of();
    }

    @Override
    public RegisteredService findServiceById(final long id) {
        if (WARestClient.isReady()) {
            LOG.info("Searching for application definition by id {}", id);
            return mapper.toRegisteredService(restClient.getSyncopeClient().
                    getService(RegisteredClientAppService.class).read(id));
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> T findServiceByExactServiceName(final String name, final Class<T> clazz) {
        if (WARestClient.isReady()) {
            LOG.info("Searching for application definition by name {} and type {}", name, clazz);
            if (clazz.isInstance(OidcRegisteredService.class)) {
                return (T) mapper.toRegisteredService(restClient.getSyncopeClient().
                        getService(RegisteredClientAppService.class).read(name, ClientAppType.OIDCRP));
            } else if (clazz.isInstance(SamlRegisteredService.class)) {
                return (T) mapper.toRegisteredService(restClient.getSyncopeClient().
                        getService(RegisteredClientAppService.class).read(name, ClientAppType.SAML2SP));
            }
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return null;
    }

    @Override
    public RegisteredService findServiceByExactServiceName(final String name) {
        if (WARestClient.isReady()) {
            LOG.info("Searching for application definition by name {}", name);
            return mapper.toRegisteredService(restClient.getSyncopeClient().
                    getService(RegisteredClientAppService.class).read(name));
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> T findServiceById(final long id, final Class<T> clazz) {
        if (WARestClient.isReady()) {
            LOG.info("Searching for application definition by id {} and type {}", id, clazz);
            if (clazz.isInstance(OidcRegisteredService.class)) {
                return (T) mapper.toRegisteredService(restClient.getSyncopeClient().
                        getService(RegisteredClientAppService.class).read(id, ClientAppType.OIDCRP));
            } else if (clazz.isInstance(SamlRegisteredService.class)) {
                return (T) mapper.toRegisteredService(restClient.getSyncopeClient().
                        getService(RegisteredClientAppService.class).read(id, ClientAppType.SAML2SP));
            }
        }
        LOG.debug("Syncope client is not yet ready to fetch application definitions");
        return null;
    }

}
