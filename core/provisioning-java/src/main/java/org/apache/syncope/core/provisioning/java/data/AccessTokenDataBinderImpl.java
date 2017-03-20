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

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenDataBinderImpl implements AccessTokenDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "owner" };

    private static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();

    private static final JwsHeaders JWS_HEADERS = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);

    @Resource(name = "jwtIssuer")
    private String jwtIssuer;

    @Autowired
    private JwsSignatureProvider jwsSignatureProvider;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Triple<String, String, Date> generateJWT(
            final String subject, final int duration, final Map<String, Object> claims) {

        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, duration);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID_GENERATOR.generate().toString());
        jwtClaims.setSubject(subject);
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer(jwtIssuer);
        jwtClaims.setExpiryTime(expiry.getTime().getTime());
        jwtClaims.setNotBefore(now.getTime());
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            jwtClaims.setClaim(entry.getKey(), entry.getValue());
        }

        JwtToken token = new JwtToken(JWS_HEADERS, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);

        String signed = producer.signWith(jwsSignatureProvider);

        return Triple.of(jwtClaims.getTokenId(), signed, expiry.getTime());
    }

    @Override
    public String create(final String subject, final Map<String, Object> claims, final boolean replaceExisting) {
        String body = null;

        AccessToken existing = accessTokenDAO.findByOwner(subject);
        if (existing != null) {
            body = existing.getBody();
        }

        if (replaceExisting || body == null) {
            Triple<String, String, Date> created = generateJWT(
                    subject,
                    confDAO.find("jwt.lifetime.minutes", "120").getValues().get(0).getLongValue().intValue(),
                    claims);

            body = created.getMiddle();

            AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
            accessToken.setKey(created.getLeft());
            accessToken.setBody(body);
            accessToken.setExpiryTime(created.getRight());
            accessToken.setOwner(subject);
            accessTokenDAO.save(accessToken);
        }

        if (replaceExisting && existing != null) {
            accessTokenDAO.delete(existing);
        }

        return body;
    }

    @Override
    public String update(final AccessToken accessToken) {
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(accessToken.getBody());

        Date now = new Date();
        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE,
                confDAO.find("jwt.lifetime.minutes", "120").getValues().get(0).getLongValue().intValue());
        consumer.getJwtClaims().setExpiryTime(expiry.getTime().getTime());

        JwtToken token = new JwtToken(JWS_HEADERS, consumer.getJwtClaims());
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);

        String body = producer.signWith(jwsSignatureProvider);

        accessToken.setBody(body);
        accessToken.setExpiryTime(expiry.getTime());

        accessTokenDAO.save(accessToken);

        return body;
    }

    @Override
    public AccessTokenTO getAccessTokenTO(final AccessToken accessToken) {
        AccessTokenTO accessTokenTO = new AccessTokenTO();
        BeanUtils.copyProperties(accessToken, accessTokenTO, IGNORE_PROPERTIES);
        accessTokenTO.setOwner(accessToken.getOwner());

        return accessTokenTO;
    }
}
