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
package org.apache.syncope.client.console;

import org.apache.syncope.client.console.resources.BpmnProcessGETResource;
import org.apache.syncope.client.console.resources.BpmnProcessPUTResource;
import org.apache.syncope.client.console.rest.BpmnProcessRestClient;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.rest.UserWorkflowTaskRestClient;
import org.apache.syncope.client.console.wizards.any.UserRequestFormFinalizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlowableConsoleContext {

    @ConditionalOnMissingBean
    @Bean
    public BpmnProcessRestClient bpmnProcessRestClient() {
        return new BpmnProcessRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRequestRestClient userRequestRestClient() {
        return new UserRequestRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserWorkflowTaskRestClient userWorkflowTaskRestClient() {
        return new UserWorkflowTaskRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public BpmnProcessGETResource bpmnProcessGETResource(final BpmnProcessRestClient bpmnProcessRestClient) {
        return new BpmnProcessGETResource(bpmnProcessRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public BpmnProcessPUTResource bpmnProcessPUTResource(final BpmnProcessRestClient bpmnProcessRestClient) {
        return new BpmnProcessPUTResource(bpmnProcessRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRequestFormFinalizer userRequestFormFinalizer(final UserRequestRestClient userRequestRestClient) {
        return new UserRequestFormFinalizer(userRequestRestClient);
    }
}
