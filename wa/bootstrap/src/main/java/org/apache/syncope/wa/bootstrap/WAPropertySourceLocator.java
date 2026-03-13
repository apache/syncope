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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.OIDCStandardScope;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.OIDCOpEntityService;
import org.apache.syncope.common.rest.api.service.PasswordManagementService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.wa.bootstrap.mapping.AttrRepoPropertySourceMapper;
import org.apache.syncope.wa.bootstrap.mapping.AuthModulePropertySourceMapper;
import org.apache.syncope.wa.bootstrap.mapping.PasswordManagementPropertySourceMapper;
import org.apereo.cas.configuration.model.support.oidc.OidcDiscoveryProperties;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Order
public class WAPropertySourceLocator implements PropertySourceLocator {

    protected static final Logger LOG = LoggerFactory.getLogger(WAPropertySourceLocator.class);

    protected final WARestClient waRestClient;

    protected final AuthModulePropertySourceMapper authModulePropertySourceMapper;

    protected final AttrRepoPropertySourceMapper attrRepoPropertySourceMapper;

    protected final PasswordManagementPropertySourceMapper passwordManagementPropertySourceMapper;

    protected final CipherExecutor<String, String> configurationCipher;

    public WAPropertySourceLocator(
            final WARestClient waRestClient,
            final AuthModulePropertySourceMapper authModulePropertySourceMapper,
            final AttrRepoPropertySourceMapper attrRepoPropertySourceMapper,
            final PasswordManagementPropertySourceMapper passwordManagementPropertySourceMapper,
            final CipherExecutor<String, String> configurationCipher) {

        this.waRestClient = waRestClient;
        this.authModulePropertySourceMapper = authModulePropertySourceMapper;
        this.attrRepoPropertySourceMapper = attrRepoPropertySourceMapper;
        this.passwordManagementPropertySourceMapper = passwordManagementPropertySourceMapper;
        this.configurationCipher = configurationCipher;
    }

    protected Map<String, Object> index(final Map<String, Object> map, final Map<String, Integer> prefixes) {
        Map<String, Object> indexed = map;

        if (!map.isEmpty()) {
            String prefix = map.keySet().iterator().next();
            if (prefix.contains("[]")) {
                prefix = StringUtils.substringBefore(prefix, "[]");
                Integer index = prefixes.getOrDefault(prefix, 0);

                indexed = map.entrySet().stream().
                        map(e -> Pair.of(e.getKey().replace("[]", "[" + index + "]"), e.getValue())).
                        collect(Collectors.toMap(Pair::getKey, Pair::getValue));

                prefixes.put(prefix, index + 1);
            }
        }

        return indexed;
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        }

        LOG.info("Bootstrapping WA configuration");
        Map<String, Object> properties = new TreeMap<>();
        Map<String, Integer> prefixes = new HashMap<>();

        syncopeClient.getService(AuthModuleService.class).list().forEach(authModuleTO -> {
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            Map<String, Object> map = authModuleTO.getConf().map(authModuleTO, authModulePropertySourceMapper);
            properties.putAll(index(map, prefixes));
        });

        syncopeClient.getService(AttrRepoService.class).list().forEach(attrRepoTO -> {
            LOG.debug("Mapping attr repo {} ", attrRepoTO.getKey());

            Map<String, Object> map = attrRepoTO.getConf().map(attrRepoTO, attrRepoPropertySourceMapper);
            properties.putAll(index(map, prefixes));
        });

        syncopeClient.getService(PasswordManagementService.class).list().stream().
                filter(PasswordManagementTO::isEnabled).findFirst().ifPresent(passwordManagementTO -> {
            LOG.debug("Mapping password module {} ", passwordManagementTO.getKey());

            Map<String, Object> map = passwordManagementTO.getConf().
                    map(passwordManagementTO, passwordManagementPropertySourceMapper);
            properties.putAll(index(map, prefixes));
        });

        Map<String, Set<String>> customScopes;
        try {
            customScopes = syncopeClient.getService(OIDCOpEntityService.class).get().getCustomScopes();
        } catch (Exception e) {
            LOG.warn("Could not read OIDC OP: no custom scopes or claims will be set", e);
            customScopes = Map.of();
        }
        if (!customScopes.isEmpty()) {
            properties.put("cas.authn.oidc.discovery.scopes",
                    Stream.concat(Stream.of(OIDCStandardScope.values()).map(OIDCStandardScope::name),
                            customScopes.keySet().stream()).
                            distinct().
                            collect(Collectors.joining(",")));
            properties.put("cas.authn.oidc.discovery.claims",
                    Stream.concat(new OidcDiscoveryProperties().getClaims().stream(),
                            customScopes.values().stream().flatMap(Set::stream)).
                            distinct().
                            collect(Collectors.joining(",")));

            customScopes.forEach((scope, claims) -> properties.put(
                    "cas.authn.oidc.core.user-defined-scopes." + scope, String.join(",", claims)));
        }

        syncopeClient.getService(WAConfigService.class).list().
                forEach(attr -> properties.put(attr.getSchema(), String.join(",", attr.getValues())));

        LOG.debug("Collected WA properties: {}", properties);
        Map<String, Object> decodedProperties = configurationCipher.decode(properties, ArrayUtils.EMPTY_OBJECT_ARRAY);
        LOG.debug("Decoded WA properties: {}", decodedProperties);
        return new MapPropertySource(getClass().getName(), decodedProperties);
    }
}
