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
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
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
    public TargetResource find(final String name) {
        Query query = entityManager.createNamedQuery("TargetResource.find");
        query.setParameter("name", name);

        try {
            return (TargetResource) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<TargetResource> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM TargetResource e");
        return query.getResultList();
    }

    @Override
    public TargetResource save(final TargetResource resource) {
        return entityManager.merge(resource);
    }

    @Override
    public List<SchemaMapping> getMappings(final String schemaName,
            final SchemaType schemaType) {

        Query query = entityManager.createNamedQuery(
                "TargetResource.getMappings");
        query.setParameter("schemaName", schemaName);
        query.setParameter("schemaType", schemaType);

        return query.getResultList();
    }

    @Override
    public List<SchemaMapping> getMappings(final String schemaName,
            final SchemaType schemaType, final String resourceName) {

        Query query = entityManager.createNamedQuery(
                "TargetResource.getMappingsByTargetResource");
        query.setParameter("schemaName", schemaName);
        query.setParameter("schemaType", schemaType);
        query.setParameter("resourceName", resourceName);

        return query.getResultList();
    }

    @Override
    public String getSchemaNameForAccountId(final String resourceName) {
        Query query = entityManager.createQuery(
                "SELECT m FROM SchemaMapping m "
                + "WHERE m.resource.name=:resourceName "
                + "AND m.accountid = 1");
        query.setParameter("resourceName", resourceName);

        return ((SchemaMapping) query.getSingleResult()).getSchemaName();
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
        LOG.debug("Removed {} schema mappings", items);
    }

    @Override
    public void deleteAllMappings(final TargetResource resource) {
        Query query = entityManager.createQuery("DELETE FROM "
                + SchemaMapping.class.getSimpleName()
                + " m WHERE m.resource=:resource");
        query.setParameter("resource", resource);

        int items = query.executeUpdate();
        LOG.debug("Removed {} schema mappings", items);

        resource.getMappings().clear();
    }

    @Override
    public void delete(final String name) {
        TargetResource resource = find(name);
        if (resource == null) {
            return;
        }

        resource.getMappings().clear();

        resource.getTasks().clear();

        for (SyncopeUser user : resource.getUsers()) {
            user.removeTargetResource(resource);
        }
        resource.getUsers().clear();

        for (SyncopeRole role : resource.getRoles()) {
            role.removeTargetResource(resource);
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
