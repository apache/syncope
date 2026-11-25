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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FormAwareServiceTask;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.impl.bpmn.parser.BpmnParseHandlers;
import org.junit.jupiter.api.Test;

class ShellServiceTaskDisablingBpmnParseHandlerTest {

    private static void assertRejected(final ServiceTask serviceTask) {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellServiceTaskDisablingBpmnParseHandler().parse(null, serviceTask));
    }

    @Test
    void rejectsShellServiceTask() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId("serviceTask");
        serviceTask.setType(ServiceTask.SHELL_TASK);

        assertRejected(serviceTask);
    }

    @Test
    void rejectsShellTypeIgnoringCase() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId("serviceTask");
        serviceTask.setType("SHELL");

        assertRejected(serviceTask);
    }

    @Test
    void rejectsShellOnServiceTaskSubclasses() {
        FormAwareServiceTask formAware = new FormAwareServiceTask();
        formAware.setId("formAware");
        formAware.setType(ServiceTask.SHELL_TASK);
        assertRejected(formAware);

        HttpServiceTask http = new HttpServiceTask();
        http.setId("http");
        http.setType(ServiceTask.SHELL_TASK);
        assertRejected(http);
    }

    @Test
    void registersForAllServiceTaskSubclasses() {
        BpmnParseHandlers handlers = new BpmnParseHandlers();
        handlers.addHandlers(List.of(new ShellServiceTaskDisablingBpmnParseHandler()));

        for (Class<? extends ServiceTask> type : List.of(
                ServiceTask.class,
                CaseServiceTask.class,
                ExternalWorkerServiceTask.class,
                FormAwareServiceTask.class,
                HttpServiceTask.class,
                SendEventServiceTask.class)) {

            assertNotNull(handlers.getHandlersFor(type), type.getName());
        }
    }

    @Test
    void allowsDelegateExpressionServiceTask() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId("serviceTask");
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        serviceTask.setImplementation("${create}");

        assertDoesNotThrow(() -> new ShellServiceTaskDisablingBpmnParseHandler().parse(null, serviceTask));
    }
}
