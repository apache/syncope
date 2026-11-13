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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlowableBpmnProcessManagerTest {

    private DomainProcessEngine engine;

    private RepositoryService repositoryService;

    private ProcessDefinitionQuery processDefinitionQuery;

    private FlowableBpmnProcessManager manager;

    @BeforeEach
    void setup() {
        engine = mock(DomainProcessEngine.class);
        repositoryService = mock(RepositoryService.class);
        processDefinitionQuery = mock(ProcessDefinitionQuery.class);

        when(engine.getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);

        manager = new FlowableBpmnProcessManager(engine);
    }

    @Test
    void getProcesses() {
        ProcessDefinition procDef = mock(ProcessDefinition.class);
        when(procDef.getKey()).thenReturn(FlowableRuntimeUtils.WF_PROCESS_ID);
        when(procDef.getName()).thenReturn("User workflow");
        when(procDef.getDeploymentId()).thenReturn("deployment");

        ModelQuery modelQuery = mock(ModelQuery.class);
        Model model = mock(Model.class);
        when(repositoryService.createModelQuery()).thenReturn(modelQuery);
        when(modelQuery.deploymentId("deployment")).thenReturn(modelQuery);
        when(modelQuery.singleResult()).thenReturn(model);
        when(model.getId()).thenReturn("model");

        when(processDefinitionQuery.list()).thenReturn(List.of(procDef));

        List<BpmnProcess> processes = manager.getProcesses();

        assertEquals(1, processes.size());
        assertEquals(FlowableRuntimeUtils.WF_PROCESS_ID, processes.getFirst().getKey());
        assertEquals("User workflow", processes.getFirst().getName());
        assertEquals("model", processes.getFirst().getModelId());
        assertTrue(processes.getFirst().isUserWorkflow());
    }

    @Test
    void exportProcessNotFound() {
        when(processDefinitionQuery.singleResult()).thenReturn(null);

        assertThrows(
                NotFoundException.class,
                () -> manager.exportProcess("missing", BpmnProcessFormat.XML, mock(OutputStream.class)));
    }

    @Test
    void deleteMainProcess() {
        assertThrows(WorkflowException.class, () -> manager.deleteProcess(FlowableRuntimeUtils.WF_PROCESS_ID));

        verifyNoInteractions(repositoryService);
    }

    @Test
    void deleteProcess() {
        ProcessDefinition procDef = mock(ProcessDefinition.class);
        when(procDef.getDeploymentId()).thenReturn("deployment");

        ModelQuery modelQuery = mock(ModelQuery.class);
        Model model = mock(Model.class);
        when(repositoryService.createModelQuery()).thenReturn(modelQuery);
        when(modelQuery.deploymentId("deployment")).thenReturn(modelQuery);
        when(modelQuery.singleResult()).thenReturn(model);
        when(model.getId()).thenReturn("model");
        when(processDefinitionQuery.list()).thenReturn(List.of(procDef));

        manager.deleteProcess("process");

        verify(repositoryService).deleteModel("model");
        verify(repositoryService).deleteDeployment("deployment", true);
    }
}
