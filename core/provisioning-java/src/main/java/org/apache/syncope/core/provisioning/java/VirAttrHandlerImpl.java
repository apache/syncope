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

import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class VirAttrHandlerImpl implements VirAttrHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VirAttrHandler.class);

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private ConnectorFactory connFactory;

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

    @Override
    public VirSchema getVirSchema(final String virSchemaName) {
        VirSchema virtualSchema = null;
        if (StringUtils.isNotBlank(virSchemaName)) {
            virtualSchema = virSchemaDAO.find(virSchemaName);

            if (virtualSchema == null) {
                LOG.debug("Ignoring invalid virtual schema {}", virSchemaName);
            }
        }

        return virtualSchema;
    }

    @Override
    public void updateOnResourcesIfMappingMatches(final Any<?, ?, ?> any, final String schemaKey,
            final Iterable<? extends ExternalResource> resources, final IntMappingType mappingType,
            final PropagationByResource propByRes) {

        for (ExternalResource resource : resources) {
            for (MappingItem mapItem : MappingUtils.getMappingItems(
                    resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                if (schemaKey.equals(mapItem.getIntAttrName()) && mapItem.getIntMappingType() == mappingType) {
                    propByRes.add(ResourceOperation.UPDATE, resource.getKey());
                }
            }
        }
    }

    private Iterable<? extends ExternalResource> getAllResources(final Any<?, ?, ?> any) {
        return any instanceof User
                ? userDAO.findAllResources((User) any)
                : any instanceof AnyObject
                        ? anyObjectDAO.findAllResources((AnyObject) any)
                        : any instanceof Group
                                ? ((Group) any).getResources()
                                : Collections.<ExternalResource>emptySet();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public PropagationByResource fillVirtual(final Any any,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        PropagationByResource propByRes = new PropagationByResource();

        Iterable<? extends ExternalResource> externalResources = getAllResources(any);

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            VirSchema virSchema = getVirSchema(vAttrToBeRemoved);
            if (virSchema != null) {
                VirAttr virAttr = any.getVirAttr(virSchema.getKey());
                if (virAttr == null) {
                    LOG.debug("No virtual attribute found for schema {}", virSchema.getKey());
                } else {
                    any.remove(virAttr);
                    virAttrDAO.delete(virAttr);
                }

                for (ExternalResource resource : externalResources) {
                    for (MappingItem mapItem : MappingUtils.getMappingItems(
                            resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                        if (virSchema.getKey().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == anyUtils.virIntMappingType()) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                            // Using virtual attribute as ConnObjectKey must be avoided
                            if (mapItem.isConnObjectKey() && virAttr != null && !virAttr.getValues().isEmpty()) {
                                propByRes.addOldConnObjectKey(resource.getKey(), virAttr.getValues().get(0).toString());
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attributes to be removed:\n{}", propByRes);

        // 2. virtual attributes to be updated
        for (AttrMod vAttrToBeUpdated : vAttrsToBeUpdated) {
            VirSchema virSchema = getVirSchema(vAttrToBeUpdated.getSchema());
            if (virSchema != null) {
                VirAttr virAttr = any.getVirAttr(virSchema.getKey());
                if (virAttr == null) {
                    virAttr = anyUtils.newVirAttr();
                    virAttr.setOwner(any);
                    virAttr.setSchema(virSchema);

                    any.add(virAttr);
                }

                updateOnResourcesIfMappingMatches(
                        any, virSchema.getKey(), externalResources, anyUtils.derIntMappingType(), propByRes);

                List<String> values = new ArrayList<>(virAttr.getValues());
                values.removeAll(vAttrToBeUpdated.getValuesToBeRemoved());
                values.addAll(vAttrToBeUpdated.getValuesToBeAdded());

                virAttr.getValues().clear();
                virAttr.getValues().addAll(values);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void fillVirtual(final Any any, final Collection<AttrTO> vAttrs) {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        for (AttrTO attrTO : vAttrs) {
            VirAttr virAttr = any.getVirAttr(attrTO.getSchema());
            if (virAttr == null) {
                VirSchema virSchema = getVirSchema(attrTO.getSchema());
                if (virSchema != null) {
                    virAttr = anyUtils.newVirAttr();
                    virAttr.setSchema(virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema was found", attrTO);
                    } else {
                        virAttr.setOwner(any);
                        any.add(virAttr);
                        virAttr.getValues().clear();
                        virAttr.getValues().addAll(attrTO.getValues());
                    }
                }
            } else {
                virAttr.getValues().clear();
                virAttr.getValues().addAll(attrTO.getValues());
            }
        }
    }

    private Any<?, ?, ?> find(final Long key, final AnyTypeKind anyTypeKind) {
        Any<?, ?, ?> result;

        switch (anyTypeKind) {
            case USER:
                result = userDAO.authFind(key);
                break;

            case GROUP:
                result = groupDAO.authFind(key);
                break;

            case ANY_OBJECT:
            default:
                result = anyObjectDAO.authFind(key);
        }

        return result;
    }

    @Transactional
    @Override
    public PropagationByResource fillVirtual(
            final Long key, final AnyTypeKind anyTypeKind,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated) {

        return fillVirtual(
                find(key, anyTypeKind),
                vAttrsToBeRemoved,
                vAttrsToBeUpdated);
    }

    @Override
    public void retrieveVirAttrValues(final Any<?, ?, ?> any) {
        IntMappingType type = any.getType().getKind() == AnyTypeKind.USER
                ? IntMappingType.UserVirtualSchema
                : any.getType().getKind() == AnyTypeKind.GROUP
                        ? IntMappingType.GroupVirtualSchema
                        : IntMappingType.AnyObjectVirtualSchema;

        Map<String, ConnectorObject> resources = new HashMap<>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (VirAttr<?> virAttr : any.getVirAttrs()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(any, virAttr, type, resources);
            }
        }
        // -----------------------
    }

    private void retrieveVirAttrValue(
            final Any<?, ?, ?> any,
            final VirAttr<?> virAttr,
            final IntMappingType type,
            final Map<String, ConnectorObject> externalResources) {

        String schemaName = virAttr.getSchema().getKey();
        VirAttrCacheValue virAttrCacheValue = virAttrCache.get(any.getType().getKey(), any.getKey(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {} ({})", schemaName, type);

        if (virAttrCache.isValidEntry(virAttrCacheValue)) {
            // cached ...
            LOG.debug("Values found in cache {}", virAttrCacheValue);
            virAttr.getValues().clear();
            virAttr.getValues().addAll(new ArrayList<>(virAttrCacheValue.getValues()));
        } else {
            // not cached ...
            LOG.debug("Need one or more remote connections");

            VirAttrCacheValue toBeCached = new VirAttrCacheValue();

            AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

            for (ExternalResource resource : getTargetResources(virAttr, type, any.getType())) {
                Provision provision = resource.getProvision(any.getType());
                LOG.debug("Search values into {},{}", resource, provision);

                try {
                    List<MappingItem> mappings = MappingUtils.getMappingItems(provision, MappingPurpose.BOTH);

                    ConnectorObject connectorObject;
                    if (externalResources.containsKey(resource.getKey())) {
                        connectorObject = externalResources.get(resource.getKey());
                    } else {
                        LOG.debug("Perform connection to {}", resource.getKey());
                        String connObjectKey = MappingUtils.getConnObjectKeyItem(provision) == null
                                ? null
                                : MappingUtils.getConnObjectKeyValue(any, provision);

                        if (StringUtils.isBlank(connObjectKey)) {
                            throw new IllegalArgumentException("No ConnObjectKey found for " + resource.getKey());
                        }

                        Connector connector = connFactory.getConnector(resource);

                        OperationOptions oo =
                                connector.getOperationOptions(MappingUtils.getMatchingMappingItems(mappings, type));

                        connectorObject =
                                connector.getObject(provision.getObjectClass(), new Uid(connObjectKey), oo);
                        externalResources.put(resource.getKey(), connectorObject);
                    }

                    if (connectorObject != null) {
                        // ask for searched virtual attribute value
                        Collection<MappingItem> virAttrMappings =
                                MappingUtils.getMatchingMappingItems(mappings, schemaName, type);

                        // the same virtual attribute could be mapped with one or more external attribute 
                        for (MappingItem mapping : virAttrMappings) {
                            Attribute attribute = connectorObject.getAttributeByName(mapping.getExtAttrName());

                            if (attribute != null && attribute.getValue() != null) {
                                for (Object obj : attribute.getValue()) {
                                    if (obj != null) {
                                        virAttr.getValues().add(obj.toString());
                                    }
                                }
                            }
                        }

                        toBeCached.setResourceValues(resource.getKey(), new HashSet<>(virAttr.getValues()));

                        LOG.debug("Retrieved values {}", virAttr.getValues());
                    }
                } catch (Exception e) {
                    LOG.error("Error reading connector object from {}", resource.getKey(), e);

                    if (virAttrCacheValue != null) {
                        toBeCached.forceExpiring();
                        LOG.debug("Search for a cached value (even expired!) ...");
                        final Set<String> cachedValues = virAttrCacheValue.getValues(resource.getKey());
                        if (cachedValues != null) {
                            LOG.debug("Use cached value {}", cachedValues);
                            virAttr.getValues().addAll(cachedValues);
                            toBeCached.setResourceValues(resource.getKey(), new HashSet<>(cachedValues));
                        }
                    }
                }
            }

            virAttrCache.put(any.getType().getKey(), any.getKey(), schemaName, toBeCached);
        }
    }

    private Collection<ExternalResource> getTargetResources(
            final VirAttr<?> attr, final IntMappingType type, final AnyType anyType) {

        return CollectionUtils.select(getAllResources(attr.getOwner()), new Predicate<ExternalResource>() {

            @Override
            public boolean evaluate(final ExternalResource resource) {
                return resource.getProvision(anyType) != null
                        && !MappingUtils.getMatchingMappingItems(
                                MappingUtils.getMappingItems(resource.getProvision(anyType), MappingPurpose.BOTH),
                                attr.getSchema().getKey(), type).isEmpty();
            }
        });
    }
}
