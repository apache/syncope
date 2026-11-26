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
package org.apache.syncope.ext.scimv2.api.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.junit.jupiter.api.Test;

public class SCIMValuePathFilterTest {

    @Test
    public void eq() {
        SCIMValuePathFilter filter = SCIMValuePathFilter.parse("type EQ \"work\"");

        assertTrue(filter.matches(Map.of("type", "work")));
        assertFalse(filter.matches(Map.of("type", "home")));
        assertFalse(filter.matches(Map.of("value", "work")));
    }

    @Test
    public void attributeNamesAreCaseInsensitive() {
        assertTrue(SCIMValuePathFilter.matches("TYPE eq \"work\"", Map.of("type", "work")));
        assertTrue(SCIMValuePathFilter.matches("VaLuE Pr", Map.of("value", "work@example.com")));
    }

    @Test
    public void ne() {
        assertTrue(SCIMValuePathFilter.matches("type ne \"work\"", Map.of("type", "home")));
        assertFalse(SCIMValuePathFilter.matches("type ne \"work\"", Map.of("type", "work")));
    }

    @Test
    public void coSwEw() {
        Map<String, String> attributes = Map.of("value", "pocvictim2@example.com");

        assertTrue(SCIMValuePathFilter.matches("value co \"example\"", attributes));
        assertTrue(SCIMValuePathFilter.matches("value sw \"pocvictim2\"", attributes));
        assertTrue(SCIMValuePathFilter.matches("value ew \".com\"", attributes));
        assertFalse(SCIMValuePathFilter.matches("value sw \"example\"", attributes));
    }

    @Test
    public void present() {
        assertTrue(SCIMValuePathFilter.matches("type pr", Map.of("type", "work")));
        assertFalse(SCIMValuePathFilter.matches("type pr", Map.of("value", "work")));
        assertFalse(SCIMValuePathFilter.matches("type pr", Map.of("type", "")));
    }

    @Test
    public void andOrWithParentheses() {
        Map<String, String> attributes = Map.of("type", "work", "value", "work@example.com");

        assertTrue(SCIMValuePathFilter.matches(
                "type eq \"work\" and (value sw \"work\" or value pr)", attributes));
        assertFalse(SCIMValuePathFilter.matches(
                "type eq \"home\" or (value sw \"home\" and value pr)", attributes));
    }

    @Test
    public void escapedQuotes() {
        assertTrue(SCIMValuePathFilter.matches(
                "value eq \"a\\\"b\"", Map.of("value", "a\"b")));
    }

    @Test
    public void rejectJexlAndOtherExpressions() {
        String[] invalid = {
            "type eq \"work\" and ((n) -> { var i = 0; while (i < n) { i = i + 1 }; true })(20000000)",
            "type eq \"work\" and \"x\".repeat(300000000).length() > 0",
            "type eq \"work\" or new java.lang.ProcessBuilder(\"id\").start()",
            "while (true) {}",
            "type eq 'work'",
            "type gt \"work\"",
            "type eq work",
            "type eq",
            "eq \"work\"",
            "not (type pr)",
            "type pr and",
            "(type eq \"work\"",
            "type eq \"work\")",
        };

        for (String filter : invalid) {
            BadRequestException e = assertThrows(
                    BadRequestException.class, () -> SCIMValuePathFilter.parse(filter), filter);
            assertEquals(ErrorType.invalidFilter, e.getErrorType());
        }
    }

    @Test
    public void rejectOversizedInput() {
        assertThrows(BadRequestException.class,
                () -> SCIMValuePathFilter.parse("type eq \"" + "x".repeat(8192) + "\""));

        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            filter.append(i == 0 ? "" : " or ").append("type eq \"").append(i).append('"');
        }
        assertThrows(BadRequestException.class, () -> SCIMValuePathFilter.parse(filter.toString()));

        StringBuilder nested = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            nested.append('(');
        }
        nested.append("type pr");
        for (int i = 0; i < 32; i++) {
            nested.append(')');
        }
        assertThrows(BadRequestException.class, () -> SCIMValuePathFilter.parse(nested.toString()));
    }
}
