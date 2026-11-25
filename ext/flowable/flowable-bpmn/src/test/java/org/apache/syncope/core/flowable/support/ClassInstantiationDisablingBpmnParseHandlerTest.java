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
package org.apache.syncope.core.flowable.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CollectionHandler;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableHttpRequestHandler;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.bpmn.model.VariableAggregationDefinitions;
import org.flowable.common.engine.impl.util.io.StringStreamSource;
import org.junit.jupiter.api.Test;

class ClassInstantiationDisablingBpmnParseHandlerTest {

    private static final String SHELL_CLASS = "org.flowable.engine.impl.bpmn.behavior.ShellActivityBehavior";

    private static Process processWith(final FlowElement element) {
        Process process = new Process();
        process.setId("test");
        process.addFlowElement(element);
        return process;
    }

    private static ServiceTask serviceTask() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId("serviceTask");
        return serviceTask;
    }

    private static FlowableListener listener(final String implementationType, final String implementation) {
        FlowableListener listener = new FlowableListener();
        listener.setEvent("start");
        listener.setImplementationType(implementationType);
        listener.setImplementation(implementation);
        return listener;
    }

    private static void assertRejected(final Process process) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ClassInstantiationDisablingBpmnParseHandler().parse(null, process));
        assertTrue(ex.getMessage().contains(SHELL_CLASS), ex.getMessage());
    }

    @Test
    void rejectsClassBasedServiceTaskFromXml() {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://syncope.apache.org/test">
                  <process id="shell" isExecutable="true">
                    <serviceTask id="t1" flowable:class="%s">
                      <extensionElements>
                        <flowable:field name="command" stringValue="sh"/>
                        <flowable:field name="arg1" stringValue="-c"/>
                        <flowable:field name="arg2" stringValue="id"/>
                      </extensionElements>
                    </serviceTask>
                  </process>
                </definitions>
                """.
                        formatted(SHELL_CLASS);

        Process process = new BpmnXMLConverter().convertToBpmnModel(
                new StringStreamSource(xml), false, true).getProcesses().get(0);

        assertRejected(process);
    }

    @Test
    void allowsDelegateExpressionAndExpressionServiceTasks() {
        for (String implementationType : List.of(
                ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION,
                ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION)) {

            ServiceTask serviceTask = serviceTask();
            serviceTask.setImplementationType(implementationType);
            serviceTask.setImplementation("${create}");

            assertDoesNotThrow(() -> new ClassInstantiationDisablingBpmnParseHandler().
                    parse(null, processWith(serviceTask)));
        }
    }

    @Test
    void rejectsClassBasedExecutionListener() {
        ServiceTask serviceTask = serviceTask();
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        serviceTask.setImplementation("${create}");
        serviceTask.setExecutionListeners(List.of(listener(ImplementationType.IMPLEMENTATION_TYPE_CLASS, SHELL_CLASS)));

        assertRejected(processWith(serviceTask));
    }

    @Test
    void rejectsClassBasedCustomPropertiesResolver() {
        FlowableListener listener = listener(
                ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${create}");
        listener.setCustomPropertiesResolverImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        listener.setCustomPropertiesResolverImplementation(SHELL_CLASS);

        ServiceTask serviceTask = serviceTask();
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        serviceTask.setImplementation("${create}");
        serviceTask.setExecutionListeners(List.of(listener));

        assertRejected(processWith(serviceTask));
    }

    @Test
    void rejectsClassBasedTaskListener() {
        UserTask userTask = new UserTask();
        userTask.setId("userTask");
        userTask.setTaskListeners(List.of(
                listener(ImplementationType.IMPLEMENTATION_TYPE_CLASS, SHELL_CLASS)));

        assertRejected(processWith(userTask));
    }

    @Test
    void rejectsClassBasedProcessEventListener() {
        EventListener listener = new EventListener();
        listener.setEvents("ENTITY_CREATED");
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        listener.setImplementation(SHELL_CLASS);

        Process process = new Process();
        process.setId("test");
        process.setEventListeners(List.of(listener));

        assertRejected(process);
    }

    @Test
    void rejectsClassBasedBusinessRuleTask() {
        BusinessRuleTask businessRuleTask = new BusinessRuleTask();
        businessRuleTask.setId("businessRuleTask");
        businessRuleTask.setClassName(SHELL_CLASS);

        assertRejected(processWith(businessRuleTask));
    }

    @Test
    void rejectsClassBasedHttpHandler() {
        HttpServiceTask serviceTask = new HttpServiceTask();
        serviceTask.setId("httpServiceTask");

        FlowableHttpRequestHandler handler = new FlowableHttpRequestHandler();
        handler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        handler.setImplementation(SHELL_CLASS);
        serviceTask.setHttpRequestHandler(handler);

        assertRejected(processWith(serviceTask));
    }

    @Test
    void rejectsClassBasedCollectionHandler() {
        CollectionHandler handler = new CollectionHandler();
        handler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        handler.setImplementation(SHELL_CLASS);

        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setHandler(handler);

        ServiceTask serviceTask = serviceTask();
        serviceTask.setLoopCharacteristics(loopCharacteristics);

        assertRejected(processWith(serviceTask));
    }

    @Test
    void rejectsClassBasedVariableAggregation() {
        VariableAggregationDefinition aggregation = new VariableAggregationDefinition();
        aggregation.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        aggregation.setImplementation(SHELL_CLASS);

        VariableAggregationDefinitions aggregations = new VariableAggregationDefinitions();
        aggregations.getAggregations().add(aggregation);

        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setAggregations(aggregations);

        ServiceTask serviceTask = serviceTask();
        serviceTask.setLoopCharacteristics(loopCharacteristics);

        assertRejected(processWith(serviceTask));
    }

    @Test
    void rejectsClassBasedServiceTaskInSubProcess() {
        ServiceTask serviceTask = serviceTask();
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        serviceTask.setImplementation(SHELL_CLASS);

        SubProcess subProcess = new SubProcess();
        subProcess.setId("subProcess");
        subProcess.addFlowElement(serviceTask);

        assertRejected(processWith(subProcess));
    }
}
