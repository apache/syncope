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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.to.MappingItemTO;
import org.apache.syncope.client.to.MappingTO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.ResourceDataBinder;
import org.apache.syncope.core.AbstractTest;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.IntMappingType;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    public void databinding() throws IOException {
        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        ResourceTO resourceTO = resourceDataBinder.getResourceTO(resource);
        assertNotNull(resourceTO);

        ExternalResource fromto = resourceDataBinder.update(resource, resourceTO);
        assertNotNull(fromto);
        assertEquals(resource, fromto);

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, resourceTO);

        assertEquals(resourceTO, mapper.readValue(writer.toString(), ResourceTO.class));

        List<ResourceTO> resourceTOs = resourceDataBinder.getResourceTOs(resourceDAO.findAll());
        assertNotNull(resourceTOs);
        assertFalse(resourceTOs.isEmpty());

        writer = new StringWriter();
        mapper.writeValue(writer, resourceTOs);

        ResourceTO[] actual = mapper.readValue(writer.toString(), ResourceTO[].class);
        assertEquals(resourceTOs, Arrays.asList(actual));
    }

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

    /**
     * @see http://code.google.com/p/syncope/issues/detail?id=42
     */
    @Test
    public void issue42() {
        USchema userId = schemaDAO.find("userId", USchema.class);

        Set<AbstractMappingItem> beforeUserIdMappings = new HashSet<AbstractMappingItem>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getUmapping() != null) {
                for (AbstractMappingItem mapItem : res.getUmapping().getItems()) {
                    if (userId.getName().equals(mapItem.getIntAttrName())) {
                        beforeUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setName("resource-issue42");
        resourceTO.setConnectorId(100L);
        resourceTO.setPropagationMode(PropagationMode.ONE_PHASE);
        resourceTO.setEnforceMandatoryCondition(true);

        MappingTO mapping = new MappingTO();
        resourceTO.setUmapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserSchema);
        item.setExtAttrName("campo1");
        item.setAccountid(true);
        item.setMandatoryCondition("false");
        mapping.setAccountIdItem(item);

        ExternalResource resource = resourceDataBinder.create(resourceTO);
        resource = resourceDAO.save(resource);
        assertNotNull(resource);
        assertNotNull(resource.getUmapping());
        assertEquals(1, resource.getUmapping().getItems().size());

        resourceDAO.flush();

        ExternalResource actual = resourceDAO.find("resource-issue42");
        assertNotNull(actual);
        assertEquals(resource, actual);

        userId = schemaDAO.find("userId", USchema.class);

        Set<AbstractMappingItem> afterUserIdMappings = new HashSet<AbstractMappingItem>();
        for (ExternalResource res : resourceDAO.findAll()) {
            if (res.getUmapping() != null) {
                for (AbstractMappingItem mapItem : res.getUmapping().getItems()) {
                    if (userId.getName().equals(mapItem.getIntAttrName())) {
                        afterUserIdMappings.add(mapItem);
                    }
                }
            }
        }

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
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
            mapping.addItem(item);
            item.setMapping(mapping);
        }
        UMappingItem accountId = new UMappingItem();
        accountId.setExtAttrName("username");
        accountId.setIntAttrName("username");
        accountId.setIntMappingType(IntMappingType.UserId);
        mapping.setAccountIdItem(accountId);
        accountId.setMapping(mapping);

        // map a derived attribute
        UMappingItem derived = new UMappingItem();
        derived.setAccountid(false);
        derived.setExtAttrName("fullname");
        derived.setIntAttrName("cn");
        derived.setIntMappingType(IntMappingType.UserDerivedSchema);
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
        csv.getUmapping().addItem(newMapItem);

        resourceDAO.save(csv);
        resourceDAO.flush();

        csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);
        assertEquals(origMapItems + 1, csv.getUmapping().getItems().size());
    }
}
