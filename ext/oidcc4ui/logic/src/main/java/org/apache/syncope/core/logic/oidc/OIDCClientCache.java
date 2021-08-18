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
package org.apache.syncope.core.logic.oidc;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic in-memory cache for available {@link OidcClient} instances.
 */
public class OIDCClientCache {

    protected static final Logger LOG = LoggerFactory.getLogger(OIDCClientCache.class);

    protected final List<OidcClient> cache = Collections.synchronizedList(new ArrayList<>());

    protected static OIDCProviderMetadata getDiscoveryDocument(final String issuer) {
        String discoveryDocumentURL = issuer + "/.well-known/openid-configuration";
        try {
            HttpResponse<String> response = HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder(URI.create(discoveryDocumentURL)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            return OIDCProviderMetadata.parse(response.body());
        } catch (IOException | InterruptedException | ParseException e) {
            LOG.error("While getting the Discovery Document at {}", discoveryDocumentURL, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    public static void importMetadata(final OIDCC4UIProviderTO opTO) {
        OIDCProviderMetadata metadata = getDiscoveryDocument(opTO.getIssuer());

        opTO.setIssuer(
                Optional.ofNullable(metadata.getIssuer()).map(Issuer::getValue).orElse(null));
        opTO.setJwksUri(
                Optional.ofNullable(metadata.getJWKSetURI()).map(URI::toASCIIString).orElse(null));
        opTO.setAuthorizationEndpoint(
                Optional.ofNullable(metadata.getAuthorizationEndpointURI()).map(URI::toASCIIString).orElse(null));
        opTO.setTokenEndpoint(
                Optional.ofNullable(metadata.getTokenEndpointURI()).map(URI::toASCIIString).orElse(null));
        opTO.setUserinfoEndpoint(
                Optional.ofNullable(metadata.getUserInfoEndpointURI()).map(URI::toASCIIString).orElse(null));
        opTO.setEndSessionEndpoint(
                Optional.ofNullable(metadata.getEndSessionEndpointURI()).map(URI::toASCIIString).orElse(null));
    }

    public Optional<OidcClient> get(final String opName) {
        return cache.stream().filter(c -> opName.equals(c.getName())).findFirst();
    }

    public OidcClient add(final OIDCC4UIProvider op, final String callbackUrl) {
        OIDCProviderMetadata metadata = new OIDCProviderMetadata(
                new Issuer(op.getIssuer()),
                List.of(SubjectType.PUBLIC),
                Optional.ofNullable(op.getJwksUri()).map(URI::create).orElse(null));
        metadata.setAuthorizationEndpointURI(
                Optional.ofNullable(op.getAuthorizationEndpoint()).map(URI::create).orElse(null));
        metadata.setTokenEndpointURI(
                Optional.ofNullable(op.getTokenEndpoint()).map(URI::create).orElse(null));
        metadata.setUserInfoEndpointURI(
                Optional.ofNullable(op.getUserinfoEndpoint()).map(URI::create).orElse(null));
        metadata.setEndSessionEndpointURI(
                Optional.ofNullable(op.getEndSessionEndpoint()).map(URI::create).orElse(null));

        OidcConfiguration config = new OidcConfiguration();
        config.setClientId(op.getClientID());
        config.setSecret(op.getClientSecret());
        config.setProviderMetadata(metadata);
        config.setScope("openid profile email address phone offline_access");
        config.setUseNonce(false);
        config.setLogoutHandler(new NoOpLogoutHandler());

        OidcClient client = new OidcClient(config);
        client.setName(op.getName());
        client.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());
        client.setCallbackUrl(callbackUrl);
        client.init();
        return client;
    }

    public boolean removeAll(final String opName) {
        return cache.removeIf(c -> opName.equals(c.getName()));
    }
}
