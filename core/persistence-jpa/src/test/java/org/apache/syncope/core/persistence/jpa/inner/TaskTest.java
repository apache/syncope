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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findByName() {
        Optional<SchedTask> task = taskDAO.findByName(TaskType.SCHEDULED, "SampleJob Task");
        assertTrue(task.isPresent());
        assertEquals(taskDAO.findById(TaskType.SCHEDULED, "e95555d2-1b09-42c8-b25b-f4c4ec597979"), task);
    }

    @Test
    public void findWithoutExecs() {
        List<PropagationTask> tasks = taskDAO.findToExec(TaskType.PROPAGATION);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
    }

    @Test
    public void findPaginated() {
        List<PropagationTask> tasks = taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, PageRequest.of(0, 2));
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (PropagationTask task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, PageRequest.of(1, 2));
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (PropagationTask task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(TaskType.PROPAGATION, null, null, null, null, PageRequest.of(1000, 2));
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

        PropagationTask actual = (PropagationTask) taskDAO.findById(TaskType.PROPAGATION, task.getKey()).orElseThrow();
        assertEquals(task, actual);
    }

    @Test
    public void saveMacroTask() throws Exception {
        MacroTask task = entityFactory.newEntity(MacroTask.class);
        task.setRealm(realmDAO.getRoot());
        task.setJobDelegate(implementationDAO.findById("MacroJobDelegate").orElseThrow());
        task.setName("Macro test");
        task.setContinueOnError(true);

        Implementation command = entityFactory.newEntity(Implementation.class);
        command.setKey("command");
        command.setType(IdRepoImplementationType.COMMAND);
        command.setEngine(ImplementationEngine.JAVA);
        command.setBody("clazz");
        command = implementationDAO.save(command);
        assertNotNull(command);

        MacroTaskCommand macroTaskCommand = entityFactory.newEntity(MacroTaskCommand.class);
        macroTaskCommand.setCommand(command);
        macroTaskCommand.setMacroTask(task);
        task.add(macroTaskCommand);

        FormPropertyDef formPropertyDef = entityFactory.newEntity(FormPropertyDef.class);
        formPropertyDef.setName("one");
        formPropertyDef.getLabels().put(Locale.ENGLISH, "One");
        formPropertyDef.setType(FormPropertyType.Enum);
        formPropertyDef.setMacroTask(task);
        task.add(formPropertyDef);

        Implementation macroActions = entityFactory.newEntity(Implementation.class);
        macroActions.setKey("macroActions");
        macroActions.setType(IdRepoImplementationType.MACRO_ACTIONS);
        macroActions.setEngine(ImplementationEngine.JAVA);
        macroActions.setBody("clazz");
        macroActions = implementationDAO.save(macroActions);
        assertNotNull(macroActions);
        task.setMacroAction(macroActions);

        try {
            taskDAO.save(task);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
        formPropertyDef.setEnumValues(Map.of("key", "value"));

        task = taskDAO.save(task);
        assertNotNull(task);
        assertEquals(1, task.getCommands().size());
        assertEquals(command, task.getCommands().getFirst().getCommand());
        assertEquals(1, task.getFormPropertyDefs().size());
        assertNotNull(task.getFormPropertyDefs().getFirst().getKey());
        assertEquals(formPropertyDef, task.getFormPropertyDefs().getFirst());

        MacroTask actual = (MacroTask) taskDAO.findById(TaskType.MACRO, task.getKey()).orElseThrow();
        assertEquals(task, actual);
    }

    @Test
    public void delete() {
        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        ExternalResource resource = task.getResource();
        assertNotNull(resource);

        taskDAO.delete(task);

        assertTrue(taskDAO.findById(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").isEmpty());

        resource = resourceDAO.findById(resource.getKey()).orElseThrow();
        assertFalse(taskDAO.<PropagationTask>findAll(
                TaskType.PROPAGATION, resource, null, null, null, Pageable.unpaged()).
                contains(task));
    }
}
