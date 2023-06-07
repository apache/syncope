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

import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.syncope.client.console.rest.BpmnProcessRestClient;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.common.lib.to.BpmnProcess;

/**
 * Mirror REST resource for obtaining BPMN process in JSON (used by Flowable Modeler).
 */
@Resource(key = "bpmnProcessGET", path = "/bpmnProcessGET")
public class BpmnProcessGETResource extends AbstractBpmnProcessResource {

    private static final long serialVersionUID = 4637304868056148970L;

    public BpmnProcessGETResource(final BpmnProcessRestClient bpmnProcessRestClient) {
        super(bpmnProcessRestClient);
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        final BpmnProcess toGet = getBpmnProcess(attributes);

        ResourceResponse response = new ResourceResponse();
        response.disableCaching();
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                writeStream(
                        attributes,
                        bpmnProcessRestClient.getDefinition(MediaType.APPLICATION_JSON_TYPE, toGet.getKey()));
            }
        });

        return response;
    }
}
