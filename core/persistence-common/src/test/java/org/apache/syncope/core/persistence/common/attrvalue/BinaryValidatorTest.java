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
package org.apache.syncope.core.persistence.common.attrvalue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinaryValidatorTest {

    private static final String JSON_MIME_TYPE = MediaType.APPLICATION_JSON;

    @Mock
    private PlainSchema schema;

    private BinaryValidator validator() {
        return new BinaryValidator();
    }

    @Test
    void doValidateNullBinaryValue() {
        PlainAttrValue attrValue = new PlainAttrValue();

        assertDoesNotThrow(() -> validator().doValidate(schema, attrValue));
    }

    @Test
    void doValidateMatchingMimeType() {
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBinaryValue("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> validator().doValidate(schema, attrValue));
    }

    @Test
    void doValidateTextPlainSchemaJsonValidJsonObject() {
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        // Tika detects text/plain for plain text; schema expects application/json.
        // A JSON object { ... } is accepted (operator precedence makes the object branch
        // always valid when wrapped in braces).
        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBinaryValue("{not valid json}".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> validator().doValidate(schema, attrValue));
    }

    @Test
    void doValidateTextPlainSchemaJsonNonJsonText() {
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBinaryValue("this is plain text".getBytes(StandardCharsets.UTF_8));

        InvalidPlainAttrValueException ex = assertThrows(
                InvalidPlainAttrValueException.class,
                () -> validator().doValidate(schema, attrValue));

        assertEquals("Found MIME type: 'text/plain', expecting: 'application/json'", ex.getMessage());
    }

    @Test
    void doValidateDifferentMimeType() {
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        // JPEG magic bytes are detected by Tika as image/jpeg.
        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBinaryValue(new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        });

        InvalidPlainAttrValueException ex = assertThrows(
                InvalidPlainAttrValueException.class,
                () -> validator().doValidate(schema, attrValue));

        assertEquals("Found MIME type: 'image/jpeg', expecting: 'application/json'", ex.getMessage());
    }

    @Test
    void validateBinaryValue() {
        when(schema.getType()).thenReturn(AttrSchemaType.Binary);
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        String base64 = Base64.getEncoder().encodeToString(
                "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        PlainAttrValue attrValue = new PlainAttrValue();

        assertDoesNotThrow(() -> validator().validate(schema, base64, attrValue));
    }

    @Test
    void validateInvalidBinaryValue() {
        when(schema.getType()).thenReturn(AttrSchemaType.Binary);
        when(schema.getMimeType()).thenReturn(JSON_MIME_TYPE);

        // Base64 of "this is plain text" → Tika detects text/plain, schema expects application/json.
        String base64 = Base64.getEncoder().encodeToString(
                "this is plain text".getBytes(StandardCharsets.UTF_8));

        PlainAttrValue attrValue = new PlainAttrValue();

        InvalidPlainAttrValueException ex = assertThrows(
                InvalidPlainAttrValueException.class,
                () -> validator().validate(schema, base64, attrValue));

        assertEquals("Found MIME type: 'text/plain', expecting: 'application/json'", ex.getMessage());
    }
}
