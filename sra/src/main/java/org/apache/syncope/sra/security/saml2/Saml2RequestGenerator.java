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
package org.apache.syncope.sra.security.saml2;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlLogoutRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2RedirectAuthenticationRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

abstract class Saml2RequestGenerator {

    protected final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private Saml2AuthenticationRequestFactory authenticationRequestFactory = new OpenSamlAuthenticationRequestFactory();

    private OpenSamlLogoutRequestFactory logoutRequestFactory;

    protected Saml2RequestGenerator(
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
    }

    public void setAuthenticationRequestFactory(final Saml2AuthenticationRequestFactory authenticationRequestFactory) {
        Assert.notNull(authenticationRequestFactory, "authenticationRequestFactory cannot be null");
        this.authenticationRequestFactory = authenticationRequestFactory;
    }

    public void setLogoutRequestFactory(final OpenSamlLogoutRequestFactory logoutRequestFactory) {
        Assert.notNull(logoutRequestFactory, "logoutRequestFactory cannot be null");
        this.logoutRequestFactory = logoutRequestFactory;
    }

    private static void addParameter(final String name, final String value, final UriComponentsBuilder builder) {
        Assert.hasText(name, "name cannot be empty or null");
        if (StringUtils.hasText(value)) {
            builder.queryParam(
                    UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                    UriUtils.encode(value, StandardCharsets.ISO_8859_1));
        }
    }

    private URI createSamlRequestRedirectUrl(final Saml2AuthenticationRequestContext authnRequestCtx) {
        Saml2RedirectAuthenticationRequest authNData =
                this.authenticationRequestFactory.createRedirectAuthenticationRequest(authnRequestCtx);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authNData.getAuthenticationRequestUri());
        addParameter(Saml2Constants.SAML_REQUEST, authNData.getSamlRequest(), uriBuilder);
        addParameter(Saml2Constants.RELAY_STATE, authNData.getRelayState(), uriBuilder);
        addParameter("SigAlg", authNData.getSigAlg(), uriBuilder);
        addParameter("Signature", authNData.getSignature(), uriBuilder);
        return uriBuilder.build(true).toUri();
    }

    protected Mono<Void> sendRedirect(
            final ServerHttpResponse response,
            final Saml2AuthenticationRequestContext authnRequestCtx) {

        return Mono.fromRunnable(() -> {
            response.setStatusCode(HttpStatus.SEE_OTHER);
            response.getHeaders().setLocation(createSamlRequestRedirectUrl(authnRequestCtx));
        });
    }

    private URI createSamlRequestRedirectUrl(
            final String issuer,
            final ExtendedRelyingPartyRegistration rp,
            final String relayState) {

        Saml2RedirectAuthenticationRequest authNData =
                this.logoutRequestFactory.createRedirectLogoutRequest(issuer, rp, relayState);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authNData.getAuthenticationRequestUri());
        addParameter(Saml2Constants.SAML_REQUEST, authNData.getSamlRequest(), uriBuilder);
        addParameter(Saml2Constants.RELAY_STATE, authNData.getRelayState(), uriBuilder);
        addParameter("SigAlg", authNData.getSigAlg(), uriBuilder);
        addParameter("Signature", authNData.getSignature(), uriBuilder);
        return uriBuilder.build(true).toUri();
    }

    protected Mono<Void> sendRedirect(
            final ServerHttpResponse response,
            final String issuer,
            final ExtendedRelyingPartyRegistration rp,
            final String relayState) {

        return Mono.fromRunnable(() -> {
            response.setStatusCode(HttpStatus.SEE_OTHER);
            response.getHeaders().setLocation(createSamlRequestRedirectUrl(issuer, rp, relayState));
        });
    }

    private static String htmlEscape(final String value) {
        if (StringUtils.hasText(value)) {
            return HtmlUtils.htmlEscape(value);
        }
        return value;
    }

    private static String createSamlPostRequestFormData(final Saml2PostAuthenticationRequest request) {
        String destination = request.getAuthenticationRequestUri();
        String relayState = htmlEscape(request.getRelayState());
        String samlRequest = htmlEscape(request.getSamlRequest());
        StringBuilder postHtml = new StringBuilder()
                .append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("    <head>\n")
                .append("        <meta charset=\"utf-8\" />\n")
                .append("    </head>\n")
                .append("    <body onload=\"document.forms[0].submit()\">\n")
                .append("        <noscript>\n")
                .append("            <p>\n")
                .append("                <strong>Note:</strong> Since your browser does not support JavaScript,\n")
                .append("                you must press the Continue button once to proceed.\n")
                .append("            </p>\n")
                .append("        </noscript>\n")
                .append("        \n")
                .append("        <form action=\"").append(destination).append("\" method=\"post\">\n")
                .append("            <div>\n")
                .append("                <input type=\"hidden\" name=\"SAMLRequest\" value=\"")
                .append(samlRequest)
                .append("\"/>\n");
        if (StringUtils.hasText(relayState)) {
            postHtml
                    .append("                <input type=\"hidden\" name=\"RelayState\" value=\"")
                    .append(relayState)
                    .append("\"/>\n");
        }
        postHtml
                .append("            </div>\n")
                .append("            <noscript>\n")
                .append("                <div>\n")
                .append("                    <input type=\"submit\" value=\"Continue\"/>\n")
                .append("                </div>\n")
                .append("            </noscript>\n")
                .append("        </form>\n")
                .append("        \n")
                .append("    </body>\n")
                .append("</html>");
        return postHtml.toString();
    }

    protected Mono<Void> sendPost(
            final ServerHttpResponse response,
            final Saml2AuthenticationRequestContext authnRequestCtx) {

        return Mono.defer(() -> {
            Saml2PostAuthenticationRequest authNData =
                    this.authenticationRequestFactory.createPostAuthenticationRequest(authnRequestCtx);
            String html = createSamlPostRequestFormData(authNData);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(html.getBytes())));
        });
    }

    protected Mono<Void> sendPost(
            final ServerHttpResponse response,
            final String issuer,
            final ExtendedRelyingPartyRegistration rp) {

        return Mono.defer(() -> {
            Saml2PostAuthenticationRequest authNData = this.logoutRequestFactory.createPostLogoutRequest(issuer, rp);
            String html = createSamlPostRequestFormData(authNData);
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(html.getBytes())));
        });
    }
}
