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
package org.apache.syncope.core.logic;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenLogic extends AbstractTransactionalLogic<AccessTokenTO> {

    private static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();

    private static final JwsHeaders JWS_HEADERS = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);

    @Resource(name = "jwtIssuer")
    private String jwtIssuer;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Autowired
    private JwsSignatureProvider jwsSignatureProvider;

    @Autowired
    private AccessTokenDataBinder binder;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private ConfDAO confDAO;

    @PreAuthorize("isAuthenticated()")
    public String login(final String remoteHost) {
        if (anonymousUser.equals(AuthContextUtils.getUsername())) {
            throw new IllegalArgumentException(anonymousUser + " cannot be granted for an access token");
        }

        String body = null;

        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
        if (accessToken != null) {
            body = accessToken.getBody();
        }

        if (body == null) {
            Date now = new Date();
            Calendar expiry = Calendar.getInstance();
            expiry.setTime(now);
            expiry.add(Calendar.MINUTE,
                    confDAO.find("jwt.lifetime.minutes", "120").getValues().get(0).getLongValue().intValue());

            JwtClaims claims = new JwtClaims();
            claims.setTokenId(UUID_GENERATOR.generate().toString());
            claims.setSubject(AuthContextUtils.getUsername());
            claims.setIssuedAt(now.getTime());
            claims.setIssuer(jwtIssuer);
            claims.setExpiryTime(expiry.getTime().getTime());
            claims.setNotBefore(now.getTime());
            claims.setClaim(SyncopeConstants.JWT_CLAIM_REMOTE_HOST, remoteHost);

            JwtToken token = new JwtToken(JWS_HEADERS, claims);
            JwsJwtCompactProducer producer = new JwsJwtCompactProducer(token);

            body = producer.signWith(jwsSignatureProvider);

            binder.create(claims.getTokenId(), body, expiry.getTime());
        }

        return body;
    }

    @PreAuthorize("isAuthenticated()")
    public String refresh() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
        if (accessToken == null) {
            throw new NotFoundException("AccessToken for " + AuthContextUtils.getUsername());
        }

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

        binder.update(accessToken, body, expiry.getTime());

        return body;
    }

    @PreAuthorize("isAuthenticated()")
    public void logout() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
        if (accessToken == null) {
            throw new NotFoundException("AccessToken for " + AuthContextUtils.getUsername());
        }

        delete(accessToken.getKey());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ACCESS_TOKEN_LIST + "')")
    public int count() {
        return accessTokenDAO.count();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ACCESS_TOKEN_LIST + "')")
    public List<AccessTokenTO> list(
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        return CollectionUtils.collect(accessTokenDAO.findAll(page, size, orderByClauses),
                new Transformer<AccessToken, AccessTokenTO>() {

            @Override
            public AccessTokenTO transform(final AccessToken input) {
                return binder.getAccessTokenTO(input);
            }
        }, new ArrayList<AccessTokenTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ACCESS_TOKEN_DELETE + "')")
    public void delete(final String key) {
        accessTokenDAO.delete(key);
    }

    @Override
    protected AccessTokenTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }

}
