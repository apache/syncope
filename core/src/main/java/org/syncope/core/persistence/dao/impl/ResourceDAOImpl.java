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

import javassist.NotFoundException;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.types.SourceMappingType;

@Repository
public class ResourceDAOImpl extends AbstractDAOImpl
        implements ResourceDAO {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Override
    public TargetResource find(final String name) {
        Query query = entityManager.createQuery(
                "SELECT e FROM TargetResource e WHERE e.name = :name");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
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
        TargetResource merged = entityManager.merge(resource);
        try {
            connInstanceLoader.registerConnector(merged);
        } catch (NotFoundException e) {
            LOG.error("While registering connector for resource", e);
        }
        return merged;
    }

    @Override
    public List<SchemaMapping> findAllMappings() {
        Query query = entityManager.createQuery(
                "SELECT e FROM SchemaMapping e");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);

        return query.getResultList();
    }

    @Override
    public SchemaMapping getMappingForAccountId(
            final String resourceName) {

        Query query = entityManager.createQuery(
                "SELECT m FROM SchemaMapping m "
                + "WHERE m.resource.name=:resourceName "
                + "AND m.accountid = 1");
        query.setParameter("resourceName", resourceName);

        return (SchemaMapping) query.getSingleResult();
    }

    @Override
    public void deleteMappings(final String sourceAttrName,
            final SourceMappingType sourceMappingType) {

        if (sourceMappingType == SourceMappingType.SyncopeUserId
                || sourceMappingType == SourceMappingType.Password) {

            return;
        }

        Query query = entityManager.createQuery("DELETE FROM "
                + SchemaMapping.class.getSimpleName()
                + " m WHERE m.sourceAttrName=:sourceAttrName "
                + "AND m.sourceMappingType=:sourceMappingType");
        query.setParameter("sourceAttrName", sourceAttrName);
        query.setParameter("sourceMappingType", sourceMappingType);

        int items = query.executeUpdate();
        LOG.debug("Removed {} schema mappings", items);

        // Make empty SchemaMapping query cache
        entityManager.getEntityManagerFactory().getCache().
                evict(SchemaMapping.class);
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

        // Make empty SchemaMapping query cache
        entityManager.getEntityManagerFactory().getCache().
                evict(SchemaMapping.class);
    }

    @Override
    public void delete(final String name) {
        TargetResource resource = find(name);
        if (resource == null) {
            return;
        }

        deleteAllMappings(resource);

        taskDAO.deleteAll(resource, PropagationTask.class);
        taskDAO.deleteAll(resource, SyncTask.class);

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
