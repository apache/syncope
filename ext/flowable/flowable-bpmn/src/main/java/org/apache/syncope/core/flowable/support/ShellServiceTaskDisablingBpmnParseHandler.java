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

import java.util.Set;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FormAwareServiceTask;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;

public class ShellServiceTaskDisablingBpmnParseHandler extends AbstractBpmnParseHandler<ServiceTask> {

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return ServiceTask.class;
    }

    @Override
    public Set<Class<? extends BaseElement>> getHandledTypes() {
        // Flowable looks up parse handlers by exact element class, so all known ServiceTask subclasses need to be
        // registered explicitly; otherwise flowable:type="shell" can be used on e.g. a FormAwareServiceTask.
        return Set.of(
                ServiceTask.class,
                CaseServiceTask.class,
                ExternalWorkerServiceTask.class,
                FormAwareServiceTask.class,
                HttpServiceTask.class,
                SendEventServiceTask.class);
    }

    @Override
    protected void executeParse(final BpmnParse bpmnParse, final ServiceTask element) {
        if (ServiceTask.SHELL_TASK.equalsIgnoreCase(element.getType())) {
            throw new IllegalArgumentException("Shell Service Tasks are not allowed");
        }
    }
}
