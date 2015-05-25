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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class VirAttrHandler {

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
    private AnyUtilsFactory anyUtilsFactory;

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

    public void updateOnResourcesIfMappingMatches(final Any<?, ?, ?> any, final AnyUtils anyUtils,
            final String schemaKey, final Set<ExternalResource> resources, final IntMappingType mappingType,
            final PropagationByResource propByRes) {

        for (ExternalResource resource : resources) {
            for (MappingItem mapItem : anyUtils.getMappingItems(
                    resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                if (schemaKey.equals(mapItem.getIntAttrName()) && mapItem.getIntMappingType() == mappingType) {
                    propByRes.add(ResourceOperation.UPDATE, resource.getKey());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PropagationByResource fillVirtual(final Any any,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated, final AnyUtils anyUtils) {

        PropagationByResource propByRes = new PropagationByResource();

        Set<ExternalResource> externalResources = new HashSet<>();
        if (any instanceof User) {
            externalResources.addAll(userDAO.findAllResources((User) any));
        } else if (any instanceof Group) {
            externalResources.addAll(((Group) any).getResources());
        } else if (any instanceof AnyObject) {
            externalResources.addAll(anyObjectDAO.findAllResources((AnyObject) any));
        }

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
                    for (MappingItem mapItem : anyUtils.getMappingItems(
                            resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                        if (virSchema.getKey().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == anyUtils.virIntMappingType()) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                            // Using virtual attribute as ConnObjectKey must be avoided
                            if (mapItem.isConnObjectKey() && virAttr != null && !virAttr.getValues().isEmpty()) {
                                propByRes.addOldAccountId(resource.getKey(), virAttr.getValues().get(0).toString());
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
            VirAttr virAttr = null;
            if (virSchema != null) {
                virAttr = any.getVirAttr(virSchema.getKey());
                if (virAttr == null) {
                    virAttr = anyUtils.newVirAttr();
                    virAttr.setSchema(virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema was found", vAttrToBeUpdated);
                    } else {
                        any.add(virAttr);
                    }
                }
            }

            if (virSchema != null && virAttr != null && virAttr.getSchema() != null) {
                updateOnResourcesIfMappingMatches(any, anyUtils, virSchema.getKey(),
                        externalResources, anyUtils.derIntMappingType(), propByRes);

                List<String> values = new ArrayList<>(virAttr.getValues());
                values.removeAll(vAttrToBeUpdated.getValuesToBeRemoved());
                values.addAll(vAttrToBeUpdated.getValuesToBeAdded());

                virAttr.getValues().clear();
                virAttr.getValues().addAll(values);

                // Owner cannot be specified before otherwise a virtual attribute remove will be invalidated.
                virAttr.setOwner(any);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    /**
     * Add virtual attributes and specify values to be propagated.
     *
     * @param any any.
     * @param vAttrs virtual attributes to be added.
     * @param anyUtils utils
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void fillVirtual(final Any any, final Collection<AttrTO> vAttrs, final AnyUtils anyUtils) {
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

    /**
     * SYNCOPE-459: build virtual attribute changes in case no other changes were made.
     *
     * @param key user id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    public PropagationByResource fillVirtual(
            final Long key, final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated) {

        return fillVirtual(
                anyObjectDAO.authFind(key),
                vAttrsToBeRemoved,
                vAttrsToBeUpdated,
                anyUtilsFactory.getInstance(AnyTypeKind.USER));
    }
}
