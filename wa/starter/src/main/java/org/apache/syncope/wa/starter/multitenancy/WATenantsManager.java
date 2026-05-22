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
package org.apache.syncope.wa.starter.multitenancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.wa.bootstrap.WAPropertySourceLocator;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.bootstrap.mapping.AttrRepoPropertySourceMapper;
import org.apache.syncope.wa.bootstrap.mapping.AuthModulePropertySourceMapper;
import org.apereo.cas.multitenancy.DefaultTenantAuthenticationPolicy;
import org.apereo.cas.multitenancy.DefaultTenantDelegatedAuthenticationPolicy;
import org.apereo.cas.multitenancy.TenantDefinition;
import org.apereo.cas.multitenancy.TenantsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WATenantsManager implements TenantsManager {

    protected static final Logger LOG = LoggerFactory.getLogger(WATenantsManager.class);

    protected final DomainOps domainOps;

    protected final WARestClient waRestClient;

    protected final AuthModulePropertySourceMapper authModulePropertySourceMapper;

    protected final AttrRepoPropertySourceMapper attrRepoPropertySourceMapper;

    public WATenantsManager(
            final DomainOps domainOps,
            final WARestClient waRestClient,
            final AuthModulePropertySourceMapper authModulePropertySourceMapper,
            final AttrRepoPropertySourceMapper attrRepoPropertySourceMapper) {

        this.domainOps = domainOps;
        this.waRestClient = waRestClient;
        this.authModulePropertySourceMapper = authModulePropertySourceMapper;
        this.attrRepoPropertySourceMapper = attrRepoPropertySourceMapper;
    }

    protected TenantDefinition buildTenantDefinition(final SyncopeClient syncopeClient) {
        TenantDefinition tenantDefinition = new TenantDefinition();
        tenantDefinition.setId(syncopeClient.getDomain());

        Map<String, Object> properties = new TreeMap<>();
        Map<String, Integer> prefixes = new HashMap<>();

        DefaultTenantAuthenticationPolicy authPolicy = new DefaultTenantAuthenticationPolicy();
        tenantDefinition.setAuthenticationPolicy(authPolicy);

        DefaultTenantDelegatedAuthenticationPolicy delegatedAuthPolicy =
                new DefaultTenantDelegatedAuthenticationPolicy();
        tenantDefinition.setDelegatedAuthenticationPolicy(delegatedAuthPolicy);

        authPolicy.setAuthenticationHandlers(new ArrayList<>());
        delegatedAuthPolicy.setAllowedProviders(new ArrayList<>());
        syncopeClient.getService(AuthModuleService.class).list().forEach(authModuleTO -> {
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            Map<String, Object> map = authModuleTO.getConf().map(authModuleTO, authModulePropertySourceMapper);
            properties.putAll(WAPropertySourceLocator.index(map, prefixes));

            if (map.keySet().stream().anyMatch(k -> k.contains("pac4j"))) {
                delegatedAuthPolicy.getAllowedProviders().add(authModuleTO.getKey());
            } else {
                authPolicy.getAuthenticationHandlers().add(authModuleTO.getKey());
            }
        });

        authPolicy.setAttributeRepositories(new ArrayList<>());
        syncopeClient.getService(AttrRepoService.class).list().forEach(attrRepoTO -> {
            LOG.debug("Mapping attr repo {} ", attrRepoTO.getKey());

            Map<String, Object> map = attrRepoTO.getConf().map(attrRepoTO, attrRepoPropertySourceMapper);
            properties.putAll(WAPropertySourceLocator.index(map, prefixes));

            authPolicy.getAttributeRepositories().add(attrRepoTO.getKey());
        });

        syncopeClient.getService(WAConfigService.class).list().
                forEach(attr -> properties.put(attr.getSchema(), String.join(",", attr.getValues())));

        tenantDefinition.setProperties(properties);
        LOG.debug("Collected Tenant {} properties: {}", tenantDefinition.getId(), tenantDefinition.getProperties());

        return tenantDefinition;
    }

    @Override
    public Optional<TenantDefinition> findTenant(final String tenantId) {
        return waRestClient.getSyncopeClient(tenantId).map(this::buildTenantDefinition);
    }

    @Override
    public List<TenantDefinition> findTenants() {
        List<TenantDefinition> tenants = new ArrayList<>();
        domainOps.list().forEach(domain -> findTenant(domain.getKey()).ifPresent(tenants::add));
        return tenants;
    }
}
