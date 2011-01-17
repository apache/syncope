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
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.types.SourceMappingType;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    /**
     * @see http://code.google.com/p/syncope/issues/detail?id=42
     */
    @Test
    public final void issue42() {
        USchema userId = schemaDAO.find("userId", USchema.class);

        Set<SchemaMapping> beforeUserIdMappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (mapping.getSourceAttrName().equals(userId.getName())
                    && mapping.getSourceMappingType()
                    == SourceMappingType.UserSchema) {

                beforeUserIdMappings.add(mapping);
            }
        }

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setSourceAttrName("userId");
        schemaMappingTO.setSourceMappingType(SourceMappingType.UserSchema);
        schemaMappingTO.setDestAttrName("campo1");
        schemaMappingTO.setAccountid(true);
        schemaMappingTO.setPassword(false);
        schemaMappingTO.setMandatoryCondition("false");

        List<SchemaMappingTO> schemaMappingTOs =
                new ArrayList<SchemaMappingTO>();
        schemaMappingTOs.add(schemaMappingTO);

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName("resource-issue42");
        resourceTO.setConnectorId(100L);
        resourceTO.setMappings(schemaMappingTOs);
        resourceTO.setForceMandatoryConstraint(true);

        TargetResource resource = resourceDataBinder.getResource(resourceTO);
        resource = resourceDAO.save(resource);
        resourceDAO.flush();

        TargetResource actual = resourceDAO.find("resource-issue42");
        assertEquals(resource, actual);

        userId = schemaDAO.find("userId", USchema.class);

        Set<SchemaMapping> afterUserIdMappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (mapping.getSourceAttrName().equals(userId.getName())
                    && mapping.getSourceMappingType()
                    == SourceMappingType.UserSchema) {

                afterUserIdMappings.add(mapping);
            }
        }

        assertEquals(beforeUserIdMappings.size(),
                afterUserIdMappings.size() - 1);
    }

    @Test
    public final void save()
            throws ClassNotFoundException {
        TargetResource resource = new TargetResource();
        resource.setName("ws-target-resource-save");

        // specify the connector
        ConnectorInstance connector = connectorInstanceDAO.find(100L);

        assertNotNull("connector not found", connector);

        resource.setConnector(connector);
        connector.addResource(resource);

        // search for the user schema
        USchema userSchema =
                schemaDAO.find("username", USchema.class);

        SchemaMapping mapping = null;

        for (int i = 0; i < 3; i++) {
            mapping = new SchemaMapping();
            mapping.setDestAttrName("test" + i);

            mapping.setSourceAttrName(userSchema.getName());
            mapping.setSourceMappingType(SourceMappingType.UserSchema);
            mapping.setMandatoryCondition("false");

            resource.addMapping(mapping);
        }
        SchemaMapping accountId = new SchemaMapping();
        accountId.setResource(resource);
        accountId.setAccountid(true);
        accountId.setDestAttrName("username");
        accountId.setSourceAttrName(userSchema.getName());
        accountId.setSourceMappingType(SourceMappingType.SyncopeUserId);

        resource.addMapping(accountId);

        // specify an user schema
        SyncopeUser user = userDAO.find(1L);

        assertNotNull("user not found", user);

        resource.addUser(user);
        user.addTargetResource(resource);

        // save the resource
        TargetResource actual = resourceDAO.save(resource);

        assertNotNull(actual);

        resourceDAO.flush();

        // retrieve resource
        resource = resourceDAO.find(actual.getName());

        assertNotNull(resource);

        // check connector
        connector = connectorInstanceDAO.find(100L);

        assertNotNull(connector);

        List<TargetResource> resources = connector.getResources();

        assertNotNull(resources);

        assertTrue(connector.getResources().contains(resource));

        assertNotNull(resource.getConnector());

        assertTrue(resource.getConnector().equals(connector));

        // check mappings
        List<SchemaMapping> schemaMappings = resource.getMappings();

        assertNotNull(schemaMappings);
        assertEquals(4, schemaMappings.size());
    }

    @Test
    public final void delete() {
        TargetResource resource = resourceDAO.find("ws-target-resource-2");

        assertNotNull("find to delete did not work", resource);

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

        // Get tasks
        List<Task> tasks = resource.getTasks();

        // delete resource
        resourceDAO.delete(resource.getName());

        // close the transaction
        resourceDAO.flush();

        // resource must be removed
        TargetResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull("delete did not work", actual);

        // resource must be not referenced any more from users
        SyncopeUser actualUser;
        Collection<TargetResource> resources;
        for (Long id : userIds) {
            actualUser = userDAO.find(id);
            assertNotNull(actualUser);
            resources = actualUser.getTargetResources();
            for (TargetResource res : resources) {
                assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
            }
        }

        // resource must be not referenced any more from the connector
        ConnectorInstance actualConnector =
                connectorInstanceDAO.find(connectorId);
        assertNotNull(actualConnector);
        resources = actualConnector.getResources();
        for (TargetResource res : resources) {
            assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
        }

        // there must be no tasks
        for (Task task : tasks) {
            assertNull(taskDAO.find(task.getId()));
        }
    }
}
