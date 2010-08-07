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
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.membership.MembershipSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl
        implements SchemaDAO {

    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private SchemaMappingDAO schemaMappingDAO;

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> T find(String name, Class<T> reference) {
        return entityManager.find(reference, name);
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> List<T> findAll(Class<T> reference) {
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

        for (AbstractDerivedSchema derivedSchema : schema.getDerivedSchemas()) {
            derivedSchema.removeSchema(schema);
        }
        schema.setDerivedSchemas(Collections.EMPTY_SET);

        for (AbstractAttribute attribute : schema.getAttributes()) {
            attribute.setSchema(null);
            attributeDAO.delete(attribute.getId(), attribute.getClass());
        }
        schema.setAttributes(Collections.EMPTY_SET);

        for (SchemaMapping schemaMapping : schema.getMappings()) {
            if (schema instanceof UserSchema) {
                schemaMapping.setUserSchema(null);
            }
            if (schema instanceof RoleSchema) {
                schemaMapping.setRoleSchema(null);
            }
            if (schema instanceof MembershipSchema) {
                schemaMapping.setMembershipSchema(null);
            }

            schemaMappingDAO.delete(schemaMapping.getId());
        }
        schema.setMappings(Collections.EMPTY_SET);

        entityManager.remove(schema);
    }
}
