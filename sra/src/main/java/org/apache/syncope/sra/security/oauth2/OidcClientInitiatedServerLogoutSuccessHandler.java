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
package org.apache.syncope.sra.security.oauth2;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.syncope.sra.security.AbstractServerLogoutSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * A reactive logout success handler for initiating OIDC logout through the user agent.
 *
 * @see <a href="https://openid.net/specs/openid-connect-session-1_0.html#RPLogout">RP-Initiated Logout</a>
 * @see org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
 */
public class OidcClientInitiatedServerLogoutSuccessHandler extends AbstractServerLogoutSuccessHandler {

    @Autowired
    @Qualifier("oidcClientRegistrationRepository")
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    protected final RedirectServerLogoutSuccessHandler serverLogoutSuccessHandler =
            new RedirectServerLogoutSuccessHandler();

    /**
     * The URL to redirect to after successfully logging out when not originally an OIDC login
     *
     * @param logoutSuccessUrl the url to redirect to. Default is "/login?logout".
     */
    public void setLogoutSuccessUrl(final URI logoutSuccessUrl) {
        Assert.notNull(logoutSuccessUrl, "logoutSuccessUrl cannot be null");
        this.serverLogoutSuccessHandler.setLogoutSuccessUrl(logoutSuccessUrl);
    }

    @Override
    public Mono<Void> onLogoutSuccess(final WebFilterExchange exchange, final Authentication authentication) {
        return Mono.just(authentication).
                filter(OAuth2AuthenticationToken.class::isInstance).
                filter(token -> authentication.getPrincipal() instanceof OidcUser).
                map(OAuth2AuthenticationToken.class::cast).
                flatMap(this::endSessionEndpoint).
                map(endSessionEndpoint -> endpointUri(exchange, endSessionEndpoint, authentication)).
                switchIfEmpty(serverLogoutSuccessHandler.onLogoutSuccess(exchange, authentication).then(Mono.empty())).
                flatMap(endpointUri -> redirectStrategy.sendRedirect(exchange.getExchange(), endpointUri));
    }

    private Mono<URI> endSessionEndpoint(final OAuth2AuthenticationToken token) {
        String registrationId = token.getAuthorizedClientRegistrationId();
        return clientRegistrationRepository.findByRegistrationId(registrationId).
                map(ClientRegistration::getProviderDetails).
                map(ClientRegistration.ProviderDetails::getConfigurationMetadata).
                flatMap(configurationMetadata -> Mono.justOrEmpty(configurationMetadata.get("end_session_endpoint"))).
                map(Object::toString).
                map(URI::create);
    }

    private URI endpointUri(
            final WebFilterExchange exchange,
            final URI endSessionEndpoint,
            final Authentication authentication) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(endSessionEndpoint);
        builder.queryParam("id_token_hint", idToken(authentication));

        URI postLogout = getPostLogout(exchange);
        builder.queryParam("post_logout_redirect_uri", postLogout);

        return builder.encode(StandardCharsets.UTF_8).build().toUri();
    }

    private String idToken(final Authentication authentication) {
        return ((OidcUser) authentication.getPrincipal()).getIdToken().getTokenValue();
    }
}
