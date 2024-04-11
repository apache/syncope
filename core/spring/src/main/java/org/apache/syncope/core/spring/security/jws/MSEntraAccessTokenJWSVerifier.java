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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.jca.JCAAware;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.integration.CacheLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSEntraAccessTokenJWSVerifier implements JWSVerifier {

    protected static final Logger LOG = LoggerFactory.getLogger(MSEntraAccessTokenJWSVerifier.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final Cache<String, JWSVerifier> verifiersCache;

    public MSEntraAccessTokenJWSVerifier(
            final CacheManager cacheManager,
            final CacheLoader<String, JWSVerifier> cacheLoader,
            final Duration cacheExpireAfterWrite) {

        /*
         * At any given point in time, Entra ID (formerly: Azure AD) may sign an ID token using
         * any one of a certain set of public-private key pairs. Entra ID rotates the possible
         * set of keys on a periodic basis, so the application should be written to handle those
         * key changes automatically. A reasonable frequency to check for updates to the public
         * keys used by Entra ID is every 24 hours.
         */
        verifiersCache = cacheManager.createCache(
                SecureRandomUtils.generateRandomUUID().toString(),
                new MutableConfiguration<String, JWSVerifier>().
                        setTypes(String.class, JWSVerifier.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setCacheLoaderFactory(new FactoryBuilder.SingletonFactory<>(cacheLoader)).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(cacheExpireAfterWrite)));
    }

    protected Stream<JWSVerifier> getAllFromCache() {
        // Ensure cache is populated and gets refreshed, if expired
        verifiersCache.getAll(Set.of(StringUtils.EMPTY));

        return StreamSupport.stream(verifiersCache.spliterator(), false).map(Cache.Entry::getValue);
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return getAllFromCache().
                flatMap(jwsVerifier -> jwsVerifier.supportedJWSAlgorithms().stream()).
                collect(Collectors.toSet());
    }

    @Override
    public JCAContext getJCAContext() {
        return getAllFromCache().
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
                String.format("JSON Web Key Set cache could not retrieve a public key for given key id '%s'", keyId)));

        return delegate.verify(header, signingInput, signature);
    }
}
