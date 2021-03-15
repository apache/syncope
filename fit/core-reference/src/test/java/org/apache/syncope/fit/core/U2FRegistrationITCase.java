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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.U2FDeviceQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class U2FRegistrationITCase extends AbstractITCase {

    private static U2FDevice createDeviceRegistration() {
        return new U2FDevice.Builder()
                .issueDate(new Date())
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
        u2FRegistrationService.delete(new U2FDeviceQuery.Builder().build());
    }

    @Test
    public void create() {
        assertDoesNotThrow(() -> u2FRegistrationService.create(
                UUID.randomUUID().toString(), createDeviceRegistration()));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        U2FDevice device = createDeviceRegistration();
        Response response = u2FRegistrationService.create(owner, device);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(u2FRegistrationService.read(key));

        List<U2FDevice> devices = u2FRegistrationService.search(
                new U2FDeviceQuery.Builder().owner(owner).expirationDate(
                        Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())).
                        build()).getResult();
        assertEquals(1, devices.size());

        u2FRegistrationService.delete(new U2FDeviceQuery.Builder().id(device.getId()).build());

        devices = u2FRegistrationService.search(new U2FDeviceQuery.Builder().build()).getResult();
        assertTrue(devices.isEmpty());
    }

    @Test
    public void delete() {
        U2FDevice device = createDeviceRegistration();
        Response response = u2FRegistrationService.create(UUID.randomUUID().toString(), device);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(u2FRegistrationService.read(key));

        u2FRegistrationService.delete(new U2FDeviceQuery.Builder().entityKey(key).build());
        assertNull(u2FRegistrationService.read(key));

        Date date = Date.from(LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        u2FRegistrationService.delete(new U2FDeviceQuery.Builder().expirationDate(date).build());

        assertTrue(u2FRegistrationService.search(
                new U2FDeviceQuery.Builder().expirationDate(date).build()).getResult().isEmpty());
    }

    @Test
    public void update() {
        U2FDevice device = createDeviceRegistration();
        Response response = u2FRegistrationService.create(UUID.randomUUID().toString(), device);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        device = u2FRegistrationService.read(key);
        assertNotNull(device);

        device.setRecord("newRecord");
        u2FRegistrationService.update(device);

        device = u2FRegistrationService.read(key);
        assertEquals("newRecord", device.getRecord());
    }
}
