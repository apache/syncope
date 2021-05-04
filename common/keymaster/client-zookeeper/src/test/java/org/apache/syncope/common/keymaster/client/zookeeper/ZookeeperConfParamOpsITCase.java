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
package org.apache.syncope.common.keymaster.client.zookeeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { ZookeeperKeymasterClientContext.class, ZookeeperTestContext.class })
public class ZookeeperConfParamOpsITCase {

    public static final String DOMAIN = "domain";

    @Autowired
    private ConfParamOps confParamOps;

    @Test
    public void list() {
        Map<String, Object> confParams = confParamOps.list(DOMAIN);
        assertNotNull(confParams);
        assertFalse(confParams.isEmpty());
    }

    @Test
    public void get() {
        String stringValue = confParamOps.get(DOMAIN, "password.cipher.algorithm", null, String.class);
        assertNotNull(stringValue);
        assertEquals("SSHA256", stringValue);

        Long longValue = confParamOps.get(DOMAIN, "jwt.lifetime.minutes", null, Long.class);
        assertNotNull(longValue);
        assertEquals(120L, longValue.longValue());

        Double doubleValue = confParamOps.get(DOMAIN, "double.value", null, Double.class);
        assertNotNull(doubleValue);
        assertEquals(0.5, doubleValue.doubleValue());

        Date dateValue = confParamOps.get(DOMAIN, "date.value", null, Date.class);
        assertNotNull(dateValue);
        assertEquals(new Date(1554982140000L), dateValue);

        Boolean booleanValue = confParamOps.get(DOMAIN, "return.password.value", null, Boolean.class);
        assertNotNull(booleanValue);
        assertEquals(false, booleanValue);

        List<String> stringValues =
                List.of(confParamOps.get(DOMAIN, "authentication.attributes", null, String[].class));
        assertNotNull(stringValues);
        List<String> actualStringValues = new ArrayList<>();
        actualStringValues.add("created");
        actualStringValues.add("active");
        assertEquals(actualStringValues, stringValues);
    }

    @Test
    public void setGetRemove() {
        String key = UUID.randomUUID().toString();

        String stringValue = "stringValue";
        confParamOps.set(DOMAIN, key, stringValue);
        String actualStringValue = confParamOps.get(DOMAIN, key, null, String.class);
        assertEquals(stringValue, actualStringValue);

        Long longValue = 1L;
        confParamOps.set(DOMAIN, key, longValue);
        Long actualLongValue = confParamOps.get(DOMAIN, key, null, Long.class);
        assertEquals(longValue, actualLongValue);

        Double doubleValue = 2.0;
        confParamOps.set(DOMAIN, key, doubleValue);
        Double actualDoubleValue = confParamOps.get(DOMAIN, key, null, Double.class);
        assertEquals(doubleValue, actualDoubleValue);

        Date dateValue = new Date();
        confParamOps.set(DOMAIN, key, dateValue);
        Date actualDateValue = confParamOps.get(DOMAIN, key, null, Date.class);
        assertEquals(dateValue, actualDateValue);

        Boolean booleanValue = true;
        confParamOps.set(DOMAIN, key, booleanValue);
        Boolean actualBooleanValue = confParamOps.get(DOMAIN, key, null, Boolean.class);
        assertEquals(booleanValue, actualBooleanValue);

        List<String> stringValues = new ArrayList<>();
        stringValues.add("stringValue1");
        stringValues.add("stringValue2");
        confParamOps.set(DOMAIN, key, stringValues);
        List<String> actualStringValues = List.of(confParamOps.get(DOMAIN, key, null, String[].class));
        assertEquals(stringValues, actualStringValues);

        confParamOps.remove(DOMAIN, key);
        assertNull(confParamOps.get(DOMAIN, key, null, String.class));
        assertEquals("defaultValue", confParamOps.get(DOMAIN, key, "defaultValue", String.class));
    }
}
