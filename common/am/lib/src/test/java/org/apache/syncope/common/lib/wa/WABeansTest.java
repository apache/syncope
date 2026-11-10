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
package org.apache.syncope.common.lib.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class WABeansTest {

    @Test
    void consentDecision() {
        LocalDateTime createdDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        WAConsentDecision decision = new WAConsentDecision.Builder(1L, "principal", "service", createdDate).
                options(WAConsentDecision.ReminderOptions.ATTRIBUTE_VALUE).
                reminder(2L).
                reminderTimeUnit(ChronoUnit.HOURS).
                attributes("attributes").
                build();

        assertEquals(1L, decision.getId());
        assertEquals("principal", decision.getPrincipal());
        assertEquals("service", decision.getService());
        assertEquals(createdDate, decision.getCreatedDate());
        assertEquals(WAConsentDecision.ReminderOptions.ATTRIBUTE_VALUE, decision.getOptions());
        assertEquals(2L, decision.getReminder());
        assertEquals(ChronoUnit.HOURS, decision.getReminderTimeUnit());
        assertEquals("attributes", decision.getAttributes());
        assertTrue(decision.toString().contains("principal"));

        WAConsentDecision same = new WAConsentDecision.Builder(1L, "principal", "service", createdDate).
                options(WAConsentDecision.ReminderOptions.ATTRIBUTE_VALUE).
                reminder(2L).
                reminderTimeUnit(ChronoUnit.HOURS).
                attributes("attributes").
                build();
        WAConsentDecision other = new WAConsentDecision.Builder(2L, "principal", "service", createdDate).build();

        assertEquals(decision, decision);
        assertNotEquals(decision, null);
        assertNotEquals(decision, "string");
        assertEquals(decision, same);
        assertEquals(decision.hashCode(), same.hashCode());
        assertNotEquals(decision, other);
    }

    @Test
    void reminderOptions() {
        assertEquals(0, WAConsentDecision.ReminderOptions.ALWAYS.getValue());
        assertEquals(1, WAConsentDecision.ReminderOptions.ATTRIBUTE_NAME.getValue());
        assertEquals(2, WAConsentDecision.ReminderOptions.ATTRIBUTE_VALUE.getValue());
    }

    @Test
    void webAuthnAccount() {
        WebAuthnDeviceCredential first = new WebAuthnDeviceCredential.Builder().
                identifier("first").json("{}").build();
        WebAuthnDeviceCredential second = new WebAuthnDeviceCredential.Builder().
                identifier("second").json("{}").build();

        WebAuthnAccount account = new WebAuthnAccount.Builder().
                credential(first).
                credentials(second).
                credentials(List.of(first)).
                build();

        assertEquals(3, account.getCredentials().size());
        assertTrue(account.toString().contains("credentials"));

        WebAuthnAccount same = new WebAuthnAccount.Builder().
                credential(first).
                credentials(second).
                credentials(List.of(first)).
                build();

        assertEquals(account, account);
        assertNotEquals(account, null);
        assertNotEquals(account, "string");
        assertNotEquals(account, same);
    }

    @Test
    void webAuthnDeviceCredential() {
        WebAuthnDeviceCredential credential = new WebAuthnDeviceCredential.Builder().
                identifier("identifier").json("json").build();

        assertEquals("identifier", credential.getIdentifier());
        assertEquals("json", credential.getJson());
        assertTrue(credential.toString().contains("identifier"));

        WebAuthnDeviceCredential same = new WebAuthnDeviceCredential.Builder().
                identifier("identifier").json("json").build();
        WebAuthnDeviceCredential other = new WebAuthnDeviceCredential.Builder().
                identifier("identifier").json("other").build();

        assertEquals(credential, credential);
        assertNotEquals(credential, null);
        assertNotEquals(credential, "string");
        assertEquals(credential, same);
        assertEquals(credential.hashCode(), same.hashCode());
        assertNotEquals(credential, other);
    }

    @Test
    void googleMfaAuthToken() {
        LocalDateTime issueDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        GoogleMfaAuthToken token = new GoogleMfaAuthToken.Builder().issueDate(issueDate).token(123456).build();

        assertEquals(123456, token.getOtp());
        assertEquals(issueDate, token.getIssueDate());
        assertTrue(token.toString().contains("issueDate"));

        GoogleMfaAuthToken same = new GoogleMfaAuthToken.Builder().issueDate(issueDate).token(123456).build();
        GoogleMfaAuthToken other = new GoogleMfaAuthToken.Builder().issueDate(issueDate).token(654321).build();

        assertEquals(token, token);
        assertNotEquals(token, null);
        assertNotEquals(token, "string");
        assertEquals(token, same);
        assertEquals(token.hashCode(), same.hashCode());
        assertNotEquals(token, other);
    }

    @Test
    void googleMfaAuthAccount() {
        ZonedDateTime registrationDate = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount.Builder().
                id(1L).
                name("name").
                username("username").
                secretKey("secret").
                validationCode(123).
                scratchCodes(List.of(1, 2, 3)).
                registrationDate(registrationDate).
                source("source").
                build();

        assertEquals(1L, account.getId());
        assertEquals("name", account.getName());
        assertEquals("username", account.getUsername());
        assertEquals("secret", account.getSecretKey());
        assertEquals(123, account.getValidationCode());
        assertEquals(3, account.getScratchCodes().size());
        assertEquals(registrationDate, account.getRegistrationDate());
        assertEquals("source", account.getSource());
        assertTrue(account.toString().contains("username"));

        GoogleMfaAuthAccount same = new GoogleMfaAuthAccount.Builder().
                id(1L).
                name("name").
                username("username").
                secretKey("secret").
                validationCode(123).
                scratchCodes(List.of(1, 2, 3)).
                registrationDate(registrationDate).
                source("source").
                build();
        GoogleMfaAuthAccount other = new GoogleMfaAuthAccount.Builder().
                id(2L).
                name("name").
                username("username").
                secretKey("secret").
                validationCode(123).
                scratchCodes(List.of(1, 2, 3)).
                registrationDate(registrationDate).
                source("source").
                build();

        assertEquals(account, account);
        assertNotEquals(account, null);
        assertNotEquals(account, "string");
        assertEquals(account, same);
        assertEquals(account.hashCode(), same.hashCode());
        assertNotEquals(account, other);
    }

    @Test
    void impersonationAccount() {
        ImpersonationAccount account = new ImpersonationAccount.Builder().impersonated("impersonated").build();

        assertEquals("impersonated", account.getImpersonated());
        assertTrue(account.toString().contains("impersonated"));

        ImpersonationAccount same = new ImpersonationAccount.Builder().impersonated("impersonated").build();
        ImpersonationAccount other = new ImpersonationAccount.Builder().impersonated("other").build();

        assertEquals(account, account);
        assertNotEquals(account, null);
        assertNotEquals(account, "string");
        assertEquals(account, same);
        assertEquals(account.hashCode(), same.hashCode());
        assertNotEquals(account, other);
    }

    @Test
    void mfaTrustedDevice() {
        ZonedDateTime recordDate = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime expirationDate = recordDate.plusDays(1);
        MfaTrustedDevice device = new MfaTrustedDevice();
        device.setId(1L);
        device.setName("name");
        device.setDeviceFingerprint("fingerprint");
        device.setRecordKey("key");
        device.setRecordDate(recordDate);
        device.setExpirationDate(expirationDate);

        assertEquals(1L, device.getId());
        assertEquals("name", device.getName());
        assertEquals("fingerprint", device.getDeviceFingerprint());
        assertEquals("key", device.getRecordKey());
        assertEquals(recordDate, device.getRecordDate());
        assertEquals(expirationDate, device.getExpirationDate());
        assertTrue(device.toString().contains("fingerprint"));

        MfaTrustedDevice same = new MfaTrustedDevice();
        same.setId(1L);
        same.setName("name");
        same.setDeviceFingerprint("fingerprint");
        same.setRecordKey("key");
        same.setRecordDate(recordDate);
        same.setExpirationDate(expirationDate);

        MfaTrustedDevice other = new MfaTrustedDevice();
        other.setId(2L);
        other.setName("name");
        other.setDeviceFingerprint("fingerprint");
        other.setRecordKey("key");
        other.setRecordDate(recordDate);
        other.setExpirationDate(expirationDate);

        assertEquals(device, device);
        assertNotEquals(device, null);
        assertNotEquals(device, "string");
        assertEquals(device, same);
        assertEquals(device.hashCode(), same.hashCode());
        assertNotEquals(device, other);
        assertTrue(device.hashCode() != other.hashCode());
    }
}
