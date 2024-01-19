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
package org.apache.syncope.core.provisioning.java.data;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;

public class AccessTokenDataBinderImpl implements AccessTokenDataBinder {

    protected final SecurityProperties securityProperties;

    protected final AccessTokenJWSSigner jwsSigner;

    protected final AccessTokenDAO accessTokenDAO;

    protected final ConfParamOps confParamOps;

    protected final EntityFactory entityFactory;

    protected final DefaultCredentialChecker credentialChecker;

    public AccessTokenDataBinderImpl(
            final SecurityProperties securityProperties,
            final AccessTokenJWSSigner jwsSigner,
            final AccessTokenDAO accessTokenDAO,
            final ConfParamOps confParamOps,
            final EntityFactory entityFactory,
            final DefaultCredentialChecker credentialChecker) {

        this.securityProperties = securityProperties;
        this.jwsSigner = jwsSigner;
        this.accessTokenDAO = accessTokenDAO;
        this.confParamOps = confParamOps;
        this.entityFactory = entityFactory;
        this.credentialChecker = credentialChecker;
    }

    @Override
    public Pair<String, OffsetDateTime> generateJWT(
            final String tokenId,
            final String subject,
            final long duration,
            final Map<String, Object> claims) {

        credentialChecker.checkIsDefaultJWSKeyInUse();

        OffsetDateTime currentTime = OffsetDateTime.now();
        Date issueTime = new Date(currentTime.toInstant().toEpochMilli());

        OffsetDateTime expiration = currentTime.plusMinutes(duration);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(subject).
                issuer(securityProperties.getJwtIssuer()).
                issueTime(issueTime).
                expirationTime(new Date(expiration.toInstant().toEpochMilli())).
                notBeforeTime(issueTime);
        claims.forEach(claimsSet::claim);

        SignedJWT jwt = new SignedJWT(new JWSHeader(jwsSigner.getJwsAlgorithm()), claimsSet.build());
        try {
            jwt.sign(jwsSigner);
        } catch (JOSEException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        return Pair.of(jwt.serialize(), expiration);
    }

    private AccessToken replace(
            final String subject,
            final Map<String, Object> claims,
            final byte[] authorities,
            final AccessToken accessToken) {

        Pair<String, OffsetDateTime> generated = generateJWT(
                accessToken.getKey(),
                subject,
                confParamOps.get(AuthContextUtils.getDomain(), "jwt.lifetime.minutes", 120L, Long.class),
                claims);

        accessToken.setBody(generated.getLeft());
        accessToken.setExpirationTime(generated.getRight());
        accessToken.setOwner(subject);

        if (!securityProperties.getAdminUser().equals(accessToken.getOwner())) {
            accessToken.setAuthorities(authorities);
        }

        return accessTokenDAO.save(accessToken);
    }

    @Override
    public Pair<String, OffsetDateTime> create(
            final String subject,
            final Map<String, Object> claims,
            final byte[] authorities,
            final boolean replace) {

        AccessToken accessToken = accessTokenDAO.findByOwner(subject).
                map(at -> {
                    if (replace
                            || at.getExpirationTime() == null
                            || at.getExpirationTime().isBefore(OffsetDateTime.now())) {

                        // AccessToken found, but either replace was requested or it is expired: update existing
                        return replace(subject, claims, authorities, at);
                    }
                    return at;
                }).
                orElseGet(() -> {
                    // no AccessToken found: create new
                    AccessToken at = entityFactory.newEntity(AccessToken.class);
                    at.setKey(SecureRandomUtils.generateRandomUUID().toString());

                    return replace(subject, claims, authorities, at);
                });

        return Pair.of(accessToken.getBody(), accessToken.getExpirationTime());
    }

    @Override
    public Pair<String, OffsetDateTime> update(final AccessToken accessToken, final byte[] authorities) {
        credentialChecker.checkIsDefaultJWSKeyInUse();

        long duration = confParamOps.get(AuthContextUtils.getDomain(), "jwt.lifetime.minutes", 120L, Long.class);

        OffsetDateTime currentTime = OffsetDateTime.now();

        OffsetDateTime expiration = currentTime.plusMinutes(duration);

        SignedJWT jwt;
        try {
            JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder(
                    SignedJWT.parse(accessToken.getBody()).getJWTClaimsSet()).
                    expirationTime(new Date(expiration.toInstant().toEpochMilli()));

            jwt = new SignedJWT(new JWSHeader(jwsSigner.getJwsAlgorithm()), claimsSet.build());
            jwt.sign(jwsSigner);
        } catch (ParseException | JOSEException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        String body = jwt.serialize();

        accessToken.setBody(body);
        accessToken.setExpirationTime(expiration);

        if (!securityProperties.getAdminUser().equals(accessToken.getOwner())) {
            accessToken.setAuthorities(authorities);
        }

        accessTokenDAO.save(accessToken);

        return Pair.of(body, expiration);
    }

    @Override
    public AccessTokenTO getAccessTokenTO(final AccessToken accessToken) {
        AccessTokenTO accessTokenTO = new AccessTokenTO();
        accessTokenTO.setKey(accessToken.getKey());
        accessTokenTO.setBody(accessToken.getBody());
        accessTokenTO.setExpirationTime(accessToken.getExpirationTime());
        accessTokenTO.setOwner(accessToken.getOwner());

        return accessTokenTO;
    }
}
