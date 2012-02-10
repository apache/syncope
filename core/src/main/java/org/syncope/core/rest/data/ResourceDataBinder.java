/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.IntMappingType;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ResourceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ResourceDataBinder.class);

    private static final String[] MAPPING_IGNORE_PROPERTIES = {
        "id", "resource", "syncToken"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    @Autowired
    private JexlUtil jexlUtil;

    @Autowired
    private PolicyDAO policyDAO;

    public ExternalResource create(final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        return update(new ExternalResource(), resourceTO);
    }

    public ExternalResource update(final ExternalResource resource,
            final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        if (resourceTO == null) {
            return null;
        }

        resource.setName(resourceTO.getName());

        if (resourceTO.getConnectorId() != null) {
            ConnInstance connector =
                    connectorInstanceDAO.find(resourceTO.getConnectorId());
            resource.setConnector(connector);
        }

        resource.setForceMandatoryConstraint(
                resourceTO.isForceMandatoryConstraint());

        resource.setPropagationPrimary(resourceTO.isPropagationPrimary());

        resource.setPropagationPriority(resourceTO.getPropagationPriority());

        resource.setPropagationMode(
                resourceTO.getPropagationMode());

        resource.setMappings(
                getSchemaMappings(resource, resourceTO.getMappings()));

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

        resource.setConnectorConfigurationProperties(
                new HashSet<ConnConfProperty>(
                resourceTO.getConnConfProperties()));

        if (resourceTO.getSyncToken() == null) {
            resource.setSerializedSyncToken(null);
        }

        return resource;
    }

    public List<ResourceTO> getResourceTOs(
            Collection<ExternalResource> resources) {

        if (resources == null) {
            return null;
        }

        List<ResourceTO> resourceTOs = new ArrayList<ResourceTO>();
        for (ExternalResource resource : resources) {
            resourceTOs.add(getResourceTO(resource));
        }

        return resourceTOs;
    }

    public ResourceTO getResourceTO(ExternalResource resource) {

        if (resource == null) {
            return null;
        }

        ResourceTO resourceTO = new ResourceTO();

        // set the resource name
        resourceTO.setName(resource.getName());

        // set the connector instance
        ConnInstance connector = resource.getConnector();

        resourceTO.setConnectorId(
                connector != null ? connector.getId() : null);

        // set the mappings
        resourceTO.setMappings(getSchemaMappingTOs(resource.getMappings()));

        resourceTO.setAccountLink(resource.getAccountLink());

        resourceTO.setForceMandatoryConstraint(
                resource.isForceMandatoryConstraint());

        resourceTO.setPropagationPrimary(resource.isPropagationPrimary());

        resourceTO.setPropagationPriority(resource.getPropagationPriority());

        resourceTO.setPropagationMode(
                resource.getPropagationMode());

        resourceTO.setCreateTraceLevel(resource.getCreateTraceLevel());
        resourceTO.setUpdateTraceLevel(resource.getUpdateTraceLevel());
        resourceTO.setDeleteTraceLevel(resource.getDeleteTraceLevel());
        resourceTO.setSyncTraceLevel(resource.getSyncTraceLevel());

        resourceTO.setPasswordPolicy(resource.getPasswordPolicy() != null
                ? resource.getPasswordPolicy().getId() : null);

        resourceTO.setAccountPolicy(resource.getAccountPolicy() != null
                ? resource.getAccountPolicy().getId() : null);

        resourceTO.setSyncPolicy(resource.getSyncPolicy() != null
                ? resource.getSyncPolicy().getId() : null);

        resourceTO.setConnectorConfigurationProperties(
                resource.getConfiguration());
        resourceTO.setSyncToken(resource.getSerializedSyncToken());

        return resourceTO;
    }

    private Set<SchemaMapping> getSchemaMappings(
            ExternalResource resource, List<SchemaMappingTO> mappings) {

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

    private SchemaMapping getSchemaMapping(ExternalResource resource,
            SchemaMappingTO mappingTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
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
                    mappingTO.setIntAttrName(
                            IntMappingType.SyncopeUserId.toString());
                    break;

                case Password:
                    mappingTO.setIntAttrName(
                            IntMappingType.Password.toString());
                    break;

                case Username:
                    mappingTO.setIntAttrName(
                            IntMappingType.Username.toString());
                    break;

                default:
                    requiredValuesMissing.addElement("intAttrName");
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // no mandatory condition implies mandatory condition false
        if (!jexlUtil.isExpressionValid(
                mappingTO.getMandatoryCondition() != null
                ? mappingTO.getMandatoryCondition() : "false")) {

            SyncopeClientException invalidMandatoryCondition =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(
                    mappingTO.getMandatoryCondition());

            compositeErrorException.addException(invalidMandatoryCondition);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        SchemaMapping mapping = new SchemaMapping();

        BeanUtils.copyProperties(mappingTO, mapping, MAPPING_IGNORE_PROPERTIES);

        mapping.setResource(resource);

        return mapping;
    }

    public List<SchemaMappingTO> getSchemaMappingTOs(
            Collection<SchemaMapping> mappings) {

        if (mappings == null) {
            LOG.error("No mapping provided.");

            return null;
        }

        List<SchemaMappingTO> schemaMappingTOs =
                new ArrayList<SchemaMappingTO>();
        for (SchemaMapping mapping : mappings) {
            LOG.debug("Asking for TO for {}", mapping);

            schemaMappingTOs.add(getSchemaMappingTO(mapping));
        }

        LOG.debug("Collected TOs: {}", schemaMappingTOs);

        return schemaMappingTOs;
    }

    public SchemaMappingTO getSchemaMappingTO(SchemaMapping schemaMapping) {
        if (schemaMapping == null) {
            LOG.error("Provided null mapping");

            return null;
        }

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();

        BeanUtils.copyProperties(
                schemaMapping, schemaMappingTO, MAPPING_IGNORE_PROPERTIES);

        schemaMappingTO.setId(schemaMapping.getId());

        LOG.debug("Obtained SchemaMappingTO {}", schemaMappingTO);

        return schemaMappingTO;
    }
}
