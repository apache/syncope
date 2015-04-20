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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Mapping;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.SyncPolicy;
import org.apache.syncope.core.persistence.api.entity.group.GMapping;
import org.apache.syncope.core.persistence.api.entity.group.GMappingItem;
import org.apache.syncope.core.persistence.api.entity.user.UMapping;
import org.apache.syncope.core.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.core.provisioning.api.ConnectorRegistry;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceDataBinderImpl implements ResourceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceDataBinder.class);

    private static final String[] MAPPINGITEM_IGNORE_PROPERTIES = { "key", "mapping" };

    @Autowired
    private ConnectorRegistry connRegistry;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ExternalResource create(final ResourceTO resourceTO) {
        return update(entityFactory.newEntity(ExternalResource.class), resourceTO);
    }

    @Override
    public ExternalResource update(final ExternalResource resource, final ResourceTO resourceTO) {
        if (resourceTO == null) {
            return null;
        }

        resource.setKey(resourceTO.getKey());

        if (resourceTO.getConnectorId() != null) {
            ConnInstance connector = connInstanceDAO.find(resourceTO.getConnectorId());
            resource.setConnector(connector);

            if (!connector.getResources().contains(resource)) {
                connector.addResource(resource);
            }
        }

        resource.setEnforceMandatoryCondition(resourceTO.isEnforceMandatoryCondition());

        resource.setPropagationPrimary(resourceTO.isPropagationPrimary());

        resource.setPropagationPriority(resourceTO.getPropagationPriority());

        resource.setRandomPwdIfNotProvided(resourceTO.isRandomPwdIfNotProvided());

        resource.setPropagationMode(resourceTO.getPropagationMode());

        if (resourceTO.getUmapping() == null || resourceTO.getUmapping().getItems().isEmpty()) {
            resource.setUmapping(null);
        } else {
            UMapping mapping = entityFactory.newEntity(UMapping.class);
            mapping.setResource(resource);
            resource.setUmapping(mapping);
            populateMapping(resourceTO.getUmapping(), mapping, entityFactory.newEntity(UMappingItem.class));
        }
        if (resourceTO.getGmapping() == null || resourceTO.getGmapping().getItems().isEmpty()) {
            resource.setGmapping(null);
        } else {
            GMapping mapping = entityFactory.newEntity(GMapping.class);
            mapping.setResource(resource);
            resource.setGmapping(mapping);
            populateMapping(resourceTO.getGmapping(), mapping, entityFactory.newEntity(GMappingItem.class));
        }

        resource.setCreateTraceLevel(resourceTO.getCreateTraceLevel());
        resource.setUpdateTraceLevel(resourceTO.getUpdateTraceLevel());
        resource.setDeleteTraceLevel(resourceTO.getDeleteTraceLevel());
        resource.setSyncTraceLevel(resourceTO.getSyncTraceLevel());

        resource.setPasswordPolicy(resourceTO.getPasswordPolicy() == null
                ? null : (PasswordPolicy) policyDAO.find(resourceTO.getPasswordPolicy()));

        resource.setAccountPolicy(resourceTO.getAccountPolicy() == null
                ? null : (AccountPolicy) policyDAO.find(resourceTO.getAccountPolicy()));

        resource.setSyncPolicy(resourceTO.getSyncPolicy() == null
                ? null : (SyncPolicy) policyDAO.find(resourceTO.getSyncPolicy()));

        resource.setConnInstanceConfiguration(new HashSet<>(resourceTO.getConnConfProperties()));

        if (resourceTO.getUsyncToken() == null) {
            resource.setUsyncToken(null);
        }
        if (resourceTO.getRsyncToken() == null) {
            resource.setRsyncToken(null);
        }

        resource.getPropagationActionsClassNames().clear();
        resource.getPropagationActionsClassNames().addAll(resourceTO.getPropagationActionsClassNames());

        return resource;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void populateMapping(final MappingTO mappingTO, final Mapping mapping, final MappingItem prototype) {
        mapping.setAccountLink(mappingTO.getAccountLink());

        for (MappingItem item : getMappingItems(mappingTO.getItems(), prototype)) {
            item.setMapping(mapping);
            if (item.isAccountid()) {
                mapping.setAccountIdItem(item);
            } else if (item.isPassword()) {
                ((UMapping) mapping).setPasswordItem((UMappingItem) item);
            } else {
                mapping.addItem(item);
            }
        }
    }

    private Set<MappingItem> getMappingItems(final Collection<MappingItemTO> itemTOs, final MappingItem prototype) {
        Set<MappingItem> items = new HashSet<>(itemTOs.size());
        for (MappingItemTO itemTO : itemTOs) {
            items.add(getMappingItem(itemTO, prototype));
        }

        return items;
    }

    private MappingItem getMappingItem(final MappingItemTO itemTO, final MappingItem prototype) {
        if (itemTO == null || itemTO.getIntMappingType() == null) {
            LOG.error("Null mappingTO provided");
            return null;
        }

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        SyncopeClientException requiredValuesMissing = SyncopeClientException.build(
                ClientExceptionType.RequiredValuesMissing);

        if (itemTO.getIntAttrName() == null) {
            if (IntMappingType.getEmbedded().contains(itemTO.getIntMappingType())) {
                itemTO.setIntAttrName(itemTO.getIntMappingType().toString());
            } else {
                requiredValuesMissing.getElements().add("intAttrName");
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // no mandatory condition implies mandatory condition false
        if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                ? "false" : itemTO.getMandatoryCondition())) {

            SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                    ClientExceptionType.InvalidValues);
            invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        MappingItem item = SerializationUtils.clone(prototype);
        BeanUtils.copyProperties(itemTO, item, MAPPINGITEM_IGNORE_PROPERTIES);
        return item;
    }

    @Override
    public ConnInstance getConnInstance(final ResourceTO resourceTO) {
        ConnInstance connInstance = connInstanceDAO.find(resourceTO.getConnectorId());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + resourceTO.getConnectorId() + "'");
        }

        final ConnInstance connInstanceClone = SerializationUtils.clone(connInstance);
        return connRegistry.getOverriddenConnInstance(connInstanceClone, resourceTO.getConnConfProperties());
    }

    private void populateMappingTO(final Mapping<?> mapping, final MappingTO mappingTO) {
        mappingTO.setAccountLink(mapping.getAccountLink());

        for (MappingItem item : mapping.getItems()) {
            MappingItemTO itemTO = new MappingItemTO();
            itemTO.setKey(item.getKey());
            BeanUtils.copyProperties(item, itemTO, MAPPINGITEM_IGNORE_PROPERTIES);

            if (itemTO.isAccountid()) {
                mappingTO.setAccountIdItem(itemTO);
            } else if (itemTO.isPassword()) {
                mappingTO.setPasswordItem(itemTO);
            } else {
                mappingTO.addItem(itemTO);
            }
        }
    }

    @Override
    public ResourceTO getResourceTO(final ExternalResource resource) {
        if (resource == null) {
            return null;
        }

        ResourceTO resourceTO = new ResourceTO();

        // set sys info
        resourceTO.setCreator(resource.getCreator());
        resourceTO.setCreationDate(resource.getCreationDate());
        resourceTO.setLastModifier(resource.getLastModifier());
        resourceTO.setLastChangeDate(resource.getLastChangeDate());

        // set the resource name
        resourceTO.setKey(resource.getKey());

        // set the connector instance
        ConnInstance connector = resource.getConnector();

        resourceTO.setConnectorId(connector == null ? null : connector.getKey());
        resourceTO.setConnectorDisplayName(connector == null ? null : connector.getDisplayName());

        // set the mappings
        if (resource.getUmapping() != null) {
            MappingTO mappingTO = new MappingTO();
            resourceTO.setUmapping(mappingTO);
            populateMappingTO(resource.getUmapping(), mappingTO);
        }
        if (resource.getGmapping() != null) {
            MappingTO mappingTO = new MappingTO();
            resourceTO.setGmapping(mappingTO);
            populateMappingTO(resource.getGmapping(), mappingTO);
        }

        resourceTO.setEnforceMandatoryCondition(resource.isEnforceMandatoryCondition());

        resourceTO.setPropagationPrimary(resource.isPropagationPrimary());

        resourceTO.setPropagationPriority(resource.getPropagationPriority());

        resourceTO.setRandomPwdIfNotProvided(resource.isRandomPwdIfNotProvided());

        resourceTO.setPropagationMode(resource.getPropagationMode());

        resourceTO.setCreateTraceLevel(resource.getCreateTraceLevel());
        resourceTO.setUpdateTraceLevel(resource.getUpdateTraceLevel());
        resourceTO.setDeleteTraceLevel(resource.getDeleteTraceLevel());
        resourceTO.setSyncTraceLevel(resource.getSyncTraceLevel());

        resourceTO.setPasswordPolicy(resource.getPasswordPolicy() == null
                ? null : resource.getPasswordPolicy().getKey());

        resourceTO.setAccountPolicy(resource.getAccountPolicy() == null
                ? null : resource.getAccountPolicy().getKey());

        resourceTO.setSyncPolicy(resource.getSyncPolicy() == null
                ? null : resource.getSyncPolicy().getKey());

        resourceTO.getConnConfProperties().addAll(resource.getConnInstanceConfiguration());

        resourceTO.setUsyncToken(resource.getSerializedUSyncToken());
        resourceTO.setRsyncToken(resource.getSerializedRSyncToken());

        resourceTO.getPropagationActionsClassNames().addAll(resource.getPropagationActionsClassNames());

        return resourceTO;
    }
}
