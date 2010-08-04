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

import java.util.ArrayList;
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
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.test.persistence.AbstractTest;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    private SchemaMappingDAO schemaMappingDAO;

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;

    @Test
    public final void save() throws ClassNotFoundException {
        Resource resource = new Resource();
        resource.setName("ws-target-resource-save");

        // specify the connector
        ConnectorInstance connector = connectorInstanceDAO.find(100L);

        assertNotNull("connector not found", connector);

        resource.setConnector(connector);
        connector.addResource(resource);

        // specify a mapping
        List<SchemaMapping> mappings = new ArrayList<SchemaMapping>();

        // search for the user schema
        UserSchema userSchema =
                schemaDAO.find("username", UserSchema.class);

        // search for the role schema
        RoleSchema roleSchema = schemaDAO.find(
                "icon", RoleSchema.class);

        SchemaMapping mapping = null;

        for (int i = 0; i < 3; i++) {
            mapping = new SchemaMapping();
            mapping.setField("test" + i);

            mapping.setUserSchema(userSchema);
            mapping.setRoleSchema(roleSchema);

            mapping.setResource(resource);
            resource.addMapping(mapping);

            mappings.add(mapping);
        }

        // specify an user schema
        SyncopeUser user = syncopeUserDAO.find(1L);

        assertNotNull("user not found", user);

        resource.setUsers(Collections.singleton(user));
        user.addResource(resource);

        // save the resource
        Resource actual = resourceDAO.save(resource);

        assertNotNull(actual);

        resourceDAO.flush();

        // retrieve resource
        resource = resourceDAO.find(actual.getName());

        assertNotNull(resource);

        // check connector
        connector = connectorInstanceDAO.find(100L);

        assertNotNull(connector);

        Set<Resource> resources = connector.getResources();

        assertNotNull(resources);

        assertTrue(connector.getResources().contains(resource));

        assertNotNull(resource.getConnector());

        assertTrue(resource.getConnector().equals(connector));

        // check mappings
        Set<SchemaMapping> schemaMappings = resource.getMappings();

        assertNotNull(schemaMappings);

        assertTrue(schemaMappings.size() == 3);
    }

    @Test
    public final void delete() {

        Resource resource = resourceDAO.find("ws-target-resource-2");

        assertNotNull("find to delete did not work", resource);

        // -------------------------------------
        // Get originally associated mappings
        // -------------------------------------
        Set<SchemaMapping> mappings = resource.getMappings();

        assertNotNull(mappings);

        Set<Long> mappingIds = new HashSet<Long>();
        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }
        // -------------------------------------

        // -------------------------------------
        // Get originally associated connector
        // -------------------------------------
        ConnectorInstance connector = resource.getConnector();

        assertNotNull(connector);

        Long connectorId = connector.getId();
        // -------------------------------------

        // -------------------------------------
        // Get originally assoicated users
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
        resourceDAO.flush();

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
