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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.Attr;
import org.junit.jupiter.api.Test;

class RealmTOTest {

    @Test
    void realm() {
        RealmTO realm = new RealmTO();
        realm.setKey("key");
        realm.setName("name");
        realm.setParent("/");
        realm.setFullPath("/name");
        realm.getAnyTypeClasses().add("anyTypeClass");
        realm.setAccountPolicy("account");
        realm.setPasswordPolicy("password");
        realm.setAuthPolicy("auth");
        realm.setAccessPolicy("access");
        realm.setAttrReleasePolicy("attrRelease");
        realm.setTicketExpirationPolicy("ticketExpiration");
        realm.getActions().add("action");
        realm.getResources().add("resource");
        realm.getTemplates().put("USER", new UserTO());
        realm.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        realm.getDerAttrs().add(new Attr.Builder("derived").value("value").build());

        assertEquals("key", realm.getKey());
        assertEquals("name", realm.getName());
        assertEquals("/", realm.getParent());
        assertEquals("/name", realm.getFullPath());
        assertEquals(1, realm.getAnyTypeClasses().size());
        assertEquals("account", realm.getAccountPolicy());
        assertEquals("password", realm.getPasswordPolicy());
        assertEquals("auth", realm.getAuthPolicy());
        assertEquals("access", realm.getAccessPolicy());
        assertEquals("attrRelease", realm.getAttrReleasePolicy());
        assertEquals("ticketExpiration", realm.getTicketExpirationPolicy());
        assertEquals(1, realm.getActions().size());
        assertEquals(1, realm.getResources().size());
        assertEquals(1, realm.getTemplates().size());
        assertTrue(realm.getPlainAttr("plain").isPresent());
        assertFalse(realm.getPlainAttr("other").isPresent());
        assertTrue(realm.getDerAttr("derived").isPresent());
        assertFalse(realm.getDerAttr("other").isPresent());
    }

    @Test
    void equalsAndHashCode() {
        RealmTO first = new RealmTO();
        first.setKey("key");
        first.setName("name");
        RealmTO second = new RealmTO();
        second.setKey("key");
        second.setName("name");
        RealmTO other = new RealmTO();
        other.setKey("key");
        other.setName("other");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }
}
