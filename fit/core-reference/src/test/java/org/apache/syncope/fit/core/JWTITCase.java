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
package org.apache.syncope.fit.core;

import static org.junit.Assert.*;

import java.security.AccessControlException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

/**
 * Some tests for JWT Tokens
 */
public class JWTITCase extends AbstractITCase {

    @Test
    public void testGetJWTToken() throws ParseException {
        // Get the token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        String expiry = response.getHeaderString(RESTHeaders.TOKEN_EXPIRE);
        assertNotNull(expiry);

        // Validate the signature
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        JwsSignatureVerifier jwsSignatureVerifier =
            new HmacJwsSignatureVerifier(JWS_KEY.getBytes(), SignatureAlgorithm.HS512);
        assertTrue(consumer.verifySignatureWith(jwsSignatureVerifier));

        Date now = new Date();

        // Verify the expiry header matches that of the token
        Long expiryTime = consumer.getJwtClaims().getExpiryTime();
        assertNotNull(expiryTime);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date tokenDate = dateFormat.parse(dateFormat.format(new Date(expiryTime.longValue())));
        Date parsedDate = dateFormat.parse(expiry);

        assertEquals(tokenDate, parsedDate);
        assertTrue(parsedDate.after(now));

        // Verify issuedAt
        Long issuedAt = consumer.getJwtClaims().getIssuedAt();
        assertNotNull(issuedAt);
        assertTrue(new Date(issuedAt.longValue()).before(now));

        // Validate subject + issuer
        assertEquals("admin", consumer.getJwtClaims().getSubject());
        assertEquals(JWT_ISSUER, consumer.getJwtClaims().getIssuer());

        // Verify NotBefore
        Long notBefore = consumer.getJwtClaims().getNotBefore();
        assertNotNull(notBefore);
        assertTrue(new Date(notBefore.longValue()).before(now));
    }

    @Test
    public void testQueryUsingToken() throws ParseException {
        // Get the token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);

        // Query the UserSelfService using the token
        SyncopeClient jwtClient = clientFactory.create(token);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        jwtUserSelfService.read();

        // Test a "bad" token
        jwtClient = clientFactory.create(token + "xyz");
        jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on a modified token");
        } catch (WebServiceException ex) {
            // expected
        }
    }

    @Test
    public void testTokenValidation() throws ParseException {
        // Get an initial token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject("admin");
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime());
        jwtClaims.setNotBefore(now.getTime());

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider jwsSignatureProvider =
            new HmacJwsSignatureProvider(JWS_KEY.getBytes(), SignatureAlgorithm.HS512);
        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        jwtUserSelfService.read();
    }

    @Test
    public void testInvalidIssuer() throws ParseException {
        // Get an initial token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject("admin");
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer("UnknownIssuer");
        jwtClaims.setExpiryTime(expiry.getTime().getTime());
        jwtClaims.setNotBefore(now.getTime());

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider jwsSignatureProvider =
            new HmacJwsSignatureProvider(JWS_KEY.getBytes(), SignatureAlgorithm.HS512);
        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an invalid issuer");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void testExpiredToken() throws ParseException {
        // Get an initial token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject("admin");
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(now.getTime() - 5000L);
        jwtClaims.setNotBefore(now.getTime());

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider jwsSignatureProvider =
            new HmacJwsSignatureProvider(JWS_KEY.getBytes(), SignatureAlgorithm.HS512);
        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an expired token");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void testNotBefore() throws ParseException {
        // Get an initial token
        SyncopeClient adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = adminClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject("admin");
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime());
        jwtClaims.setNotBefore(now.getTime() + 60000L);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.HS512);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider jwsSignatureProvider =
            new HmacJwsSignatureProvider(JWS_KEY.getBytes(), SignatureAlgorithm.HS512);
        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on a token that is not valid yet");
        } catch (AccessControlException ex) {
            // expected
        }
    }
}
