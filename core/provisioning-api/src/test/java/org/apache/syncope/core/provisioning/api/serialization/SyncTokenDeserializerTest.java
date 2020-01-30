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
package org.apache.syncope.core.provisioning.api.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class SyncTokenDeserializerTest extends AbstractTest {

    @Mock
    private JsonParser jp;

    @Mock
    private DeserializationContext ct;

    @Mock
    private JsonNode node;

    @Mock
    private ObjectNode tree;

    private final SyncTokenDeserializer deserializer = new SyncTokenDeserializer();

    @BeforeEach
    public void initTest() throws IOException {
        when(jp.readValueAsTree()).thenReturn(tree);
        when(tree.has("value")).thenReturn(Boolean.TRUE);
        when(tree.get("value")).thenReturn(node);
    }

    @Test
    public void deserializeIsBoolean() throws IOException {
        Boolean value = Boolean.TRUE;
        when(node.isBoolean()).thenReturn(value);
        when(node.asBoolean()).thenReturn(value);
        assertEquals(value, deserializer.deserialize(jp, ct).getValue());
    }

    @Test
    public void deserializeIsDouble() throws IOException {
        Double value = 9000.1;
        when(node.isDouble()).thenReturn(Boolean.TRUE);
        when(node.asDouble()).thenReturn(value);
        assertEquals(value, deserializer.deserialize(jp, ct).getValue());
    }

    @Test
    public void deserializeIsLong() throws IOException {
        Long value = 9000L;
        when(node.isLong()).thenReturn(Boolean.TRUE);
        when(node.asLong()).thenReturn(value);
        assertEquals(value, deserializer.deserialize(jp, ct).getValue());
    }

    @Test
    public void deserializeIsInt() throws IOException {
        Integer value = 9000;
        when(node.isInt()).thenReturn(Boolean.TRUE);
        when(node.asInt()).thenReturn(value);
        assertEquals(value, deserializer.deserialize(jp, ct).getValue());
    }

    @Test
    public void deserializeIsString() throws IOException {
        String value = "testValue";
        when(node.asText()).thenReturn(value);
        assertEquals(value, deserializer.deserialize(jp, ct).getValue());

        value = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.ISO_8859_1));
        when(node.asText()).thenReturn(value);
        assertTrue(EqualsBuilder.reflectionEquals(Base64.getDecoder().decode(value),
                deserializer.deserialize(jp, ct).getValue()));
    }
}
