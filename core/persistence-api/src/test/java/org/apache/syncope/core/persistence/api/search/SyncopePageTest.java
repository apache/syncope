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
package org.apache.syncope.core.persistence.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class SyncopePageTest {

    @Test
    void getTotalElementsReturnsTotal() {
        final SyncopePage<String> page = new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L);

        assertEquals(42L, page.getTotalElements());
    }

    @Test
    void getTotalPagesReturnsOneWhenSizeIsZero() {
        final Pageable pageable = Mockito.mock(Pageable.class);
        final SyncopePage<String> page = new SyncopePage<>(List.of(), pageable, 42L);

        assertEquals(1, page.getTotalPages());
    }

    @Test
    void getTotalPagesComputesTotalPages() {
        final SyncopePage<String> page = new SyncopePage<>(List.of(), PageRequest.of(0, 20), 50L);

        assertEquals(3, page.getTotalPages());
    }

    @Test
    void equalsReturnsTrueForEqualPages() {
        final SyncopePage<String> page = new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L);

        assertEquals(page, new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L));
    }

    @Test
    void equalsReturnsFalseForDifferentPages() {
        final SyncopePage<String> page = new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L);

        assertNotEquals(page, new SyncopePage<>(List.of("b"), PageRequest.of(0, 10), 42L));
    }

    @Test
    void hashCodeReturnsSameValueForEqualPages() {
        final SyncopePage<String> page = new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L);

        assertEquals(page.hashCode(), new SyncopePage<>(List.of("a"), PageRequest.of(0, 10), 42L).hashCode());
    }
}
