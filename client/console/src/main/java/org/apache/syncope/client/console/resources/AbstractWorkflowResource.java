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
package org.apache.syncope.client.console.resources;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWorkflowResource extends AbstractResource {

    private static final long serialVersionUID = 5163553843196539019L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractWorkflowResource.class);

    protected final WorkflowRestClient restClient = new WorkflowRestClient();

    protected WorkflowDefinitionTO getWorkflowDefinition(final Attributes attributes) {
        final StringValue modelId =
                attributes.getRequest().getQueryParameters().getParameterValue(Constants.MODEL_ID_PARAM);

        WorkflowDefinitionTO workflowDefinition = modelId == null ? null
                : IterableUtils.find(restClient.getDefinitions(), new Predicate<WorkflowDefinitionTO>() {

                    @Override
                    public boolean evaluate(final WorkflowDefinitionTO object) {
                        return modelId.toString().equals(object.getModelId());
                    }
                });

        return workflowDefinition;
    }
}
