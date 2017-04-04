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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.io.IOUtils;

/**
 * Mirror REST resource for obtaining user workflow definition in JSON (used by Activiti / Flowable Modeler).
 *
 * @see org.apache.syncope.common.rest.api.service.WorkflowService#exportDefinition
 */
public class WorkflowDefGETResource extends AbstractResource {

    private static final long serialVersionUID = 4637304868056148970L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.disableCaching();
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                IOUtils.copy(
                        new WorkflowRestClient().getDefinition(MediaType.APPLICATION_JSON_TYPE),
                        attributes.getResponse().getOutputStream());
            }
        });

        return response;
    }

}
