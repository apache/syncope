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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.WAConfigTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WAConfigITCase extends AbstractITCase {
    private static <T extends Serializable> WAConfigTO<T> runTest(final T initialValue, final T updatedValue) {
        WAConfigTO<T> configTO = new WAConfigTO.Builder()
            .key(UUID.randomUUID().toString())
            .value(initialValue)
            .build();
        Response response = waConfigService.create(configTO);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);

        assertFalse(waConfigService.list().isEmpty());

        configTO = waConfigService.read(key);
        assertNotNull(configTO);

        configTO.setValue(updatedValue);
        waConfigService.update(configTO);

        WAConfigTO<T> updatedTO = waConfigService.read(key);
        if (updatedTO.getValue() instanceof Collection) {
            ((Collection) updatedTO.getValue()).stream().allMatch(((Collection) updatedValue)::contains);
        } else if (updatedTO.getValue() instanceof Map) {
            ((Map) updatedTO.getValue()).
                entrySet().
                stream().
                allMatch(entry -> ((Map) updatedValue).get(((Map.Entry) entry).getKey()).equals(((Map.Entry) entry).getValue()));
        } else {
            assertEquals(updatedValue.toString(), String.valueOf(updatedTO.getValue()));
        }
        return updatedTO;
    }

    private static <T extends Serializable> void deleteEntry(final WAConfigTO<T> configTO) {
        waConfigService.delete(configTO.getKey());
        assertThrows(SyncopeClientException.class, () -> waConfigService.read(configTO.getKey()));
    }

    @Test
    public void verify() {
        deleteEntry(runTest("v1,v2,v3", "newValue"));
        deleteEntry(runTest(123456, 98765));
        deleteEntry(runTest(123.45, 987.65));
        deleteEntry(runTest(new ArrayList<>(Arrays.asList(1, 2, 3, 4)), new ArrayList<>(Arrays.asList(9, 8, 7, 6, 5))));
        deleteEntry(runTest(new TreeSet<>(Arrays.asList(3, 4, 5)), new TreeSet<>(Arrays.asList(6, 7, 8))));
        deleteEntry(runTest(new TreeMap<>(Map.of("key1", 12.1F)), new TreeMap<>(Map.of("key3", 22.5F))));
    }
}
