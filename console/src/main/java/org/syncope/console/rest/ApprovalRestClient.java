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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowFormTO;

/**
 * Console client for invoking Rest Todo services.
 */
@Component
public class ApprovalRestClient extends AbstractBaseRestClient {

    public List<WorkflowFormTO> getForms() {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "user/workflow/form/list", WorkflowFormTO[].class));
    }

    public WorkflowFormTO claimForm(final String taskId) {
        return restTemplate.getForObject(
                baseURL + "user/workflow/form/claim/{taskId}",
                WorkflowFormTO.class, taskId);
    }

    public void submitForm(final WorkflowFormTO form) {
        restTemplate.postForObject(baseURL + "user/workflow/form/submit",
                form, UserTO.class);
    }
}
