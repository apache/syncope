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
import java.util.Calendar;
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
    public Pair<String, Date> generateJWT(
            final String tokenId,
            final String subject,
            final long duration,
            final Map<String, Object> claims) {

        credentialChecker.checkIsDefaultJWSKeyInUse();

        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, (int) duration);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(subject).
                issueTime(currentTime).
                issuer(securityProperties.getJwtIssuer()).
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        claims.forEach(claimsSet::claim);

        SignedJWT jwt = new SignedJWT(new JWSHeader(jwsSigner.getJwsAlgorithm()), claimsSet.build());
        try {
            jwt.sign(jwsSigner);
        } catch (JOSEException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        return Pair.of(jwt.serialize(), expiration.getTime());
    }

    private AccessToken replace(
            final String subject,
            final Map<String, Object> claims,
            final byte[] authorities,
            final AccessToken accessToken) {

        Pair<String, Date> generated = generateJWT(
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
    public Pair<String, Date> create(
            final String subject,
            final Map<String, Object> claims,
            final byte[] authorities,
            final boolean replace) {

        AccessToken accessToken = accessTokenDAO.findByOwner(subject);
        if (accessToken == null) {
            // no AccessToken found: create new
            accessToken = entityFactory.newEntity(AccessToken.class);
            accessToken.setKey(SecureRandomUtils.generateRandomUUID().toString());

            accessToken = replace(subject, claims, authorities, accessToken);
        } else if (replace || accessToken.getExpirationTime() == null
                || accessToken.getExpirationTime().before(new Date())) {

            // AccessToken found, but either replace was requested or it is expired: update existing
            accessToken = replace(subject, claims, authorities, accessToken);
        }

        return Pair.of(accessToken.getBody(), accessToken.getExpirationTime());
    }

    @Override
    public Pair<String, Date> update(final AccessToken accessToken, final byte[] authorities) {
        credentialChecker.checkIsDefaultJWSKeyInUse();

        long duration = confParamOps.get(AuthContextUtils.getDomain(), "jwt.lifetime.minutes", 120L, Long.class);

        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, (int) duration);

        SignedJWT jwt;
        try {
            JWTClaimsSet.Builder claimsSet =
                    new JWTClaimsSet.Builder(SignedJWT.parse(accessToken.getBody()).getJWTClaimsSet()).
                            expirationTime(expiration.getTime());

            jwt = new SignedJWT(new JWSHeader(jwsSigner.getJwsAlgorithm()), claimsSet.build());
            jwt.sign(jwsSigner);
        } catch (ParseException | JOSEException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        String body = jwt.serialize();

        accessToken.setBody(body);
        accessToken.setExpirationTime(expiration.getTime());

        if (!securityProperties.getAdminUser().equals(accessToken.getOwner())) {
            accessToken.setAuthorities(authorities);
        }

        accessTokenDAO.save(accessToken);

        return Pair.of(body, expiration.getTime());
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
