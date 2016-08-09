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
package org.apache.syncope.client.console.rest;

import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.WorkflowService;

public class WorkflowRestClient extends BaseRestClient {

    private static final long serialVersionUID = 5049285686167071017L;

    private WorkflowService getService(final MediaType mediaType) {
        return SyncopeConsoleSession.get().getService(mediaType, WorkflowService.class);
    }

    public InputStream getDefinition(final MediaType mediaType) {
        Response response = getService(mediaType).exportDefinition(AnyTypeKind.USER);

        return (InputStream) response.getEntity();
    }

    public byte[] getDiagram() {
        WorkflowService service = getService(WorkflowService.class);
        WebClient.client(service).accept(RESTHeaders.MEDIATYPE_IMAGE_PNG);
        Response response = service.exportDiagram(AnyTypeKind.USER);

        byte[] diagram;
        try {
            diagram = IOUtils.readBytesFromStream((InputStream) response.getEntity());
        } catch (Exception e) {
            LOG.error("Could not get workflow diagram", e);
            diagram = new byte[0];
        }
        return diagram;
    }

    public void updateDefinition(final MediaType mediaType, final String definition) {
        getService(mediaType).importDefinition(AnyTypeKind.USER, definition);
    }
}
