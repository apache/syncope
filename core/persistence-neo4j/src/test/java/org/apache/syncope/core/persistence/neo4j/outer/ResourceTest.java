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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void accountPolicy() {
        ExternalResource resource = resourceDAO.findById("resource-testdb").orElseThrow();
        assertEquals(
                policyDAO.findById("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7").orElseThrow(),
                resource.getAccountPolicy());
    }

    @Test
    public void findByConnInstance() {
        List<ExternalResource> resources = resourceDAO.findByConnInstance("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertEquals(6, resources.size());
        assertTrue(resources.contains(resourceDAO.findById("ws-target-resource-1").orElseThrow()));
    }

    @Test
    public void findByProvisionSorter() {
        Implementation impl = entityFactory.newEntity(Implementation.class);
        impl.setType(IdMImplementationType.PROVISION_SORTER);
        impl.setEngine(ImplementationEngine.GROOVY);
        impl.setKey("ProvSorter");
        impl = implementationDAO.save(impl);

        assertTrue(resourceDAO.findByProvisionSorter(impl).isEmpty());

        ExternalResource csv = resourceDAO.findById("resource-csv").orElseThrow();
        csv.setProvisionSorter(impl);
        csv = resourceDAO.save(csv);

        assertEquals(List.of(csv), resourceDAO.findByProvisionSorter(impl));
    }

    @Test
    public void findByPropagationActionsContaining() {
        Implementation impl = implementationDAO.findById("GenerateRandomPasswordPropagationActions").orElseThrow();

        assertEquals(
                Set.of(resourceDAO.findById("resource-testdb2").orElseThrow(),
                        resourceDAO.findById("resource-ldap").orElseThrow()),
            new HashSet<>(resourceDAO.findByPropagationActionsContaining(impl)));
    }

    @Test
    public void createWithPasswordPolicy() {
        final String resourceName = "resourceWithPasswordPolicy";

        PasswordPolicy policy = policyDAO.findById("986d1236-3ac5-4a19-810c-5ab21d79cba1", PasswordPolicy.class).
                orElseThrow();
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey(resourceName);
        resource.setPasswordPolicy(policy);

        ConnInstance connector = connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow();
        assertNotNull(connector);
        resource.setConnector(connector);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        actual = resourceDAO.findById(actual.getKey()).orElseThrow();
        assertEquals(policy, actual.getPasswordPolicy());

        resourceDAO.deleteById(resourceName);
        assertTrue(resourceDAO.findById(resourceName).isEmpty());

        assertTrue(policyDAO.findById("986d1236-3ac5-4a19-810c-5ab21d79cba1").isPresent());
    }

    @Test
    public void save() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-save");

        // specify the connector
        ConnInstance connector = connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow();

        resource.setConnector(connector);

        Provision provision = new Provision();
        provision.setAnyType(AnyTypeKind.USER.name());
        provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resource.getProvisions().add(provision);

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        // specify mappings
        for (int i = 0; i < 3; i++) {
            Item item = new Item();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("nonexistent" + i);
            item.setMandatoryCondition("false");
            item.setPurpose(MappingPurpose.PULL);
            mapping.add(item);
        }
        Item connObjectKey = new Item();
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("username");
        connObjectKey.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setConnObjectKeyItem(connObjectKey);

        // map a derived attribute
        Item derived = new Item();
        derived.setConnObjectKey(false);
        derived.setExtAttrName("fullname");
        derived.setIntAttrName("cn");
        derived.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(derived);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);
        assertNotNull(actual.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping());

        // assign the new resource to an user
        User user = userDAO.findByUsername("rossini").orElseThrow();
        user.add(actual);
        userDAO.save(user);

        // retrieve resource
        resource = resourceDAO.findById(actual.getKey()).orElseThrow();

        // check connector
        connector = connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow();
        assertNotNull(connector.getResources());

        assertNotNull(resource.getConnector());
        assertTrue(resource.getConnector().equals(connector));

        // check mappings
        List<Item> items = resource.getProvisionByAnyType(
                AnyTypeKind.USER.name()).get().getMapping().getItems();
        assertNotNull(items);
        assertEquals(5, items.size());

        // check user
        user = userDAO.findByUsername("rossini").orElseThrow();
        assertNotNull(user.getResources());
        assertTrue(user.getResources().contains(actual));
    }

    @Test
    public void delete() {
        ExternalResource resource = resourceDAO.findById("resource-testdb").orElseThrow();

        // -------------------------------------
        // Get originally associated connector
        // -------------------------------------
        ConnInstance connector = resource.getConnector();
        assertNotNull(connector);
        // -------------------------------------

        // -------------------------------------
        // Get originally associated users
        // -------------------------------------
        List<User> users = userDAO.findByResourcesContaining(resource);
        assertNotNull(users);

        Set<String> userKeys = users.stream().map(User::getKey).collect(Collectors.toSet());
        // -------------------------------------

        // Get tasks
        List<PropagationTask> propagationTasks = taskDAO.findAll(
                TaskType.PROPAGATION, resource, null, null, null, Pageable.unpaged());
        assertFalse(propagationTasks.isEmpty());

        // delete resource
        resourceDAO.deleteById(resource.getKey());

        // resource must be removed
        assertTrue(resourceDAO.findById("resource-testdb").isEmpty());

        // resource must be not referenced any more from users
        userKeys.stream().map(u -> userDAO.findById(u).orElseThrow()).forEach(user -> {
            assertNotNull(user);
            userDAO.findAllResources(user).
                    forEach(r -> assertFalse(r.getKey().equalsIgnoreCase(resource.getKey())));
        });

        // resource must be not referenced any more from the connector
        ConnInstance actualConnector = connInstanceDAO.findById(connector.getKey()).orElseThrow();
        actualConnector.getResources().
                forEach(res -> assertFalse(res.getKey().equalsIgnoreCase(resource.getKey())));

        // there must be no tasks
        propagationTasks.forEach(task -> assertTrue(taskDAO.findById(task.getKey()).isEmpty()));
    }

    @Test
    public void addAndRemovePropagationActions() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdMImplementationType.PROPAGATION_ACTIONS);
        implementation.setBody("TestPropagationActions");
        implementation = implementationDAO.save(implementation);

        ExternalResource resource = resourceDAO.findById("ws-target-resource-2").orElseThrow();
        assertTrue(resource.getPropagationActions().isEmpty());

        resource.add(implementation);
        resourceDAO.save(resource);

        resource = resourceDAO.findById("ws-target-resource-2").orElseThrow();
        assertEquals(1, resource.getPropagationActions().size());
        assertEquals(implementation, resource.getPropagationActions().getFirst());

        resource.getPropagationActions().clear();
        resource = resourceDAO.save(resource);
        assertTrue(resource.getPropagationActions().isEmpty());

        resource = resourceDAO.findById("ws-target-resource-2").orElseThrow();
        assertTrue(resource.getPropagationActions().isEmpty());
    }

    @Test
    public void issue243() {
        ExternalResource csv = resourceDAO.findById("resource-csv").orElseThrow();

        int origMapItems = csv.getProvisionByAnyType(
                AnyTypeKind.USER.name()).get().getMapping().getItems().size();

        Item newMapItem = new Item();
        newMapItem.setIntAttrName("TEST");
        newMapItem.setExtAttrName("TEST");
        newMapItem.setPurpose(MappingPurpose.PROPAGATION);
        csv.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().add(newMapItem);

        resourceDAO.save(csv);

        csv = resourceDAO.findById("resource-csv").orElseThrow();
        assertEquals(
                origMapItems + 1,
                csv.getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().size());
    }
}
