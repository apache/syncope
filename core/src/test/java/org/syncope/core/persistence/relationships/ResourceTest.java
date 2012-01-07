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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.types.PropagationMode;
import org.syncope.types.IntMappingType;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    @Autowired
    private PolicyDAO policyDAO;

    public final void createWithPasswordPolicy() {
        final String resourceName = "resourceWithPasswordPolicy";

        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        ExternalResource resource = new ExternalResource();
        resource.setName(resourceName);
        resource.setPasswordPolicy(policy);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        actual = resourceDAO.find(actual.getName());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        resourceDAO.delete(resourceName);
        assertNull(resourceDAO.find(resourceName));

        assertNotNull(policyDAO.find(4L));
    }

    /**
     * @see http://code.google.com/p/syncope/issues/detail?id=42
     */
    @Test
    public final void issue42() {
        USchema userId = schemaDAO.find("userId", USchema.class);

        Set<SchemaMapping> beforeUserIdMappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (userId.getName().equals(mapping.getIntAttrName())
                    && mapping.getIntMappingType()
                    == IntMappingType.UserSchema) {

                beforeUserIdMappings.add(mapping);
            }
        }

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setIntAttrName("userId");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setExtAttrName("campo1");
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
        resourceTO.setPropagationMode(PropagationMode.ONE_PHASE);
        resourceTO.setForceMandatoryConstraint(true);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        resourceDAO.flush();

        ExternalResource actual = resourceDAO.find("resource-issue42");
        assertEquals(resource, actual);

        userId = schemaDAO.find("userId", USchema.class);

        Set<SchemaMapping> afterUserIdMappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (userId.getName().equals(mapping.getIntAttrName())
                    && mapping.getIntMappingType()
                    == IntMappingType.UserSchema) {

                afterUserIdMappings.add(mapping);
            }
        }

        assertEquals(beforeUserIdMappings.size(),
                afterUserIdMappings.size() - 1);
    }

    @Test
    public final void save() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-save");

        // specify the connector
        ConnInstance connector = connInstanceDAO.find(100L);
        assertNotNull("connector not found", connector);

        resource.setConnector(connector);
        connector.addResource(resource);

        // specify mappings
        SchemaMapping mapping = null;
        for (int i = 0; i < 3; i++) {
            mapping = new SchemaMapping();
            mapping.setExtAttrName("test" + i);
            mapping.setIntAttrName("nonexistent" + i);
            mapping.setIntMappingType(IntMappingType.UserSchema);
            mapping.setMandatoryCondition("false");

            mapping.setResource(resource);
            resource.addMapping(mapping);
        }
        SchemaMapping accountId = new SchemaMapping();
        accountId.setAccountid(true);
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("username");
        accountId.setIntMappingType(IntMappingType.SyncopeUserId);

        accountId.setResource(resource);
        resource.addMapping(accountId);

        // map a derived attribute
        SchemaMapping derived = new SchemaMapping();
        derived.setAccountid(false);
        derived.setExtAttrName("fullname");
        derived.setIntAttrName("cn");
        derived.setIntMappingType(IntMappingType.UserDerivedSchema);

        derived.setResource(resource);
        resource.addMapping(derived);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        resourceDAO.flush();

        // assign the new resource to an user
        SyncopeUser user = userDAO.find(1L);
        assertNotNull("user not found", user);

        actual.addUser(user);
        user.addExternalResource(actual);

        resourceDAO.flush();

        // retrieve resource
        resource = resourceDAO.find(actual.getName());
        assertNotNull(resource);

        // check connector
        connector = connInstanceDAO.find(100L);
        assertNotNull(connector);

        assertNotNull(connector.getResources());
        assertTrue(connector.getResources().contains(resource));

        assertNotNull(resource.getConnector());
        assertTrue(resource.getConnector().equals(connector));

        // check mappings
        Set<SchemaMapping> schemaMappings = resource.getMappings();
        assertNotNull(schemaMappings);
        assertEquals(5, schemaMappings.size());

        // check user
        user = userDAO.find(1L);
        assertNotNull(user);
        assertNotNull(user.getExternalResources());
        assertTrue(user.getExternalResources().contains(actual));
    }

    @Test
    public final void delete() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull("find to delete did not work", resource);

        // -------------------------------------
        // Get originally associated connector
        // -------------------------------------
        ConnInstance connector = resource.getConnector();
        assertNotNull(connector);

        Long connectorId = connector.getId();
        // -------------------------------------

        // -------------------------------------
        // Get originally associated users
        // -------------------------------------
        Set<SyncopeUser> users = resource.getUsers();
        assertNotNull(users);

        Set<Long> userIds = new HashSet<Long>();
        for (SyncopeUser user : users) {
            userIds.add(user.getId());
        }
        // -------------------------------------

        // Get tasks
        List<PropagationTask> propagationTasks = taskDAO.findAll(resource,
                PropagationTask.class);
        assertFalse(propagationTasks.isEmpty());

        // delete resource
        resourceDAO.delete(resource.getName());

        // close the transaction
        resourceDAO.flush();

        // resource must be removed
        ExternalResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull("delete did not work", actual);

        // resource must be not referenced any more from users
        for (Long id : userIds) {
            SyncopeUser actualUser = userDAO.find(id);
            assertNotNull(actualUser);
            for (ExternalResource res : actualUser.getExternalResources()) {
                assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
            }
        }

        // resource must be not referenced any more from the connector
        ConnInstance actualConnector = connInstanceDAO.find(connectorId);
        assertNotNull(actualConnector);
        for (ExternalResource res : actualConnector.getResources()) {
            assertFalse(res.getName().equalsIgnoreCase(resource.getName()));
        }

        // there must be no tasks
        for (PropagationTask task : propagationTasks) {
            assertNull(taskDAO.find(task.getId()));
        }
    }

    @Test
    public void issue243() {
        ExternalResource csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);
        int origMappings = csv.getMappings().size();

        SchemaMapping newMapping = new SchemaMapping();
        newMapping.setIntMappingType(IntMappingType.Username);
        newMapping.setExtAttrName("TEST");
        newMapping.setResource(csv);
        csv.addMapping(newMapping);

        resourceDAO.save(csv);
        resourceDAO.flush();

        csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);
        assertEquals(origMappings + 1, csv.getMappings().size());

        resourceDAO.clear();
        
        int currentMappings = 0;
        List<SchemaMapping> allMappings = resourceDAO.findAllMappings();
        for (SchemaMapping mapping : allMappings) {
            if ("resource-csv".equals(mapping.getResource().getName())) {
                currentMappings++;
            }
        }
        assertEquals(csv.getMappings().size(), currentMappings);
    }
}
