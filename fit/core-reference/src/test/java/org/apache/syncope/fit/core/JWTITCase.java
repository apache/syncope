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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.CustomJWTSSOProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Some tests for JWT Tokens.
 */
public class JWTITCase extends AbstractITCase {

    private static AccessTokenJWSSigner JWS_SIGNER;

    private static AccessTokenJWSVerifier JWS_VERIFIER;

    @BeforeAll
    public static void setupVerifier() throws Exception {
        JWS_SIGNER = new AccessTokenJWSSigner(JWS_ALGORITHM, JWS_KEY);
        JWS_VERIFIER = new AccessTokenJWSVerifier(JWS_ALGORITHM, JWS_KEY);
    }

    @Test
    public void getJWTToken() throws ParseException, JOSEException {
        // Get the token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        String expiration = response.getHeaderString(RESTHeaders.TOKEN_EXPIRE);
        assertNotNull(expiration);

        // Validate the signature
        SignedJWT jwt = SignedJWT.parse(token);
        jwt.verify(JWS_VERIFIER);
        assertTrue(jwt.verify(JWS_VERIFIER));

        Date now = new Date();

        // Verify the expiry header matches that of the token
        Date tokenDate = jwt.getJWTClaimsSet().getExpirationTime();
        assertNotNull(tokenDate);

        Date parsedDate = new Date(OffsetDateTime.parse(expiration).
                truncatedTo(ChronoUnit.SECONDS).toInstant().toEpochMilli());

        assertEquals(tokenDate, parsedDate);
        assertTrue(parsedDate.after(now));

        // Verify issuedAt
        Date issueTime = jwt.getJWTClaimsSet().getIssueTime();
        assertNotNull(issueTime);
        assertTrue(issueTime.before(now));

        // Validate subject + issuer
        assertEquals(ADMIN_UNAME, jwt.getJWTClaimsSet().getSubject());
        assertEquals(JWT_ISSUER, jwt.getJWTClaimsSet().getIssuer());

        // Verify NotBefore
        Date notBeforeTime = jwt.getJWTClaimsSet().getNotBeforeTime();
        assertNotNull(notBeforeTime);
        assertTrue(notBeforeTime.before(now));
    }

