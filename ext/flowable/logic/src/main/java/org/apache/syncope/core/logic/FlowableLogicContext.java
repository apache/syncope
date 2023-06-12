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

import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.flowable.api.WorkflowTaskManager;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.logic.init.FlowableLoader;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class FlowableLogicContext {

    @ConditionalOnMissingBean
    @Bean
    public FlowableLoader flowableLoader(
            final DomainProcessEngine engine,
            @Qualifier("userWorkflowDef")
            final Resource userWorkflowDef) {

        return new FlowableLoader(userWorkflowDef, engine);
    }

    @ConditionalOnMissingBean
    @Bean
    public BpmnProcessLogic bpmnProcessLogic(final BpmnProcessManager bpmnProcessManager) {
        return new BpmnProcessLogic(bpmnProcessManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRequestLogic userRequestLogic(
            final UserRequestHandler userRequestHandler,
            final UserDataBinder binder,
            final BpmnProcessManager bpmnProcessManager,
            final PropagationTaskExecutor taskExecutor,
            final UserDAO userDAO,
            final PropagationManager propagationManager) {

        return new UserRequestLogic(
                bpmnProcessManager,
                userRequestHandler,
                propagationManager,
                taskExecutor,
                binder,
                userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserWorkflowTaskLogic userWorkflowTaskLogic(
            final WorkflowTaskManager wfTaskManager,
            final UserDataBinder binder,
            final UserDAO userDAO,
            final PropagationTaskExecutor taskExecutor,
            final PropagationManager propagationManager) {

        return new UserWorkflowTaskLogic(wfTaskManager, propagationManager, taskExecutor, binder, userDAO);
    }
}
