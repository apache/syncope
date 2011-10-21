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
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.types.IntMappingType;

@Repository
public class ResourceDAOImpl extends AbstractDAOImpl
        implements ResourceDAO {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Override
    public ExternalResource find(final String name) {
        Query query = entityManager.createQuery("SELECT e "
                + "FROM " + ExternalResource.class.getSimpleName() + " e "
                + "WHERE e.name = :name");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
        query.setParameter("name", name);

        try {
            return (ExternalResource) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ExternalResource> findAll() {
        Query query = entityManager.createQuery("SELECT e "
                + "FROM  " + ExternalResource.class.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public List<ExternalResource> findAllByPriority() {
        Query query = entityManager.createQuery("SELECT e "
                + "FROM  " + ExternalResource.class.getSimpleName() + " e "
                + "ORDER BY e.propagationPriority");
        return query.getResultList();
    }

    @Override
    public ExternalResource save(final ExternalResource resource) {
        ExternalResource merged = entityManager.merge(resource);
        try {
            connInstanceLoader.registerConnector(merged);
        } catch (NotFoundException e) {
            LOG.error("While registering connector for resource", e);
        }
        return merged;
    }

    @Override
    public List<SchemaMapping> findAllMappings() {
        Query query = entityManager.createQuery("SELECT e FROM "
                + SchemaMapping.class.getSimpleName() + " e");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);

        return query.getResultList();
    }

    @Override
    public SchemaMapping getMappingForAccountId(
            final String resourceName) {

        Query query = entityManager.createQuery("SELECT m FROM "
                + SchemaMapping.class.getSimpleName() + " m "
                + "WHERE m.resource.name=:resourceName "
                + "AND m.accountid = 1");
        query.setParameter("resourceName", resourceName);

        return (SchemaMapping) query.getSingleResult();
    }

    @Override
    public void deleteMappings(final String intAttrName,
            final IntMappingType intMappingType) {

        if (intMappingType == IntMappingType.SyncopeUserId
                || intMappingType == IntMappingType.Password
                || intMappingType == IntMappingType.Username) {

            return;
        }

        Query query = entityManager.createQuery("DELETE FROM "
                + SchemaMapping.class.getSimpleName()
                + " m WHERE m.intAttrName=:intAttrName "
                + "AND m.intMappingType=:intMappingType");
        query.setParameter("intAttrName", intAttrName);
        query.setParameter("intMappingType", intMappingType);

        int items = query.executeUpdate();
        LOG.debug("Removed {} schema mappings", items);

        // Make empty SchemaMapping query cache
        entityManager.getEntityManagerFactory().getCache().
                evict(SchemaMapping.class);
    }

    @Override
    public void deleteAllMappings(final ExternalResource resource) {
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
        ExternalResource resource = find(name);
        if (resource == null) {
            return;
        }

        deleteAllMappings(resource);

        taskDAO.deleteAll(resource, PropagationTask.class);
        taskDAO.deleteAll(resource, SyncTask.class);

        for (SyncopeUser user : resource.getUsers()) {
            user.removeExternalResource(resource);
        }
        resource.getUsers().clear();

        for (SyncopeRole role : resource.getRoles()) {
            role.removeExternalResource(resource);
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
