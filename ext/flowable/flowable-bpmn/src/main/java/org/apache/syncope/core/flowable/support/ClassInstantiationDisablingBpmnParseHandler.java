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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.AbstractFlowableHttpHandler;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CollectionHandler;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;

/**
 * Rejects class-based implementations in BPMN definitions, unless the referenced class is explicitly allowed.
 * <p>
 * Flowable instantiates the classes referenced via {@code flowable:class} (service tasks, business rule tasks,
 * execution / task / event listeners, HTTP handlers, collection handlers and variable aggregations) by itself,
 * without going through UEL, so the expression sandbox cannot protect against them.
 * <p>
 * Delegate expressions and expressions are not affected; the default allowlist is empty, as the stock Syncope
 * definitions only use {@code delegateExpression}.
 */
public class ClassInstantiationDisablingBpmnParseHandler extends AbstractBpmnParseHandler<Process> {

    protected static void checkImplementation(
            final String implementationType, final String implementation, final String reference) {

        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(implementationType)) {
            throwClassImplementationsNotAllowed(implementation, reference);
        }
    }

    protected static void throwClassImplementationsNotAllowed(final String className, final String reference) {
        throw new IllegalArgumentException(
                "Class-based implementation not allowed for " + reference + ": " + className);
    }

    protected static void checkFlowElements(final String processId, final FlowElementsContainer container) {
        for (FlowElement element : container.getFlowElements()) {
            String reference = StringUtils.isBlank(element.getId()) ? processId : element.getId();

            if (element instanceof ServiceTask serviceTask) {
                checkImplementation(serviceTask.getImplementationType(), serviceTask.getImplementation(), reference);
                if (serviceTask instanceof HttpServiceTask httpServiceTask) {
                    checkHttpHandler(httpServiceTask.getHttpRequestHandler(), reference);
                    checkHttpHandler(httpServiceTask.getHttpResponseHandler(), reference);
                }
            } else if (element instanceof BusinessRuleTask businessRuleTask
                    && StringUtils.isNotBlank(businessRuleTask.getClassName())) {

                throwClassImplementationsNotAllowed(businessRuleTask.getClassName(), reference);
            }

            if (element instanceof Activity activity && activity.getLoopCharacteristics() != null) {
                checkLoopCharacteristics(activity.getLoopCharacteristics(), reference);
            }

            checkListeners(reference, element.getExecutionListeners());
            if (element instanceof UserTask userTask) {
                checkListeners(reference, userTask.getTaskListeners());
            }

            if (element instanceof FlowElementsContainer subContainer) {
                checkFlowElements(processId, subContainer);
            }
        }
    }

    protected static void checkLoopCharacteristics(
            final MultiInstanceLoopCharacteristics loopCharacteristics, final String reference) {

        CollectionHandler handler = loopCharacteristics.getHandler();
        if (handler != null) {
            checkImplementation(handler.getImplementationType(), handler.getImplementation(), reference);
        }

        if (loopCharacteristics.getAggregations() != null) {
            for (VariableAggregationDefinition aggregation
                    : loopCharacteristics.getAggregations().getAggregations()) {

                checkImplementation(
                        aggregation.getImplementationType(), aggregation.getImplementation(), reference);
            }
        }
    }

    protected static void checkHttpHandler(final AbstractFlowableHttpHandler handler, final String reference) {
        if (handler != null) {
            checkImplementation(handler.getImplementationType(), handler.getImplementation(), reference);
        }
    }

    protected static void checkListeners(final String reference, final List<FlowableListener> listeners) {
        if (listeners == null) {
            return;
        }

        for (FlowableListener listener : listeners) {
            checkImplementation(listener.getImplementationType(), listener.getImplementation(), reference);
            checkImplementation(
                    listener.getCustomPropertiesResolverImplementationType(),
                    listener.getCustomPropertiesResolverImplementation(),
                    reference);
        }
    }

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return Process.class;
    }

    @Override
    protected void executeParse(final BpmnParse bpmnParse, final Process process) {
        checkListeners(process.getId(), process.getExecutionListeners());

        if (process.getEventListeners() != null) {
            for (EventListener listener : process.getEventListeners()) {
                checkImplementation(listener.getImplementationType(), listener.getImplementation(), process.getId());
            }
        }

        checkFlowElements(process.getId(), process);
    }
}
