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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.to.WorkflowDefinitionTO;
import org.apache.syncope.services.WorkflowService;

public class WorkflowServiceProxy extends SpringServiceProxy implements WorkflowService {

    public WorkflowServiceProxy(String baseUrl, SpringRestTemplate callback) {
        super(baseUrl, callback);
    }

    @Override
    public WorkflowDefinitionTO getDefinition(String type) {
        return getRestTemplate()
                .getForObject(baseUrl + "workflow/definition/" + type, WorkflowDefinitionTO.class);
    }

    @Override
    public void updateDefinition(String type, WorkflowDefinitionTO definition) {
        getRestTemplate().put(baseUrl + "workflow/definition/" + type, definition);
    }

    @Override
    public List<String> getDefinedTasks(final String type) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "workflow/tasks/{type}", String.class, type));
    }

}
