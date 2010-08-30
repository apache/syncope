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
import org.syncope.client.to.ResourceTOs;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.types.SchemaType;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ResourceDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            ResourceDataBinder.class);

    private static final String[] ignoreMappingProperties = {
        "id", "resource"};

    private SchemaDAO schemaDAO;

    private ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    public ResourceDataBinder(
            SchemaDAO schemaDAO, ConnectorInstanceDAO connectorInstanceDAO) {

        this.schemaDAO = schemaDAO;
        this.connectorInstanceDAO = connectorInstanceDAO;
    }

    public TargetResource getResource(ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        return getResource(new TargetResource(), resourceTO);
    }

    public TargetResource getResource(TargetResource resource, ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (resourceTO == null) return null;

        if (resourceTO.getName() == null) {
            requiredValuesMissing.addElement("name");
        }

        ConnectorInstance connector = null;

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

        resource.setMappings(
                getSchemaMappings(resource, resourceTO.getMappings()));

        resource.setConnector(connector);
        connector.addResource(resource);

        return resource;
    }

    public ResourceTOs getResourceTOs(Collection<TargetResource> resources) {

        if (resources == null) return null;

        ResourceTOs resourceTOs = new ResourceTOs();

        for (TargetResource resource : resources) {
            resourceTOs.addResource(getResourceTO(resource));
        }

        return resourceTOs;
    }

    public ResourceTO getResourceTO(TargetResource resource) {

        if (resource == null) return null;

        ResourceTO resourceTO = new ResourceTO();

        // set the resource name
        resourceTO.setName(resource.getName());

        // set the connector instance
        ConnectorInstance connector = resource.getConnector();

        resourceTO.setConnectorId(
                connector != null ? connector.getId() : null);

        // set the mappings
        resourceTO.setMappings(getSchemaMappingTOs(resource.getMappings()));

        return resourceTO;
    }

    public List<SchemaMapping> getSchemaMappings(
            TargetResource resource,
            SchemaMappingTOs mappings) {

        if (mappings == null) return null;

        List<SchemaMapping> schemaMappings = new ArrayList<SchemaMapping>();

        for (SchemaMappingTO mapping : mappings) {

            schemaMappings.add(getSchemaMapping(resource, mapping));

        }

        return schemaMappings;
    }

    public SchemaMapping getSchemaMapping(
            TargetResource resource,
            SchemaMappingTO mapping)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (mapping == null) {
            if (log.isErrorEnabled()) {
                log.error("Provided null mapping");
            }

            return null;
        }

        if (mapping.getSchemaName() == null) {
            requiredValuesMissing.addElement("schema");
        }

        if (mapping.getField() == null) {
            requiredValuesMissing.addElement("field");
        }

        if (mapping.getSchemaType() == null) {
            requiredValuesMissing.addElement("type");
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

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        SchemaMapping schemaMapping = new SchemaMapping();

        BeanUtils.copyProperties(
                mapping, schemaMapping, ignoreMappingProperties);

        schemaMapping.setResource(resource);

        if (log.isInfoEnabled()) {
            log.info("Save mapping " + mapping);
        }

        schemaDAO.saveMapping(schemaMapping);

        SchemaType schemaType = mapping.getSchemaType();

        try {
            schemaType.getSchemaType().asSubclass(AbstractSchema.class);


            // search for the attribute schema
            AbstractSchema schema = schemaDAO.find(
                    mapping.getSchemaName(),
                    mapping.getSchemaType().getSchemaType());

            if (schema != null)
                schema.addMapping(schemaMapping);


            if (log.isInfoEnabled()) {
                log.info("Merge schema " + schema);
            }

            schemaDAO.save(schema);

        } catch (ClassCastException e) {
            // no real schema provided
            if (log.isDebugEnabled()) {
                log.debug("Wrong schema type " + schemaType.getClassName());
            }
        } catch (MultiUniqueValueException e) {
            log.error("Error during schema persistence", e);
        }

        return schemaMapping;
    }

    public SchemaMappingTOs getSchemaMappingTOs(
            Collection<SchemaMapping> mappings) {

        if (mappings == null) {
            if (log.isErrorEnabled()) {
                log.error("No mapping provided.");
            }

            return null;
        }

        SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();

        for (SchemaMapping mapping : mappings) {
            if (log.isDebugEnabled()) {
                log.debug("Ask for " + mapping + " TO");
            }

            schemaMappingTOs.addMapping(getSchemaMappingTO(mapping));
        }

        if (log.isDebugEnabled()) {
            log.debug("Collected TOs " + schemaMappingTOs.getMappings());
        }

        return schemaMappingTOs;
    }

    public SchemaMappingTO getSchemaMappingTO(
            SchemaMapping schemaMapping) {
        if (schemaMapping == null) {
            if (log.isErrorEnabled()) {
                log.error("Provided null mapping");
            }

            return null;
        }

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();

        BeanUtils.copyProperties(
                schemaMapping, schemaMappingTO, ignoreMappingProperties);

        schemaMappingTO.setId(schemaMapping.getId());

        if (log.isDebugEnabled()) {
            log.debug("Obtained TO " + schemaMappingTO);
        }

        return schemaMappingTO;
    }
}
