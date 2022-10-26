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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @Test
    public void read() {
        PropagationTask task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        assertNotNull(task.getExecs());
        assertFalse(task.getExecs().isEmpty());
        assertEquals(1, task.getExecs().size());
    }

    @Test
    public void readMultipleOrderBy() {
        List<OrderByClause> orderByClauses = new ArrayList<>();
        OrderByClause clause1 = new OrderByClause();
        clause1.setField("start");
        OrderByClause clause2 = new OrderByClause();
        clause2.setField("latestExecStatus");
        OrderByClause clause3 = new OrderByClause();
        clause3.setField("connObjectKey");
        orderByClauses.add(clause1);
        orderByClauses.add(clause2);
        orderByClauses.add(clause3);
        assertFalse(taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, -1, -1, orderByClauses).isEmpty());
    }

    @Test
    public void savePropagationTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        User user = userDAO.findByUsername("verdi");
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

        entityManager().flush();

        assertTrue(taskDAO.<PropagationTask>findAll(
                TaskType.PROPAGATION, resource, null, null, null, -1, -1, List.of()).
                contains(task));
    }

    @Test
    public void addPropagationTaskExecution() {
        PropagationTask task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<PropagationTask> execution = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTaskExec();
        execution.setTask(task);
        execution.setStatus(ExecStatus.CREATED.name());
        execution.setStart(OffsetDateTime.now());
        execution.setExecutor("admin");
        task.add(execution);

        taskDAO.save(task);
        entityManager().flush();

        task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void addPullTaskExecution() {
        PullTask task = (PullTask) taskDAO.find(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c");
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(TaskType.PULL).newTaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(OffsetDateTime.now());
        execution.setMessage("A message");
        execution.setExecutor("admin");
        task.add(execution);

        taskDAO.save(task);
        entityManager().flush();

        task = (PullTask) taskDAO.find(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c");
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void addPushTaskExecution() {
        PushTask task = (PushTask) taskDAO.find(TaskType.PUSH, "af558be4-9d2f-4359-bf85-a554e6e90be1");
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(TaskType.PUSH).newTaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(OffsetDateTime.now());
        execution.setMessage("A message");
        execution.setExecutor("admin");
        task.add(execution);

        taskDAO.save(task);
        entityManager().flush();

        task = (PushTask) taskDAO.find(TaskType.PUSH, "af558be4-9d2f-4359-bf85-a554e6e90be1");
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void deleteTask() {
        taskDAO.delete(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");

        entityManager().flush();

        assertTrue(taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c").isEmpty());
        assertTrue(taskExecDAO.find("e58ca1c7-178a-4012-8a71-8aa14eaf0655").isEmpty());
    }

    @Test
    public void deleteTaskExecution() {
        TaskExec<PropagationTask> execution =
                taskExecDAO.find(TaskType.PROPAGATION, "e58ca1c7-178a-4012-8a71-8aa14eaf0655");
        int executionNumber = execution.getTask().getExecs().size();

        taskExecDAO.delete(TaskType.PROPAGATION, "e58ca1c7-178a-4012-8a71-8aa14eaf0655");

        entityManager().flush();

        assertTrue(taskExecDAO.find("e58ca1c7-178a-4012-8a71-8aa14eaf0655").isEmpty());

        PropagationTask task = taskDAO.find(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertEquals(task.getExecs().size(), executionNumber - 1);
    }

    @Test
    public void savePullTask() {
        PullTask task = entityFactory.newEntity(PullTask.class);
        task.setName("savePullTask");
        task.setDescription("PullTask description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.setJobDelegate(implementationDAO.find("PullJobDelegate"));
        task.setDestinationRealm(realmDAO.getRoot());
        task.setCronExpression("BLA BLA");
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);

        // now adding PullActions
        Implementation pullActions = entityFactory.newEntity(Implementation.class);
        pullActions.setKey("PullActions" + UUID.randomUUID().toString());
        pullActions.setEngine(ImplementationEngine.JAVA);
        pullActions.setType(IdMImplementationType.PULL_ACTIONS);
        pullActions.setBody(PullActions.class.getName());
        pullActions = implementationDAO.save(pullActions);
        entityManager().flush();

        task.add(pullActions);

        // this save() fails because of an invalid Cron Expression
        try {
            taskDAO.save(task);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
        task.setCronExpression(null);

        // this save() fails because a PullTask requires a target resource
        try {
            taskDAO.save(task);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
        task.setResource(resourceDAO.find("ws-target-resource-1"));

        // this save() finally works
        task = (PullTask) taskDAO.save(task);
        assertNotNull(task);

        PullTask actual = (PullTask) taskDAO.find(TaskType.PULL, task.getKey());
        assertEquals(task, actual);
    }

    @Test
    public void issueSYNCOPE144() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        Implementation pullActions = entityFactory.newEntity(Implementation.class);
        pullActions.setKey("syncope144");
        pullActions.setEngine(ImplementationEngine.JAVA);
        pullActions.setType(IdMImplementationType.PULL_ACTIONS);
        pullActions.setBody(PullActions.class.getName());
        pullActions = implementationDAO.save(pullActions);

        PullTask task = entityFactory.newEntity(PullTask.class);

        task.setResource(resource);
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.add(pullActions);
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);

        task = (PullTask) taskDAO.save(task);
        assertNotNull(task);

        PullTask actual = (PullTask) taskDAO.find(TaskType.PULL, task.getKey());
        assertEquals(task, actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        actual.setName("issueSYNCOPE144_2");
        actual.setDescription("issueSYNCOPE144 Description_2");

        actual = (PullTask) taskDAO.save(actual);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }
}
