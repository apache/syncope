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
package org.springframework.security.saml2.provider.service.authentication;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.syncope.sra.security.saml2.ExtendedRelyingPartyRegistration;
import org.apache.syncope.sra.security.saml2.Saml2Constants;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.security.credential.Credential;
import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.util.Assert;

public class OpenSamlLogoutRequestFactory {

    private final OpenSamlImplementation saml = OpenSamlImplementation.getInstance();

    private final Credential credential;

    private Clock clock = Clock.systemUTC();

    public OpenSamlLogoutRequestFactory(final Credential credential) {
        this.credential = credential;
    }

    public Saml2PostAuthenticationRequest createPostLogoutRequest(
            final String issuer, final ExtendedRelyingPartyRegistration rp) {

        LogoutRequest logoutRequest = createLogoutRequest(issuer, rp.getLogoutDetails().getWebSsoUrl());
        if (rp.getRelyingPartyRegistration().getProviderDetails().isSignAuthNRequest()) {
            OpenSamlUtils.sign(logoutRequest, credential);
        }
        String xml = this.saml.serialize(logoutRequest);

        Saml2AuthenticationRequestContext context = Saml2AuthenticationRequestContext.builder().
                issuer(issuer).
                relyingPartyRegistration(rp.getRelyingPartyRegistration()).
                assertionConsumerServiceUrl("<none>").
                build();

        return Saml2PostAuthenticationRequest.withAuthenticationRequestContext(context).
                samlRequest(Saml2Utils.samlEncode(xml.getBytes(StandardCharsets.UTF_8))).
                authenticationRequestUri(rp.getLogoutDetails().getWebSsoUrl()).
                build();
    }

    public Saml2RedirectAuthenticationRequest createRedirectLogoutRequest(
            final String issuer,
            final ExtendedRelyingPartyRegistration rp,
            final String relayState) {

        LogoutRequest logoutRequest = createLogoutRequest(issuer, rp.getLogoutDetails().getWebSsoUrl());
        String xml = this.saml.serialize(logoutRequest);

        Saml2AuthenticationRequestContext context = Saml2AuthenticationRequestContext.builder().
                issuer(issuer).
                relayState(relayState).
                relyingPartyRegistration(rp.getRelyingPartyRegistration()).
                assertionConsumerServiceUrl("<none>").
                build();
        Saml2RedirectAuthenticationRequest.Builder result =
                Saml2RedirectAuthenticationRequest.withAuthenticationRequestContext(context).
                        authenticationRequestUri(rp.getLogoutDetails().getWebSsoUrl());
        String deflatedAndEncoded = Saml2Utils.samlEncode(Saml2Utils.samlDeflate(xml));
        result.samlRequest(deflatedAndEncoded).relayState(relayState);

        if (rp.getLogoutDetails().isSignAuthNRequest()) {
            List<Saml2X509Credential> signingCredentials = rp.getRelyingPartyRegistration().getSigningCredentials();
            Map<String, String> signedParams = this.saml.signQueryParameters(
                    signingCredentials,
                    deflatedAndEncoded,
                    relayState);
            result.samlRequest(signedParams.get(Saml2Constants.SAML_REQUEST)).
                    relayState(signedParams.get(Saml2Constants.RELAY_STATE)).
                    sigAlg(signedParams.get("SigAlg")).
                    signature(signedParams.get("Signature"));
        }

        return result.build();
    }

    @SuppressModernizer
    private LogoutRequest createLogoutRequest(final String issuer, final String destination) {
        LogoutRequest logout = this.saml.buildSamlObject(LogoutRequest.DEFAULT_ELEMENT_NAME);
        logout.setID("LRQ" + UUID.randomUUID().toString().substring(1));
        logout.setIssueInstant(new DateTime(this.clock.millis()));

        Issuer iss = this.saml.buildSamlObject(Issuer.DEFAULT_ELEMENT_NAME);
        iss.setValue(issuer);
        logout.setIssuer(iss);

        logout.setDestination(destination);

        return logout;
    }

    /**
     * Use this {@link Clock} with {@link java.time.Instant#now()} for generating timestamps.
     *
     * @param clock
     */
    public void setClock(final Clock clock) {
        Assert.notNull(clock, "clock cannot be null");
        this.clock = clock;
    }
}
