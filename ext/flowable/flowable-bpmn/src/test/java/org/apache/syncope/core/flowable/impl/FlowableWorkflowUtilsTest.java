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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;

class FlowableWorkflowUtilsTest {

    @Test
    void isUserIngroup() {
        UserTO user = new UserTO();
        user.getMemberships().add(new MembershipTO.Builder("groupKey").groupName("group").build());

        assertTrue(FlowableWorkflowUtils.isUserIngroup(user, "group"));
        assertFalse(FlowableWorkflowUtils.isUserIngroup(user, "other"));
        assertFalse(FlowableWorkflowUtils.isUserIngroup(user, null));
        assertFalse(FlowableWorkflowUtils.isUserIngroup(new UserTO(), "group"));
    }

    @Test
    void workflowVariables() {
        String userKey = "userKey";
        String procInstId = "procInstId";

        DomainProcessEngine engine = mock(DomainProcessEngine.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        User user = mock(User.class);

        when(engine.getRuntimeService()).thenReturn(runtimeService);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceBusinessKey(FlowableRuntimeUtils.getWFProcBusinessKey(userKey))).thenReturn(query);
        when(query.singleResult()).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(procInstId);
        when(user.getKey()).thenReturn(userKey);
        when(runtimeService.getVariable(procInstId, "variable")).thenReturn("value");

        FlowableWorkflowUtils utils = new FlowableWorkflowUtils(engine);

        utils.setUserWorkflowVariable("variable", "value", user);
        verify(runtimeService).setVariable(procInstId, "variable", "value");

        assertEquals("value", utils.getUserWorkflowVariable("variable", user));
    }

    @Test
    void workflowVariablesWithoutProcessInstance() {
        DomainProcessEngine engine = mock(DomainProcessEngine.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        User user = mock(User.class);

        when(engine.getRuntimeService()).thenReturn(runtimeService);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceBusinessKey(anyString())).thenReturn(query);
        when(query.singleResult()).thenReturn(null);
        when(user.getKey()).thenReturn("userKey");

        FlowableWorkflowUtils utils = new FlowableWorkflowUtils(engine);

        assertNull(utils.getUserWorkflowVariable("variable", user));
    }
}
