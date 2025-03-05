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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.List;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.apache.syncope.core.logic.BpmnProcessLogic;

public class BpmnProcessServiceImpl extends AbstractService implements BpmnProcessService {

    protected final BpmnProcessLogic logic;

    public BpmnProcessServiceImpl(final BpmnProcessLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<BpmnProcess> list() {
        return logic.list();
    }

    @Override
    public Response get(final String key) {
        BpmnProcessFormat format =
                messageContext.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)
                ? BpmnProcessFormat.JSON
                : BpmnProcessFormat.XML;

        StreamingOutput sout = os -> logic.exportDefinition(key, format, os);

        return Response.ok(sout).
                type(format.getMediaType()).
                build();
    }

    @Override
    public Response exportDiagram(final String key) {
        StreamingOutput sout = (os) -> logic.exportDiagram(key, os);

        return Response.ok(sout).
                type(RESTHeaders.MEDIATYPE_IMAGE_PNG).
                build();
    }

    @Override
    public void set(final String key, final String definition) {
        BpmnProcessFormat format =
                messageContext.getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                ? BpmnProcessFormat.JSON
                : BpmnProcessFormat.XML;

        logic.importDefinition(key, format, definition);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
