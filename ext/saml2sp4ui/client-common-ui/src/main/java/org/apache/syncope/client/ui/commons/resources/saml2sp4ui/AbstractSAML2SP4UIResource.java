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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.common.lib.saml2.SAML2Constants;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.wicket.Session;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSAML2SP4UIResource extends AbstractResource {

    private static final long serialVersionUID = 865306127846395310L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSAML2SP4UIResource.class);

    protected String spEntityID(final Attributes attributes) {
        HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
        return StringUtils.substringBefore(request.getRequestURL().toString(), SAML2SP4UIConstants.URL_CONTEXT);
    }

    protected ResourceResponse send(final SAML2Request request) {
        Session.get().setAttribute(SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID, request.getIdpEntityID());

        ResourceResponse response = new ResourceResponse();
        response.getHeaders().addHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.getHeaders().addHeader("Pragma", "no-cache");
        switch (request.getBindingType()) {
            case REDIRECT:
                response.setStatusCode(Response.Status.FOUND.getStatusCode());
                response.getHeaders().addHeader(HttpHeaders.LOCATION, request.getContent());
                break;

            case POST:
            default:
                response.setContentType(MediaType.TEXT_HTML);
                response.setWriteCallback(new WriteCallback() {

                    @Override
                    public void writeData(final Attributes attributes) {
                        attributes.getResponse().
                                write(new String(Base64.getMimeDecoder().decode(request.getContent())));
                    }
                });
        }
        return response;
    }

    protected SAML2Response buildResponse(
            final Attributes attributes,
            final String samlResponse,
            final String relayState) {

        SAML2Response response = new SAML2Response();

        response.setIdpEntityID((String) Session.get().getAttribute(SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID));
        if (StringUtils.isBlank(response.getIdpEntityID())) {
            response.setIdpEntityID(attributes.getRequest().getQueryParameters().
                    getParameterValue(SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID).toOptionalString());
            if (StringUtils.isBlank(response.getIdpEntityID())) {
                Stream.of(((HttpServletRequest) attributes.getRequest().getContainerRequest()).getCookies()).
                        filter(cookie -> SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID.equals(cookie.getName())).
                        findFirst().ifPresent(cookie -> response.setIdpEntityID(cookie.getValue()));
            }
        }

        response.setSpEntityID(spEntityID(attributes));
        response.setUrlContext(SAML2SP4UIConstants.URL_CONTEXT);
        response.setSamlResponse(samlResponse);
        response.setRelayState(relayState);

        return response;
    }

    protected SAML2Response extract(final Attributes attributes) {
        String samlResponse = attributes.getRequest().getRequestParameters().
                getParameterValue(SAML2Constants.SAML_RESPONSE).toOptionalString();
        LOG.debug("Received SAML Response: {}", samlResponse);

        String relayState = attributes.getRequest().getRequestParameters().
                getParameterValue(SAML2Constants.RELAY_STATE).toOptionalString();
        LOG.debug("Received Relay State: {}", relayState);

        return buildResponse(attributes, samlResponse, relayState);
    }
}
