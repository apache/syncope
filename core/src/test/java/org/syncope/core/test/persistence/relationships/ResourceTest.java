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
package org.syncope.core.test.persistence.relationships;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.test.persistence.AbstractTest;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    ResourceDAO resourceDAO;

    @Autowired
    ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    SchemaMappingDAO schemaMappingDAO;

    @Autowired
    SyncopeUserDAO syncopeUserDAO;

    @Test
    public final void save() {
        Resource resource = new Resource();
        resource.setName("ws-target-resource-3");

        // specify the connector
        ConnectorInstance connector = connectorInstanceDAO.find(100L);

        assertNotNull("connector not found", connector);

        resource.setConnector(connector);
        connector.addResource(resource);

        // specify a mapping
        List<SchemaMapping> mappings = schemaMappingDAO.findAll();

        assertNotNull("mappings not found", mappings);

        assertFalse("no mapping specified", mappings.isEmpty());

        resource.setMappings(new HashSet<SchemaMapping>(mappings));

        for (SchemaMapping mapping : mappings) {
            mapping.setResource(resource);
        }

        // specify an user schema
        SyncopeUser user = syncopeUserDAO.find(1L);

        assertNotNull("user not found", user);

        resource.setUsers(Collections.singleton(user));
        user.addResource(resource);

        // save the resource
        Resource actual = resourceDAO.save(resource);

        assertNotNull(actual);

        resourceDAO.getEntityManager().flush();

        // connector should be updated
        connector = connectorInstanceDAO.find(100L);

        assertNotNull(connector);

        Set<Resource> resources = connector.getResources();
        assertNotNull(resources);
        assertTrue(resources.size() > 1);

        assertTrue(resources.contains(resourceDAO.find(actual.getName())));

        // mapping should be updated
        SchemaMapping mapping = schemaMappingDAO.find(100L);

        assertNotNull(mapping);

        resource = mapping.getResource();
        assertNotNull(resource);
        assertEquals(resource, actual);

        // user must bu updated
        user = syncopeUserDAO.find(1L);

        assertNotNull(user);

        resources = user.getResources();
        assertNotNull(resources);
        assertTrue(resources.size() == 1);
        assertTrue(resources.contains(actual));
    }

    @Test
    public final void delete() {

        Resource resource = resourceDAO.find("ws-target-resource-2");

        assertNotNull("find to delete did not work", resource);

        // -------------------------------------
        // Get original associated mappings
        // -------------------------------------
        Set<SchemaMapping> mappings = resource.getMappings();

        assertNotNull(mappings);

        Set<Long> mappingIds = new HashSet<Long>();
        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }
        // -------------------------------------

        // -------------------------------------
        // Get original associated connector
        // -------------------------------------
        ConnectorInstance connector = resource.getConnector();

        assertNotNull(connector);

        Long connectorId = connector.getId();
        // -------------------------------------

        // -------------------------------------
        // Get original users assoicated
        // -------------------------------------
        Set<SyncopeUser> users = resource.getUsers();

        assertNotNull(users);

        Set<Long> userIds = new HashSet<Long>();
        for (SyncopeUser user : users) {
            userIds.add(user.getId());
        }
        // -------------------------------------

        // delete resource
        resourceDAO.delete(resource.getName());

        // close the transaction
        resourceDAO.getEntityManager().flush();

        // resource must be removed
        Resource actual = resourceDAO.find("ws-target-resource-2");
        assertNull("delete did not work", actual);

        // mappings must be removed
        for (Long id : mappingIds) {
            assertNull("mapping delete did not work",
                    schemaMappingDAO.find(id));
        }

        // resource must be not referenced any more from users
        SyncopeUser actualUser = null;
        Set<Resource> resources = null;
        for (Long id : userIds) {
            actualUser = syncopeUserDAO.find(id);
            assertNotNull(actualUser);
            resources = actualUser.getResources();
            for (Resource res : resources) {
                assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
            }
        }

        // resource must be not referenced any more from the connector
        ConnectorInstance actualConnector =
                connectorInstanceDAO.find(connectorId);
        assertNotNull(actualConnector);
        resources = actualConnector.getResources();
        for (Resource res : resources) {
            assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
        }
    }
}
