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
package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.common.rest.api.service.UserWorkflowTaskService;
import org.apache.syncope.core.logic.BpmnProcessLogic;
import org.apache.syncope.core.logic.UserRequestLogic;
import org.apache.syncope.core.logic.UserWorkflowTaskLogic;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.rest.cxf.service.BpmnProcessServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserRequestServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserWorkflowTaskServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlowableRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public BpmnProcessService bpmnProcessService(final BpmnProcessLogic bpmnProcessLogic) {
        return new BpmnProcessServiceImpl(bpmnProcessLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRequestService userRequestService(final UserRequestLogic userRequestLogic, final UserDAO userDAO) {
        return new UserRequestServiceImpl(userRequestLogic, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserWorkflowTaskService userWorkflowTaskService(final UserWorkflowTaskLogic userWorkflowTaskLogic) {
        return new UserWorkflowTaskServiceImpl(userWorkflowTaskLogic);
    }
}
