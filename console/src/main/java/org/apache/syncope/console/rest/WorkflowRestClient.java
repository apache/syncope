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
package org.apache.syncope.console.rest;

import org.springframework.stereotype.Component;
import org.apache.syncope.client.to.WorkflowDefinitionTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.SyncopeSession;

@Component
public class WorkflowRestClient extends BaseRestClient {

    public WorkflowDefinitionTO getDefinition()
            throws SyncopeClientCompositeErrorException {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "workflow/definition/user.json", WorkflowDefinitionTO.class);
    }

    public void updateDefinition(final WorkflowDefinitionTO workflowDef)
            throws SyncopeClientCompositeErrorException {
        SyncopeSession.get().getRestTemplate().put(baseURL + "workflow/definition/user.json", workflowDef);
    }
}
