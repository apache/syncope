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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAuthProfile;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuthProfileTest extends AbstractTest {

    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @BeforeEach
    public void beforeEach() {
        neo4jTemplate.deleteAll(Neo4jAuthProfile.class);
    }

    @Test
    public void googleMfaToken() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithToken(id, 123456);

        Optional<? extends AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(PageRequest.of(0, 100)).isEmpty());

        AuthProfile authProfile = result.get();
        result = authProfileDAO.findById(authProfile.getKey());
        assertTrue(result.isPresent());

        authProfile.setOwner("SyncopeCreate-New");
        authProfile.setGoogleMfaAuthTokens(List.of());
        authProfileDAO.save(authProfile);

        assertFalse(authProfileDAO.findByOwner(id).isPresent());
    }

    @Test
    public void webAuthnRegisteredDevice() {
        String id = SecureRandomUtils.generateRandomUUID().toString();
        String record = "[ {"
                + "    \"userIdentity\" : {"
                + "      \"name\" : \"casuser\","
                + "      \"displayName\" : \"casuser\""
                + "    },"
                + "    \"credential\" : {"
                + "      \"credentialId\" : \"fFGyV3K5x1\""
                + "    },"
                + "    \"username\" : \"casuser\""
                + "  } ]";

        WebAuthnDeviceCredential credential = new WebAuthnDeviceCredential.Builder().
                json(record).
                identifier("fFGyV3K5x1").
                build();

        createAuthProfileWithWebAuthnDevice(id, List.of(credential));

        Optional<? extends AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(PageRequest.of(0, 100)).isEmpty());

        AuthProfile authProfile = result.get();
        result = authProfileDAO.findById(authProfile.getKey());
        assertTrue(result.isPresent());

        authProfile.setOwner("newowner");
        authProfile.setWebAuthnDeviceCredentials(List.of());
        authProfileDAO.save(authProfile);

        assertFalse(authProfileDAO.findByOwner(id).isPresent());
    }

    @Test
    public void googleMfaAccount() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithAccount(id);

        Optional<? extends AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(PageRequest.of(0, 100)).isEmpty());

        AuthProfile authProfile = result.get();
        result = authProfileDAO.findById(authProfile.getKey());
        assertTrue(result.isPresent());

        String secret = SecureRandomUtils.generateRandomUUID().toString();
        List<GoogleMfaAuthAccount> googleMfaAuthAccounts = authProfile.getGoogleMfaAuthAccounts();
        assertFalse(googleMfaAuthAccounts.isEmpty());
        GoogleMfaAuthAccount googleMfaAuthAccount = googleMfaAuthAccounts.getFirst();
        googleMfaAuthAccount.setSecretKey(secret);

        authProfile.setGoogleMfaAuthAccounts(googleMfaAuthAccounts);
        authProfile = authProfileDAO.save(authProfile);
        assertEquals(secret, authProfile.getGoogleMfaAuthAccounts().getFirst().getSecretKey());
    }

    @Test
    public void impersonationAccounts() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithAccount(id);

        Optional<? extends AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        AuthProfile authProfile = result.get();
        result = authProfileDAO.findById(authProfile.getKey());
        assertTrue(result.isPresent());

        List<ImpersonationAccount> accounts = IntStream.range(1, 10).
                mapToObj(i -> new ImpersonationAccount.Builder().impersonated("impersonatee" + i).build()).
                toList();

        authProfile.setImpersonationAccounts(accounts);
        authProfile = authProfileDAO.save(authProfile);
        assertEquals(accounts.size(), authProfile.getImpersonationAccounts().size());
    }

    private AuthProfile createAuthProfileWithToken(final String owner, final Integer otp) {
        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        GoogleMfaAuthToken token = new GoogleMfaAuthToken.Builder().issueDate(LocalDateTime.now()).token(otp).build();
        profile.setGoogleMfaAuthTokens(List.of(token));
        return authProfileDAO.save(profile);
    }

    private AuthProfile createAuthProfileWithWebAuthnDevice(
            final String owner,
            final List<WebAuthnDeviceCredential> credentials) {

        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        profile.setWebAuthnDeviceCredentials(credentials);
        return authProfileDAO.save(profile);
    }

    private AuthProfile createAuthProfileWithAccount(final String owner) {
        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount.Builder()
                .registrationDate(OffsetDateTime.now())
                .scratchCodes(List.of(1, 2, 3, 4, 5))
                .secretKey(SecureRandomUtils.generateRandomUUID().toString())
                .validationCode(123456)
                .name(SecureRandomUtils.generateRandomUUID().toString())
                .build();
        profile.setGoogleMfaAuthAccounts(List.of(account));
        return authProfileDAO.save(profile);
    }
}
