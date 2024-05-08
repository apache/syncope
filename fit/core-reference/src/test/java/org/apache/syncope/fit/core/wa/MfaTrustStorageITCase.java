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
package org.apache.syncope.fit.core.wa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.rest.api.beans.MfaTrustedDeviceQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MfaTrustStorageITCase extends AbstractITCase {

    private static MfaTrustedDevice createDeviceRegistration() {
        MfaTrustedDevice device = new MfaTrustedDevice();
        device.setId(System.currentTimeMillis());
        device.setDeviceFingerprint(UUID.randomUUID().toString());
        device.setName(UUID.randomUUID().toString());
        device.setRecordKey(UUID.randomUUID().toString());
        device.setRecordDate(ZonedDateTime.now());
        device.setExpirationDate(ZonedDateTime.now().plusDays(30));
        return device;
    }

    @BeforeEach
    public void setup() {
        MFA_TRUST_STORAGE_SERVICE.delete(new MfaTrustedDeviceQuery.Builder().build());
    }

    @Test
    public void create() {
        assertDoesNotThrow(() -> MFA_TRUST_STORAGE_SERVICE.create(
                UUID.randomUUID().toString(), createDeviceRegistration()));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        MfaTrustedDevice device = createDeviceRegistration();
        MFA_TRUST_STORAGE_SERVICE.create(owner, device);

        List<MfaTrustedDevice> devices = MFA_TRUST_STORAGE_SERVICE.search(new MfaTrustedDeviceQuery.Builder().
                principal(owner).build()).getResult();
        assertEquals(1, devices.size());

        MFA_TRUST_STORAGE_SERVICE.delete(new MfaTrustedDeviceQuery.Builder().recordKey(device.getRecordKey()).build());

        devices = MFA_TRUST_STORAGE_SERVICE.search(new MfaTrustedDeviceQuery.Builder().build()).getResult();
        assertTrue(devices.isEmpty());
    }

    @Test
    public void delete() {
        MfaTrustedDevice device = createDeviceRegistration();
        String owner = UUID.randomUUID().toString();
        MFA_TRUST_STORAGE_SERVICE.create(owner, device);

        MFA_TRUST_STORAGE_SERVICE.delete(new MfaTrustedDeviceQuery.Builder().recordKey(device.getRecordKey()).build());
        assertTrue(MFA_TRUST_STORAGE_SERVICE.search(
                new MfaTrustedDeviceQuery.Builder().id(device.getId()).build()).getResult().isEmpty());

        OffsetDateTime date = OffsetDateTime.now().plusDays(1);

        MFA_TRUST_STORAGE_SERVICE.delete(new MfaTrustedDeviceQuery.Builder().expirationDate(date).build());

        assertTrue(MFA_TRUST_STORAGE_SERVICE.search(
                new MfaTrustedDeviceQuery.Builder().id(device.getId()).build()).getResult().isEmpty());
    }
}
