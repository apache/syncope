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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.security.AccessControlException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureProvider;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureVerifier;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.CustomJWTSSOProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Some tests for JWT Tokens.
 */
public class JWTITCase extends AbstractITCase {

    private JwsSignatureProvider jwsSignatureProvider;

    private JwsSignatureVerifier jwsSignatureVerifier;

    @BeforeEach
    public void setupVerifier() throws Exception {
        AccessTokenJwsSignatureProvider atjsp = new AccessTokenJwsSignatureProvider();
        atjsp.setJwsAlgorithm(JWS_ALGORITHM);
        atjsp.setJwsKey(JWS_KEY);
        atjsp.afterPropertiesSet();
        this.jwsSignatureProvider = atjsp;

        AccessTokenJwsSignatureVerifier atjsv = new AccessTokenJwsSignatureVerifier();
        atjsv.setJwsAlgorithm(JWS_ALGORITHM);
        atjsv.setJwsKey(JWS_KEY);
        atjsv.afterPropertiesSet();
        this.jwsSignatureVerifier = atjsv;
    }

    @Test
    public void getJWTToken() throws ParseException {
        // Get the token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        String expiry = response.getHeaderString(RESTHeaders.TOKEN_EXPIRE);
        assertNotNull(expiry);

        // Validate the signature
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        assertTrue(consumer.verifySignatureWith(jwsSignatureVerifier));

        Date now = new Date();

        // Verify the expiry header matches that of the token
        Long expiryTime = consumer.getJwtClaims().getExpiryTime();
        assertNotNull(expiryTime);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date tokenDate = dateFormat.parse(dateFormat.format(new Date(expiryTime * 1000L)));
        Date parsedDate = dateFormat.parse(expiry);

        assertEquals(tokenDate, parsedDate);
        assertTrue(parsedDate.after(now));

        // Verify issuedAt
        Long issuedAt = consumer.getJwtClaims().getIssuedAt();
        assertNotNull(issuedAt);
        assertTrue(new Date(issuedAt).before(now));

        // Validate subject + issuer
        assertEquals(ADMIN_UNAME, consumer.getJwtClaims().getSubject());
        assertEquals(JWT_ISSUER, consumer.getJwtClaims().getIssuer());

        // Verify NotBefore
        Long notBefore = consumer.getJwtClaims().getNotBefore();
        assertNotNull(notBefore);
        assertTrue(new Date(notBefore).before(now));
    }

    @Test
    public void queryUsingToken() throws ParseException {
        // Get the token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

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
    public void tokenValidation() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(ADMIN_UNAME);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        jwtUserSelfService.read();
    }

    @Test
    public void invalidIssuer() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(ADMIN_UNAME);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer("UnknownIssuer");
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

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
    public void expiredToken() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(ADMIN_UNAME);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime((now.getTime() - 5000L) / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

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
    public void notBefore() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(ADMIN_UNAME);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime + 60L);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

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

    @Test
    public void noneSignature() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(token);
        String tokenId = consumer.getJwtClaims().getTokenId();

