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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlainAttrTest {

    private PlainAttr plainAttr;

    private PlainSchema plainSchema;

    @BeforeEach
    void setUp() {
        plainAttr = new PlainAttr();
        plainSchema = org.mockito.Mockito.mock(PlainSchema.class);
        org.mockito.Mockito.when(plainSchema.getKey()).thenReturn("schema");
    }

    @Test
    void setPlainSchemaSetsSchema() {
        plainAttr.setPlainSchema(plainSchema);

        assertEquals("schema", plainAttr.getSchema());
    }

    @Test
    void addAddsValueToValues() {
        final PlainAttrValue value = new PlainAttrValue();

        plainAttr.add(value);

        assertEquals(1, plainAttr.getValues().size());
    }

    @Test
    void getValuesAsStringsWithoutUniqueValue() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("stored");
        plainAttr.add(value);

        final List<String> result = plainAttr.getValuesAsStrings();

        assertEquals(List.of("stored"), result);
    }

    @Test
    void getValuesAsStringsWithUniqueValue() {
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("stored");
        plainAttr.setUniqueValue(value);

        final List<String> result = plainAttr.getValuesAsStrings();

        assertEquals(List.of("stored"), result);
    }

    @Test
    void isValidReturnsFalseWhenMembershipAndRelationshipAreSet() {
        plainAttr.setMembership("membership");
        plainAttr.setRelationship("relationship");

        assertFalse(plainAttr.isValid());
    }

    @Test
    void isValidReturnsFalseWhenNoValueIsSet() {
        plainAttr.setPlainSchema(plainSchema);

        assertFalse(plainAttr.isValid());
    }

    @Test
    void isValidReturnsTrueWhenValueIsSet() {
        plainAttr.setPlainSchema(plainSchema);
        final PlainAttrValue value = new PlainAttrValue();
        value.setStringValue("stored");
        plainAttr.add(value);

        assertTrue(plainAttr.isValid());
    }

    @Test
    void toStringContainsDetails() {
        plainAttr.setSchema("schema");

        assertNotNull(plainAttr.toString());
    }
}
