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
package org.apache.syncope.common.lib.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FormTest {

    @Test
    void formProperty() {
        FormProperty property = new FormProperty();
        property.setId("id");
        property.setName("name");
        property.setType(FormPropertyType.String);
        property.setReadable(true);
        property.setWritable(true);
        property.setRequired(true);
        property.setStringRegEx(Pattern.compile("[a-z]+"));
        property.setDatePattern("yyyy-MM-dd");
        property.setDropdownSingleSelection(false);
        property.setDropdownFreeForm(true);
        property.setMimeType("text/plain");
        property.setValue("value");
        property.getEnumValues().add(new FormPropertyValue("one", "1"));
        property.getDropdownValues().add(new FormPropertyValue("two", "2"));

        assertEquals("id", property.getId());
        assertEquals("name", property.getName());
        assertEquals(FormPropertyType.String, property.getType());
        assertTrue(property.isReadable());
        assertTrue(property.isWritable());
        assertTrue(property.isRequired());
        assertEquals("[a-z]+", property.getStringRegEx().pattern());
        assertEquals("yyyy-MM-dd", property.getDatePattern());
        assertFalse(property.isDropdownSingleSelection());
        assertTrue(property.isDropdownFreeForm());
        assertEquals("text/plain", property.getMimeType());
        assertEquals("value", property.getValue());
        assertEquals(1, property.getEnumValues().size());
        assertEquals(1, property.getDropdownValues().size());
    }

    @Test
    void formPropertyDefaults() {
        FormProperty property = new FormProperty();

        assertFalse(property.isReadable());
        assertFalse(property.isWritable());
        assertFalse(property.isRequired());
        assertTrue(property.isDropdownSingleSelection());
        assertFalse(property.isDropdownFreeForm());
        assertTrue(property.getEnumValues().isEmpty());
        assertTrue(property.getDropdownValues().isEmpty());
    }

    @Test
    void formPropertyEqualsAndHashCode() {
        FormProperty first = new FormProperty();
        first.setId("id");
        FormProperty second = new FormProperty();
        second.setId("id");
        FormProperty other = new FormProperty();
        other.setId("other");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void formPropertyValue() {
        FormPropertyValue value = new FormPropertyValue("key", "value");
        FormPropertyValue same = new FormPropertyValue("key", "value");
        FormPropertyValue other = new FormPropertyValue("key", "other");

        assertEquals("key", value.getKey());
        assertEquals("value", value.getValue());
        assertEquals(value, value);
        assertNotEquals(value, null);
        assertNotEquals(value, "string");
        assertEquals(value, same);
        assertEquals(value.hashCode(), same.hashCode());
        assertNotEquals(value, other);
    }

    @Test
    void syncopeForm() {
        SyncopeForm form = new SyncopeForm();
        FormProperty property = new FormProperty();
        property.setId("id");
        form.getProperties().add(property);

        assertEquals(1, form.getProperties().size());
        assertTrue(form.getProperty("id").isPresent());
        assertFalse(form.getProperty("other").isPresent());
        assertEquals(property, form.getProperty("id").orElseThrow());
    }

    @Test
    void syncopeFormEqualsAndHashCode() {
        SyncopeForm first = new SyncopeForm();
        FormProperty property = new FormProperty();
        property.setId("id");
        first.getProperties().add(property);

        SyncopeForm second = new SyncopeForm();
        FormProperty sameProperty = new FormProperty();
        sameProperty.setId("id");
        second.getProperties().add(sameProperty);

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new SyncopeForm());
    }
}
