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

import java.util.List;
import java.util.Set;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaMappingDAO;

@Repository
public class SchemaMappingDAOImpl extends AbstractDAOImpl
        implements SchemaMappingDAO {

    @Override
    public SchemaMapping find(Long id) {
        return entityManager.find(SchemaMapping.class, id);
    }

    @Override
    public List<SchemaMapping> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM SchemaMapping e");
        return query.getResultList();
    }

    @Override
    public SchemaMapping save(SchemaMapping mapping) {
        return entityManager.merge(mapping);
    }

    @Override
    public void delete(Long id) {
        SchemaMapping mapping = find(id);

        RoleSchema roleAttribueSchemas = mapping.getRoleSchema();

        Set<SchemaMapping> mappings = null;

        if (roleAttribueSchemas != null) {
            mappings = roleAttribueSchemas.getMappings();
        }

        if (mappings != null) {
            mappings.remove(mapping);
        }

        mapping.setRoleSchema(null);

        UserSchema userAttributeSchemas = mapping.getUserSchema();

        mappings = null;
        if (userAttributeSchemas != null) {
            mappings = userAttributeSchemas.getMappings();
        }

        if (mappings != null) {
            mappings.remove(mapping);
        }

        mapping.setUserSchema(null);

        Resource resource = mapping.getResource();

        mappings = null;
        if (resource != null) {
            mappings = resource.getMappings();
        }

        if (mappings != null) {
            mappings.remove(mapping);
        }

        mapping.setResource(null);

        entityManager.remove(mapping);
    }
}
