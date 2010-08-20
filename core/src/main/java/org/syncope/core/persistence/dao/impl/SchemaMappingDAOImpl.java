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
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.membership.MembershipSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaMappingDAO;

@Repository
public class SchemaMappingDAOImpl extends AbstractDAOImpl
        implements SchemaMappingDAO {

    @Override
    @Transactional(readOnly = true)
    public SchemaMapping find(Long id) {
        return entityManager.find(SchemaMapping.class, id);
    }

    @Override
    @Transactional(readOnly = true)
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

        RoleSchema roleSchema = mapping.getRoleSchema();
        if (roleSchema != null && roleSchema.getMappings() != null) {
            roleSchema.getMappings().remove(mapping);
        }
        mapping.setRoleSchema(null);

        UserSchema userSchema = mapping.getUserSchema();
        if (userSchema != null && userSchema.getMappings() != null) {
            userSchema.getMappings().remove(mapping);
        }
        mapping.setUserSchema(null);

        MembershipSchema membershipSchema = mapping.getMembershipSchema();
        if (membershipSchema != null && membershipSchema.getMappings() != null) {
            membershipSchema.getMappings().remove(mapping);
        }
        mapping.setMembershipSchema(null);

        TargetResource resource = mapping.getResource();
        if (resource != null && resource.getMappings() != null) {
            resource.getMappings().remove(mapping);
        }
        mapping.setResource(null);

        entityManager.remove(mapping);
    }
}
