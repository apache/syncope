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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.net.URI;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.oidc.client.OidcClient;

class OIDCClientCacheTest {

    private static OIDCC4UIProvider provider() {
        OIDCC4UIProvider op = mock(OIDCC4UIProvider.class);
        when(op.getName()).thenReturn("op");
        when(op.getClientID()).thenReturn("clientID");
        when(op.getClientSecret()).thenReturn("clientSecret");
        when(op.getIssuer()).thenReturn("https://op.example.com");
        when(op.getJwksUri()).thenReturn("https://op.example.com/jwks");
        when(op.getAuthorizationEndpoint()).thenReturn("https://op.example.com/authorize");
        when(op.getTokenEndpoint()).thenReturn("https://op.example.com/token");
        when(op.getUserinfoEndpoint()).thenReturn("https://op.example.com/userinfo");
        when(op.getEndSessionEndpoint()).thenReturn("https://op.example.com/logout");
        when(op.getScopes()).thenReturn(List.of("openid", "email"));
        when(op.getHasDiscovery()).thenReturn(false);
        return op;
    }

    @Test
    void nonceOnlyWithRealSessionStore() {
        assertFalse(new OIDCClientCache(NoOpSessionStore.INSTANCE).useNonce);
        assertTrue(new OIDCClientCache(mock(SessionStore.class)).useNonce);
    }

    @Test
    void addGetAndRemove() {
        OIDCClientCache cache = new OIDCClientCache(NoOpSessionStore.INSTANCE);

        assertTrue(cache.get("op").isEmpty());
        assertFalse(cache.removeAll("op"));

        OIDCProviderMetadata metadata = new OIDCProviderMetadata(
                new Issuer("https://op.example.com"),
                List.of(SubjectType.PUBLIC),
                URI.create("https://op.example.com/jwks"));
        metadata.setIDTokenJWSAlgs(List.of(JWSAlgorithm.RS256));

        OIDCC4UIProvider provider = provider();
        when(provider.getHasDiscovery()).thenReturn(true);

        try (MockedStatic<OIDCClientCache> mocked = mockStatic(OIDCClientCache.class)) {
            mocked.when(() -> OIDCClientCache.fetchMetadata("https://op.example.com")).thenReturn(metadata);

            OidcClient client = cache.add(provider, "https://client.example.com/callback");

            assertSame(client, cache.get("op").orElseThrow());
            assertEquals("https://client.example.com/callback", client.getCallbackUrl());
            assertFalse(client.getConfiguration().isUseNonce());
        }

        assertTrue(cache.removeAll("op"));
        assertTrue(cache.get("op").isEmpty());
    }
}
