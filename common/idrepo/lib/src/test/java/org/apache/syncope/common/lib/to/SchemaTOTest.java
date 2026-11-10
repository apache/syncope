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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.junit.jupiter.api.Test;

class SchemaTOTest {

    @Test
    void plainSchema() {
        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setKey("key");
        schema.setAnyTypeClass("anyTypeClass");
        schema.setType(AttrSchemaType.Long);
        schema.setMandatoryCondition("true");
        schema.setMultivalue(true);
        schema.setUniqueConstraint(true);
        schema.setReadonly(true);
        schema.setConversionPattern("pattern");
        schema.setValidator("validator");
        schema.setDropdownValueProvider("provider");
        schema.setSecretKey("secret");
        schema.setCipherAlgorithm(CipherAlgorithm.AES);
        schema.setMimeType("text/plain");
        schema.getEnumValues().put("one", "1");
        schema.getLabels().put(Locale.ENGLISH, "English label");

        assertEquals("key", schema.getKey());
        assertEquals("anyTypeClass", schema.getAnyTypeClass());
        assertEquals(AttrSchemaType.Long, schema.getType());
        assertEquals("true", schema.getMandatoryCondition());
        assertTrue(schema.isMultivalue());
        assertTrue(schema.isUniqueConstraint());
        assertTrue(schema.isReadonly());
        assertEquals("pattern", schema.getConversionPattern());
        assertEquals("validator", schema.getValidator());
        assertEquals("provider", schema.getDropdownValueProvider());
        assertEquals("secret", schema.getSecretKey());
        assertEquals(CipherAlgorithm.AES, schema.getCipherAlgorithm());
        assertEquals("text/plain", schema.getMimeType());
        assertEquals(1, schema.getEnumValues().size());
        assertEquals("English label", schema.getLabel(Locale.ENGLISH));
        assertEquals("key", schema.getLabel(Locale.ITALIAN));
        assertEquals(PlainSchemaTO.class.getName(), schema.getDiscriminator());
    }

    @Test
    void plainSchemaDefaults() {
        PlainSchemaTO schema = new PlainSchemaTO();

        assertEquals(AttrSchemaType.String, schema.getType());
        assertEquals("false", schema.getMandatoryCondition());
        assertFalse(schema.isMultivalue());
        assertFalse(schema.isUniqueConstraint());
        assertFalse(schema.isReadonly());
        assertTrue(schema.getEnumValues().isEmpty());
        assertTrue(schema.getLabels().isEmpty());
    }

    @Test
    void derSchema() {
        DerSchemaTO schema = new DerSchemaTO();
        schema.setKey("key");
        schema.setExpression("expression");

        assertEquals("key", schema.getKey());
        assertEquals("expression", schema.getExpression());
        assertEquals(DerSchemaTO.class.getName(), schema.getDiscriminator());
    }

    @Test
    void equalsAndHashCode() {
        PlainSchemaTO first = new PlainSchemaTO();
        first.setKey("key");
        PlainSchemaTO second = new PlainSchemaTO();
        second.setKey("key");
        PlainSchemaTO other = new PlainSchemaTO();
        other.setKey("other");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);

        DerSchemaTO derFirst = new DerSchemaTO();
        derFirst.setKey("key");
        derFirst.setExpression("expression");
        DerSchemaTO derSecond = new DerSchemaTO();
        derSecond.setKey("key");
        derSecond.setExpression("expression");
        DerSchemaTO derOther = new DerSchemaTO();
        derOther.setKey("key");
        derOther.setExpression("other");

        assertEquals(derFirst, derSecond);
        assertEquals(derFirst.hashCode(), derSecond.hashCode());
        assertNotEquals(derFirst, derOther);

        // different concrete types cannot be equal even with same base fields
        PlainSchemaTO plain = new PlainSchemaTO();
        plain.setKey("key");
        assertNotEquals(plain, derFirst);
    }
}