        // Create a new token using the Id of the first token
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(tokenId);
        jwtClaims.setSubject(consumer.getJwtClaims().getSubject());
        jwtClaims.setIssuedAt(consumer.getJwtClaims().getIssuedAt());
        jwtClaims.setIssuer(consumer.getJwtClaims().getIssuer());
        jwtClaims.setExpiryTime(consumer.getJwtClaims().getExpiryTime());
        jwtClaims.setNotBefore(consumer.getJwtClaims().getNotBefore());

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, SignatureAlgorithm.NONE);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider noneJwsSignatureProvider = new NoneJwsSignatureProvider();
        String signed = producer.signWith(noneJwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on no signature");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void unknownId() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);

        // Create a new token using an unknown Id
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID.randomUUID().toString());
        jwtClaims.setSubject(ADMIN_UNAME);
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(JWT_ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        String signed = producer.signWith(jwsSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an unknown id");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void thirdPartyToken() throws ParseException {
        assumeFalse(SignatureAlgorithm.isPublicKeyAlgorithm(JWS_ALGORITHM));

        // Create a new token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID.randomUUID().toString());
        jwtClaims.setSubject("puccini@apache.org");
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(CustomJWTSSOProvider.ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider customSignatureProvider =
                new HmacJwsSignatureProvider(CustomJWTSSOProvider.CUSTOM_KEY.getBytes(), JWS_ALGORITHM);
        String signed = producer.signWith(customSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);

        Pair<Map<String, Set<String>>, UserTO> self = jwtClient.self();
        assertFalse(self.getLeft().isEmpty());
        assertEquals("puccini", self.getRight().getUsername());
    }

    @Test
    public void thirdPartyTokenUnknownUser() throws ParseException {
        assumeFalse(SignatureAlgorithm.isPublicKeyAlgorithm(JWS_ALGORITHM));

        // Create a new token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID.randomUUID().toString());
        jwtClaims.setSubject("strauss@apache.org");
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(CustomJWTSSOProvider.ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider customSignatureProvider =
                new HmacJwsSignatureProvider(CustomJWTSSOProvider.CUSTOM_KEY.getBytes(), JWS_ALGORITHM);
        String signed = producer.signWith(customSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on an unknown subject");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void thirdPartyTokenUnknownIssuer() throws ParseException {
        assumeFalse(SignatureAlgorithm.isPublicKeyAlgorithm(JWS_ALGORITHM));

        // Create a new token
        Date now = new Date();
        long currentTime = now.getTime() / 1000L;

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID.randomUUID().toString());
        jwtClaims.setSubject("puccini@apache.org");
        jwtClaims.setIssuedAt(currentTime);
        jwtClaims.setIssuer(CustomJWTSSOProvider.ISSUER + "_");
        jwtClaims.setExpiryTime(expiry.getTime().getTime() / 1000L);
        jwtClaims.setNotBefore(currentTime);

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider customSignatureProvider =
                new HmacJwsSignatureProvider(CustomJWTSSOProvider.CUSTOM_KEY.getBytes(), JWS_ALGORITHM);
        String signed = producer.signWith(customSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on an unknown issuer");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void thirdPartyTokenBadSignature() throws ParseException {
        assumeFalse(SignatureAlgorithm.isPublicKeyAlgorithm(JWS_ALGORITHM));

        // Create a new token
        Date now = new Date();

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 5);

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setTokenId(UUID.randomUUID().toString());
        jwtClaims.setSubject("puccini@apache.org");
        jwtClaims.setIssuedAt(now.getTime());
        jwtClaims.setIssuer(CustomJWTSSOProvider.ISSUER);
        jwtClaims.setExpiryTime(expiry.getTime().getTime());
        jwtClaims.setNotBefore(now.getTime());

        JwsHeaders jwsHeaders = new JwsHeaders(JoseType.JWT, JWS_ALGORITHM);
        JwtToken jwtToken = new JwtToken(jwsHeaders, jwtClaims);
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(jwtToken);

        JwsSignatureProvider customSignatureProvider =
                new HmacJwsSignatureProvider((CustomJWTSSOProvider.CUSTOM_KEY + "_").getBytes(), JWS_ALGORITHM);
        String signed = producer.signWith(customSignatureProvider);

        SyncopeClient jwtClient = clientFactory.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on a bad signature");
        } catch (AccessControlException ex) {
            // expected
        }
    }

    @Test
    public void issueSYNCOPE1420() {
        Attr orig = configurationService.get("jwt.lifetime.minutes");
        try {
            // set for immediate JWT expiration
            configurationService.set(new Attr.Builder("jwt.lifetime.minutes").value("0").build());

            UserCR userCR = UserITCase.getUniqueSample("syncope164@syncope.apache.org");
            UserTO user = createUser(userCR).getEntity();
            assertNotNull(user);

            // login, get JWT with  expiryTime
            String jwt = clientFactory.create(user.getUsername(), "password123").getJWT();

            JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(jwt);
            assertTrue(consumer.verifySignatureWith(jwsSignatureVerifier));
            Long expiryTime = consumer.getJwtClaims().getExpiryTime();
            assertNotNull(expiryTime);

            // wait for 1 sec, check that JWT is effectively expired
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // ignore
            }
            assertTrue(expiryTime < System.currentTimeMillis());

            // login again, get new JWT
            // (even if ExpiredAccessTokenCleanup did not run yet, as it is scheduled every 5 minutes)
            String newJWT = clientFactory.create(user.getUsername(), "password123").getJWT();
            assertNotEquals(jwt, newJWT);
        } finally {
            configurationService.set(orig);
        }
    }
}
