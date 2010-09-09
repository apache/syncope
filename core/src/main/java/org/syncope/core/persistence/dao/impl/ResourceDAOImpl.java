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
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.types.SchemaType;

@Repository
public class ResourceDAOImpl extends AbstractDAOImpl
        implements ResourceDAO {

    @Override
    @Transactional(readOnly = true)
    public TargetResource find(String name) {
        return entityManager.find(TargetResource.class, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetResource> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM TargetResource e");
        return query.getResultList();
    }

    @Override
    public TargetResource save(TargetResource resource) {
        return entityManager.merge(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemaMapping> getMappings(final String schemaName,
            final SchemaType schemaType) {

        Query query = entityManager.createQuery("SELECT m FROM "
                + SchemaMapping.class.getSimpleName()
                + " m WHERE m.schemaName=:schemaName "
                + "AND m.schemaType=:schemaType");
        query.setParameter("schemaName", schemaName);
        query.setParameter("schemaType", schemaType);

        return query.getResultList();
    }

    @Override
    public void deleteMappings(final String schemaName,
            final SchemaType schemaType) {

        if (schemaType == SchemaType.AccountId
                || schemaType == SchemaType.Password) {
            return;
        }

        Query query = entityManager.createQuery("DELETE FROM "
                + SchemaMapping.class.getSimpleName()
                + " m WHERE m.schemaName=:schemaName "
                + "AND m.schemaType=:schemaType");
        query.setParameter("schemaName", schemaName);
        query.setParameter("schemaType", schemaType);

        int items = query.executeUpdate();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removed " + items + " schema mappings");
        }
    }

    @Override
    public void delete(String name) {
        TargetResource resource = find(name);
        if (resource == null) {
            return;
        }

        // --------------------------------------
        // Remove all mappings
        // --------------------------------------
        resource.getMappings().clear();
        // --------------------------------------

        Set<SyncopeUser> users = resource.getUsers();
        if (users != null && !users.isEmpty()) {
            for (SyncopeUser user : users) {
                user.removeTargetResource(resource);
            }
        }
        resource.getUsers().clear();

        Set<SyncopeRole> roles = resource.getRoles();
        if (roles != null && !roles.isEmpty()) {
            for (SyncopeRole role : roles) {
                role.removeTargetResource(resource);
            }
        }
        resource.getRoles().clear();

        if (resource.getConnector() != null
                && resource.getConnector().getResources() != null
                && !resource.getConnector().getResources().isEmpty()) {

            resource.getConnector().getResources().remove(resource);
        }
        resource.setConnector(null);

        entityManager.remove(resource);
    }
}
