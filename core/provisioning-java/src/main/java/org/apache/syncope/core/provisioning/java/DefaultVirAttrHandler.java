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
package org.apache.syncope.core.provisioning.java;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultVirAttrHandler implements VirAttrHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(VirAttrHandler.class);

    protected final ConnectorManager connectorManager;

    protected final Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache;

    protected final OutboundMatcher outboundMatcher;

    protected final AnyUtilsFactory anyUtilsFactory;

    public DefaultVirAttrHandler(
            final ConnectorManager connectorManager,
            final Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache,
            final OutboundMatcher outboundMatcher,
            final AnyUtilsFactory anyUtilsFactory) {

        this.connectorManager = connectorManager;
        this.virAttrCache = virAttrCache;
        this.outboundMatcher = outboundMatcher;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    @Override
    public void setValues(final Any any, final ConnectorObject connObj) {
        if (any == null) {
            LOG.debug("Null any passed, ignoring");
            return;
        }

        AllowedSchemas<VirSchema> schemas =
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, VirSchema.class);
        Stream.concat(
                schemas.getForSelf().stream(),
                schemas.getForMemberships().values().stream().flatMap(Set::stream)).forEach(schema -> {

            VirAttrCacheKey cacheKey = VirAttrCacheKey.of(any.getType().getKey(), any.getKey(), schema.getKey());

            Attribute attr = connObj.getAttributeByName(schema.getExtAttrName());
            if (attr == null) {
                virAttrCache.remove(cacheKey);
                LOG.debug("Evicted from cache: {}", cacheKey);
            } else {
                VirAttrCacheValue cacheValue = VirAttrCacheValue.of(attr.getValue());
                virAttrCache.put(cacheKey, cacheValue);
                LOG.debug("Set in cache: {}={}", cacheKey, cacheValue);
            }
        });
    }

    protected Map<VirSchema, List<String>> getValues(final Any any, final Set<VirSchema> schemas) {
        Set<ExternalResource> resources = anyUtilsFactory.getInstance(any).getAllResources(any);

        Map<VirSchema, List<String>> result = new HashMap<>();

        Map<Pair<ExternalResource, Provision>, Set<VirSchema>> toRead = new HashMap<>();

        schemas.stream().filter(schema -> resources.contains(schema.getResource())).forEach(schema -> {
            VirAttrCacheKey cacheKey = VirAttrCacheKey.of(any.getType().getKey(), any.getKey(), schema.getKey());
            VirAttrCacheValue cacheValue = virAttrCache.get(cacheKey);

            if (cacheValue != null) {
                LOG.debug("Found in cache: {}={}", cacheKey, cacheValue);
                result.put(schema, cacheValue.values());
            } else if (schema.getAnyType().equals(any.getType())) {
                schema.getResource().getProvisionByAnyType(schema.getAnyType().getKey()).ifPresent(provision -> {
                    Set<VirSchema> schemasToRead = toRead.get(Pair.of(schema.getResource(), provision));
                    if (schemasToRead == null) {
                        schemasToRead = new HashSet<>();
                        toRead.put(Pair.of(schema.getResource(), provision), schemasToRead);
                    }
                    schemasToRead.add(schema);
                });
            }
        });

        toRead.forEach((pair, schemasToRead) -> {
            LOG.debug("About to read from {}: {}", pair, schemasToRead);

            outboundMatcher.match(
                    connectorManager.getConnector(pair.getLeft()),
                    any,
                    pair.getLeft(),
                    pair.getRight(),
                    Optional.empty(),
                    schemasToRead.stream().map(VirSchema::asLinkingMappingItem).toArray(Item[]::new)).
                    forEach(connObj -> schemasToRead.forEach(schema -> {

                Attribute attr = connObj.getAttributeByName(schema.getExtAttrName());
                if (attr != null) {
                    VirAttrCacheKey cacheKey =
                            VirAttrCacheKey.of(any.getType().getKey(), any.getKey(), schema.getKey());
                    VirAttrCacheValue cacheValue = VirAttrCacheValue.of(attr.getValue());
                    virAttrCache.put(cacheKey, cacheValue);
                    LOG.debug("Set in cache: {}={}", cacheKey, cacheValue);

                    result.put(schema, cacheValue.values());
                }
            }));
        });

        return result;
    }

    @Override
    public List<String> getValues(final Any any, final VirSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().
                findAllowedSchemas(any, VirSchema.class).forSelfContains(schema)) {

            LOG.debug("{} not allowed for {}", schema, any);
            return List.of();
        }

        List<String> result = getValues(any, Set.of(schema)).get(schema);
        return result == null ? List.of() : result;
    }

    @Override
    public List<String> getValues(final Any any, final Membership<?> membership, final VirSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().
                findAllowedSchemas(any, VirSchema.class).getForMembership(membership.getRightEnd()).contains(schema)) {

            LOG.debug("{} not allowed for {}", schema, any);
            return List.of();
        }

        List<String> result = getValues(any, Set.of(schema)).get(schema);
        return result == null ? List.of() : result;
    }

    @Override
    public Map<VirSchema, List<String>> getValues(final Any any) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, VirSchema.class).getForSelf());
    }

    @Override
    public Map<VirSchema, List<String>> getValues(final Any any, final Membership<?> membership) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, VirSchema.class).
                        getForMembership(membership.getRightEnd()));
    }
}
