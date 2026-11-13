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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.provisioning.api.MappingManager.IntValues;
import org.apache.syncope.core.provisioning.api.data.JEXLItemTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class JEXLItemTransformerImplTest extends AbstractDataBinderTest {

    @Autowired
    private ApplicationContext applicationContext;

    private JEXLItemTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = applicationContext.getAutowireCapableBeanFactory().
                createBean(JEXLItemTransformerImpl.class);
    }

    protected PlainAttrValue stringValue(final String value) {
        PlainAttrValue plainAttrValue = new PlainAttrValue();
        plainAttrValue.setStringValue(value);
        return plainAttrValue;
    }

    @Test
    void beforePropagationString() {
        transformer.setPropagationJEXL("value + ' - transformed'");

        PlainAttrValue value = stringValue("hello");
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.String, new ArrayList<>(List.of(value)));

        assertEquals(AttrSchemaType.String, actual.attrSchemaType());
        assertEquals("hello - transformed", actual.values().getFirst().getStringValue());
        assertNull(actual.values().getFirst().getLongValue());
    }

    @Test
    void beforePropagationBoolean() {
        transformer.setPropagationJEXL("!value");

        PlainAttrValue value = new PlainAttrValue();
        value.setBooleanValue(true);
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.Boolean, new ArrayList<>(List.of(value)));

        assertEquals(AttrSchemaType.Boolean, actual.attrSchemaType());
        assertFalse(actual.values().getFirst().getBooleanValue());
        assertNull(actual.values().getFirst().getStringValue());
    }

    @Test
    void beforePropagationLongAndDouble() {
        transformer.setPropagationJEXL("value + 1");

        PlainAttrValue longValue = new PlainAttrValue();
        longValue.setLongValue(10L);
        IntValues longActual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.Long, new ArrayList<>(List.of(longValue)));
        assertEquals(AttrSchemaType.Long, longActual.attrSchemaType());
        assertEquals(11L, longActual.values().getFirst().getLongValue());

        transformer.setPropagationJEXL("value * 2");

        PlainAttrValue doubleValue = new PlainAttrValue();
        doubleValue.setDoubleValue(1.5D);
        IntValues doubleActual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.Double, new ArrayList<>(List.of(doubleValue)));
        assertEquals(AttrSchemaType.Double, doubleActual.attrSchemaType());
        assertEquals(3.0D, doubleActual.values().getFirst().getDoubleValue());
    }

    @Test
    void beforePropagationDate() {
        transformer.setPropagationJEXL("value.plusDays(1)");

        OffsetDateTime now = OffsetDateTime.now();
        PlainAttrValue value = new PlainAttrValue();
        value.setDateValue(now);
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.Date, new ArrayList<>(List.of(value)));

        assertEquals(AttrSchemaType.Date, actual.attrSchemaType());
        assertEquals(now.plusDays(1), actual.values().getFirst().getDateValue());
    }

    @Test
    void beforePropagationBinary() {
        transformer.setPropagationJEXL("value");

        byte[] binary = "binary".getBytes(StandardCharsets.UTF_8);
        PlainAttrValue value = new PlainAttrValue();
        value.setBinaryValue(binary);
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.Binary, new ArrayList<>(List.of(value)));

        assertEquals(AttrSchemaType.Binary, actual.attrSchemaType());
        assertArrayEquals(binary, actual.values().getFirst().getBinaryValue());
    }

    @Test
    void beforePropagationEmptyValues() {
        transformer.setPropagationJEXL("value + '!'");

        List<PlainAttrValue> values = new ArrayList<>();
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.String, values);

        assertEquals(AttrSchemaType.String, actual.attrSchemaType());
        assertEquals(1, actual.values().size());
        assertEquals("!", actual.values().getFirst().getStringValue());
        assertSame(values, actual.values());
    }

    @Test
    void beforePropagationBlankJEXL() {
        transformer.setPropagationJEXL("   ");

        PlainAttrValue value = stringValue("untouched");
        List<PlainAttrValue> values = new ArrayList<>(List.of(value));
        IntValues actual = transformer.beforePropagation(
                new Item(), null, AttrSchemaType.String, values);

        assertEquals(AttrSchemaType.String, actual.attrSchemaType());
        assertSame(values, actual.values());
        assertEquals("untouched", actual.values().getFirst().getStringValue());
    }

    @Test
    void beforePull() {
        transformer.setPullJEXL("value + ' pulled'");

        UserTO userTO = new UserTO();
        userTO.setUsername("rossini");
        userTO.getPlainAttrs().add(new Attr.Builder("firstname").value("Gioacchino").build());

        List<Object> actual = transformer.beforePull(
                new Item(), userTO, new ArrayList<>(List.of("a", "b")));

        assertEquals(List.of("a pulled", "b pulled"), actual);
    }

    @Test
    void beforePullBlankJEXL() {
        transformer.setPullJEXL(null);

        List<Object> values = new ArrayList<>(List.of("a", "b"));
        assertSame(values, transformer.beforePull(new Item(), new UserTO(), values));
        assertNull(transformer.beforePull(new Item(), new UserTO(), null));
        assertTrue(transformer.beforePull(new Item(), new UserTO(), List.of()).isEmpty());
    }
}
