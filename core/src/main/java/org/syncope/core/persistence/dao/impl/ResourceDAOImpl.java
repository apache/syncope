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
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;

@Repository
public class ResourceDAOImpl extends AbstractDAOImpl
        implements ResourceDAO {

    @Autowired
    private SchemaDAO schemaDAO;

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
    public void delete(String name) {

        TargetResource resource = find(name);
        if (resource == null) return;

        // --------------------------------------
        // Remove all mappings
        // --------------------------------------
        List<SchemaMapping> mappings = resource.getMappings();
        resource.setMappings(Collections.EMPTY_LIST);

        for (SchemaMapping mapping : mappings) {
            mapping.setResource(null);
            schemaDAO.removeMapping(mapping.getId());
        }
        // --------------------------------------

        Set<SyncopeUser> users = resource.getUsers();
        if (users != null && !users.isEmpty()) {
            for (SyncopeUser user : users) {
                user.removeTargetResource(resource);
            }
        }
        resource.setUsers(null);

        Set<SyncopeRole> roles = resource.getRoles();
        if (roles != null && !roles.isEmpty()) {
            for (SyncopeRole role : roles) {
                role.removeTargetResource(resource);
            }
        }
        resource.setRoles(null);

        ConnectorInstance connector = resource.getConnector();
        List<TargetResource> resources = null;
        if (connector != null) {
            resources = connector.getResources();
        }
        if (resources != null && !resources.isEmpty()) {
            resources.remove(resource);
        }
        resource.setConnector(null);

        entityManager.remove(resource);
    }
}
