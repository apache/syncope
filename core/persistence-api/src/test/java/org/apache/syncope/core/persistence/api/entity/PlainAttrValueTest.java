/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Base64;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.ParsingValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class PlainAttrValueTest {

    @Test
    void getStringValueReturnsEmptyStringWhenNoValueIsSet() {
        assertEquals("", new PlainAttrValue().getStringValue());
    }

    @Test
    void setBinaryValueReplacesBinaryValue() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setBinaryValue(new byte[] { 1, 2, 3 });

        assertArrayEquals(new byte[] { 1, 2, 3 }, value.getBinaryValue());
    }

    @Test
    void parseValueByBooleanType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Boolean);

        value.parseValue(schema, "true");

        assertEquals(Boolean.TRUE, value.getBooleanValue());
    }

    @Test
    void parseValueByLongType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Long);
        when(schema.getConversionPattern()).thenReturn(null);

        value.parseValue(schema, "42");

        assertEquals(42L, value.getLongValue());
    }

    @Test
    void parseValueByLongTypeWithInvalidValue() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Long);
        when(schema.getConversionPattern()).thenReturn(null);

        assertThrows(ParsingValidationException.class, () -> value.parseValue(schema, "not-a-number"));
        assertNull(value.getLongValue());
    }

    @Test
    void parseValueByDoubleType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Double);
        when(schema.getConversionPattern()).thenReturn(null);

        value.parseValue(schema, "3.14");

        assertEquals(3.14, value.getDoubleValue());
    }

    @Test
    void parseValueByDateType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Date);
        when(schema.getConversionPattern()).thenReturn(null);

        value.parseValue(schema, "2024-01-15T10:00:00Z");

        assertNotNull(value.getDateValue());
    }

    @Test
    void parseValueByBinaryType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Binary);

        value.parseValue(schema, "AQID");

        assertArrayEquals(new byte[] { 1, 2, 3 }, value.getBinaryValue());
    }

    @Test
    void parseValueByStringType() {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.String);

        value.parseValue(schema, "hello");

        assertEquals("hello", value.getStringValue());
    }

    @Test
    void parseValueByEncryptedType() throws Exception {
        final PlainAttrValue value = new PlainAttrValue();
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Encrypted);
        when(schema.getSecretKey()).thenReturn("secret-key");
        when(schema.getCipherAlgorithm()).thenReturn(CipherAlgorithm.AES);

        try (MockedStatic<ApplicationContextProvider> mocked = mockStatic(ApplicationContextProvider.class)) {
            final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
            final EncryptorManager encryptorManager = mock(EncryptorManager.class);
            final Encryptor encryptor = mock(Encryptor.class);
            mocked.when(() -> ApplicationContextProvider.getApplicationContext()).thenReturn(context);
            when(context.getBean(EncryptorManager.class)).thenReturn(encryptorManager);
            when(encryptorManager.getInstance("secret-key")).thenReturn(encryptor);
            doReturn("encoded").when(encryptor).encode(eq("value"), any(CipherAlgorithm.class));

            value.parseValue(schema, "value");

            assertEquals("encoded", value.getStringValue());
        }
    }

    @Test
    void getGenericValueReturnsTypedValue() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setBooleanValue(Boolean.TRUE);

        assertEquals(Boolean.TRUE, value.getValue());
    }

    @Test
    void getValueAsStringByBooleanType() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setBooleanValue(Boolean.TRUE);
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Boolean);

        assertEquals("true", value.getValueAsString(schema));
    }

    @Test
    void getValueAsStringByLongType() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setLongValue(42L);
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Long);
        when(schema.getConversionPattern()).thenReturn(null);

        assertEquals("42", value.getValueAsString(schema));
    }

    @Test
    void getValueAsStringByBinaryType() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setBinaryValue(new byte[] { 1, 2, 3 });
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Binary);

        final String expected = Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 });
        assertEquals(expected, value.getValueAsString(schema));
    }

    @Test
    void getValueAsStringByEncryptedType() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("secret-value");
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Encrypted);

        assertEquals("secret-value", value.getValueAsString(schema));
    }

    @Test
    void getValueAsStringFallsBackToGetValueWhenTypeValueIsNull() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("fallback");
        final PlainSchema schema = mock(PlainSchema.class);
        when(schema.getType()).thenReturn(AttrSchemaType.Boolean);

        assertEquals("fallback", value.getValueAsString(schema));
    }

    @Test
    void toStringContainsDetails() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("hello");

        assertTrue(value.toString().contains("PlainAttrValue"));
    }
}
