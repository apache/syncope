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
package org.syncope.core.persistence.dao.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.types.SchemaType;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl
        implements SchemaDAO {

    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> T find(final String name,
            final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractSchema> T save(T schema)
            throws MultiUniqueValueException {

        if (schema.isMultivalue() && schema.isUniquevalue()) {
            throw new MultiUniqueValueException(schema);
        }

        return entityManager.merge(schema);
    }

    @Override
    public <T extends AbstractSchema> void delete(
            String name, Class<T> reference) {

        T schema = find(name, reference);

        if (schema == null) {
            return;
        }

        // --------------------------------------
        // Remove all mappings
        // --------------------------------------
        List<SchemaMapping> mappings = schema.getMappings();
        schema.setMappings(Collections.EMPTY_LIST);

        for (SchemaMapping mapping : mappings) {
            removeMapping(mapping.getId());
        }
        // --------------------------------------

        for (AbstractDerivedSchema derivedSchema : schema.getDerivedSchemas()) {
            derivedSchema.removeSchema(schema);
        }

        schema.setDerivedSchemas(Collections.EMPTY_LIST);

        for (AbstractAttribute attribute : schema.getAttributes()) {
            attribute.setSchema(null);
            attributeDAO.delete(attribute.getId(), attribute.getClass());
        }
        schema.setAttributes(Collections.EMPTY_LIST);

        entityManager.remove(schema);
    }

    @Override
    @Transactional(readOnly = true)
    public SchemaMapping findMapping(Long id) {
        return entityManager.find(SchemaMapping.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemaMapping> findAllMappings() {
        Query query =
                entityManager.createQuery("SELECT e FROM SchemaMapping e");
        return query.getResultList();
    }

    @Override
    public SchemaMapping saveMapping(SchemaMapping mapping) {
        return entityManager.merge(mapping);
    }

    @Override
    public void removeMapping(Long mappingId) {

        // Get mapping object
        SchemaMapping mapping = findMapping(mappingId);

        if (mapping == null) {
            return;
        }

        // --------------------------------------
        // Synchronize schema
        // --------------------------------------
        String schemaName = mapping.getSchemaName();
        SchemaType schemaType = mapping.getSchemaType();

        try {
            // check for schema type
            schemaType.getSchemaClass().asSubclass(AbstractSchema.class);

            /**
             * Schema type could be:
             * * UserSchema
             * * RoleSchema
             * * MembershipSchema
             */
            if (log.isDebugEnabled()) {
                log.debug("Schema type " + schemaType.getClassName());
            }
            AbstractSchema schema = find(schemaName,
                    schemaType.getSchemaClass());

            if (schema != null) {
                schema.removeMapping(mapping);
            }

        } catch (ClassCastException e) {
            /**
             * Schema type could be:
             * * AccountId
             * * Password
             */
            if (log.isDebugEnabled()) {
                log.debug("Wrong schema type " + schemaType.getClassName());
            }
        }
        // --------------------------------------

        // --------------------------------------
        // Synchronize resource
        // --------------------------------------
        TargetResource resource =
                resourceDAO.find(mapping.getResource().getName());

        if (resource != null) {
            resource.removeMapping(mapping);
        }

        mapping.setResource(null);
        // --------------------------------------

        // Remove mapping
        entityManager.remove(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMandatoryOnResource(
            AbstractSchema schema, TargetResource resource) {

        Query query = entityManager.createQuery(
                "SELECT e "
                + "FROM SchemaMapping e "
                + "WHERE e.schemaName='" + schema.getName() + "' "
                + "AND e.resource.name='" + resource.getName() + "' "
                + "AND e.nullable='F'");

        return schema.isMandatory()
                || (resource.isForceMandatoryConstraint()
                && !query.getResultList().isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMandatoryOnResource(
            AbstractSchema schema, Set<TargetResource> resources) {

        StringBuilder queryBuilder = new StringBuilder();

        for (TargetResource resource : resources) {
            if (resource.isForceMandatoryConstraint()) {
                queryBuilder.append(queryBuilder.length() > 0 ? " OR " : "");
                queryBuilder.append("e.resource.name='");
                queryBuilder.append(resource.getName());
                queryBuilder.append("'");
            }
        }

        Query query = null;
        if (queryBuilder.length() > 0) {
            query = entityManager.createQuery(
                    "SELECT e "
                    + "FROM SchemaMapping e "
                    + "WHERE e.schemaName='" + schema.getName() + "' "
                    + "AND (" + queryBuilder.toString() + ") "
                    + "AND e.nullable='F'");
        }

        return query != null && !query.getResultList().isEmpty();
    }
}
