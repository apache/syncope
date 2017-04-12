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
package org.apache.syncope.ext.saml2lsp.agent;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.rest.api.service.SAML2SPService;

@WebServlet(name = "metadata", urlPatterns = { "/saml2sp/metadata" })
public class Metadata extends HttpServlet {

    private static final long serialVersionUID = 694030186105137875L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        SyncopeClient anonymous = (SyncopeClient) request.getServletContext().
                getAttribute(Constants.SYNCOPE_ANONYMOUS_CLIENT);
        SAML2SPService service = anonymous.getService(SAML2SPService.class);
        WebClient.client(service).accept(MediaType.APPLICATION_XML_TYPE).type(MediaType.APPLICATION_XML_TYPE);
        Response metadataResponse = service.getMetadata(
                StringUtils.substringBefore(request.getRequestURL().toString(), "/saml2sp"), "saml2sp");

        response.setContentType(metadataResponse.getMediaType().toString());
        IOUtils.copy((InputStream) metadataResponse.getEntity(), response.getOutputStream());
        ((InputStream) metadataResponse.getEntity()).close();
    }
}
