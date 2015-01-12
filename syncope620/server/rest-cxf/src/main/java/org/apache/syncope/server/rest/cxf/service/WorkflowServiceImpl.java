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
package org.apache.syncope.server.rest.cxf.service;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.WorkflowService;
import org.apache.syncope.server.logic.WorkflowLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowServiceImpl extends AbstractServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowLogic logic;

    @Override
    public Response getOptions(final SubjectType kind) {
        String key;
        String value;
        if (kind == SubjectType.USER) {
            key = RESTHeaders.ACTIVITI_USER_ENABLED;
            value = "false"; //Boolean.toString(ActivitiDetector.isActivitiEnabledForUsers());
        } else {
            key = RESTHeaders.ACTIVITI_ROLE_ENABLED;
            value = "false"; //Boolean.toString(ActivitiDetector.isActivitiEnabledForRoles());
        }

        Response.ResponseBuilder builder = Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW);
        if (key != null && value != null) {
            builder.header(key, value);
        }
        return builder.build();
    }

    @Override
    public Response exportDefinition(final SubjectType kind) {
        final MediaType accept =
                messageContext.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)
                        ? MediaType.APPLICATION_JSON_TYPE
                        : MediaType.APPLICATION_XML_TYPE;

        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                if (kind == SubjectType.USER) {
                    logic.exportUserDefinition(accept, os);
                } else {
                    logic.exportRoleDefinition(accept, os);
                }
            }
        };

        return Response.ok(sout).
                type(accept).
                build();
    }

    @Override
    public Response exportDiagram(final SubjectType kind) {
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                if (kind == SubjectType.USER) {
                    logic.exportUserDiagram(os);
                } else {
                    logic.exportRoleDiagram(os);
                }
            }
        };

        return Response.ok(sout).
                type(RESTHeaders.MEDIATYPE_IMAGE_PNG).
                build();
    }

    @Override
    public void importDefinition(final SubjectType kind, final String definition) {
        final MediaType contentType =
                messageContext.getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                        ? MediaType.APPLICATION_JSON_TYPE
                        : MediaType.APPLICATION_XML_TYPE;

        if (kind == SubjectType.USER) {
            logic.importUserDefinition(contentType, definition);
        } else {
            logic.importRoleDefinition(contentType, definition);
        }
    }

}
