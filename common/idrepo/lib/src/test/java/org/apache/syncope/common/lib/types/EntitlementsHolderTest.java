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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntitlementsHolderTest {

    @Test
    void singleton() {
        assertSame(EntitlementsHolder.getInstance(), EntitlementsHolder.getInstance());
    }

    @Test
    void addAndRemoveFor() {
        String anyType = "__entitlements_holder_test__";

        Set<String> added = EntitlementsHolder.getInstance().addFor(anyType);
        assertEquals(AnyEntitlement.values().length, added.size());
        for (AnyEntitlement entitlement : AnyEntitlement.values()) {
            assertTrue(added.contains(entitlement.getFor(anyType)));
            assertTrue(EntitlementsHolder.getInstance().getValues().contains(entitlement.getFor(anyType)));
        }

        Set<String> removed = EntitlementsHolder.getInstance().removeFor(anyType);
        assertEquals(added, removed);
        for (AnyEntitlement entitlement : AnyEntitlement.values()) {
            assertFalse(EntitlementsHolder.getInstance().getValues().contains(entitlement.getFor(anyType)));
        }
    }

    @Test
    void addAll() {
        Set<String> values = Set.of("__entitlements_holder_test_1__", "__entitlements_holder_test_2__");

        EntitlementsHolder.getInstance().addAll(values);

        assertTrue(EntitlementsHolder.getInstance().getValues().containsAll(values));
    }

    @Test
    void valuesAreUnmodifiable() {
        Set<String> snapshot = new HashSet<>(EntitlementsHolder.getInstance().getValues());

        Set<String> values = EntitlementsHolder.getInstance().getValues();
        assertThrows(UnsupportedOperationException.class, () -> values.add("__should_not_be_added__"));
        assertEquals(snapshot, EntitlementsHolder.getInstance().getValues());
    }
}
