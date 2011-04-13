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
import java.util.List;
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
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ResourceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ResourceDataBinder.class);

    private static final String[] MAPPING_IGNORE_PROPERTIES = {
        "id", "resource"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    @Autowired
    private JexlUtil jexlUtil;

    public TargetResource getResource(ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        return getResource(new TargetResource(), resourceTO);
    }

    public TargetResource getResource(TargetResource resource,
            ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (resourceTO == null) {
            return null;
        }

        if (resourceTO.getName() == null) {
            requiredValuesMissing.addElement("name");
        }

        ConnInstance connector = null;

        if (resourceTO.getConnectorId() != null) {
            connector = connectorInstanceDAO.find(resourceTO.getConnectorId());
        }

        if (connector == null) {
            requiredValuesMissing.addElement("connector");
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        resource.setName(resourceTO.getName());

        resource.setConnector(connector);
        connector.addResource(resource);

        resource.setForceMandatoryConstraint(
                resourceTO.isForceMandatoryConstraint());

        resource.setOptionalPropagationMode(
                resourceTO.getOptionalPropagationMode());

        resource.setMappings(
                getSchemaMappings(resource, resourceTO.getMappings()));

        resource.setAccountLink(resourceTO.getAccountLink());

        return resource;
    }

    public List<ResourceTO> getResourceTOs(
            Collection<TargetResource> resources) {

        if (resources == null) {
            return null;
        }

        List<ResourceTO> resourceTOs = new ArrayList<ResourceTO>();
        for (TargetResource resource : resources) {
            resourceTOs.add(getResourceTO(resource));
        }

        return resourceTOs;
    }

    public ResourceTO getResourceTO(TargetResource resource) {

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

        resourceTO.setOptionalPropagationMode(
                resource.getOptionalPropagationMode());

        return resourceTO;
    }

    private List<SchemaMapping> getSchemaMappings(
            TargetResource resource,
            List<SchemaMappingTO> mappings) {

        if (mappings == null) {
            return null;
        }

        List<SchemaMapping> schemaMappings = new ArrayList<SchemaMapping>();
        for (SchemaMappingTO mapping : mappings) {
            schemaMappings.add(getSchemaMapping(resource, mapping));
        }

        return schemaMappings;
    }

    private SchemaMapping getSchemaMapping(
            TargetResource resource,
            SchemaMappingTO mappingTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (mappingTO == null) {
            LOG.error("Null mappingTO provided");

            return null;
        }

        if (mappingTO.getSourceAttrName() == null) {
            requiredValuesMissing.addElement("sourceAttrName");
        }
        if (mappingTO.getDestAttrName() == null) {
            requiredValuesMissing.addElement("destAttrName");
        }
        if (mappingTO.getSourceMappingType() == null) {
            requiredValuesMissing.addElement("sourceMappingType");
        }
        if (mappingTO.getMandatoryCondition() == null) {
            requiredValuesMissing.addElement("mandatoryCondition");
        }

        // a resource must be provided
        if (resource == null) {
            requiredValuesMissing.addElement("resource");
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (!jexlUtil.isExpressionValid(mappingTO.getMandatoryCondition())) {
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
