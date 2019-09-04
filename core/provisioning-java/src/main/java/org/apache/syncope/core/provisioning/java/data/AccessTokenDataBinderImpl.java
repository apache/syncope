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

import java.util.Date;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenDataBinderImpl implements AccessTokenDataBinder {

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "jwtIssuer")
    private String jwtIssuer;

    @Autowired
    private AccessTokenJwsSignatureProvider jwsSignatureProvider;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private ConfParamOps confParamOps;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private DefaultCredentialChecker credentialChecker;

    @Override
    public Pair<String, Date> generateJWT(
            final String tokenId,
            final String subject,
            final long duration,
            final Map<String, Object> claims) {

        credentialChecker.checkIsDefaultJWSKeyInUse();

        long currentTime = new Date().getTime() / 1000L;
        long expiryTime = currentTime + 60L * duration;

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(subject);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(jwtIssuer);
        jwtClaims.setExpiryTime(expiryTime);
        jwtClaims.setNotBefore(currentTime);
        claims.forEach(jwtClaims::setClaim);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, jwsSignatureProvider.getAlgorithm());
        JwtToken token = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);

        String signed = producer.signWith(jwsSignatureProvider);

        return Pair.of(signed, new Date(expiryTime * 1000L));
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
        accessToken.setExpiryTime(generated.getRight());
        accessToken.setOwner(subject);

        if (!adminUser.equals(accessToken.getOwner())) {
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
        } else if (replace || accessToken.getExpiryTime() == null || accessToken.getExpiryTime().before(new Date())) {
            // AccessToken found, but either replace was requested or it is expired: update existing
            accessToken = replace(subject, claims, authorities, accessToken);
        }

        return Pair.of(accessToken.getBody(), accessToken.getExpiryTime());
    }

    @Override
    public Pair<String, Date> update(final AccessToken accessToken, final byte[] authorities) {
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(accessToken.getBody());

        credentialChecker.checkIsDefaultJWSKeyInUse();

        long duration = confParamOps.get(AuthContextUtils.getDomain(), "jwt.lifetime.minutes", 120L, Long.class);
        long currentTime = new Date().getTime() / 1000L;
        long expiry = currentTime + 60L * duration;
        consumer.getJwtClaims().setExpiryTime(expiry);
        Date expiryDate = new Date(expiry * 1000L);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, jwsSignatureProvider.getAlgorithm());
        JwtToken token = new JwtToken(jwsHeaders, consumer.getJwtClaims());
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);

        String body = producer.signWith(jwsSignatureProvider);

        accessToken.setBody(body);
        // AccessToken stores expiry time in milliseconds, as opposed to seconds for the JWT tokens.
        accessToken.setExpiryTime(expiryDate);

        if (!adminUser.equals(accessToken.getOwner())) {
            accessToken.setAuthorities(authorities);
        }

        accessTokenDAO.save(accessToken);

        return Pair.of(body, expiryDate);
    }

    @Override
    public AccessTokenTO getAccessTokenTO(final AccessToken accessToken) {
        AccessTokenTO accessTokenTO = new AccessTokenTO();
        accessTokenTO.setKey(accessToken.getKey());
        accessTokenTO.setBody(accessToken.getBody());
        accessTokenTO.setExpiryTime(accessToken.getExpiryTime());
        accessTokenTO.setOwner(accessToken.getOwner());

        return accessTokenTO;
    }
}
