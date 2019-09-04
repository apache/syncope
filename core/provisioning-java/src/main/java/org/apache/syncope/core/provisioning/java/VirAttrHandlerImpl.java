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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Component
public class VirAttrHandlerImpl implements VirAttrHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VirAttrHandler.class);

    @Autowired
    private ConnectorFactory connFactory;

    @Autowired
    private VirAttrCache virAttrCache;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private Map<VirSchema, List<String>> getValues(final Any<?> any, final Set<VirSchema> schemas) {
        Set<ExternalResource> ownedResources = anyUtilsFactory.getInstance(any).getAllResources(any);

        Map<VirSchema, List<String>> result = new HashMap<>();

        Map<Provision, Set<VirSchema>> toRead = new HashMap<>();

        schemas.forEach(schema -> {
            if (ownedResources.contains(schema.getProvision().getResource())) {
                VirAttrCacheValue virAttrCacheValue =
                        virAttrCache.get(any.getType().getKey(), any.getKey(), schema.getKey());

                if (virAttrCache.isValidEntry(virAttrCacheValue)) {
                    LOG.debug("Values for {} found in cache: {}", schema, virAttrCacheValue);
                    result.put(schema, virAttrCacheValue.getValues());
                } else if (schema.getProvision().getAnyType().equals(any.getType())) {
                    Set<VirSchema> schemasToRead = toRead.get(schema.getProvision());
                    if (schemasToRead == null) {
                        schemasToRead = new HashSet<>();
                        toRead.put(schema.getProvision(), schemasToRead);
                    }
                    schemasToRead.add(schema);
                }
            } else {
                LOG.debug("Not considering {} since {} is not assigned to {}",
                        schema, any, schema.getProvision().getResource());
            }
        });

        toRead.forEach((provision, schemasToRead) -> {
            LOG.debug("About to read from {}: {}", provision, schemasToRead);

            Optional<? extends MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
            String connObjectKeyValue = connObjectKeyItem.isPresent()
                    ? mappingManager.getConnObjectKeyValue(any, provision).orElse(null)
                    : null;
            if (connObjectKeyItem.isEmpty() || connObjectKeyValue == null) {
                LOG.error("No ConnObjectKey or value found for {}, ignoring...", provision);
            } else {
                Set<MappingItem> linkingMappingItems = new HashSet<>();
                linkingMappingItems.add(connObjectKeyItem.get());
                linkingMappingItems.addAll(schemasToRead.stream().
                        map(schema -> schema.asLinkingMappingItem()).collect(Collectors.toSet()));

                Connector connector = connFactory.getConnector(provision.getResource());
                try {
                    ConnectorObject connectorObject = connector.getObject(
                            provision.getObjectClass(),
                            AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKeyValue),
                            provision.isIgnoreCaseMatch(),
                            MappingUtils.buildOperationOptions(linkingMappingItems.iterator()));

                    if (connectorObject == null) {
                        LOG.debug("No read from {} with filter '{} == {}'",
                                provision, connObjectKeyItem.get().getExtAttrName(), connObjectKeyValue);
                    } else {
                        schemasToRead.forEach(schema -> {
                            Attribute attr = connectorObject.getAttributeByName(schema.getExtAttrName());
                            if (attr != null) {
                                VirAttrCacheValue virAttrCacheValue = new VirAttrCacheValue();
                                virAttrCacheValue.setValues(attr.getValue());
                                virAttrCache.put(
                                        any.getType().getKey(), any.getKey(), schema.getKey(),
                                        virAttrCacheValue);
                                LOG.debug("Values for {} set in cache: {}", schema, virAttrCacheValue);

                                result.put(schema, virAttrCacheValue.getValues());
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.error("Error reading from {}", provision, e);
                }
            }
        });

        return result;
    }

    @Override
    public List<String> getValues(final Any<?> any, final VirSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().
                findAllowedSchemas(any, VirSchema.class).forSelfContains(schema)) {

            LOG.debug("{} not allowed for {}", schema, any);
            return Collections.emptyList();
        }

        List<String> result = getValues(any, Collections.singleton(schema)).get(schema);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> getValues(final Any<?> any, final Membership<?> membership, final VirSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().
                findAllowedSchemas(any, VirSchema.class).getForMembership(membership.getRightEnd()).contains(schema)) {

            LOG.debug("{} not allowed for {}", schema, any);
            return Collections.emptyList();
        }

        List<String> result = getValues(any, Collections.singleton(schema)).get(schema);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public Map<VirSchema, List<String>> getValues(final Any<?> any) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, VirSchema.class).getForSelf());
    }

    @Override
    public Map<VirSchema, List<String>> getValues(final Any<?> any, final Membership<?> membership) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, VirSchema.class).
                        getForMembership(membership.getRightEnd()));
    }

}
