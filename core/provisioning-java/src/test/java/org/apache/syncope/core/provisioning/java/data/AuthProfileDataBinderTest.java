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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthProfileDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private AuthProfileDataBinder binder;

    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Test
    void createGetUpdate() {
        AuthProfileTO authProfileTO = new AuthProfileTO();
        authProfileTO.setOwner("rossini");
        authProfileTO.getImpersonationAccounts().add(
                new ImpersonationAccount.Builder().impersonated("verdi").build());
        authProfileTO.getGoogleMfaAuthTokens().add(
                new GoogleMfaAuthToken.Builder().token(123456).issueDate(LocalDateTime.now()).build());
        authProfileTO.getGoogleMfaAuthAccounts().add(new GoogleMfaAuthAccount.Builder().
                id(1L).name("account").username("rossini").secretKey("secret").
                validationCode(1).source("test").registrationDate(ZonedDateTime.now()).build());

        MfaTrustedDevice device = new MfaTrustedDevice();
        device.setId(1L);
        device.setName("device");
        device.setDeviceFingerprint("fingerprint");
        device.setRecordKey("recordKey");
        device.setRecordDate(ZonedDateTime.now());
        device.setExpirationDate(ZonedDateTime.now().plusDays(1));
        authProfileTO.getMfaTrustedDevices().add(device);

        authProfileTO.getWebAuthnDeviceCredentials().add(
                new WebAuthnDeviceCredential.Builder().identifier("identifier").json("{}").build());
        authProfileTO.getConsentDecisions().add(
                new WAConsentDecision.Builder(1L, "rossini", "service", LocalDateTime.now()).
                        reminder(10L).reminderTimeUnit(ChronoUnit.DAYS).attributes("{}").build());

        AuthProfile authProfile = authProfileDAO.save(binder.create(authProfileTO));
        entityManager.flush();

        AuthProfileTO actual = binder.getAuthProfileTO(
                authProfileDAO.findById(authProfile.getKey()).orElseThrow());
        assertNotNull(actual.getKey());
        assertEquals("rossini", actual.getOwner());
        assertEquals("verdi", actual.getImpersonationAccounts().getFirst().getImpersonated());
        assertEquals(123456, actual.getGoogleMfaAuthTokens().getFirst().getOtp());
        assertEquals("rossini", actual.getGoogleMfaAuthAccounts().getFirst().getUsername());
        assertEquals("device", actual.getMfaTrustedDevices().getFirst().getName());
        assertEquals("identifier", actual.getWebAuthnDeviceCredentials().getFirst().getIdentifier());
        assertEquals("service", actual.getConsentDecisions().getFirst().getService());

        // clear everything
        binder.update(authProfile, new AuthProfileTO());
        authProfileDAO.save(authProfile);
        entityManager.flush();

        AuthProfileTO cleared = binder.getAuthProfileTO(
                authProfileDAO.findById(authProfile.getKey()).orElseThrow());
        assertEquals("rossini", cleared.getOwner());
        assertTrue(cleared.getImpersonationAccounts().isEmpty());
        assertTrue(cleared.getGoogleMfaAuthTokens().isEmpty());
        assertTrue(cleared.getGoogleMfaAuthAccounts().isEmpty());
        assertTrue(cleared.getMfaTrustedDevices().isEmpty());
        assertTrue(cleared.getWebAuthnDeviceCredentials().isEmpty());
        assertTrue(cleared.getConsentDecisions().isEmpty());
    }
}
