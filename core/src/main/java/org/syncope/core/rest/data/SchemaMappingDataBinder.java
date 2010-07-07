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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class SchemaMappingDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaMappingDataBinder.class);

    private static final String[] ignoreProperties = {
        "id", "userSchema", "roleSchema", "resource"};

    private SchemaDAO schemaDAO;

    @Autowired
    public SchemaMappingDataBinder(SchemaDAO schemaDAO) {
        this.schemaDAO = schemaDAO;
    }

    public Set<SchemaMapping> getSchemaMappings(
            Resource resource,
            SchemaMappingTOs mappings) {

        Set<SchemaMapping> schemaMappings = new HashSet<SchemaMapping>();

        for (SchemaMappingTO mapping : mappings) {

            schemaMappings.add(getSchemaMapping(resource, mapping));

        }

        return schemaMappings;
    }

    public SchemaMapping getSchemaMapping(
            Resource resource,
            SchemaMappingTO mapping)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValueMissing);

        if (mapping == null) {
            if (log.isErrorEnabled()) {
                log.error("Provided null mapping");
            }

            return null;
        }

        if (mapping.getField() == null) {
            requiredValuesMissing.addElement("field");
        }

        // search for the user schema
        UserSchema userSchema = null;
        if (mapping.getUserSchema() != null) {
            userSchema = schemaDAO.find(
                    mapping.getUserSchema(), UserSchema.class);
        }

        // search for the role schema
        RoleSchema roleSchema = null;
        if (mapping.getRoleSchema() != null) {
            roleSchema = schemaDAO.find(
                    mapping.getUserSchema(), RoleSchema.class);
        }

        // at least one schema must be provided
        if (userSchema == null && roleSchema == null) {
            requiredValuesMissing.addElement("schema");
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
                mapping, schemaMapping, ignoreProperties);

        schemaMapping.setResource(resource);
        schemaMapping.setUserSchema(userSchema);
        schemaMapping.setRoleSchema(roleSchema);

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
                schemaMapping, schemaMappingTO, ignoreProperties);

        if (schemaMapping.getUserSchema() != null) {
            schemaMappingTO.setUserSchema(
                    schemaMapping.getUserSchema().getName());
        }

        if (schemaMapping.getRoleSchema() != null) {
            schemaMappingTO.setRoleSchema(
                    schemaMapping.getRoleSchema().getName());
        }

        schemaMappingTO.setId(schemaMapping.getId());

        if (log.isDebugEnabled()) {
            log.debug("Obtained TO " + schemaMappingTO);
        }

        return schemaMappingTO;
    }
}