    @Test
    public void queryUsingToken() throws ParseException {
        // Get the token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);

        // Query the UserSelfService using the token
        SyncopeClient jwtClient = CLIENT_FACTORY.create(token);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        jwtUserSelfService.read();

        // Test a "bad" token
        jwtClient = CLIENT_FACTORY.create(token + "xyz");
        jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on a modified token");
        } catch (NotAuthorizedException e) {
            assertEquals("Invalid signature found in JWT", e.getMessage());
        }
    }

    @Test
    public void tokenValidation() throws ParseException, JOSEException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        SignedJWT jwt = SignedJWT.parse(token);
        String tokenId = jwt.getJWTClaimsSet().getJWTID();

        // Create a new token using the Id of the first token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(ADMIN_UNAME).
                issueTime(currentTime).
                issuer(JWT_ISSUER).
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        jwtUserSelfService.read();
    }

    @Test
    public void invalidIssuer() throws ParseException, JOSEException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        SignedJWT jwt = SignedJWT.parse(token);
        String tokenId = jwt.getJWTClaimsSet().getJWTID();

        // Create a new token using the Id of the first token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(ADMIN_UNAME).
                issueTime(currentTime).
                issuer("UnknownIssuer").
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an invalid issuer");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void expiredToken() throws ParseException, JOSEException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        SignedJWT jwt = SignedJWT.parse(token);
        String tokenId = jwt.getJWTClaimsSet().getJWTID();

        // Create a new token using the Id of the first token
        Date currentTime = new Date();

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(ADMIN_UNAME).
                issueTime(currentTime).
                issuer(JWT_ISSUER).
                expirationTime(new Date(currentTime.getTime() - 5000L)).
                notBeforeTime(currentTime);
        jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an expired token");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void notBefore() throws ParseException, JOSEException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        SignedJWT jwt = SignedJWT.parse(token);
        String tokenId = jwt.getJWTClaimsSet().getJWTID();

        // Create a new token using the Id of the first token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(tokenId).
                subject(ADMIN_UNAME).
                issueTime(currentTime).
                issuer(JWT_ISSUER).
                expirationTime(expiration.getTime()).
                notBeforeTime(new Date(currentTime.getTime() + 60000L));
        jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on a token that is not valid yet");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void noSignature() throws ParseException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        JWT jwt = SignedJWT.parse(token);

        // Create a new token using the Id of the first token
        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder(jwt.getJWTClaimsSet());
        jwt = new PlainJWT(claimsSet.build());
        String bearer = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(bearer);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on no signature");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void unknownId() throws ParseException, JOSEException {
        // Get an initial token
        SyncopeClient localClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);
        AccessTokenService accessTokenService = localClient.getService(AccessTokenService.class);

        Response response = accessTokenService.login();
        String token = response.getHeaderString(RESTHeaders.TOKEN);
        assertNotNull(token);
        SignedJWT jwt = SignedJWT.parse(token);

        // Create a new token using an unknown Id
        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder(jwt.getJWTClaimsSet()).
                jwtID(UUID.randomUUID().toString());
        jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);
        UserSelfService jwtUserSelfService = jwtClient.getService(UserSelfService.class);
        try {
            jwtUserSelfService.read();
            fail("Failure expected on an unknown id");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void thirdPartyToken() throws ParseException, JOSEException {
        assumeFalse(JWSAlgorithm.Family.RSA.contains(JWS_ALGORITHM));

        // Create a new token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(UUID.randomUUID().toString()).
                subject("puccini@apache.org").
                issueTime(currentTime).
                issuer(CustomJWTSSOProvider.ISSUER).
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWS_ALGORITHM), claimsSet.build());
        jwt.sign(new MACSigner(CustomJWTSSOProvider.CUSTOM_KEY));
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);

        Triple<Map<String, Set<String>>, List<String>, UserTO> self = jwtClient.self();
        assertFalse(self.getLeft().isEmpty());
        assertEquals("puccini", self.getRight().getUsername());
    }

    @Test
    public void thirdPartyTokenUnknownUser() throws ParseException, JOSEException {
        assumeFalse(JWSAlgorithm.Family.RSA.contains(JWS_ALGORITHM));

        // Create a new token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(UUID.randomUUID().toString()).
                subject("strauss@apache.org").
                issueTime(currentTime).
                issuer(CustomJWTSSOProvider.ISSUER).
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on an unknown subject");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void thirdPartyTokenUnknownIssuer() throws ParseException, JOSEException {
        assumeFalse(JWSAlgorithm.Family.RSA.contains(JWS_ALGORITHM));

        // Create a new token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(UUID.randomUUID().toString()).
                subject("puccini@apache.org").
                issueTime(currentTime).
                issuer(CustomJWTSSOProvider.ISSUER + "_").
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWS_SIGNER.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(JWS_SIGNER);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on an unknown issuer");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void thirdPartyTokenBadSignature()
            throws ParseException, KeyLengthException, NoSuchAlgorithmException,
            InvalidKeySpecException, JOSEException {

        assumeFalse(JWSAlgorithm.Family.RSA.contains(JWS_ALGORITHM));

        // Create a new token
        Date currentTime = new Date();

        Calendar expiration = Calendar.getInstance();
        expiration.setTime(currentTime);
        expiration.add(Calendar.MINUTE, 5);

        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder().
                jwtID(UUID.randomUUID().toString()).
                subject("puccini@apache.org").
                issueTime(currentTime).
                issuer(CustomJWTSSOProvider.ISSUER).
                expirationTime(expiration.getTime()).
                notBeforeTime(currentTime);

        AccessTokenJWSSigner customJWSSigner =
                new AccessTokenJWSSigner(JWS_ALGORITHM, RandomStringUtils.insecure().nextAlphanumeric(512));

        SignedJWT jwt = new SignedJWT(new JWSHeader(customJWSSigner.getJwsAlgorithm()), claimsSet.build());
        jwt.sign(customJWSSigner);
        String signed = jwt.serialize();

        SyncopeClient jwtClient = CLIENT_FACTORY.create(signed);

        try {
            jwtClient.self();
            fail("Failure expected on a bad signature");
        } catch (NotAuthorizedException e) {
            // expected
        }
    }

    @Test
    public void issueSYNCOPE1420() throws ParseException {
        Long orig = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, "jwt.lifetime.minutes", null, Long.class);
        try {
            // set for immediate JWT expiration
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "jwt.lifetime.minutes", 0);

            UserCR userCR = UserITCase.getUniqueSample("syncope164@syncope.apache.org");
            UserTO user = createUser(userCR).getEntity();
            assertNotNull(user);

            // login, get JWT with  expiryTime
            String jwt = CLIENT_FACTORY.create(user.getUsername(), "password123").getJWT();

            Date expirationTime = SignedJWT.parse(jwt).getJWTClaimsSet().getExpirationTime();
            assertNotNull(expirationTime);

            // wait for 1 sec, check that JWT is effectively expired
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // ignore
            }
            assertTrue(expirationTime.before(new Date()));

            // login again, get new JWT
            // (even if ExpiredAccessTokenCleanup did not run yet, as it is scheduled every 5 minutes)
            String newJWT = CLIENT_FACTORY.create(user.getUsername(), "password123").getJWT();
            assertNotEquals(jwt, newJWT);
        } finally {
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "jwt.lifetime.minutes", orig);
        }
    }
}
