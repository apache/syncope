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
package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.junit.jupiter.api.Test;

class MappingUtilsTest {

    private static Item item(final MappingPurpose purpose) {
        Item item = new Item();
        item.setPurpose(purpose);
        return item;
    }

    @Test
    void getPropagationItems() {
        Stream<Item> items = Stream.of(
                item(MappingPurpose.PROPAGATION),
                item(MappingPurpose.BOTH),
                item(MappingPurpose.PULL),
                item(MappingPurpose.NONE));

        List<Item> result = MappingUtils.getPropagationItems(items).toList();

        assertEquals(2, result.size());
        assertEquals(MappingPurpose.PROPAGATION, result.getFirst().getPurpose());
        assertEquals(MappingPurpose.BOTH, result.get(1).getPurpose());
    }

    @Test
    void getInboundItems() {
        Stream<Item> items = Stream.of(
                item(MappingPurpose.PULL),
                item(MappingPurpose.BOTH),
                item(MappingPurpose.PROPAGATION),
                item(MappingPurpose.NONE));

        List<Item> result = MappingUtils.getInboundItems(items).toList();

        assertEquals(2, result.size());
        assertEquals(MappingPurpose.PULL, result.getFirst().getPurpose());
        assertEquals(MappingPurpose.BOTH, result.get(1).getPurpose());
    }

    @Test
    void getPropagationItemsEmpty() {
        assertEquals(0, MappingUtils.getPropagationItems(Stream.of()).count());
    }

    @Test
    void getInboundItemsEmpty() {
        assertEquals(0, MappingUtils.getInboundItems(Stream.of()).count());
    }
}
