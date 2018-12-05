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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "bpmnProcessesList", path = "/api/flowable/bpmnProcesses/")
public class BpmnProcessList extends BaseResource {

    private static final long serialVersionUID = 7273151109078469253L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        LOG.debug("List available Flowable BPMN processes definitions [{}] useful to start User Requests");

        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            final List<BpmnProcess> bpmnProcesses = SyncopeEnduserSession.get().
                    getService(BpmnProcessService.class).list();

            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    // retain also not userWorkflow processes
                    attributes.getResponse().write(MAPPER.writeValueAsString(bpmnProcesses == null
                            ? Collections.<BpmnProcess>emptyList()
                            : bpmnProcesses.stream().filter(bpmnProcess -> !bpmnProcess.isUserWorkflow()).collect(
                                    Collectors.toList())));
                }
            });

            response.setContentType(MediaType.APPLICATION_JSON);
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving BPMN processes", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
