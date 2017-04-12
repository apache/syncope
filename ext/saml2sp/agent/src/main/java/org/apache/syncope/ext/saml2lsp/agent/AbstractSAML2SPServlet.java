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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.syncope.common.lib.SSOConstants;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSAML2SPServlet extends HttpServlet {

    private static final long serialVersionUID = 7969539245875799817L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSAML2SPServlet.class);

    protected void prepare(final HttpServletResponse response, final SAML2RequestTO requestTO) throws IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        switch (requestTO.getBindingType()) {
            case REDIRECT:
                UriBuilder ub = UriBuilder.fromUri(requestTO.getIdpServiceAddress());
                ub.queryParam(SSOConstants.SAML_REQUEST, requestTO.getContent());
                ub.queryParam(SSOConstants.RELAY_STATE, requestTO.getRelayState());
                ub.queryParam(SSOConstants.SIG_ALG, requestTO.getSignAlg());
                ub.queryParam(SSOConstants.SIGNATURE, requestTO.getSignature());

                response.setStatus(HttpServletResponse.SC_SEE_OTHER);
                response.setHeader("Location", ub.build().toASCIIString());
                break;

            case POST:
            default:
                response.setContentType(MediaType.TEXT_HTML);
                response.getWriter().write(""
                        + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                        + " <body onLoad=\"document.forms[0].submit();\">"
                        + "  <form action=\"" + requestTO.getIdpServiceAddress() + "\" method=\"POST\">"
                        + "   <input type=\"hidden\" name=\"" + SSOConstants.SAML_REQUEST + "\""
                        + "          value=\"" + requestTO.getContent() + "\"/>"
                        + "   <input type=\"hidden\" name=\"" + SSOConstants.RELAY_STATE + "\""
                        + "          value=\"" + requestTO.getRelayState() + "\"/>"
                        + "   <input type=\"submit\" style=\"visibility: hidden;\"/>"
                        + "  </form>"
                        + " </body>"
                        + "</html>");
        }
    }

    protected SAML2ReceivedResponseTO extract(final InputStream response) throws IOException {
        String strForm = IOUtils.toString(response);
        MultivaluedMap<String, String> params = JAXRSUtils.getStructuredParams(strForm, "&", false, false);

        String samlResponse = URLDecoder.decode(
                params.getFirst(SSOConstants.SAML_RESPONSE), StandardCharsets.UTF_8.name());
        LOG.debug("Received SAML Response: {}", samlResponse);

        String relayState = params.getFirst(SSOConstants.RELAY_STATE);
        LOG.debug("Received Relay State: {}", relayState);

        SAML2ReceivedResponseTO receivedResponseTO = new SAML2ReceivedResponseTO();
        receivedResponseTO.setSamlResponse(samlResponse);
        receivedResponseTO.setRelayState(relayState);
        return receivedResponseTO;
    }
}
