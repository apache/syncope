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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.types.SchemaType;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;
    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private ResourceDataBinder resourceDataBinder;

    /**
     * @see http://code.google.com/p/syncope/issues/detail?id=42
     */
    @Test
    public final void issue42() {
        UserSchema userId = schemaDAO.find("userId", UserSchema.class);
        int beforeUserIdMappings = resourceDAO.getMappings(
                userId.getName(),
                SchemaType.UserSchema).size();

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setSchemaName("userId");
        schemaMappingTO.setSchemaType(SchemaType.UserSchema);
        schemaMappingTO.setField("campo1");
        schemaMappingTO.setAccountid(true);
        schemaMappingTO.setPassword(false);
        schemaMappingTO.setMandatoryCondition("false");

        SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();
        schemaMappingTOs.addMapping(schemaMappingTO);

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

        userId = schemaDAO.find("userId", UserSchema.class);
        int afterUserIdMappings = resourceDAO.getMappings(
                userId.getName(),
                SchemaType.UserSchema).size();

        assertEquals(beforeUserIdMappings, afterUserIdMappings - 1);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        TargetResource resource = new TargetResource();
        resource.setName("ws-target-resource-save");

        // specify the connector
        ConnectorInstance connector = connectorInstanceDAO.find(100L);

        assertNotNull("connector not found", connector);

        resource.setConnector(connector);
        connector.addResource(resource);

        // search for the user schema
        UserSchema userSchema =
                schemaDAO.find("username", UserSchema.class);

        SchemaMapping mapping = null;

        for (int i = 0; i < 3; i++) {
            mapping = new SchemaMapping();
            mapping.setField("test" + i);

            mapping.setSchemaName(userSchema.getName());
            mapping.setSchemaType(SchemaType.UserSchema);
            mapping.setMandatoryCondition("false");

            resource.addMapping(mapping);
        }

        // specify an user schema
        SyncopeUser user = syncopeUserDAO.find(1L);

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
        assertEquals(3, schemaMappings.size());
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

        // delete resource
        resourceDAO.delete(resource.getName());

        // close the transaction
        resourceDAO.flush();

        // resource must be removed
        TargetResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull("delete did not work", actual);

        // resource must be not referenced any more from users
        SyncopeUser actualUser = null;
        Collection<TargetResource> resources = null;
        for (Long id : userIds) {
            actualUser = syncopeUserDAO.find(id);
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
    }
}
