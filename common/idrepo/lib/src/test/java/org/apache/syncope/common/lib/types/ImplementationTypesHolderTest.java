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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ImplementationTypesHolderTest {

    @Test
    void singleton() {
        assertSame(ImplementationTypesHolder.getInstance(), ImplementationTypesHolder.getInstance());
    }

    @Test
    void putAll() {
        Map<String, String> values = Map.of(
                "__implementation_types_holder_test_1__", "first",
                "__implementation_types_holder_test_2__", "second");

        ImplementationTypesHolder.getInstance().putAll(values);

        assertTrue(ImplementationTypesHolder.getInstance().getValues().entrySet().containsAll(values.entrySet()));
    }

    @Test
    void valuesAreUnmodifiable() {
        Map<String, String> values = ImplementationTypesHolder.getInstance().getValues();

        assertThrows(UnsupportedOperationException.class, () -> values.put("a", "b"));
    }
}
