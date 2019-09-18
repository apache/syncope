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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.SSOConstants;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public abstract class AbstractSAML2SPServlet extends HttpServlet {

    private static final long serialVersionUID = 7969539245875799817L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSAML2SPServlet.class);

    private static final String SYNCOPE_CLIENT_FACTORY = "SyncopeClientFactory";

    private static final String SYNCOPE_ANONYMOUS_CLIENT = "SyncopeAnonymousClient";

    private final ApplicationContext ctx;

    public AbstractSAML2SPServlet(final ApplicationContext ctx) {
        super();
        this.ctx = ctx;
    }

    protected SyncopeClientFactoryBean getClientFactory(
            final ServletContext servletContext,
            final boolean useGZIPCompression) {

        SyncopeClientFactoryBean clientFactory =
                (SyncopeClientFactoryBean) servletContext.getAttribute(SYNCOPE_CLIENT_FACTORY);
        if (clientFactory == null) {
            ServiceOps serviceOps = ctx.getBean(ServiceOps.class);
            clientFactory = new SyncopeClientFactoryBean().
                    setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                    setUseCompression(useGZIPCompression);

            servletContext.setAttribute(SYNCOPE_CLIENT_FACTORY, clientFactory);
        }

        return clientFactory;
    }

    protected SyncopeClient getAnonymousClient(
            final ServletContext servletContext,
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression) {

        SyncopeClient anonymousClient = (SyncopeClient) servletContext.getAttribute(SYNCOPE_ANONYMOUS_CLIENT);
        if (anonymousClient == null) {
            SyncopeClientFactoryBean clientFactory = getClientFactory(servletContext, useGZIPCompression);
            anonymousClient = clientFactory.create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey));

            servletContext.setAttribute(SYNCOPE_ANONYMOUS_CLIENT, anonymousClient);
        }

        return anonymousClient;
    }

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
                response.setHeader(HttpHeaders.LOCATION, ub.build().toASCIIString());
                break;

            case POST:
            default:
                response.setContentType(MediaType.TEXT_HTML);
                response.getWriter().write(""
                        + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                        + " <body onLoad=\"document.forms[0].submit();\">"
                        + "  <form action=\"" + requestTO.getIdpServiceAddress() + "\" method=\"POST\">"
                        + "   <input type=\"hidden\" name=\"" + SSOConstants.SAML_REQUEST + '"'
                        + "          value=\"" + requestTO.getContent() + "\"/>"
                        + "   <input type=\"hidden\" name=\"" + SSOConstants.RELAY_STATE + '"'
                        + "          value=\"" + requestTO.getRelayState() + "\"/>"
                        + "   <input type=\"submit\" style=\"visibility: hidden;\"/>"
                        + "  </form>"
                        + " </body>"
                        + "</html>");
        }
    }

    protected SAML2ReceivedResponseTO extract(
            final String spEntityID,
            final String urlContext,
            final String clientAddress,
            final InputStream response) throws IOException {

        String strForm = IOUtils.toString(response);
        MultivaluedMap<String, String> params = JAXRSUtils.getStructuredParams(strForm, "&", false, false);

        String samlResponse = params.getFirst(SSOConstants.SAML_RESPONSE);
        if (StringUtils.isNotBlank(samlResponse)) {
            samlResponse = URLDecoder.decode(samlResponse, StandardCharsets.UTF_8);
            LOG.debug("Received SAML Response: {}", samlResponse);
        }

        String relayState = params.getFirst(SSOConstants.RELAY_STATE);
        LOG.debug("Received Relay State: {}", relayState);

        SAML2ReceivedResponseTO receivedResponseTO = new SAML2ReceivedResponseTO();
        receivedResponseTO.setSpEntityID(spEntityID);
        receivedResponseTO.setUrlContext(urlContext);
        receivedResponseTO.setSamlResponse(samlResponse);
        receivedResponseTO.setRelayState(relayState);
        return receivedResponseTO;
    }
}
