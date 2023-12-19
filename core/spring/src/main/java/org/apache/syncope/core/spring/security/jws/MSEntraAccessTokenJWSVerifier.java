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
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jca.JCAAware;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64URL;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSEntraAccessTokenJWSVerifier implements JWSVerifier {

    protected static final Logger LOG = LoggerFactory.getLogger(MSEntraAccessTokenJWSVerifier.class);

    protected final String tenantId;

    protected final String appId;

    protected final Duration cacheExpireAfterWrite;

    protected final HttpClient httpClient;

    protected final JsonMapper jsonMapper;

    protected final LoadingCache<String, JWSVerifier> verifiersCache;

    public MSEntraAccessTokenJWSVerifier(
            final String tenantId,
            final String appId,
            final Duration cacheExpireAfterWrite) {

        this.tenantId = tenantId;
        this.appId = appId;
        this.cacheExpireAfterWrite = cacheExpireAfterWrite;

        this.httpClient = HttpClient.newHttpClient();
        this.jsonMapper = JsonMapper.builder().findAndAddModules().build();

        /*
         * At any given point in time, Entra ID (formerly: Azure AD) may sign an ID token using
         * any one of a certain set of public-private key pairs. Entra ID rotates the possible
         * set of keys on a periodic basis, so the application should be written to handle those
         * key changes automatically. A reasonable frequency to check for updates to the public
         * keys used by Entra ID is every 24 hours.
         */
        this.verifiersCache = Caffeine.newBuilder().
                expireAfterWrite(cacheExpireAfterWrite).
                build(new CacheLoader<>() {

                    @Override
                    public JWSVerifier load(final String key) {
                        return loadAll(Set.of(key)).get(key);
                    }

                    @Override
                    public Map<? extends String, ? extends JWSVerifier> loadAll(final Set<? extends String> keys) {
                        // Ignore keys argument, as we have to fetch the full JSON Web Key Set
                        String openIdDocUrl = getOpenIDMetadataDocumentUrl();
                        String openIdDoc = fetchDocument(openIdDocUrl);
                        String jwksUri = extractJwksUri(openIdDoc);
                        String jwks = fetchDocument(jwksUri);

                        return parseJsonWebKeySet(jwks);
                    }
                });
    }

    protected String getOpenIDMetadataDocumentUrl() {
        return String.format(
                "https://login.microsoftonline.com/%s/.well-known/openid-configuration%s",
                Optional.ofNullable(tenantId).orElse("common"),
                Optional.ofNullable(appId).map(i -> String.format("?appid=%s", i)).orElse(""));
    }

    protected String extractJwksUri(final String openIdMetadataDocument) {
        try {
            return jsonMapper.readTree(openIdMetadataDocument).get("jwks_uri").asText();
        } catch (IOException e) {
            throw new IllegalArgumentException("Extracting value of 'jwks_url' key from OpenID Metadata JSON document"
                    + " for Microsoft Entra failed:", e);
        }
    }

    protected String fetchDocument(final String url) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(String.format("Received HTTP status code %d", response.statusCode()));
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
                LOG.warn(
                        "Skipped non-asymmetric JSON Web Key with key id '{}' from retrieved JSON Web Key Set "
                        + "for Microsoft Entra", key.getKeyID());
                continue;
            }

            try {
                PublicKey pubKey = ((AsymmetricJWK) key).toPublicKey();
                if (pubKey instanceof RSAPublicKey) {
                    verifiers.put(
                            key.getKeyID(),
                            new RSASSAVerifier((RSAPublicKey) pubKey));
                } else if (pubKey instanceof ECPublicKey) {
                    verifiers.put(
                            key.getKeyID(),
                            new ECDSAVerifier((ECPublicKey) pubKey));
                }
            } catch (JOSEException e) {
                throw new IllegalArgumentException(
                        "Extracting public key from asymmetric JSON Web Key from retrieved JSON Web Key Set for"
                        + " Microsoft Entra failed:", e);
            }
        }

        return verifiers;
    }

    protected Map<String, JWSVerifier> getAllFromCache() {
        // Ensure cache is populated and gets refreshed, if expired
        verifiersCache.getAll(Set.of(StringUtils.EMPTY));

        return verifiersCache.asMap();
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return getAllFromCache().
                values().stream().
                flatMap(jwsVerifier -> jwsVerifier.supportedJWSAlgorithms().stream()).
                collect(Collectors.toSet());
    }

    @Override
    public JCAContext getJCAContext() {
        return getAllFromCache().
                values().stream().
                map(JCAAware::getJCAContext).
                findFirst().
                orElseThrow(() -> new IllegalStateException("JSON Web Key Set cache for Microsoft Entra is empty"));
    }

    @Override
    public boolean verify(
            final JWSHeader header,
            final byte[] signingInput,
            final Base64URL signature) throws JOSEException {

        String keyId = header.getKeyID();
        JWSVerifier delegate = Optional.ofNullable(verifiersCache.get(keyId)).
                orElseThrow(() -> new JOSEException(
                String.format("Microsoft Entra JSON Web Key Set cache could not retrieve a public key for "
                        + "given key id '%s'", keyId)));

        return delegate.verify(header, signingInput, signature);
    }
}
