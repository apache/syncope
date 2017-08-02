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

import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.WorkflowService;
import org.apache.syncope.core.logic.WorkflowLogic;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowServiceImpl extends AbstractServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowLogic logic;

    @Override
    public List<WorkflowDefinitionTO> list(final String anyType) {
        return logic.list(anyType);
    }

    @Override
    public Response get(final String anyType, final String key) {
        WorkflowDefinitionFormat format =
                messageContext.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)
                ? WorkflowDefinitionFormat.JSON
                : WorkflowDefinitionFormat.XML;

        StreamingOutput sout = (os) -> logic.exportDefinition(anyType, key, format, os);

        return Response.ok(sout).
                type(format == WorkflowDefinitionFormat.JSON
                        ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XHTML_XML_TYPE).
                build();
    }

    @Override
    public Response exportDiagram(final String anyType, final String key) {
        StreamingOutput sout = (os) -> logic.exportDiagram(anyType, key, os);

        return Response.ok(sout).
                type(RESTHeaders.MEDIATYPE_IMAGE_PNG).
                build();
    }

    @Override
    public void set(final String anyType, final String key, final String definition) {
        WorkflowDefinitionFormat format =
                messageContext.getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                ? WorkflowDefinitionFormat.JSON
                : WorkflowDefinitionFormat.XML;

        logic.importDefinition(anyType, key, format, definition);
    }

    @Override
    public void delete(final String anyType, final String key) {
        logic.delete(anyType, key);
    }

}
