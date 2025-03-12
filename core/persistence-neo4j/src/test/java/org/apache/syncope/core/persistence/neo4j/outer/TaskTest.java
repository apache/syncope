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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @Test
    public void read() {
        Task<?> task = taskDAO.findById(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        assertNotNull(task.getExecs());
        assertFalse(task.getExecs().isEmpty());
        assertEquals(1, task.getExecs().size());
    }

    @Test
    public void readMultipleOrderBy() {
        List<Sort.Order> orderByClauses = new ArrayList<>();
        orderByClauses.add(new Sort.Order(Sort.DEFAULT_DIRECTION, "start"));
        orderByClauses.add(new Sort.Order(Sort.DEFAULT_DIRECTION, "latestExecStatus"));
        orderByClauses.add(new Sort.Order(Sort.DEFAULT_DIRECTION, "connObjectKey"));
        assertFalse(taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, Pageable.unpaged(Sort.by(orderByClauses))).
                isEmpty());
    }

    @Test
    public void findAllByResource() {
        List<PropagationTask> tasks = taskDAO.findAll(
                TaskType.PROPAGATION,
                resourceDAO.findById("ws-target-resource-2").orElseThrow(),
                null,
                null,
                null,
                Pageable.unpaged());
        assertEquals(3, tasks.size());
    }

    @Test
    public void savePropagationTask() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-1").orElseThrow();

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

        Task<?> actual = taskDAO.findById(TaskType.PROPAGATION, task.getKey()).orElseThrow();
        assertEquals(task, actual);

        assertTrue(taskDAO.<PropagationTask>findAll(
                TaskType.PROPAGATION, resource, null, null, null, Pageable.unpaged()).
                contains(task));
    }

    @Test
    public void addPropagationTaskExecution() {
        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<PropagationTask> execution = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTaskExec();
        execution.setTask(task);
        execution.setStatus(ExecStatus.CREATED.name());
        execution.setStart(OffsetDateTime.now());
        execution.setExecutor("admin");
        task.add(execution);
        execution.setTask(task);

        taskDAO.save(task);

        task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void addPullTaskExecution() {
        PullTask task = (PullTask) taskDAO.findById(
                TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(TaskType.PULL).newTaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(OffsetDateTime.now());
        execution.setMessage("A message");
        execution.setExecutor("admin");
        task.add(execution);
        execution.setTask(task);

        taskDAO.save(task);

        task = (PullTask) taskDAO.findById(
                TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void addPushTaskExecution() {
        PushTask task = (PushTask) taskDAO.findById(
                TaskType.PUSH, "af558be4-9d2f-4359-bf85-a554e6e90be1").orElseThrow();
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(TaskType.PUSH).newTaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(OffsetDateTime.now());
        execution.setMessage("A message");
        execution.setExecutor("admin");
        task.add(execution);
        execution.setTask(task);

        taskDAO.save(task);

        task = (PushTask) taskDAO.findById(TaskType.PUSH, "af558be4-9d2f-4359-bf85-a554e6e90be1").orElseThrow();
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void deleteTask() {
        taskDAO.delete(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c");

        assertTrue(taskDAO.findById("1e697572-b896-484c-ae7f-0c8f63fcbc6c").isEmpty());
        assertTrue(taskExecDAO.findById("e58ca1c7-178a-4012-8a71-8aa14eaf0655").isEmpty());
    }

    @Test
    public void deleteTaskExecution() {
        @SuppressWarnings("unchecked")
        TaskExec<PropagationTask> execution = (TaskExec<PropagationTask>) taskExecDAO.findById(
                TaskType.PROPAGATION, "e58ca1c7-178a-4012-8a71-8aa14eaf0655").orElseThrow();
        int executionNumber = execution.getTask().getExecs().size();

        taskExecDAO.delete(TaskType.PROPAGATION, "e58ca1c7-178a-4012-8a71-8aa14eaf0655");

        assertTrue(taskExecDAO.findById("e58ca1c7-178a-4012-8a71-8aa14eaf0655").isEmpty());

        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertEquals(task.getExecs().size(), executionNumber - 1);
    }

    @Test
    public void savePullTask() {
        PullTask task = entityFactory.newEntity(PullTask.class);
        task.setName("savePullTask");
        task.setDescription("PullTask description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.setJobDelegate(implementationDAO.findById("PullJobDelegate").orElseThrow());
        task.setDestinationRealm(realmDAO.getRoot());
        task.setCronExpression("BLA BLA");
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);

        // now adding InboundActions
        Implementation inboundActions = entityFactory.newEntity(Implementation.class);
        inboundActions.setKey("InboundActions" + UUID.randomUUID());
        inboundActions.setEngine(ImplementationEngine.JAVA);
        inboundActions.setType(IdMImplementationType.INBOUND_ACTIONS);
        inboundActions.setBody(InboundActions.class.getName());
        inboundActions = implementationDAO.save(inboundActions);

        task.add(inboundActions);

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
        task.setResource(resourceDAO.findById("ws-target-resource-1").orElseThrow());

        // this save() finally works
        task = taskDAO.save(task);
        assertNotNull(task);

        PullTask actual = (PullTask) taskDAO.findById(TaskType.PULL, task.getKey()).orElseThrow();
        assertEquals(task, actual);
    }

    @Test
    public void addAndRemoveInboundActions() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdMImplementationType.INBOUND_ACTIONS);
        implementation.setBody("TestInboundActions");
        implementation = implementationDAO.save(implementation);

        PullTask task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").
                orElseThrow();
        assertTrue(task.getActions().isEmpty());

        task.add(implementation);
        taskDAO.save(task);

        task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertEquals(1, task.getActions().size());
        assertEquals(implementation, task.getActions().getFirst());

        task.getActions().clear();
        task = taskDAO.save(task);
        assertTrue(task.getActions().isEmpty());

        task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertTrue(task.getActions().isEmpty());
    }

    @Test
    public void saveLiveSyncTask() {
        LiveSyncTask task = entityFactory.newEntity(LiveSyncTask.class);
        task.setName("saveLiveSyncTask");
        task.setDescription("LiveSyncTask description");
        task.setActive(true);
        task.setJobDelegate(implementationDAO.findById("LiveSyncJobDelegate").orElseThrow());
        task.setDestinationRealm(realmDAO.getRoot());
        task.setCronExpression("BLA BLA");
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);
        task.setCronExpression(null);
        task.setResource(resourceDAO.findById("ws-target-resource-1").orElseThrow());

        task = taskDAO.save(task);
        assertNotNull(task);

        LiveSyncTask actual = (LiveSyncTask) taskDAO.findById(TaskType.LIVE_SYNC, task.getKey()).orElseThrow();
        assertEquals(task, actual);
    }

    @Test
    public void addAndRemoveReconFilterBuilder() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdMImplementationType.RECON_FILTER_BUILDER);
        implementation.setBody("TestReconFilterBuilder");
        implementation = implementationDAO.save(implementation);

        PullTask task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").
                orElseThrow();
        assertNull(task.getReconFilterBuilder());

        task.setReconFilterBuilder(implementation);
        taskDAO.save(task);

        task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertNotNull(task.getReconFilterBuilder());

        task.setReconFilterBuilder(null);
        task = taskDAO.save(task);
        assertNull(task.getReconFilterBuilder());

        task = (PullTask) taskDAO.findById(TaskType.PULL, "c41b9b71-9bfa-4f90-89f2-84787def4c5c").orElseThrow();
        assertNull(task.getReconFilterBuilder());
    }

    @Rollback(false)
    @Test
    public void macroTaskCommandsOrdering() {
        MacroTask task = entityFactory.newEntity(MacroTask.class);
        task.setName("macro");
        task.setJobDelegate(implementationDAO.findById("MacroJobDelegate").orElseThrow());
        task.setRealm(realmDAO.getRoot());

        Implementation command1 = entityFactory.newEntity(Implementation.class);
        command1.setKey("impl1");
        command1.setEngine(ImplementationEngine.JAVA);
        command1.setType(IdRepoImplementationType.COMMAND);
        command1.setBody("TestCommand");
        command1 = implementationDAO.save(command1);

        Implementation command2 = entityFactory.newEntity(Implementation.class);
        command2.setKey("impl2");
        command2.setEngine(ImplementationEngine.GROOVY);
        command2.setType(IdRepoImplementationType.COMMAND);
        command2.setBody("class GroovyCommand implements Command<CommandArgs> {}");
        command2 = implementationDAO.save(command2);

        MacroTaskCommand macroTaskCommand1 = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand1.setCommand(command1);
        macroTaskCommand1.setMacroTask(task);
        task.add(macroTaskCommand1);

        MacroTaskCommand macroTaskCommand2 = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand2.setCommand(command2);
        macroTaskCommand2.setMacroTask(task);
        task.add(macroTaskCommand2);

        task = taskDAO.save(task);
        assertEquals(2, task.getCommands().size());
        assertEquals(macroTaskCommand1, task.getCommands().get(0));
        assertEquals(macroTaskCommand2, task.getCommands().get(1));

        task = (MacroTask) taskDAO.findById(TaskType.MACRO, task.getKey()).orElseThrow();
        assertEquals(2, task.getCommands().size());
        assertEquals(macroTaskCommand1, task.getCommands().get(0));
        assertEquals(macroTaskCommand2, task.getCommands().get(1));
    }

    @Test
    public void saveMacroTaskSameCommandMultipleOccurrences() {
        MacroTask task = entityFactory.newEntity(MacroTask.class);
        task.setRealm(realmDAO.getRoot());
        task.setJobDelegate(implementationDAO.findById("MacroJobDelegate").orElseThrow());
        task.setName("saveMacroTaskSameCommandMultipleOccurrences");
        task.setContinueOnError(true);

        Implementation command1 = entityFactory.newEntity(Implementation.class);
        command1.setKey("command1");
        command1.setType(IdRepoImplementationType.COMMAND);
        command1.setEngine(ImplementationEngine.JAVA);
        command1.setBody("clazz1");
        command1 = implementationDAO.save(command1);
        assertNotNull(command1);

        Implementation command2 = entityFactory.newEntity(Implementation.class);
        command2.setKey("command2");
        command2.setType(IdRepoImplementationType.COMMAND);
        command2.setEngine(ImplementationEngine.JAVA);
        command2.setBody("clazz2");
        command2 = implementationDAO.save(command2);
        assertNotNull(command2);

        MacroTaskCommand macroTaskCommand1 = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand1.setCommand(command1);
        macroTaskCommand1.setMacroTask(task);
        task.add(macroTaskCommand1);

        MacroTaskCommand macroTaskCommand2 = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand2.setCommand(command2);
        macroTaskCommand2.setMacroTask(task);
        task.add(macroTaskCommand2);

        MacroTaskCommand macroTaskCommand3 = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand3.setCommand(command1);
        macroTaskCommand3.setMacroTask(task);
        task.add(macroTaskCommand3);

        task = taskDAO.save(task);
        assertNotNull(task);
        assertEquals(3, task.getCommands().size());
        assertEquals(command1, task.getCommands().get(0).getCommand());
        assertEquals(command2, task.getCommands().get(1).getCommand());
        assertEquals(command1, task.getCommands().get(2).getCommand());

        MacroTask actual = (MacroTask) taskDAO.findById(TaskType.MACRO, task.getKey()).orElseThrow();
        assertEquals(task, actual);
    }

    @Test
    public void issueSYNCOPE144() {
        ExternalResource resource = resourceDAO.findById("ws-target-resource-1").orElseThrow();
        assertNotNull(resource);

        Implementation inboundActions = entityFactory.newEntity(Implementation.class);
        inboundActions.setKey("syncope144");
        inboundActions.setEngine(ImplementationEngine.JAVA);
        inboundActions.setType(IdMImplementationType.INBOUND_ACTIONS);
        inboundActions.setBody(InboundActions.class.getName());
        inboundActions = implementationDAO.save(inboundActions);

        PullTask task = entityFactory.newEntity(PullTask.class);

        task.setResource(resource);
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.add(inboundActions);
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);
        task.setJobDelegate(implementationDAO.findById("PullJobDelegate").orElseThrow());
        task.setDestinationRealm(realmDAO.getRoot());

        task = taskDAO.save(task);
        assertNotNull(task);

        PullTask actual = (PullTask) taskDAO.findById(TaskType.PULL, task.getKey()).orElseThrow();
        assertEquals(task, actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        actual.setName("issueSYNCOPE144_2");
        actual.setDescription("issueSYNCOPE144 Description_2");

        actual = taskDAO.save(actual);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }
}
