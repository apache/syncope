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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpEventTest {

    @Test
    void custom() {
        OpEvent aln = new OpEvent(
                OpEvent.CategoryType.CUSTOM, null, null, "MY_EVENT", OpEvent.Outcome.SUCCESS);
        assertEquals("[CUSTOM]:[]:[]:[MY_EVENT]:[SUCCESS]", aln.toString());

        assertEquals(aln, OpEvent.fromString(aln.toString()));
    }

    @Test
    void defaults() {
        OpEvent event = new OpEvent(null, null, null, null, null);

        assertEquals(OpEvent.CategoryType.CUSTOM, event.getType());
        assertEquals(OpEvent.Outcome.SUCCESS, event.getOutcome());
        assertEquals("[CUSTOM]:[]:[]:[]:[SUCCESS]", event.toString());
    }

    @Test
    void fullEvent() {
        OpEvent event = new OpEvent(
                OpEvent.CategoryType.PROPAGATION, "category", "subcategory", "op", OpEvent.Outcome.FAILURE);

        assertEquals(OpEvent.CategoryType.PROPAGATION, event.getType());
        assertEquals("category", event.getCategory());
        assertEquals("subcategory", event.getSubcategory());
        assertEquals("op", event.getOp());
        assertEquals(OpEvent.Outcome.FAILURE, event.getOutcome());
        assertEquals("[PROPAGATION]:[category]:[subcategory]:[op]:[FAILURE]", event.toString());

        assertEquals(event, OpEvent.fromString(event.toString()));
        assertEquals(event.hashCode(), OpEvent.fromString(event.toString()).hashCode());
    }

    @Test
    void staticToString() {
        assertEquals("[TASK]:[]:[]:[]:[]",
                OpEvent.toString(OpEvent.CategoryType.TASK, null, "", " ", null));
        assertEquals("[]:[category]:[subcategory]:[op]:[SUCCESS]",
                OpEvent.toString(null, "category", "subcategory", "op", OpEvent.Outcome.SUCCESS));
    }

    @Test
    void fromSingleElement() {
        OpEvent event = OpEvent.fromString("[something]");

        assertEquals(OpEvent.CategoryType.CUSTOM, event.getType());
        assertEquals(OpEvent.Outcome.SUCCESS, event.getOutcome());
        assertEquals("[something]", event.getOp());
    }

    @Test
    void fromPartial() {
        OpEvent event = OpEvent.fromString("[LOGIC]:[category]:[subcategory]");

        assertEquals(OpEvent.CategoryType.LOGIC, event.getType());
        assertEquals("category", event.getCategory());
        assertEquals("subcategory", event.getSubcategory());
        assertEquals(OpEvent.Outcome.SUCCESS, event.getOutcome());
        assertEquals("[LOGIC]:[category]:[subcategory]:[]:[SUCCESS]", event.toString());
    }

    @Test
    void fromTrailingEmptyElements() {
        OpEvent event = OpEvent.fromString("[LOGIC]:[]:[]");

        assertEquals(OpEvent.CategoryType.CUSTOM, event.getType());
        assertEquals("[LOGIC]:[]:[]", event.getOp());
    }

    @Test
    void fromBlank() {
        assertThrows(IllegalArgumentException.class, () -> OpEvent.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> OpEvent.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> OpEvent.fromString("   "));
    }

    @Test
    void equalsAndHashCode() {
        OpEvent event = new OpEvent(OpEvent.CategoryType.WA, "category", null, "op", OpEvent.Outcome.SUCCESS);

        assertTrue(event.equals(event));
        assertFalse(event.equals(null));
        assertFalse(event.equals("string"));
        assertNotEquals(event,
                new OpEvent(OpEvent.CategoryType.WA, "other", null, "op", OpEvent.Outcome.SUCCESS));
        assertEquals(event,
                new OpEvent(OpEvent.CategoryType.WA, "category", null, "op", OpEvent.Outcome.SUCCESS));
    }
}
