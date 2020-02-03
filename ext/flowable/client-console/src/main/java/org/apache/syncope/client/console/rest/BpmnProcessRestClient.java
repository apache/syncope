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
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;

public class BpmnProcessRestClient extends BaseRestClient {

    private static final long serialVersionUID = 5049285686167071017L;

    public static List<BpmnProcess> getDefinitions() {
        return getService(BpmnProcessService.class).list();
    }

    private static BpmnProcessService getService(final MediaType mediaType) {
        BpmnProcessService service = getService(BpmnProcessService.class);

        MetadataMap<String, String> headers = new MetadataMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, List.of(mediaType.toString()));
        headers.put(HttpHeaders.ACCEPT, List.of(mediaType.toString()));
        WebClient.client(service).headers(headers);

        return service;
    }

    public static InputStream getDefinition(final MediaType mediaType, final String key) {
        Response response = getService(mediaType).get(key);
        SyncopeConsoleSession.get().resetClient(BpmnProcessService.class);

        return (InputStream) response.getEntity();
    }

    public static void setDefinition(final MediaType mediaType, final String key, final String definition) {
        getService(mediaType).set(key, definition);
        SyncopeConsoleSession.get().resetClient(BpmnProcessService.class);
    }

    public static byte[] getDiagram(final String key) {
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

    public static void deleteDefinition(final String key) {
        getService(BpmnProcessService.class).delete(key);
    }
}
