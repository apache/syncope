/* 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.rest;

import org.springframework.stereotype.Component;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

@Component
public class WorkflowRestClient extends AbstractBaseRestClient {

    public WorkflowDefinitionTO getDefinition()
            throws SyncopeClientCompositeErrorException {

        return restTemplate.getForObject(baseURL
                + "workflow/definition.json",
                WorkflowDefinitionTO.class);
    }

    public void updateDefinition(final WorkflowDefinitionTO workflowDef)
            throws SyncopeClientCompositeErrorException {

        restTemplate.put(baseURL
                + "/workflow/definition.json", workflowDef);
    }
}
