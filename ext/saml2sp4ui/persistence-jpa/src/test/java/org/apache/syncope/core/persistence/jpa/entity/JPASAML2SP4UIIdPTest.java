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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SP4UIImplementationType;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.junit.jupiter.api.Test;

class JPASAML2SP4UIIdPTest {

    @Test
    void metadataRoundTrip() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();
        assertNull(idp.getMetadata());

        byte[] metadata = "metadata".getBytes(StandardCharsets.UTF_8);
        idp.setMetadata(metadata);
        assertArrayEquals(metadata, idp.getMetadata());

        idp.setMetadata(null);
        assertNull(idp.getMetadata());
    }

    @Test
    void connObjectKeyItem() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();
        Item item = new Item();
        item.setIntAttrName("username");
        item.setExtAttrName("NameID");

        idp.setConnObjectKeyItem(item);

        assertTrue(item.isConnObjectKey());
        assertTrue(idp.getItems().contains(item));
        assertSame(item, idp.getConnObjectKeyItem().orElseThrow());
    }

    @Test
    void actionsMustBeOfExpectedType() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();

        JPAImplementation action = new JPAImplementation();
        action.setKey("action");
        action.setType(SAML2SP4UIImplementationType.IDP_ACTIONS);

        assertTrue(idp.add(action));
        assertTrue(idp.add(action));
        assertEquals(List.of(action), idp.getActions());

        JPAImplementation wrongType = new JPAImplementation();
        wrongType.setKey("wrong");
        wrongType.setType("OTHER");
        assertThrows(ClassCastException.class, () -> idp.add(wrongType));
    }

    @Test
    void requestedAuthnContextProviderMustBeOfExpectedType() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();

        JPAImplementation provider = new JPAImplementation();
        provider.setKey("provider");
        provider.setType(SAML2SP4UIImplementationType.REQUESTED_AUTHN_CONTEXT_PROVIDER);

        idp.setRequestedAuthnContextProvider(provider);
        assertSame(provider, idp.getRequestedAuthnContextProvider());

        JPAImplementation wrongType = new JPAImplementation();
        wrongType.setKey("wrong");
        wrongType.setType("OTHER");
        assertThrows(ClassCastException.class, () -> idp.setRequestedAuthnContextProvider(wrongType));
    }

    @Test
    void userTemplateMustBeJPAImplementation() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();
        assertThrows(ClassCastException.class, () -> idp.setUserTemplate(mock(SAML2SP4UIUserTemplate.class)));
    }

    @Test
    void bindingType() {
        JPASAML2SP4UIIdP idp = new JPASAML2SP4UIIdP();

        idp.setBindingType(SAML2BindingType.POST);
        assertEquals(SAML2BindingType.POST, idp.getBindingType());
    }
}
