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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.types.MappingPurpose;
import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void item() {
        Item item = new Item();
        item.setIntAttrName("username");
        item.setExtAttrName("cn");
        item.setConnObjectKey(true);
        item.setPassword(true);
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.PROPAGATION);
        item.setPropagationJEXLTransformer("propagation");
        item.setPullJEXLTransformer("pull");
        item.getTransformers().add("transformer");

        assertEquals("username", item.getIntAttrName());
        assertEquals("cn", item.getExtAttrName());
        assertTrue(item.isConnObjectKey());
        assertTrue(item.isPassword());
        assertEquals("true", item.getMandatoryCondition());
        assertEquals(MappingPurpose.PROPAGATION, item.getPurpose());
        assertEquals("propagation", item.getPropagationJEXLTransformer());
        assertEquals("pull", item.getPullJEXLTransformer());
        assertEquals(1, item.getTransformers().size());
        assertTrue(item.toString().contains("username"));
    }

    @Test
    void itemDefaults() {
        Item item = new Item();

        assertFalse(item.isConnObjectKey());
        assertFalse(item.isPassword());
        assertEquals("false", item.getMandatoryCondition());
        assertTrue(item.getTransformers().isEmpty());
    }

    @Test
    void equalsAndHashCode() {
        Item first = new Item();
        first.setIntAttrName("username");
        first.setExtAttrName("cn");
        Item second = new Item();
        second.setIntAttrName("username");
        second.setExtAttrName("cn");
        Item other = new Item();
        other.setIntAttrName("username");
        other.setExtAttrName("uid");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void itemContainer() {
        TestItemContainer container = new TestItemContainer();

        Item key = new Item();
        key.setIntAttrName("key");

        assertFalse(container.getConnObjectKeyItem().isPresent());
        assertTrue(container.setConnObjectKeyItem(key));
        assertTrue(key.isConnObjectKey());
        assertEquals("true", key.getMandatoryCondition());
        assertSame(key, container.getConnObjectKeyItem().orElseThrow());

        assertTrue(container.add(key));
        assertEquals(1, container.getItems().size());
        assertFalse(container.add(null));

        assertTrue(container.setConnObjectKeyItem(null));
        assertFalse(container.getConnObjectKeyItem().isPresent());
        assertEquals(0, container.getItems().size());
    }

    @Test
    void itemContainerEqualsAndHashCode() {
        TestItemContainer first = new TestItemContainer();
        Item item = new Item();
        item.setIntAttrName("username");
        first.add(item);

        TestItemContainer second = new TestItemContainer();
        Item sameItem = new Item();
        sameItem.setIntAttrName("username");
        second.add(sameItem);

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static class TestItemContainer extends ItemContainer {

        private static final long serialVersionUID = 1L;
    }
}
