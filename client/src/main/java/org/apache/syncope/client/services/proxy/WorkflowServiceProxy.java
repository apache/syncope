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
package org.apache.syncope.client.services.proxy;

import java.util.Arrays;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.services.WorkflowTasks;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.types.AttributableType;
import org.springframework.web.client.RestTemplate;

public class WorkflowServiceProxy extends SpringServiceProxy implements WorkflowService {

    public WorkflowServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public WorkflowDefinitionTO getDefinition(final AttributableType type) {
        return getRestTemplate().getForObject(baseUrl + "workflow/definition/" + type.name().toLowerCase(),
                WorkflowDefinitionTO.class);
    }

    @Override
    public void updateDefinition(final AttributableType type, final WorkflowDefinitionTO definition) {
        getRestTemplate().put(baseUrl + "workflow/definition/" + type.name().toLowerCase(), definition);
    }

    @Override
    public WorkflowTasks getDefinedTasks(final AttributableType type) {
        return new WorkflowTasks(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "workflow/tasks/{type}", String[].class, type.name().toLowerCase())));
    }
}
