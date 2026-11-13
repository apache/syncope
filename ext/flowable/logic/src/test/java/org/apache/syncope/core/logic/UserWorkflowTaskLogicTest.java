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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserWorkflowTaskLogicTest {

    private WorkflowTaskManager wfTaskManager;

    private PropagationManager propagationManager;

    private PropagationTaskExecutor taskExecutor;

    private UserDataBinder binder;

    private UserDAO userDAO;

    private UserWorkflowTaskLogic logic;

    @BeforeEach
    void setup() {
        wfTaskManager = mock(WorkflowTaskManager.class);
        propagationManager = mock(PropagationManager.class);
        taskExecutor = mock(PropagationTaskExecutor.class);
        binder = mock(UserDataBinder.class);
        userDAO = mock(UserDAO.class);

        logic = new UserWorkflowTaskLogic(wfTaskManager, propagationManager, taskExecutor, binder, userDAO);
    }

    @Test
    void getAvailableTasks() {
        User user = mock(User.class);
        when(user.getKey()).thenReturn("userKey");
        when(userDAO.authFind("userKey")).thenReturn(user);

        List<WorkflowTask> tasks = List.of(new WorkflowTask());
        when(wfTaskManager.getAvailableTasks("userKey")).thenReturn(tasks);

        assertSame(tasks, logic.getAvailableTasks("userKey"));
    }

    @Test
    void executeNextTask() {
        WorkflowTaskExecInput input = new WorkflowTaskExecInput();
        UserWorkflowResult<String> wfResult = new UserWorkflowResult<>(
                "userKey",
                new UserWorkflowResult.PropagationInfo(new PropagationByResource<>(), null),
                Set.of("task"));
        when(wfTaskManager.executeNextTask(input)).thenReturn(wfResult);
        when(propagationManager.getUserUpdateTasks(any())).thenReturn(List.of());

        PropagationReporter reporter = mock(PropagationReporter.class);
        when(taskExecutor.execute(any(), eq(false), eq("admin"))).thenReturn(reporter);

        UserTO userTO = new UserTO();
        userTO.setKey("userKey");
        when(binder.getUserTO("userKey")).thenReturn(userTO);

        AuthContextUtils.callAs(
                "master",
                "admin",
                List.of(),
                () -> {
                    assertSame(userTO, logic.executeNextTask(input));
                    return null;
                });

        verify(taskExecutor).execute(any(), eq(false), eq("admin"));
    }

    @Test
    void resolveReference() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference((Method) null));
    }
}
