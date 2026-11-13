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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;

class FlowableDeployUtilsTest {

    private static final byte[] VALID_DEFINITION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://syncope.apache.org">
              <process id="test" isExecutable="true">
                <startEvent id="start"/>
              </process>
            </definitions>
            """.getBytes(StandardCharsets.UTF_8);

    private static final byte[] DEFINITION_WITH_EXPRESSION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://syncope.apache.org">
              <process id="test" isExecutable="true">
                <serviceTask id="task" flowable:expression="someExpression"/>
              </process>
            </definitions>
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void deployDefinition() {
        ProcessEngine engine = mock(ProcessEngine.class);
        RepositoryService repositoryService = mock(RepositoryService.class);
        DeploymentBuilder builder = mock(DeploymentBuilder.class);
        Deployment deployment = mock(Deployment.class);

        when(engine.getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.createDeployment()).thenReturn(builder);
        when(builder.addBytes("test.bpmn20.xml", VALID_DEFINITION)).thenReturn(builder);
        when(builder.deploy()).thenReturn(deployment);

        assertSame(deployment, FlowableDeployUtils.deployDefinition(engine, "test.bpmn20.xml", VALID_DEFINITION));

        verify(builder).addBytes("test.bpmn20.xml", VALID_DEFINITION);
        verify(builder).deploy();
    }

    @Test
    void deployDefinitionWithExpression() {
        ProcessEngine engine = mock(ProcessEngine.class);

        WorkflowException exception = assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(engine, "test.bpmn20.xml", DEFINITION_WITH_EXPRESSION));

        assertSame(WorkflowException.class, exception.getClass());
    }

    @Test
    void deployDefinitionWithMalformedXml() {
        ProcessEngine engine = mock(ProcessEngine.class);

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(
                        engine, "test.bpmn20.xml", "<definitions".getBytes(StandardCharsets.UTF_8)));
    }
}
