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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.NetworkService;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class KeymasterITCase extends AbstractITCase {

    @Test
    public void confParamList() {
        Map<String, Object> confParams = confParamOps.list(SyncopeConstants.MASTER_DOMAIN);
        assertNotNull(confParams);
        assertFalse(confParams.isEmpty());
    }

    @Test
    public void confParamGet() {
        String stringValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", null, String.class);
        assertNotNull(stringValue);
        assertEquals("SHA1", stringValue);

        Long longValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "jwt.lifetime.minutes", null, Long.class);
        assertNotNull(longValue);
        assertEquals(120L, longValue.longValue());

        Boolean booleanValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "return.password.value", null, Boolean.class);
        assertNotNull(booleanValue);
        assertEquals(false, booleanValue);

        List<String> stringValues =
                Arrays.asList(confParamOps.get(
                        SyncopeConstants.MASTER_DOMAIN, "authentication.attributes", null, String[].class));
        assertNotNull(stringValues);
        List<String> actualStringValues = new ArrayList<>();
        actualStringValues.add("username");
        actualStringValues.add("userId");
        assertEquals(actualStringValues, stringValues);
    }

    @Test
    public void confParamSetGetRemove() {
        String key = UUID.randomUUID().toString();

        String stringValue = "stringValue";
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, stringValue);
        String actualStringValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String.class);
        assertEquals(stringValue, actualStringValue);

        Long longValue = 1L;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, longValue);
        Long actualLongValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Long.class);
        assertEquals(longValue, actualLongValue);

        Double doubleValue = 2.0;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, doubleValue);
        Double actualDoubleValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Double.class);
        assertEquals(doubleValue, actualDoubleValue);

        Date dateValue = new Date();
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, dateValue);
        Date actualDateValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Date.class);
        assertEquals(dateValue, actualDateValue);

        Boolean booleanValue = true;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, booleanValue);
        Boolean actualBooleanValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Boolean.class);
        assertEquals(booleanValue, actualBooleanValue);

        List<String> stringValues = new ArrayList<>();
        stringValues.add("stringValue1");
        stringValues.add("stringValue2");
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, stringValues);
        List<String> actualStringValues =
                Arrays.asList(confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String[].class));
        assertEquals(stringValues, actualStringValues);

        confParamOps.remove(SyncopeConstants.MASTER_DOMAIN, key);
        assertNull(confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String.class));
        assertEquals(
                "defaultValue",
                confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, "defaultValue", String.class));
    }

    @Test
    public void serviceList() {
        List<NetworkService> services = serviceOps.list(NetworkService.Type.CORE);
        assertFalse(services.isEmpty());
        assertEquals(1, services.size());

        services = serviceOps.list(NetworkService.Type.SRA);
        assertTrue(services.isEmpty());

        services = serviceOps.list(NetworkService.Type.WA);
        assertTrue(services.isEmpty());
    }

    private List<NetworkService> findNetworkServices(
            final NetworkService.Type type,
            final Function<List<NetworkService>, Boolean> check,
            final int maxWaitSeconds) {

        int i = 0;
        int maxit = maxWaitSeconds;

        List<NetworkService> list = Collections.emptyList();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            list = serviceOps.list(type);

            i++;
        } while (check.apply(list) && i < maxit);
        if (check.apply(list)) {
            fail("Timeout when looking for network services of type " + type);
        }

        return list;
    }

    @Test
    public void serviceRun() {
        List<NetworkService> list = serviceOps.list(NetworkService.Type.SRA);
        assertTrue(list.isEmpty());

        NetworkService sra1 = new NetworkService();
        sra1.setType(NetworkService.Type.SRA);
        sra1.setAddress("http://localhost:9080/syncope-sra");
        serviceOps.register(sra1);

        list = findNetworkServices(NetworkService.Type.SRA, List::isEmpty, 30);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(sra1, list.get(0));

        assertEquals(sra1, serviceOps.get(NetworkService.Type.SRA));

        NetworkService sra2 = new NetworkService();
        sra2.setType(NetworkService.Type.SRA);
        sra2.setAddress("http://localhost:9080/syncope-sra");
        assertEquals(sra1, sra2);
        serviceOps.register(sra2);

        list = findNetworkServices(NetworkService.Type.SRA, List::isEmpty, 30);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(sra1, list.get(0));

        assertEquals(sra1, serviceOps.get(NetworkService.Type.SRA));

        serviceOps.unregister(sra1);
        list = findNetworkServices(NetworkService.Type.SRA, l -> !l.isEmpty(), 30);
        assertTrue(list.isEmpty());

        try {
            serviceOps.get(NetworkService.Type.SRA);
            fail();
        } catch (KeymasterException e) {
            assertNotNull(e);
        }
    }
}
