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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.OIDCClientImplementationType;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;
import org.junit.jupiter.api.Test;

class JPAOIDCC4UIProviderTest {

    @Test
    void scopes() {
        JPAOIDCC4UIProvider provider = new JPAOIDCC4UIProvider();
        assertTrue(provider.getScopes().isEmpty());

        provider.setScopes(List.of("openid", "email"));
        assertEquals(List.of("openid", "email"), provider.getScopes());
    }

    @Test
    void connObjectKeyItem() {
        JPAOIDCC4UIProvider provider = new JPAOIDCC4UIProvider();
        Item item = new Item();
        item.setIntAttrName("username");
        item.setExtAttrName("email");

        provider.setConnObjectKeyItem(item);

        assertTrue(item.isConnObjectKey());
        assertTrue(provider.getItems().contains(item));
        assertSame(item, provider.getConnObjectKeyItem().orElseThrow());
    }

    @Test
    void actionsMustBeOfExpectedType() {
        JPAOIDCC4UIProvider provider = new JPAOIDCC4UIProvider();

        JPAImplementation action = new JPAImplementation();
        action.setKey("action");
        action.setType(OIDCClientImplementationType.OP_ACTIONS);

        assertTrue(provider.add(action));
        assertTrue(provider.add(action));
        assertEquals(List.of(action), provider.getActions());

        JPAImplementation wrongType = new JPAImplementation();
        wrongType.setKey("wrong");
        wrongType.setType("OTHER");
        assertThrows(ClassCastException.class, () -> provider.add(wrongType));
    }

    @Test
    void userTemplateMustBeJPAImplementation() {
        JPAOIDCC4UIProvider provider = new JPAOIDCC4UIProvider();
        assertThrows(ClassCastException.class, () -> provider.setUserTemplate(mock(OIDCC4UIUserTemplate.class)));
    }
}
