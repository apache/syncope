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
package org.apache.syncope.common.lib.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

class SyncopeJsonMapperTest {

    private final SyncopeJsonMapper mapper = new SyncopeJsonMapper();

    @Test
    void unwrapSingleEntryMap() {
        assertEquals("value", mapper.unwrapMap(Map.of("key", "value")));
    }

    @Test
    void unwrapMultiEntryMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");

        assertSame(map, mapper.unwrapMap(map));
    }

    @Test
    void unwrapNonMap() {
        assertEquals("value", mapper.unwrapMap("value"));
        assertNull(mapper.unwrapMap(null));
    }

    @Test
    void writeValueAsStringUnwrapsSingleEntryMap() {
        assertEquals("\"value\"", mapper.writeValueAsString(Map.of("key", "value")));
    }

    @Test
    void writeValueAsStringKeepsMultiEntryMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");

        String json = mapper.writeValueAsString(map);

        assertEquals("{\"one\":\"1\",\"two\":\"2\"}", json);
    }

    @Test
    void roundTrip() {
        UserTO user = new UserTO();
        user.setKey("key");
        user.setUsername("rossini");

        String json = mapper.writeValueAsString(user);

        UserTO actual = mapper.readValue(json, UserTO.class);
        assertEquals(user, actual);
    }
}
