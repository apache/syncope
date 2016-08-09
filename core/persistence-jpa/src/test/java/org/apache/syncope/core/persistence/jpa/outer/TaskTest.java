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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;

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
    
    @Test
    public void read() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);
        
        assertNotNull(task.getExecs());
        assertFalse(task.getExecs().isEmpty());
        assertEquals(1, task.getExecs().size());
    }
    
    @Test
    public void save() {
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
        task.setAttributes(attributes);
        
        task = taskDAO.save(task);
        assertNotNull(task);
        
        PropagationTask actual = taskDAO.find(task.getKey());
        assertEquals(task, actual);
        
        taskDAO.flush();
        
        resource = resourceDAO.find("ws-target-resource-1");
        assertTrue(taskDAO.findAll(
                TaskType.PROPAGATION, resource, null, null, null, -1, -1, Collections.<OrderByClause>emptyList()).
                contains(task));
    }
    
    @Test
    public void addPropagationTaskExecution() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);
        
        int executionNumber = task.getExecs().size();
        
        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setTask(task);
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());
        execution.setStart(new Date());
        task.add(execution);
        
        taskDAO.save(task);
        taskDAO.flush();
        
        task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);
        
        assertEquals(executionNumber + 1, task.getExecs().size());
    }
    
    @Test
    public void addPullTaskExecution() {
        PullTask task = taskDAO.find("c41b9b71-9bfa-4f90-89f2-84787def4c5c");
        assertNotNull(task);
        
        int executionNumber = task.getExecs().size();
        
        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(new Date());
        execution.setMessage("A message");
        task.add(execution);
        
        taskDAO.save(task);
        taskDAO.flush();
        
        task = taskDAO.find("c41b9b71-9bfa-4f90-89f2-84787def4c5c");
        assertNotNull(task);
        
        assertEquals(executionNumber + 1, task.getExecs().size());
    }
    
    @Test
    public void addPushTaskExecution() {
        PushTask task = taskDAO.find("af558be4-9d2f-4359-bf85-a554e6e90be1");
        assertNotNull(task);
        
        int executionNumber = task.getExecs().size();
        
        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setStatus("Text-free status");
        execution.setTask(task);
        execution.setStart(new Date());
        execution.setMessage("A message");
        task.add(execution);
        
        taskDAO.save(task);
        taskDAO.flush();
        
        task = taskDAO.find("af558be4-9d2f-4359-bf85-a554e6e90be1");
        assertNotNull(task);
        
        assertEquals(executionNumber + 1, task.getExecs().size());
    }
    
    @Test
    public void deleteTask() {
        taskDAO.delete("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        
        taskDAO.flush();
        
        assertNull(taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c"));
        assertNull(taskExecDAO.find("e58ca1c7-178a-4012-8a71-8aa14eaf0655"));
    }
    
    @Test
    public void deleteTaskExecution() {
        TaskExec execution = taskExecDAO.find("e58ca1c7-178a-4012-8a71-8aa14eaf0655");
        int executionNumber = execution.getTask().getExecs().size();
        
        taskExecDAO.delete("e58ca1c7-178a-4012-8a71-8aa14eaf0655");
        
        taskExecDAO.flush();
        
        assertNull(taskExecDAO.find("e58ca1c7-178a-4012-8a71-8aa14eaf0655"));
        
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertEquals(task.getExecs().size(), executionNumber - 1);
    }
    
    @Test
    public void savePullTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);
        
        AnyTemplatePullTask template = entityFactory.newEntity(AnyTemplatePullTask.class);
        template.set(new UserTO());
        
        PullTask task = entityFactory.newEntity(PullTask.class);
        task.setName("savePullTask");
        task.setDescription("PullTask description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.add(template);
        task.setCronExpression("BLA BLA");
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);

        // this save() fails because of an invalid Cron Expression
        InvalidEntityException exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);
        
        task.setCronExpression(null);
        // this save() fails because a PullTask requires a target resource
        exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);
        
        task.setResource(resource);
        task.getActionsClassNames().add(getClass().getName());

        // this save() fails because jobActionsClassName does not implement 
        // the right interface
        exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);
        
        task.getActionsClassNames().clear();
        task.getActionsClassNames().add(PullActions.class.getName());
        // this save() finally works
        task = taskDAO.save(task);
        assertNotNull(task);
        
        PullTask actual = taskDAO.find(task.getKey());
        assertEquals(task, actual);
    }
    
    @Test
    public void issueSYNCOPE144() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);
        
        PullTask task = entityFactory.newEntity(PullTask.class);
        
        task.setResource(resource);
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setActive(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.getActionsClassNames().add(PullActions.class.getName());
        task.setMatchingRule(MatchingRule.UPDATE);
        task.setUnmatchingRule(UnmatchingRule.PROVISION);
        
        task = taskDAO.save(task);
        assertNotNull(task);
        
        PullTask actual = taskDAO.find(task.getKey());
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
