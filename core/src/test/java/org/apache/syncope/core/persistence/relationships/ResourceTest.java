/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceTest extends AbstractDAOTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void createWithPasswordPolicy() {
        final String resourceName = "resourceWithPasswordPolicy";

        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        ExternalResource resource = new ExternalResource();
        resource.setName(resourceName);
        resource.setPasswordPolicy(policy);

        ConnInstance connector = connInstanceDAO.find(100L);
        assertNotNull("connector not found", connector);
        resource.setConnector(connector);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        actual = resourceDAO.find(actual.getName());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        resourceDAO.delete(resourceName);
        assertNull(resourceDAO.find(resourceName));

        assertNotNull(policyDAO.find(4L));
    }

    @Test
    public void save() {
        ExternalResource resource = new ExternalResource();
        resource.setName("ws-target-resource-save");

        // specify the connector
        ConnInstance connector = connInstanceDAO.find(100L);
        assertNotNull("connector not found", connector);

        resource.setConnector(connector);

        UMapping mapping = new UMapping();
        mapping.setResource(resource);
        resource.setUmapping(mapping);

        // specify mappings
        for (int i = 0; i < 3; i++) {
            UMappingItem item = new UMappingItem();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("nonexistent" + i);
            item.setIntMappingType(IntMappingType.UserSchema);
            item.setMandatoryCondition("false");
            item.setPurpose(MappingPurpose.SYNCHRONIZATION);
            mapping.addItem(item);
            item.setMapping(mapping);
        }
        UMappingItem accountId = new UMappingItem();
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("username");
        accountId.setIntMappingType(IntMappingType.UserId);
        accountId.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setAccountIdItem(accountId);
        accountId.setMapping(mapping);

        // map a derived attribute
        UMappingItem derived = new UMappingItem();
        derived.setAccountid(false);
        derived.setExtAttrName("fullname");
        derived.setIntAttrName("cn");
        derived.setIntMappingType(IntMappingType.UserDerivedSchema);
        derived.setPurpose(MappingPurpose.PROPAGATION);
        mapping.addItem(derived);
        derived.setMapping(mapping);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
        assertNotNull(actual.getUmapping());

        resourceDAO.flush();
        resourceDAO.detach(actual);
        resourceDAO.detach(connector);

        // assign the new resource to an user
        SyncopeUser user = userDAO.find(1L);
        assertNotNull("user not found", user);

        user.addResource(actual);

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
        List<UMappingItem> items = resource.getUmapping().getItems();
        assertNotNull(items);
        assertEquals(5, items.size());

        // check user
        user = userDAO.find(1L);
        assertNotNull(user);
        assertNotNull(user.getResources());
        assertTrue(user.getResources().contains(actual));
    }

    @Test
    public void delete() {
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
        List<SyncopeUser> users = userDAO.findByResource(resource);
        assertNotNull(users);

        Set<Long> userIds = new HashSet<Long>();
        for (SyncopeUser user : users) {
            userIds.add(user.getId());
        }
        // -------------------------------------

        // Get tasks
        List<PropagationTask> propagationTasks = taskDAO.findAll(resource, PropagationTask.class);
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
            for (ExternalResource res : actualUser.getResources()) {
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

        int origMapItems = csv.getUmapping().getItems().size();

        UMappingItem newMapItem = new UMappingItem();
        newMapItem.setIntMappingType(IntMappingType.Username);
        newMapItem.setExtAttrName("TEST");
        newMapItem.setPurpose(MappingPurpose.PROPAGATION);
        csv.getUmapping().addItem(newMapItem);

        resourceDAO.save(csv);
        resourceDAO.flush();

        csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);
        assertEquals(origMapItems + 1, csv.getUmapping().getItems().size());
    }
}
