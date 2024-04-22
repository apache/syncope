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
package org.apache.syncope.core.spring.security.jws;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSEntraJWSVerifierCacheLoader implements CacheLoader<String, JWSVerifier> {

    protected static final Logger LOG = LoggerFactory.getLogger(MSEntraJWSVerifierCacheLoader.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final String tenantId;

    protected final String appId;

    public MSEntraJWSVerifierCacheLoader(final String tenantId, final String appId) {
        this.tenantId = tenantId;
        this.appId = appId;
    }

    protected String getOpenIDMetadataDocumentUrl() {
        return String.format(
                "https://login.microsoftonline.com/%s/.well-known/openid-configuration%s",
                Optional.ofNullable(tenantId).orElse("common"),
                Optional.ofNullable(appId).map(i -> String.format("?appid=%s", i)).orElse(""));
    }

    protected String extractJwksUri(final String openIdMetadataDocument) {
        try {
            return MAPPER.readTree(openIdMetadataDocument).get("jwks_uri").asText();
        } catch (IOException e) {
            throw new IllegalArgumentException("Extracting value of 'jwks_url' key from OpenID Metadata JSON "
                    + "document for Microsoft Entra failed:", e);
        }
    }

    protected String fetchDocument(final String url) {
        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        String.format("Received HTTP status code %d", response.statusCode()));
            }
            return response.body();
        } catch (IOException | InterruptedException | IllegalStateException e) {
            throw new IllegalStateException(
                    String.format("Fetching JSON document for Microsoft Entra from '%s' failed:", url), e);
        }
    }

    protected Map<String, JWSVerifier> parseJsonWebKeySet(final String jsonWebKeySet) {
        List<JWK> fetchedKeys;
        try {
            fetchedKeys = JWKSet.parse(jsonWebKeySet).getKeys();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Parsing JSON Web Key Set for MS Entra failed:", e);
        }

        Map<String, JWSVerifier> verifiers = new HashMap<>();
        for (JWK key : fetchedKeys) {
            if (!(key instanceof AsymmetricJWK)) {
                LOG.warn("Skipped non-asymmetric JSON Web Key with key id '{}' from retrieved JSON Web Key Set "
                        + "for Microsoft Entra", key.getKeyID());
                continue;
            }

            try {
                PublicKey pubKey = ((AsymmetricJWK) key).toPublicKey();
                switch (pubKey) {
                    case RSAPublicKey rsaPublicKey ->
                        verifiers.put(key.getKeyID(), new RSASSAVerifier(rsaPublicKey));
                    case ECPublicKey ecPublicKey ->
                        verifiers.put(key.getKeyID(), new ECDSAVerifier(ecPublicKey));
                    default -> {
                    }
                }
            } catch (JOSEException e) {
                throw new IllegalArgumentException(
                        "Extracting public key from asymmetric JSON Web Key from retrieved JSON Web Key Set "
                        + "for Microsoft Entra failed:", e);
            }
        }

        return verifiers;
    }

    @Override
    public JWSVerifier load(final String key) throws CacheLoaderException {
        return loadAll(Set.of(key)).get(key);
    }

    @Override
    public Map<String, JWSVerifier> loadAll(final Iterable<? extends String> keys) throws CacheLoaderException {
        // Ignore keys argument, as we have to fetch the full JSON Web Key Set
        String openIdDocUrl = getOpenIDMetadataDocumentUrl();
        String openIdDoc = fetchDocument(openIdDocUrl);
        String jwksUri = extractJwksUri(openIdDoc);
        String jwks = fetchDocument(jwksUri);

        return parseJsonWebKeySet(jwks);
    }
}
