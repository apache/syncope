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
import org.apache.commons.lang.SerializationUtils;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.to.ResourceTO;
import org.apache.syncope.to.SchemaMappingTO;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;
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

    private static final String[] MAPPING_IGNORE_PROPERTIES = {"id", "resource", "syncToken"};

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private JexlUtil jexlUtil;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ConnInstanceDataBinder connInstancebinder;

    public ExternalResource create(final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        return update(new ExternalResource(), resourceTO);
    }

    public ExternalResource update(final ExternalResource resource, final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

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

        resource.setPropagationMode(resourceTO.getPropagationMode());

        resource.setMappings(getSchemaMappings(resource, resourceTO.getMappings()));

        resource.setAccountLink(resourceTO.getAccountLink());

        resource.setCreateTraceLevel(resourceTO.getCreateTraceLevel());
        resource.setUpdateTraceLevel(resourceTO.getUpdateTraceLevel());
        resource.setDeleteTraceLevel(resourceTO.getDeleteTraceLevel());
        resource.setSyncTraceLevel(resourceTO.getSyncTraceLevel());

        resource.setPasswordPolicy(resourceTO.getPasswordPolicy() != null
                ? (PasswordPolicy) policyDAO.find(resourceTO.getPasswordPolicy())
                : null);

        resource.setAccountPolicy(resourceTO.getAccountPolicy() != null
                ? (AccountPolicy) policyDAO.find(resourceTO.getAccountPolicy())
                : null);

        resource.setSyncPolicy(resourceTO.getSyncPolicy() != null
                ? (SyncPolicy) policyDAO.find(resourceTO.getSyncPolicy())
                : null);

        resource.setConnectorConfigurationProperties(new HashSet<ConnConfProperty>(resourceTO.getConnConfProperties()));

        if (resourceTO.getSyncToken() == null) {
            resource.setSerializedSyncToken(null);
        }

        resource.setActionsClassName(resourceTO.getActionsClassName());

        return resource;
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

        resourceTO.setConnectorId(connector != null
                ? connector.getId()
                : null);

        // set the mappings
        resourceTO.setMappings(getSchemaMappingTOs(resource.getMappings()));

        resourceTO.setAccountLink(resource.getAccountLink());

        resourceTO.setEnforceMandatoryCondition(resource.isEnforceMandatoryCondition());

        resourceTO.setPropagationPrimary(resource.isPropagationPrimary());

        resourceTO.setPropagationPriority(resource.getPropagationPriority());

        resourceTO.setPropagationMode(resource.getPropagationMode());

        resourceTO.setCreateTraceLevel(resource.getCreateTraceLevel());
        resourceTO.setUpdateTraceLevel(resource.getUpdateTraceLevel());
        resourceTO.setDeleteTraceLevel(resource.getDeleteTraceLevel());
        resourceTO.setSyncTraceLevel(resource.getSyncTraceLevel());

        resourceTO.setPasswordPolicy(resource.getPasswordPolicy() != null
                ? resource.getPasswordPolicy().getId()
                : null);

        resourceTO.setAccountPolicy(resource.getAccountPolicy() != null
                ? resource.getAccountPolicy().getId()
                : null);

        resourceTO.setSyncPolicy(resource.getSyncPolicy() != null
                ? resource.getSyncPolicy().getId()
                : null);

        resourceTO.setConnectorConfigurationProperties(resource.getConfiguration());
        resourceTO.setSyncToken(resource.getSerializedSyncToken());

        resourceTO.setActionsClassName(resource.getActionsClassName());

        return resourceTO;
    }

    private Set<SchemaMapping> getSchemaMappings(final ExternalResource resource,
            final List<SchemaMappingTO> mappings) {

        if (mappings == null) {
            return null;
        }

        final Set<SchemaMapping> schemaMappings = new HashSet<SchemaMapping>();

        SchemaMapping schemaMapping;
        for (SchemaMappingTO mapping : mappings) {
            schemaMapping = getSchemaMapping(resource, mapping);
            if (schemaMapping != null) {
                schemaMappings.add(schemaMapping);
            }
        }

        return schemaMappings;
    }

    private SchemaMapping getSchemaMapping(final ExternalResource resource, final SchemaMappingTO mappingTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        // this control needs to be free to get schema names
        // without a complete/good resourceTO object
        if (mappingTO == null || mappingTO.getIntMappingType() == null) {
            LOG.error("Null mappingTO provided");
            return null;
        }

        if (mappingTO.getIntAttrName() == null) {
            switch (mappingTO.getIntMappingType()) {
                case SyncopeUserId:
                    mappingTO.setIntAttrName(IntMappingType.SyncopeUserId.toString());
                    break;

                case Password:
                    mappingTO.setIntAttrName(IntMappingType.Password.toString());
                    break;

                case Username:
                    mappingTO.setIntAttrName(IntMappingType.Username.toString());
                    break;

                default:
                    requiredValuesMissing.addElement("intAttrName");
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // no mandatory condition implies mandatory condition false
        if (!jexlUtil.isExpressionValid(mappingTO.getMandatoryCondition() != null
                ? mappingTO.getMandatoryCondition()
                : "false")) {

            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);

            invalidMandatoryCondition.addElement(mappingTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        SchemaMapping mapping = new SchemaMapping();

        BeanUtils.copyProperties(mappingTO, mapping, MAPPING_IGNORE_PROPERTIES);

        mapping.setResource(resource);

        return mapping;
    }

    public List<SchemaMappingTO> getSchemaMappingTOs(final Collection<SchemaMapping> mappings) {
        if (mappings == null) {
            LOG.error("No mapping provided.");

            return null;
        }

        List<SchemaMappingTO> schemaMappingTOs = new ArrayList<SchemaMappingTO>();
        for (SchemaMapping mapping : mappings) {
            LOG.debug("Asking for TO for {}", mapping);

            schemaMappingTOs.add(getSchemaMappingTO(mapping));
        }

        LOG.debug("Collected TOs: {}", schemaMappingTOs);

        return schemaMappingTOs;
    }

    public SchemaMappingTO getSchemaMappingTO(final SchemaMapping schemaMapping) {
        if (schemaMapping == null) {
            LOG.error("Provided null mapping");

            return null;
        }

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();

        BeanUtils.copyProperties(schemaMapping, schemaMappingTO, MAPPING_IGNORE_PROPERTIES);

        schemaMappingTO.setId(schemaMapping.getId());

        LOG.debug("Obtained SchemaMappingTO {}", schemaMappingTO);

        return schemaMappingTO;
    }

    public ConnInstance getConnInstance(final ExternalResource resource) {
        final ConnInstance connInstanceClone = (ConnInstance) SerializationUtils.clone(resource.getConnector());

        return getConnInstance(connInstanceClone, resource.getConfiguration());
    }

    public ConnInstance getConnInstance(final ResourceTO resourceTO)
            throws NotFoundException {

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
}
