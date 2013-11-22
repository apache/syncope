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
package org.apache.syncope.console.resources;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.console.rest.WorkflowRestClient;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.io.IOUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Mirror REST resource for obtaining user workflow definition in JSON (used by Activiti Modeler).
 *
 * @see org.apache.syncope.common.services.WorkflowService#exportDefinition(
 * org.apache.syncope.common.types.AttributableType)
 */
public class WorkflowDefGETResource extends AbstractResource {

    private static final long serialVersionUID = 4637304868056148970L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);

        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                IOUtils.copy(WebApplicationContextUtils.getWebApplicationContext(
                        WebApplication.get().getServletContext()).getBean(WorkflowRestClient.class).
                        getDefinition(MediaType.APPLICATION_JSON_TYPE),
                        attributes.getResponse().getOutputStream());
            }
        });

        return response;
    }

}
