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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class UserRequestLogicTest {

    private BpmnProcessManager bpmnProcessManager;

    private UserRequestHandler userRequestHandler;

    private PropagationManager propagationManager;

    private PropagationTaskExecutor taskExecutor;

    private UserDataBinder binder;

    private UserDAO userDAO;

    private UserRequestLogic logic;

    @BeforeEach
    void setup() {
        bpmnProcessManager = mock(BpmnProcessManager.class);
        userRequestHandler = mock(UserRequestHandler.class);
        propagationManager = mock(PropagationManager.class);
        taskExecutor = mock(PropagationTaskExecutor.class);
        binder = mock(UserDataBinder.class);
        userDAO = mock(UserDAO.class);

        logic = new UserRequestLogic(
                bpmnProcessManager, userRequestHandler, propagationManager, taskExecutor, binder, userDAO);
    }

    @Test
    void securityChecks() {
        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    UserRequestLogic.securityChecks("user", FlowableEntitlement.USER_REQUEST_LIST, "not allowed");
                    return null;
                });

        AuthContextUtils.callAs(
                "master",
                "admin",
                List.of(new SyncopeGrantedAuthority(FlowableEntitlement.USER_REQUEST_LIST, "/")),
                () -> {
                    UserRequestLogic.securityChecks("user", FlowableEntitlement.USER_REQUEST_LIST, "not allowed");
                    return null;
                });

        SyncopeClientException exception = assertThrows(
                SyncopeClientException.class,
                () -> AuthContextUtils.callAs(
                        "master",
                        "admin",
                        List.of(),
                        () -> {
                            UserRequestLogic.securityChecks(
                                    "user", FlowableEntitlement.USER_REQUEST_LIST, "not allowed");
                            return null;
                        }));

        assertEquals(ClientExceptionType.DelegatedAdministration, exception.getType());
        assertEquals(Set.of("not allowed"), exception.getElements());
    }

    @Test
    void listRequests() {
        Page<UserRequest> page = Page.empty();
        when(userRequestHandler.getUserRequests(null, Pageable.unpaged())).thenReturn(page);

        AuthContextUtils.callAs(
                "master",
                "admin",
                List.of(new SyncopeGrantedAuthority(FlowableEntitlement.USER_REQUEST_LIST, "/")),
                () -> {
                    assertSame(page, logic.listRequests(null, Pageable.unpaged()));
                    return null;
                });

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("user");
        doReturn(Optional.of(user)).when(userDAO).findById("userKey");
        when(userRequestHandler.getUserRequests("userKey", Pageable.unpaged())).thenReturn(page);

        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    assertSame(page, logic.listRequests("userKey", Pageable.unpaged()));
                    return null;
                });
    }

    @Test
    void listRequestsNotFound() {
        doReturn(Optional.empty()).when(userDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.listRequests("missing", Pageable.unpaged()));
    }

    @Test
    void startRequest() {
        User user = mock(User.class);
        WorkflowTaskExecInput input = new WorkflowTaskExecInput();
        UserRequest userRequest = new UserRequest();
        doReturn(Optional.of(user)).when(userDAO).findByUsername("user");
        when(userDAO.authFind("userKey")).thenReturn(user);
        when(userRequestHandler.start("process", user, input)).thenReturn(userRequest);

        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    assertSame(userRequest, logic.startRequest("process", input));
                    return null;
                });

        AuthContextUtils.callAs(
                "master",
                "admin",
                List.of(new SyncopeGrantedAuthority(FlowableEntitlement.USER_REQUEST_START, "/")),
                () -> {
                    assertSame(userRequest, logic.startRequest("process", "userKey", input));
                    return null;
                });

        verify(bpmnProcessManager, times(2)).exportProcess(
                "process", BpmnProcessFormat.XML, NullOutputStream.INSTANCE);
    }

    @Test
    void claimFormDenied() {
        UserRequestForm form = new UserRequestForm();
        form.setUsername("user");
        when(userRequestHandler.claimForm("task")).thenReturn(form);

        SyncopeClientException exception = assertThrows(
                SyncopeClientException.class,
                () -> AuthContextUtils.callAs(
                        "master",
                        "admin",
                        List.of(),
                        () -> logic.claimForm("task")));

        assertEquals(ClientExceptionType.DelegatedAdministration, exception.getType());
    }

    @Test
    void cancelRequest() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(userRequestHandler.parse("execution")).thenReturn(Pair.of(processInstance, "userKey"));
        when(userDAO.findUsername("userKey")).thenReturn(Optional.of("user"));

        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    logic.cancelRequest("execution", "reason");
                    return null;
                });

        verify(userRequestHandler).cancel(processInstance, "reason");
    }

    @Test
    void submitFormWithoutPropagation() {
        UserRequestForm form = new UserRequestForm();
        form.setUsername("user");

        UserUR userUR = new UserUR.Builder("userKey").build();
        UserWorkflowResult<UserUR> wfResult = new UserWorkflowResult<>(
                userUR,
                new UserWorkflowResult.PropagationInfo(new PropagationByResource<>(), null),
                Set.of());
        when(userRequestHandler.submitForm(form)).thenReturn(wfResult);
        doReturn(Optional.empty()).when(userDAO).findById("userKey");

        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    ProvisioningResult<UserTO> result = logic.submitForm(form, false);
                    assertEquals("userKey", result.getEntity().getKey());
                    assertEquals(0, result.getPropagationStatuses().size());
                    return null;
                });
    }

    @Test
    void submitFormWithPropagation() {
        UserRequestForm form = new UserRequestForm();
        form.setUsername("user");

        UserUR userUR = new UserUR.Builder("userKey").build();
        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.CREATE, "resource");
        UserWorkflowResult<UserUR> wfResult = new UserWorkflowResult<>(
                userUR,
                new UserWorkflowResult.PropagationInfo(propByRes, null),
                Set.of());
        when(userRequestHandler.submitForm(form)).thenReturn(wfResult);

        UserTO userTO = new UserTO();
        userTO.setKey("userKey");
        User user = mock(User.class);
        doReturn(Optional.of(user)).when(userDAO).findById("userKey");
        when(binder.getUserTO("userKey")).thenReturn(userTO);
        when(propagationManager.getUserUpdateTasks(any())).thenReturn(List.of());
        PropagationReporter reporter = mock(PropagationReporter.class);
        when(reporter.getStatuses()).thenReturn(List.of());
        when(taskExecutor.execute(any(), eq(true), eq("user"))).thenReturn(reporter);

        AuthContextUtils.callAs(
                "master",
                "user",
                List.of(),
                () -> {
                    ProvisioningResult<UserTO> result = logic.submitForm(form, true);
                    assertSame(userTO, result.getEntity());
                    return null;
                });

        verify(taskExecutor).execute(any(), eq(true), eq("user"));
    }
}
