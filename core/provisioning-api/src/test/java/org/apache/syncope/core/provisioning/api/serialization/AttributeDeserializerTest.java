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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class AttributeDeserializerTest extends AbstractTest {

    @Mock
    private JsonParser jp;

    @Mock
    private DeserializationContext ct;

    @Mock
    private JsonNode node;

    @Mock
    private JsonNode node2;

    @Mock
    private ObjectNode tree;

    private final AttributeDeserializer deserializer = new AttributeDeserializer();

    private String name;

    @BeforeEach
    public void initTest() throws IOException {
        name = Name.NAME;
        when(jp.readValueAsTree()).thenReturn(tree);
        when(tree.get("name")).thenReturn(node2);
        when(tree.get("value")).thenReturn(node);
        when(node.iterator()).thenReturn(List.of(node).iterator());
    }

    @Test
    public void deserializeIsNull() throws IOException {
        when(node2.asText()).thenReturn(name);
        when(node.isNull()).thenReturn(Boolean.TRUE);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(Collections.singletonList(null), attr.getValue());
    }

    @Test
    public void deserializeIsBoolean() throws IOException {
        when(node2.asText()).thenReturn(name);
        when(node.isBoolean()).thenReturn(Boolean.TRUE);
        when(node.asBoolean()).thenReturn(Boolean.TRUE);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(List.of(Boolean.TRUE.toString()).getFirst(), attr.getValue().getFirst());
    }

    @Test
    public void deserializeIsDouble() throws IOException {
        Double number = 9000.1;
        name = "__TEST__";
        when(node2.asText()).thenReturn(name);
        when(node.isDouble()).thenReturn(Boolean.TRUE);
        when(node.asDouble()).thenReturn(number);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(List.of(number).getFirst(), attr.getValue().getFirst());
    }

    @Test
    public void deserializeIsLong() throws IOException {
        Long number = 9000L;
        name = "__UID__";
        when(node2.asText()).thenReturn(name);
        when(node.isLong()).thenReturn(Boolean.TRUE);
        when(node.asLong()).thenReturn(number);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(List.of(number.toString()).getFirst(), attr.getValue().getFirst());
    }

    @Test
    public void deserializeIsInt() throws IOException {
        Integer number = 9000;
        when(node2.asText()).thenReturn(name);
        when(node.isInt()).thenReturn(Boolean.TRUE);
        when(node.asInt()).thenReturn(number);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(attr.getName(), name);
        assertEquals(List.of(number.toString()).getFirst(), attr.getValue().getFirst());
    }

    @Test
    public void deserializeIsText() throws IOException {
        String text = "<binary>test";
        when(node2.asText()).thenReturn(name);
        when(node.asText()).thenReturn(text);
        Attribute attr = deserializer.deserialize(jp, ct);
        assertEquals(attr.getName(), name);
        assertEquals(List.of(text).getFirst(), attr.getValue().getFirst());
    }
}
