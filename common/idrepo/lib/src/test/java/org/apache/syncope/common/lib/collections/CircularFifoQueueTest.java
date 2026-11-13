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
package org.apache.syncope.common.lib.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

class CircularFifoQueueTest {

    @Test
    void defaultConstructor() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>();

        assertEquals(32, queue.maxSize());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertFalse(queue.isAtFullCapacity());
        assertFalse(CircularFifoQueue.isFull());
    }

    @Test
    void invalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new CircularFifoQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularFifoQueue<>(-1));
    }

    @Test
    void fromCollection() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(List.of("a", "b", "c"));

        assertEquals(3, queue.maxSize());
        assertEquals(3, queue.size());
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(queue));
        assertEquals("a", queue.peek());
        assertEquals("a", queue.get(0));
        assertEquals("c", queue.get(2));
    }

    @Test
    void addAndOffer() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);

        assertTrue(queue.add("a"));
        assertTrue(queue.offer("b"));
        assertTrue(queue.add("c"));

        assertEquals(3, queue.size());
        assertTrue(queue.isAtFullCapacity());
        assertEquals("a", queue.element());
        assertThrows(NullPointerException.class, () -> queue.add(null));
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }

    @Test
    void getOutOfRange() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(2);
        queue.add("a");

        assertThrows(NoSuchElementException.class, () -> queue.get(-1));
        assertThrows(NoSuchElementException.class, () -> queue.get(1));
    }

    @Test
    void overwriteOldest() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.addAll(List.of("a", "b", "c"));
        queue.add("d");

        assertEquals(List.of("b", "c", "d"), new ArrayList<>(queue));
        assertEquals("b", queue.get(0));
        assertEquals("d", queue.get(2));
        assertTrue(queue.isAtFullCapacity());
    }

    @Test
    void pollRemovePeekOnEmpty() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(2);

        assertNull(queue.poll());
        assertNull(queue.peek());
        assertThrows(NoSuchElementException.class, () -> queue.remove());
        assertThrows(NoSuchElementException.class, () -> queue.element());
    }

    @Test
    void pollAndRemove() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.addAll(List.of("a", "b", "c"));

        assertEquals("a", queue.poll());
        assertEquals("b", queue.remove());
        assertEquals("c", queue.peek());
        assertEquals(1, queue.size());
    }

    @Test
    void clear() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(2);
        queue.addAll(List.of("a", "b"));

        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertFalse(queue.isAtFullCapacity());
    }

    @Test
    void iteratorOnEmpty() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(2);
        Iterator<String> iterator = queue.iterator();

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    void iteratorRemoveFirst() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(5);
        queue.addAll(List.of("a", "b", "c", "d"));

        Iterator<String> iterator = queue.iterator();
        assertEquals("a", iterator.next());
        iterator.remove();
        assertEquals("b", iterator.next());
        iterator.remove();
        assertEquals("c", iterator.next());
        assertEquals("d", iterator.next());
        assertFalse(iterator.hasNext());

        assertEquals(List.of("c", "d"), new ArrayList<>(queue));
    }

    @Test
    void iteratorRemoveMiddleAndLast() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(5);
        queue.addAll(List.of("a", "b", "c", "d"));

        Iterator<String> iterator = queue.iterator();
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        iterator.remove();
        assertEquals("c", iterator.next());
        assertEquals("d", iterator.next());
        iterator.remove();
        assertFalse(iterator.hasNext());

        assertEquals(List.of("a", "c"), new ArrayList<>(queue));
    }

    @Test
    void iteratorRemoveWrapped() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(5);
        queue.addAll(List.of("a", "b", "c", "d", "e", "f"));

        Iterator<String> iterator = queue.iterator();
        assertEquals("b", iterator.next());
        assertEquals("c", iterator.next());
        iterator.remove();
        assertEquals("d", iterator.next());
        assertEquals("e", iterator.next());
        assertEquals("f", iterator.next());
        assertFalse(iterator.hasNext());

        assertEquals(List.of("b", "d", "e", "f"), new ArrayList<>(queue));
    }

    @Test
    void iteratorRemoveTwice() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.add("a");

        Iterator<String> iterator = queue.iterator();
        assertEquals("a", iterator.next());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    void serialization() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(5);
        queue.addAll(List.of("a", "b", "c"));

        CircularFifoQueue<String> cloned = SerializationUtils.clone(queue);

        assertEquals(5, cloned.maxSize());
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(cloned));
        assertEquals("a", cloned.peek());
    }

    @Test
    void serializationWhenFull() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.addAll(List.of("a", "b", "c"));

        CircularFifoQueue<String> cloned = SerializationUtils.clone(queue);

        assertEquals(3, cloned.maxSize());
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(cloned));
        assertTrue(cloned.isAtFullCapacity());
        assertEquals("a", cloned.poll());
        assertEquals(List.of("b", "c"), new ArrayList<>(cloned));
    }
}
