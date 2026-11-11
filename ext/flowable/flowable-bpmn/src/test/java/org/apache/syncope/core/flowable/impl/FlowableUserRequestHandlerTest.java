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
package org.apache.syncope.core.flowable.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.form.FormType;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlowableUserRequestHandlerTest {

    private DomainProcessEngine engine;

    private RuntimeService runtimeService;

    private FlowableUserRequestHandler handler;

    @BeforeEach
    void setup() {
        engine = mock(DomainProcessEngine.class);
        runtimeService = mock(RuntimeService.class);
        when(engine.getRuntimeService()).thenReturn(runtimeService);

        handler = new FlowableUserRequestHandler(
                mock(UserDataBinder.class),
                "admin",
                engine,
                mock(UserDAO.class),
                mock(EntityFactory.class));
    }

    @Test
    void createProcessInstanceQuery() {
        ManagementService managementService = mock(ManagementService.class);
        when(engine.getManagementService()).thenReturn(managementService);
        when(managementService.getTableName(ExecutionEntity.class)).thenReturn("ACT_RU_EXECUTION");

        String all = handler.createProcessInstanceQuery(null).toString();
        assertTrue(all.contains("FROM ACT_RU_EXECUTION"));
        assertTrue(all.contains("BUSINESS_KEY_ NOT LIKE 'userWorkflow:%'"));
        assertTrue(all.contains("PARENT_ID_ IS NULL"));

        String forUser = handler.createProcessInstanceQuery("userKey").toString();
        assertTrue(forUser.contains("AND BUSINESS_KEY_ LIKE '%:userKey'"));
    }

    @Test
    void getUserRequest() {
        UserDAO userDAO = mock(UserDAO.class);
        when(userDAO.findUsername("userKey")).thenReturn(Optional.of("username"));

        TaskService taskService = mock(TaskService.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        Task task = mock(Task.class);
        when(engine.getTaskService()).thenReturn(taskService);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("processInstanceId")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        when(task.getTaskDefinitionKey()).thenReturn("activity");
        when(task.getId()).thenReturn("taskId");
        when(task.getFormKey()).thenReturn("formKey");

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("process:userKey");
        when(processInstance.getStartTime()).thenReturn(new Date(1_000L));
        when(processInstance.getId()).thenReturn("executionId");
        when(processInstance.getProcessInstanceId()).thenReturn("processInstanceId");

        FlowableUserRequestHandler customHandler = new FlowableUserRequestHandler(
                mock(UserDataBinder.class),
                "admin",
                engine,
                userDAO,
                mock(EntityFactory.class));

        UserRequest userRequest = customHandler.getUserRequest(processInstance);

        assertEquals("process", userRequest.getBpmnProcess());
        assertEquals("username", userRequest.getUsername());
        assertEquals("executionId", userRequest.getExecutionId());
        assertEquals("activity", userRequest.getActivityId());
        assertEquals("taskId", userRequest.getTaskId());
        assertTrue(userRequest.getHasForm());
        assertEquals(1_000L, userRequest.getStartTime().getTime());
    }

    @Test
    void getUserRequestWithoutForm() {
        UserDAO userDAO = mock(UserDAO.class);
        when(userDAO.findUsername("userKey")).thenReturn(Optional.empty());

        TaskService taskService = mock(TaskService.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        Task task = mock(Task.class);
        when(engine.getTaskService()).thenReturn(taskService);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("processInstanceId")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getFormKey()).thenReturn(null);

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("process:userKey");
        when(processInstance.getProcessInstanceId()).thenReturn("processInstanceId");

        FlowableUserRequestHandler customHandler = new FlowableUserRequestHandler(
                mock(UserDataBinder.class),
                "admin",
                engine,
                userDAO,
                mock(EntityFactory.class));

        UserRequest userRequest = customHandler.getUserRequest(processInstance);

        assertNull(userRequest.getUsername());
        assertFalse(userRequest.getHasForm());
    }

    @Test
    void fromFlowableFormType() {
        assertEquals(FormPropertyType.String, from("string"));
        assertEquals(FormPropertyType.Long, from("long"));
        assertEquals(FormPropertyType.Enum, from("enum"));
        assertEquals(FormPropertyType.Date, from("date"));
        assertEquals(FormPropertyType.Boolean, from("boolean"));
        assertEquals(FormPropertyType.Dropdown, from("dropdown"));
        assertEquals(FormPropertyType.Password, from("password"));
        assertEquals(FormPropertyType.String, from("unknown"));

        FormType noName = mock(FormType.class);
        when(noName.getName()).thenReturn(null);
        assertEquals(FormPropertyType.String, FlowableUserRequestHandler.fromFlowableFormType(noName));
    }

    @Test
    void cancelMainProcess() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessDefinitionKey()).thenReturn(FlowableRuntimeUtils.WF_PROCESS_ID);

        assertThrows(WorkflowException.class, () -> handler.cancel(processInstance, "reason"));

        verifyNoInteractions(runtimeService);
    }

    private static FormPropertyType from(final String flowableType) {
        FormType formType = mock(FormType.class);
        when(formType.getName()).thenReturn(flowableType);
        return FlowableUserRequestHandler.fromFlowableFormType(formType);
    }
}
