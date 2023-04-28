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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void findByName() {
        Optional<SchedTask> task = taskDAO.findByName(TaskType.SCHEDULED, "SampleJob Task");
        assertTrue(task.isPresent());
        assertEquals(taskDAO.find(TaskType.SCHEDULED, "e95555d2-1b09-42c8-b25b-f4c4ec597979"), task.get());
    }

    @Test
    public void findWithoutExecs() {
        List<PropagationTask> tasks = taskDAO.findToExec(TaskType.PROPAGATION);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
    }

    @Test
    public void findPaginated() {
        List<PropagationTask> tasks = taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, 1, 2, List.of());
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (PropagationTask task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, 2, 2, List.of());
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (PropagationTask task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, 1000, 2, List.of());
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());

        assertEquals(6, taskDAO.count(TaskType.PROPAGATION, null, null, null, null));
    }

    @Test
    public void findAll() {
        assertEquals(6, taskDAO.findAll(TaskType.PROPAGATION).size());
        assertEquals(1, taskDAO.findAll(TaskType.NOTIFICATION).size());
        assertEquals(3, taskDAO.findAll(TaskType.SCHEDULED).size());
        assertEquals(10, taskDAO.findAll(TaskType.PULL).size());
        assertEquals(11, taskDAO.findAll(TaskType.PUSH).size());

        List<GrantedAuthority> authorities = IdRepoEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertEquals(0, taskDAO.findAll(TaskType.MACRO).size());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void savePropagationTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        User user = userDAO.find("74cd8ece-715a-44a4-a736-e17b46c4e7e6");
        assertNotNull(user);

        PropagationTask task = entityFactory.newEntity(PropagationTask.class);
        task.setResource(resource);
        task.setAnyTypeKind(AnyTypeKind.USER);
        task.setAnyType(AnyTypeKind.USER.name());
        task.setOperation(ResourceOperation.CREATE);
        task.setConnObjectKey("one@two.com");

        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1", "testValue2"));
        attributes.add(AttributeBuilder.buildPassword("password".toCharArray()));
        task.setPropagationData(new PropagationData(attributes));

        task = taskDAO.save(task);
        assertNotNull(task);

        PropagationTask actual = taskDAO.find(TaskType.PROPAGATION, task.getKey());
        assertEquals(task, actual);
    }

    @Test
    public void delete() {
        PropagationTask task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        ExternalResource resource = task.getResource();
        assertNotNull(resource);

        taskDAO.delete(task);
        task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNull(task);

        resource = resourceDAO.find(resource.getKey());
        assertNotNull(resource);
        assertFalse(taskDAO.<PropagationTask>findAll(
                TaskType.PROPAGATION, resource, null, null, null, -1, -1, List.of()).
                contains(task));
    }
}
