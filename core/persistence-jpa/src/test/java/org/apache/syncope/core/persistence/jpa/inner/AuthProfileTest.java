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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAAuthProfile;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AuthProfileTest extends AbstractTest {

    @Autowired
    private AuthProfileDAO authProfileDAO;

    @BeforeEach
    public void beforeEach() {
        entityManager().createQuery("DELETE FROM " + JPAAuthProfile.class.getSimpleName()).executeUpdate();
    }

    @Test
    public void googleMfaToken() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithToken(id, 123456);

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(-1, -1).isEmpty());

        AuthProfile authProfile = result.get();
        result = Optional.ofNullable(authProfileDAO.find(authProfile.getKey()));
        assertTrue(result.isPresent());

        authProfile.setOwner("SyncopeCreate-New");
        authProfile.setGoogleMfaAuthTokens(List.of());
        authProfileDAO.save(authProfile);

        assertFalse(authProfileDAO.findByOwner(id).isPresent());
    }

    @Test
    public void u2fRegisteredDevice() {
        String id = SecureRandomUtils.generateRandomUUID().toString();
        createAuthProfileWithU2FDevice(id, "{ 'record': 1 }");

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(-1, -1).isEmpty());

        AuthProfile authProfile = result.get();
        result = Optional.ofNullable(authProfileDAO.find(authProfile.getKey()));
        assertTrue(result.isPresent());

        authProfile.setOwner("SyncopeCreate-NewU2F");
        authProfile.setU2FRegisteredDevices(List.of());
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

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(-1, -1).isEmpty());

        AuthProfile authProfile = result.get();
        result = Optional.ofNullable(authProfileDAO.find(authProfile.getKey()));
        assertTrue(result.isPresent());

        authProfile.setOwner("SyncopeCreate-NewU2F");
        authProfile.setWebAuthnDeviceCredentials(List.of());
        authProfileDAO.save(authProfile);

        assertFalse(authProfileDAO.findByOwner(id).isPresent());
    }

    @Test
    public void googleMfaAccount() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithAccount(id);

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll(-1, -1).isEmpty());

        AuthProfile authProfile = result.get();
        result = Optional.ofNullable(authProfileDAO.find(authProfile.getKey()));
        assertTrue(result.isPresent());

        String secret = SecureRandomUtils.generateRandomUUID().toString();
        List<GoogleMfaAuthAccount> googleMfaAuthAccounts = authProfile.getGoogleMfaAuthAccounts();
        assertFalse(googleMfaAuthAccounts.isEmpty());
        GoogleMfaAuthAccount googleMfaAuthAccount = googleMfaAuthAccounts.get(0);
        googleMfaAuthAccount.setSecretKey(secret);

        authProfile.setGoogleMfaAuthAccounts(googleMfaAuthAccounts);
        authProfile = authProfileDAO.save(authProfile);
        assertEquals(secret, authProfile.getGoogleMfaAuthAccounts().get(0).getSecretKey());
    }

    @Test
    public void impersonationAccounts() {
        String id = SecureRandomUtils.generateRandomUUID().toString();

        createAuthProfileWithAccount(id);

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        AuthProfile authProfile = result.get();
        result = Optional.ofNullable(authProfileDAO.find(authProfile.getKey()));
        assertTrue(result.isPresent());

        List<ImpersonationAccount> accounts = IntStream.range(1, 10).
                mapToObj(i -> new ImpersonationAccount.Builder().impersonated("impersonatee" + i).build()).
                collect(Collectors.toList());

        authProfile.setImpersonationAccounts(accounts);
        authProfile = authProfileDAO.save(authProfile);
        assertEquals(accounts.size(), authProfile.getImpersonationAccounts().size());
    }

    private AuthProfile createAuthProfileWithToken(final String owner, final Integer otp) {
        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        GoogleMfaAuthToken token = new GoogleMfaAuthToken.Builder().issueDate(OffsetDateTime.now()).token(otp).build();
        profile.setGoogleMfaAuthTokens(List.of(token));
        return authProfileDAO.save(profile);
    }

    private AuthProfile createAuthProfileWithU2FDevice(final String owner, final String record) {
        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        U2FDevice device = new U2FDevice.Builder().issueDate(OffsetDateTime.now()).record(record).build();
        profile.setU2FRegisteredDevices(List.of(device));
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
