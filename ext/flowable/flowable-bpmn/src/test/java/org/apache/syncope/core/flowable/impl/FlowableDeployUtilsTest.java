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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;

class FlowableDeployUtilsTest {

    private static final byte[] VALID_DEFINITION =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://syncope.apache.org">
              <process id="test" isExecutable="true">
                <startEvent id="start"/>
              </process>
            </definitions>
            """.
                    getBytes(StandardCharsets.UTF_8);

    private static final String BPMN_HEADER =
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
            + "xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"test\">";

    private static final String MALICIOUS = "${''.getClass().forName('java.io.File')}";

    private static ProcessEngine mockEngine() {
        ProcessEngine processEngine = mock(ProcessEngine.class);
        when(processEngine.getRepositoryService()).then(ic -> {
            DeploymentBuilder deploymentBuilder = mock(DeploymentBuilder.class);
            when(deploymentBuilder.addBytes(anyString(), any(byte[].class))).thenReturn(deploymentBuilder);

            RepositoryService service = mock(RepositoryService.class);
            when(service.createDeployment()).thenReturn(deploymentBuilder);

            return service;
        });
        return processEngine;
    }

    private static String bpmn(final String body) {
        return BPMN_HEADER + "<process id=\"p\">" + body + "</process></definitions>";
    }

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
    void deployDefinitionWithMalformedXml() {
        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(
                        mockEngine(), "test.bpmn20.xml", "<definitions".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shippedWorkflowAllowed() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/userWorkflow.bpmn20.xml")) {
            assertNotNull(in, "Shipped workflow definition not found on classpath");
            FlowableDeployUtils.deployDefinition(mockEngine(), "", in.readAllBytes());
        }
    }

    @Test
    void maliciousConditionExpressionRejected() {
        String bpmn = bpmn(
                "<sequenceFlow id=\"f\" sourceRef=\"a\" targetRef=\"b\">"
                + "<conditionExpression><![CDATA[" + MALICIOUS + "]]></conditionExpression>"
                + "</sequenceFlow>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousDelegateExpressionRejected() {
        String bpmn = bpmn(
                "<serviceTask id=\"t\" "
                + "flowable:delegateExpression=\"${java.lang.Runtime.getRuntime().exec('whoami')}\"/>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousListenerExpressionRejected() {
        String bpmn = bpmn(
                "<extensionElements>"
                + "<flowable:executionListener event=\"start\" expression=\"" + MALICIOUS + "\"/>"
                + "</extensionElements>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousConditionalEventRejected() {
        String bpmn = bpmn(
                "<intermediateCatchEvent id=\"e\"><conditionalEventDefinition>"
                + "<condition><![CDATA[" + MALICIOUS + "]]></condition>"
                + "</conditionalEventDefinition></intermediateCatchEvent>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousMultiInstanceCompletionConditionRejected() {
        String bpmn = bpmn(
                "<userTask id=\"t\"><multiInstanceLoopCharacteristics>"
                + "<completionCondition><![CDATA[" + MALICIOUS + "]]></completionCondition>"
                + "</multiInstanceLoopCharacteristics></userTask>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousTimerExpressionRejected() {
        String bpmn = bpmn(
                "<intermediateCatchEvent id=\"e\"><timerEventDefinition>"
                + "<timeDate><![CDATA[" + MALICIOUS + "]]></timeDate>"
                + "</timerEventDefinition></intermediateCatchEvent>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void maliciousFieldExpressionRejected() {
        String bpmn = bpmn(
                "<serviceTask id=\"s\"><extensionElements><flowable:field name=\"f\">"
                + "<flowable:expression><![CDATA[" + MALICIOUS + "]]></flowable:expression>"
                + "</flowable:field></extensionElements></serviceTask>");

        assertThrows(
                WorkflowException.class,
                () -> FlowableDeployUtils.deployDefinition(mock(ProcessEngine.class), "", bpmn.getBytes()));
    }

    @Test
    void allowedUelFormalExpression() {
        String bpmn = bpmn(
                "<userTask id=\"t\"><humanPerformer><resourceAssignmentExpression>"
                + "<formalExpression>${user.getUsername()}</formalExpression>"
                + "</resourceAssignmentExpression></humanPerformer></userTask>");

        assertDoesNotThrow(() -> FlowableDeployUtils.deployDefinition(mockEngine(), "", bpmn.getBytes()));
    }

    @Test
    void allowedPlainFormalExpressionAssignment() {
        String bpmn = bpmn(
                "<userTask id=\"t\"><potentialOwner><resourceAssignmentExpression>"
                + "<formalExpression>group(admin),user(john)</formalExpression>"
                + "</resourceAssignmentExpression></potentialOwner></userTask>");

        assertDoesNotThrow(() -> FlowableDeployUtils.deployDefinition(mockEngine(), "", bpmn.getBytes()));
    }

    @Test
    void allowedLiteralElementTexts() {
        String bpmn = bpmn(
                "<userTask id=\"t\"><multiInstanceLoopCharacteristics>"
                + "<loopCardinality>3</loopCardinality>"
                + "</multiInstanceLoopCharacteristics></userTask>"
                + "<intermediateCatchEvent id=\"e\"><timerEventDefinition>"
                + "<timeDuration>PT1H</timeDuration>"
                + "</timerEventDefinition></intermediateCatchEvent>");

        assertDoesNotThrow(() -> FlowableDeployUtils.deployDefinition(mockEngine(), "", bpmn.getBytes()));
    }
}
