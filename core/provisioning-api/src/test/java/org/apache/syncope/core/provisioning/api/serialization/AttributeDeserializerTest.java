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
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
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

    private Attribute attr;

    @BeforeEach
    public void initTest() throws IOException {
        name = "__NAME__";
        when(jp.readValueAsTree()).thenReturn(tree);
        when(tree.get("name")).thenReturn(node2);
        when(tree.get("value")).thenReturn(node);
        when(node.iterator()).thenReturn(Collections.singletonList(node).iterator());
    }

    @Test
    public void deserializeIsNull() throws IOException {
        when(node2.asText()).thenReturn(name);
        when(node.isNull()).thenReturn(Boolean.TRUE);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(Collections.singletonList(null), attr.getValue());
    }

    @Test
    public void deserializeIsBoolean() throws IOException {
        when(node2.asText()).thenReturn(name);
        when(node.isBoolean()).thenReturn(Boolean.TRUE);
        when(node.asBoolean()).thenReturn(Boolean.TRUE);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(Collections.singletonList(Boolean.TRUE.toString()).get(0), attr.getValue().get(0));
    }

    @Test
    public void deserializeIsDouble() throws IOException {
        Double number = 9000.1;
        name = "__TEST__";
        when(node2.asText()).thenReturn(name);
        when(node.isDouble()).thenReturn(Boolean.TRUE);
        when(node.asDouble()).thenReturn(number);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(Collections.singletonList(number).get(0), attr.getValue().get(0));
    }

    @Test
    public void deserializeIsLong() throws IOException {
        Long number = 9000L;
        name = "__UID__";
        when(node2.asText()).thenReturn(name);
        when(node.isLong()).thenReturn(Boolean.TRUE);
        when(node.asLong()).thenReturn(number);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(name, attr.getName());
        assertEquals(Collections.singletonList(number.toString()).get(0), attr.getValue().get(0));
    }

    @Test
    public void deserializeIsInt() throws IOException {
        Integer number = 9000;
        when(node2.asText()).thenReturn(name);
        when(node.isInt()).thenReturn(Boolean.TRUE);
        when(node.asInt()).thenReturn(number);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(attr.getName(), name);
        assertEquals(Collections.singletonList(number.toString()).get(0), attr.getValue().get(0));
    }

    @Test
    public void deserializeIsText() throws IOException {
        String text = "<binary>test";
        when(node2.asText()).thenReturn(name);
        when(node.asText()).thenReturn(text);
        attr = deserializer.deserialize(jp, ct);
        assertEquals(attr.getName(), name);
        assertEquals(Collections.singletonList(text).get(0), attr.getValue().get(0));
    }
}
