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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

public class SAML2SPRestClient extends BaseRestClient {

    private static final long serialVersionUID = -5084300184764037527L;

    public IResourceStream getMetadata(final String spEntityID) {
        SAML2SP4UIService service = SyncopeConsoleSession.get().getAnonymousService(SAML2SP4UIService.class);
        WebClient.client(service).accept(MediaType.APPLICATION_XML_TYPE).type(MediaType.APPLICATION_XML_TYPE);
        Response metadataResponse = service.getMetadata(spEntityID, SAML2SP4UIConstants.URL_CONTEXT);
        WebClient.client(service).reset();

        InputStream inputStream = (InputStream) metadataResponse.getEntity();

        return new AbstractResourceStream() {

            private static final long serialVersionUID = -2268011115723452312L;

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public void close() {
                IOUtils.closeQuietly(inputStream);
            }
        };
    }
}
