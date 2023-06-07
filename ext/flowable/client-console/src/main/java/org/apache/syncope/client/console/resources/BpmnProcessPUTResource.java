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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.syncope.client.console.rest.BpmnProcessRestClient;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.wicket.util.io.IOUtils;

/**
 * Mirror REST resource for setting BPMN process in JSON (used by Flowable Modeler).
 */
@Resource(key = "bpmnProcessPUT", path = "/bpmnProcessPUT")
public class BpmnProcessPUTResource extends AbstractBpmnProcessResource {

    private static final long serialVersionUID = 2964542005207297944L;

    public BpmnProcessPUTResource(final BpmnProcessRestClient bpmnProcessRestClient) {
        super(bpmnProcessRestClient);
    }

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
            LOG.error("Could not extract BPMN process", e);
        }

        BpmnProcess toSet = getBpmnProcess(attributes);

        if (definition == null || toSet == null) {
            return new ResourceResponse().setStatusCode(Response.Status.BAD_REQUEST.getStatusCode()).
                    setError(Response.Status.BAD_REQUEST.getStatusCode(),
                            "Could not extract BPMN process' model id and / or definition");
        }

        try {
            bpmnProcessRestClient.setDefinition(MediaType.APPLICATION_JSON_TYPE, toSet.getKey(), definition);
            return new ResourceResponse().setStatusCode(Response.Status.NO_CONTENT.getStatusCode());
        } catch (Exception e) {
            LOG.error("While updating BPMN process", e);
            return new ResourceResponse().setStatusCode(Response.Status.BAD_REQUEST.getStatusCode()).
                    setError(Response.Status.BAD_REQUEST.getStatusCode(),
                            "While updating BPMN process: " + e.getMessage());
        }
    }
}
