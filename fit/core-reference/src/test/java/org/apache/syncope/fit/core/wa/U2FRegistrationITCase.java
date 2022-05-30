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
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.rest.api.beans.U2FDeviceQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class U2FRegistrationITCase extends AbstractITCase {

    private static U2FDevice createDeviceRegistration() {
        return new U2FDevice.Builder()
                .issueDate(OffsetDateTime.now())
                .id(System.currentTimeMillis())
                .record("{\"keyHandle\":\"2_QYgDSPYcOgYBGBe8c9PVCunjigbD-3o5HcliXhu-Up_GKckYMxxVF6AgSPWubqfWy8WmJNDYQE"
                        + "J1QKZe343Q\","
                        + "\"publicKey\":\"BMj46cH-lHkRMovZhrusmm_fYL_sFausDPJIDZfx4pIiRqRNtasd4vU3yJyrTXXbdxyD36GZLx1"
                        + "WKLHGmApv7Nk\""
                        + ",\"counter\":-1,\"compromised\":false}")
                .build();
    }

    @BeforeEach
    public void setup() {
        u2fRegistrationService.delete(new U2FDeviceQuery.Builder().build());
    }

    @Test
    public void create() {
        assertDoesNotThrow(() -> u2fRegistrationService.create(
                UUID.randomUUID().toString(), createDeviceRegistration()));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        U2FDevice device = createDeviceRegistration();
        u2fRegistrationService.create(owner, device);

        List<U2FDevice> devices = u2fRegistrationService.search(new U2FDeviceQuery.Builder().
                owner(owner).
                expirationDate(OffsetDateTime.now().minusDays(1)).
                build()).getResult();
        assertEquals(1, devices.size());

        u2fRegistrationService.delete(new U2FDeviceQuery.Builder().id(device.getId()).build());

        devices = u2fRegistrationService.search(new U2FDeviceQuery.Builder().build()).getResult();
        assertTrue(devices.isEmpty());
    }

    @Test
    public void delete() {
        U2FDevice device = createDeviceRegistration();
        String owner = UUID.randomUUID().toString();
        u2fRegistrationService.create(owner, device);

        u2fRegistrationService.delete(new U2FDeviceQuery.Builder().owner(owner).build());
        assertTrue(u2fRegistrationService.search(
                new U2FDeviceQuery.Builder().owner(owner).build()).getResult().isEmpty());

        OffsetDateTime date = OffsetDateTime.now().plusDays(1);

        u2fRegistrationService.delete(new U2FDeviceQuery.Builder().expirationDate(date).build());

        assertTrue(u2fRegistrationService.search(
                new U2FDeviceQuery.Builder().expirationDate(date).build()).getResult().isEmpty());
    }
}
