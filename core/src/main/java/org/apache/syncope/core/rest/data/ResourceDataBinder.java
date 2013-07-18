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
package org.apache.syncope.core.rest.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.beans.role.RMapping;
import org.apache.syncope.core.persistence.beans.role.RMappingItem;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.util.JexlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResourceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceDataBinder.class);

    private static final String[] MAPPINGITEM_IGNORE_PROPERTIES = {"id", "mapping"};

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private JexlUtil jexlUtil;

    @Autowired
    private PolicyDAO policyDAO;

    public ExternalResource create(final ResourceTO resourceTO) {
        return update(new ExternalResource(), resourceTO);
    }

    public ExternalResource update(final ExternalResource resource, final ResourceTO resourceTO) {
        if (resourceTO == null) {
            return null;
        }

        resource.setName(resourceTO.getName());

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
            UMapping mapping = new UMapping();
            mapping.setResource(resource);
            resource.setUmapping(mapping);
            populateMapping(resourceTO.getUmapping(), mapping, new UMappingItem());
        }
        if (resourceTO.getRmapping() == null || resourceTO.getRmapping().getItems().isEmpty()) {
            resource.setRmapping(null);
        } else {
            RMapping mapping = new RMapping();
            mapping.setResource(resource);
            resource.setRmapping(mapping);
            populateMapping(resourceTO.getRmapping(), mapping, new RMappingItem());
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

        resource.setConnInstanceConfiguration(new HashSet<ConnConfProperty>(resourceTO.getConnConfProperties()));

        if (resourceTO.getUsyncToken() == null) {
            resource.setUserializedSyncToken(null);
        }
        if (resourceTO.getRsyncToken() == null) {
            resource.setRserializedSyncToken(null);
        }

        resource.setPropagationActionsClassName(resourceTO.getPropagationActionsClassName());

        return resource;
    }

    private void populateMapping(final MappingTO mappingTO, final AbstractMapping mapping,
            final AbstractMappingItem prototype) {

        mapping.setAccountLink(mappingTO.getAccountLink());

        for (AbstractMappingItem item : getMappingItems(mappingTO.getItems(), prototype)) {
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

    private Set<AbstractMappingItem> getMappingItems(final Collection<MappingItemTO> itemTOs,
            final AbstractMappingItem prototype) {

        Set<AbstractMappingItem> items = new HashSet<AbstractMappingItem>(itemTOs.size());
        for (MappingItemTO itemTO : itemTOs) {
            items.add(getMappingItem(itemTO, prototype));
        }

        return items;
    }

    private AbstractMappingItem getMappingItem(final MappingItemTO itemTO, final AbstractMappingItem prototype) {
        if (itemTO == null || itemTO.getIntMappingType() == null) {
            LOG.error("Null mappingTO provided");
            return null;
        }

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (itemTO.getIntAttrName() == null) {
            if (IntMappingType.getEmbedded().contains(itemTO.getIntMappingType())) {
                itemTO.setIntAttrName(itemTO.getIntMappingType().toString());
            } else {
                requiredValuesMissing.addElement("intAttrName");
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // no mandatory condition implies mandatory condition false
        if (!jexlUtil.isExpressionValid(itemTO.getMandatoryCondition() == null
                ? "false" : itemTO.getMandatoryCondition())) {

            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);

            invalidMandatoryCondition.addElement(itemTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        AbstractMappingItem item = (AbstractMappingItem) SerializationUtils.clone(prototype);
        BeanUtils.copyProperties(itemTO, item, MAPPINGITEM_IGNORE_PROPERTIES);
        return item;
    }

    public ConnInstance getConnInstance(final ExternalResource resource) {
        final ConnInstance connInstanceClone = (ConnInstance) SerializationUtils.clone(resource.getConnector());

        return getConnInstance(connInstanceClone, resource.getConnInstanceConfiguration());
    }

    public ConnInstance getConnInstance(final ResourceTO resourceTO) {
        ConnInstance connInstance = connInstanceDAO.find(resourceTO.getConnectorId());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + resourceTO.getConnectorId() + "'");
        }

        final ConnInstance connInstanceClone = (ConnInstance) SerializationUtils.clone(connInstance);
        return getConnInstance(connInstanceClone, resourceTO.getConnConfProperties());
    }

    private ConnInstance getConnInstance(final ConnInstance connInstance, final Set<ConnConfProperty> overridden) {
        final Set<ConnConfProperty> configuration = new HashSet<ConnConfProperty>();
        final Map<String, ConnConfProperty> overridable = new HashMap<String, ConnConfProperty>();

        // add not overridable properties
        for (ConnConfProperty prop : connInstance.getConfiguration()) {
            if (prop.isOverridable()) {
                overridable.put(prop.getSchema().getName(), prop);
            } else {
                configuration.add(prop);
            }
        }

        // add overridden properties
        for (ConnConfProperty prop : overridden) {
            if (overridable.containsKey(prop.getSchema().getName()) && !prop.getValues().isEmpty()) {
                configuration.add(prop);
                overridable.remove(prop.getSchema().getName());
            }
        }

        // add overridable properties not overridden
        configuration.addAll(overridable.values());

        connInstance.setConfiguration(configuration);

        return connInstance;
    }

    public List<ResourceTO> getResourceTOs(final Collection<ExternalResource> resources) {
        List<ResourceTO> resourceTOs = new ArrayList<ResourceTO>();
        for (ExternalResource resource : resources) {
            resourceTOs.add(getResourceTO(resource));
        }

        return resourceTOs;
    }

    public ResourceTO getResourceTO(final ExternalResource resource) {
        if (resource == null) {
            return null;
        }

        ResourceTO resourceTO = new ResourceTO();

        // set the resource name
        resourceTO.setName(resource.getName());

        // set the connector instance
        ConnInstance connector = resource.getConnector();

        resourceTO.setConnectorId(connector == null ? null : connector.getId());
        resourceTO.setConnectorDisplayName(connector == null ? null : connector.getDisplayName());

        // set the mappings
        if (resource.getUmapping() != null) {
            MappingTO mappingTO = new MappingTO();
            resourceTO.setUmapping(mappingTO);
            populateMappingTO(resource.getUmapping(), mappingTO);
        }
        if (resource.getRmapping() != null) {
            MappingTO mappingTO = new MappingTO();
            resourceTO.setRmapping(mappingTO);
            populateMappingTO(resource.getRmapping(), mappingTO);
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
                ? null : resource.getPasswordPolicy().getId());

        resourceTO.setAccountPolicy(resource.getAccountPolicy() == null
                ? null : resource.getAccountPolicy().getId());

        resourceTO.setSyncPolicy(resource.getSyncPolicy() == null
                ? null : resource.getSyncPolicy().getId());

        resourceTO.setConnConfProperties(resource.getConnInstanceConfiguration());

        resourceTO.setUsyncToken(resource.getUserializedSyncToken());
        resourceTO.setRsyncToken(resource.getRserializedSyncToken());

        resourceTO.setPropagationActionsClassName(resource.getPropagationActionsClassName());

        return resourceTO;
    }

    private void populateMappingTO(final AbstractMapping mapping, final MappingTO mappingTO) {
        mappingTO.setAccountLink(mapping.getAccountLink());

        for (MappingItemTO itemTO : getMappingItemTOs(mapping.getItems())) {
            if (itemTO.isAccountid()) {
                mappingTO.setAccountIdItem(itemTO);
            } else if (itemTO.isPassword()) {
                mappingTO.setPasswordItem(itemTO);
            } else {
                mappingTO.addItem(itemTO);
            }
        }
    }

    private Set<MappingItemTO> getMappingItemTOs(final Collection<AbstractMappingItem> items) {
        Set<MappingItemTO> mappingTOs = new HashSet<MappingItemTO>();
        for (AbstractMappingItem item : items) {
            LOG.debug("Asking for TO for {}", item);
            mappingTOs.add(getMappingItemTO(item));
        }

        LOG.debug("Collected TOs: {}", mappingTOs);

        return mappingTOs;
    }

    private MappingItemTO getMappingItemTO(final AbstractMappingItem item) {
        if (item == null) {
            LOG.error("Provided null mapping");

            return null;
        }

        MappingItemTO itemTO = new MappingItemTO();

        BeanUtils.copyProperties(item, itemTO, MAPPINGITEM_IGNORE_PROPERTIES);

        itemTO.setId(item.getId());

        LOG.debug("Obtained SchemaMappingTO {}", itemTO);

        return itemTO;
    }
}
