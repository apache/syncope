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
package org.apache.syncope.client.ui.commons.resources.saml2sp4ui;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.wicket.Session;

@Resource(
        key = SAML2SP4UIConstants.URL_CONTEXT + ".metadata",
        path = "/" + SAML2SP4UIConstants.URL_CONTEXT + "/metadata")
public class MetadataResource extends AbstractSAML2SP4UIResource {

    private static final long serialVersionUID = -1694048805459648725L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        SAML2SP4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(SAML2SP4UIService.class);
        WebClient.client(service).accept(MediaType.APPLICATION_XML_TYPE).type(MediaType.APPLICATION_XML_TYPE);
        Response metadataResponse = service.getMetadata(spEntityID(attributes), SAML2SP4UIConstants.URL_CONTEXT);
        WebClient.client(service).reset();

        ResourceResponse response = new ResourceResponse();
        response.disableCaching();
        response.setContentType(MediaType.APPLICATION_XML);
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                writeStream(attributes, (InputStream) metadataResponse.getEntity());
            }
        });

        return response;
    }
}
