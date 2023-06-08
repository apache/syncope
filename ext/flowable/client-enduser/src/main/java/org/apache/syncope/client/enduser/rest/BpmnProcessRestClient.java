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
package org.apache.syncope.client.enduser.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;

public class BpmnProcessRestClient extends BaseRestClient {

    private static final long serialVersionUID = 5049285686167071017L;

    private BpmnProcessService getService(final MediaType mediaType) {
        BpmnProcessService service = getService(BpmnProcessService.class);
        Client client = WebClient.client(service);
        client.type(mediaType);
        return service;
    }

    public List<BpmnProcess> getDefinitions() {
        return getService(BpmnProcessService.class).list();
    }

    public InputStream getDefinition(final MediaType mediaType, final String key) {
        Response response = getService(mediaType).get(key);
        SyncopeEnduserSession.get().resetClient(BpmnProcessService.class);

        return (InputStream) response.getEntity();
    }

    public byte[] getDiagram(final String key) {
        BpmnProcessService service = getService(BpmnProcessService.class);
        WebClient.client(service).accept(RESTHeaders.MEDIATYPE_IMAGE_PNG);
        Response response = service.exportDiagram(key);

        byte[] diagram;
        try {
            diagram = IOUtils.readBytesFromStream((InputStream) response.getEntity());
        } catch (Exception e) {
            LOG.error("Could not get workflow diagram", e);
            diagram = new byte[0];
        }
        return diagram;
    }
}
