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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.syncope.console.rest.WorkflowRestClient;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Mirror REST resource for putting user workflow definition in JSON (used by Activiti Modeler).
 *
 * @see org.apache.syncope.common.services.WorkflowService#importDefinition(
 * org.apache.syncope.common.types.SubjectType, java.lang.String)
 */
public class WorkflowDefPUTResource extends AbstractResource {

    private static final long serialVersionUID = 2964542005207297944L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowDefPUTResource.class);

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        String definition = null;
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            String requestBody = IOUtils.toString(request.getInputStream());
            String[] split = requestBody.split("&");
            for (int i = 0; i < split.length && definition == null; i++) {
                String keyValue = split[i];
                if (keyValue.startsWith("json_xml=")) {
                    definition = UrlUtils.urlDecode(keyValue.split("=")[1]);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not extract workflow definition from request", e);
        }

        WebApplicationContextUtils.getWebApplicationContext(WebApplication.get().getServletContext()).
                getBean(WorkflowRestClient.class).
                updateDefinition(MediaType.APPLICATION_JSON_TYPE, definition);

        ResourceResponse response = new ResourceResponse();
        response.setStatusCode(204);
        return response;
    }

}
