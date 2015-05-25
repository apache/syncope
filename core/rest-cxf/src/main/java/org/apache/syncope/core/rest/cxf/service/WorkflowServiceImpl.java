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
package org.apache.syncope.core.rest.cxf.service;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.WorkflowService;
import org.apache.syncope.core.logic.WorkflowLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowServiceImpl extends AbstractServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowLogic logic;

    @Override
    public Response exportDefinition(final AnyTypeKind kind) {
        final MediaType accept =
                messageContext.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)
                        ? MediaType.APPLICATION_JSON_TYPE
                        : MediaType.APPLICATION_XML_TYPE;

        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                if (kind == AnyTypeKind.USER) {
                    logic.exportUserDefinition(accept, os);
                } else if (kind == AnyTypeKind.ANY_OBJECT) {
                    logic.exportAnyObjectDefinition(accept, os);
                } else {
                    logic.exportGroupDefinition(accept, os);
                }
            }
        };

        return Response.ok(sout).
                type(accept).
                build();
    }

    @Override
    public Response exportDiagram(final AnyTypeKind kind) {
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                if (kind == AnyTypeKind.USER) {
                    logic.exportUserDiagram(os);
                } else if (kind == AnyTypeKind.ANY_OBJECT) {
                    logic.exportAnyObjectDiagram(os);
                } else {
                    logic.exportGroupDiagram(os);
                }
            }
        };

        return Response.ok(sout).
                type(RESTHeaders.MEDIATYPE_IMAGE_PNG).
                build();
    }

    @Override
    public void importDefinition(final AnyTypeKind kind, final String definition) {
        final MediaType contentType =
                messageContext.getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                        ? MediaType.APPLICATION_JSON_TYPE
                        : MediaType.APPLICATION_XML_TYPE;

        if (kind == AnyTypeKind.USER) {
            logic.importUserDefinition(contentType, definition);
        } else if (kind == AnyTypeKind.ANY_OBJECT) {
            logic.importAnyObjectDefinition(contentType, definition);
        } else {
            logic.importGroupDefinition(contentType, definition);
        }
    }

}
